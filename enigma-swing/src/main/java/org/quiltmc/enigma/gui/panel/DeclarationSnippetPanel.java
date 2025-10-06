package org.quiltmc.enigma.gui.panel;

import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaToken;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.class_handle.ClassHandle;
import org.quiltmc.enigma.api.source.DecompiledClassSource;
import org.quiltmc.enigma.api.source.Token;
import org.quiltmc.enigma.api.translation.representation.entry.ClassDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.highlight.BoxHighlightPainter;
import org.quiltmc.enigma.util.LineIndexer;
import org.quiltmc.enigma.util.Result;
import org.quiltmc.syntaxpain.LineNumbersRuler;
import org.tinylog.Logger;

import javax.swing.JViewport;
import java.awt.Color;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Comparator.comparingInt;

public class DeclarationSnippetPanel extends BaseEditorPanel {
	private static final String NO_ENTRY_DEFINITION = "no entry definition!";
	private static final String NO_TOKEN_RANGE = "no token range!";
	// used to compose error messages
	private static final String METHOD = "method";
	private static final String LAMBDA = "lambda";

	public DeclarationSnippetPanel(Gui gui, Entry<?> target, ClassHandle targetTopClassHandle) {
		super(gui);

		Optional.ofNullable(this.editorScrollPane.getRowHeader())
				.map(JViewport::getView)
				// LineNumbersRuler is installed by syntaxpain
				.map(view -> view instanceof LineNumbersRuler lineNumbers ? lineNumbers : null)
				// TODO offset line numbers instead of removing them once
				//  offsets are implemented in syntaxpain
				.ifPresent(lineNumbers -> lineNumbers.deinstall(this.editor));

		this.addSourceSetListener(source -> {
			final Token unBoundedToken = this.navigateToTokenImpl(source.getIndex().getDeclarationToken(target));
			if (unBoundedToken == null) {
				// the source isn't very useful if it couldn't be trimmed and the declaration couldn't be navigated to
				// set this text so it doesn't waste space or cause confusion
				this.editor.setText("// Unable to locate declaration");
				this.editor.getHighlighter().removeAllHighlights();
			} else {
				this.addHighlight(unBoundedToken, BoxHighlightPainter.create(
						new Color(0, 0, 0, 0),
						Config.getCurrentSyntaxPaneColors().selectionHighlight.value()
				));
			}
		});

		this.getEditor().setEditable(false);
		this.setClassHandle(targetTopClassHandle, false, source -> this.createSnippet(source, target));

		this.editor.setCaretColor(new Color(0, 0, 0, 0));
		this.editor.getCaret().setSelectionVisible(true);
	}

	private Snippet createSnippet(DecompiledClassSource source, Entry<?> target) {
		final Token targetToken = source.getIndex().getDeclarationToken(target);

		if (targetToken == null) {
			// This can happen as a result of #252: Issue with lost parameter connection.
			// This can also happen when the token is from a library.
			return null;
		}

		final Result<Snippet, String> snippet;
		if (target instanceof ClassEntry targetClass) {
			snippet = this.findClassSnippet(source, targetToken, targetClass);
		} else if (target instanceof MethodEntry targetMethod) {
			snippet = this.findMethodSnippet(source, targetToken, targetMethod);
		} else if (target instanceof FieldEntry targetField) {
			snippet = this.findFieldSnippet(source, targetToken, targetField);
		} else if (target instanceof LocalVariableEntry targetLocal) {
			snippet = this.getVariableSnippet(source, targetToken, targetLocal);
		} else {
			// this should never be reached
			Logger.error(
					"Error trimming tooltip for '{}': unrecognized target entry type!",
					this.getFullDeobfuscatedName(target)
			);
			return null;
		}

		return snippet.unwrapOrElse(error -> {
			Logger.error(
					"Error finding declaration of '{}' for tooltip: {}",
					this.getFullDeobfuscatedName(target),
					error
			);
			return null;
		});
	}

	private Result<Snippet, String> getVariableSnippet(
			DecompiledClassSource source, Token target, LocalVariableEntry targetEntry
	) {
		final MethodEntry parent = targetEntry.getParent();
		if (parent == null) {
			return Result.err("variable parent is null!");
		}

		final Token parentToken = source.getIndex().getDeclarationToken(parent);
		if (parentToken == null) {
			return this.findLambdaVariable(source, target, targetEntry, parent);
		} else {
			if (targetEntry.isArgument()) {
				return this.findMethodSnippet(source, parentToken, parent);
			} else {
				return this.findLocalSnippet(source, parentToken, target);
			}
		}
	}

	private Result<Snippet, String> findLambdaVariable(
			DecompiledClassSource source, Token target, LocalVariableEntry targetEntry, MethodEntry parent
	) {
		final LineIndexer lineIndexer = new LineIndexer(source.toString());

		final EntryIndex entryIndex = this.gui.getController().getProject().getJarIndex().getIndex(EntryIndex.class);
		return Optional.ofNullable(entryIndex.getDefinition(parent))
			.map(parentDef -> {
				if (parentDef.getAccess().isSynthetic()) {
					return parse(source).andThen(unit -> unit
						.findAll(LambdaExpr.class, lambda -> rangeContains(lineIndexer, lambda, target))
						.stream()
						.max(depthComparatorOf(lineIndexer))
						.map(parentLambda -> {
							if (targetEntry.isArgument()) {
								return parentLambda
									.getBegin()
									.map(parentBegin -> parentLambda
										.getBody()
										.getRange()
										.map(bodyRange -> toSnippet(lineIndexer, parentBegin, bodyRange.begin))
										.<Result<Snippet, String>>map(Result::ok)
										.orElseGet(() -> Result.err("no parent lambda body range!")))
									.orElseGet(() -> Result.err("no parent lambda begin"));
							} else {
								final Statement parentBody = parentLambda.getBody();
								return parentBody.toBlockStmt()
									.map(parentBlock -> findLocalSnippet(target, parentBlock, lineIndexer, LAMBDA))
									.orElseGet(() -> parentBody.asExpressionStmt()
										.getExpression()
										.toVariableDeclarationExpr()
										.map(variableExpr ->
											findVariableExpressionSnippet(target, variableExpr, lineIndexer)
										)
										.orElseGet(() -> Result.err("local declared in non-declaration expression!"))
									);
							}
						})
						.orElseGet(() -> Result.err("failed to find local's parent lambda!")));
				} else {
					return Result.<Snippet, String>err("no parent token for non-synthetic parent!");
				}
			})
			.orElseGet(() -> Result.err("no parent definition!"));
	}

	private String getFullDeobfuscatedName(Entry<?> entry) {
		return this.gui.getController().getProject().getRemapper()
				.deobfuscate(entry)
				.getFullName();
	}

	private Result<Snippet, String> findClassSnippet(
			DecompiledClassSource source, Token target, ClassEntry targetEntry
	) {
		return this.getNodeType(targetEntry).andThen(nodeType -> {
			final LineIndexer lineIndexer = new LineIndexer(source.toString());
			return findDeclaration(source, target, nodeType, lineIndexer)
				.andThen(declaration -> findTypeDeclarationSnippet(declaration, lineIndexer));
		});
	}

	private static Result<Snippet, String> findTypeDeclarationSnippet(
			TypeDeclaration<?> declaration, LineIndexer lineIndexer
	) {
		return declaration
			.getTokenRange()
			.map(tokenRange -> findFirstToken(tokenRange, "{")
				.map(openCurlyBrace -> openCurlyBrace
					.getRange()
					.map(openCurlyRange -> openCurlyRange.begin)
					.map(openCurlyPos -> toSnippet(
						lineIndexer, declaration.getBegin().orElseThrow(), openCurlyPos
					))
					.<Result<Snippet, String>>map(Result::ok)
					.orElseGet(() -> Result.err("no class open curly brace range!")))
				.orElseGet(() -> Result.err("no class open curly brace!"))
			)
			.orElseGet(() -> Result.err(NO_TOKEN_RANGE));
	}

	private Result<Class<? extends TypeDeclaration<?>>, String> getNodeType(ClassEntry targetClass) {
		final EntryIndex entryIndex = this.gui.getController().getProject().getJarIndex().getIndex(EntryIndex.class);

		return Optional
			.ofNullable(entryIndex.getDefinition(targetClass))
			.map(targetDef -> {
				if (targetDef.getAccess().isAnnotation()) {
					return AnnotationDeclaration.class;
				} else if (targetDef.isEnum()) {
					return EnumDeclaration.class;
				} else if (targetDef.isRecord()) {
					return RecordDeclaration.class;
				} else {
					return ClassOrInterfaceDeclaration.class;
				}
			})
			.<Result<Class<? extends TypeDeclaration<?>>, String>>map(Result::ok)
			.orElseGet(() -> Result.err(NO_ENTRY_DEFINITION));
	}

	// TODO test record component getters
	private Result<Snippet, String> findMethodSnippet(
			DecompiledClassSource source, Token target, MethodEntry targetEntry
	) {
		final LineIndexer lineIndexer = new LineIndexer(source.toString());

		final Class<? extends CallableDeclaration<?>> nodeType;
		final Function<CallableDeclaration<?>, Optional<BlockStmt>> bodyGetter;
		if (targetEntry.isConstructor()) {
			nodeType = ConstructorDeclaration.class;
			bodyGetter = declaration -> Optional.of(((ConstructorDeclaration) declaration).getBody());
		} else {
			nodeType = MethodDeclaration.class;
			bodyGetter = declaration -> ((MethodDeclaration) declaration).getBody();
		}

		return findDeclaration(source, target, nodeType, lineIndexer).andThen(declaration -> {
			final Range range = declaration.getRange().orElseThrow();

			return bodyGetter.apply(declaration)
				.map(methodBody -> methodBody
					.getRange()
					.<Result<Range, String>>map(Result::ok)
					.orElseGet(() -> Result.err("no method body range!"))
					.map(bodyRange -> toSnippet(lineIndexer, range.begin, bodyRange.begin))
				)
				// no body: abstract
				.orElseGet(() -> Result.ok(toSnippet(lineIndexer, range)));
		});
	}

	private Result<Snippet, String> findFieldSnippet(
			DecompiledClassSource source, Token target, FieldEntry targetEntry
	) {
		final EntryIndex entryIndex = this.gui.getController().getProject().getJarIndex().getIndex(EntryIndex.class);

		return Optional.ofNullable(entryIndex.getDefinition(targetEntry))
			.map(targetDef -> {
				if (targetDef.getAccess().isEnum()) {
					return findEnumConstantSnippet(source, target);
				} else {
					return Optional.ofNullable(entryIndex.getDefinition(targetDef.getParent()))
						.map(parent -> parent.isRecord() && !targetDef.getAccess().isStatic()
							? this.findComponentParent(source, parent)
							: findRegularFieldSnippet(source, target)
						)
						.orElseGet(() -> Result.err("no field parent definition!"));
				}
			})
			.orElseGet(() -> Result.err(NO_ENTRY_DEFINITION));
	}

	private Result<Snippet, String> findComponentParent(DecompiledClassSource source, ClassDefEntry parent) {
		final Token parentToken = source.getIndex().getDeclarationToken(parent);

		final LineIndexer lineIndexer = new LineIndexer(source.toString());
		return findDeclaration(source, parentToken, RecordDeclaration.class, lineIndexer)
			.andThen(parentDeclaration -> parentDeclaration
				.getImplementedTypes()
				.getFirst()
				// exclude implemented types if present
				.map(ignored -> parentDeclaration
					.getTokenRange()
					.map(parentTokenRange -> findFirstToken(parentTokenRange, "implements")
						.map(implToken -> implToken
							.getRange()
							.map(implRange -> implRange.begin.right(-1))
							.map(beforeImpl -> toSnippet(
								lineIndexer,
								parentDeclaration.getBegin().orElseThrow(),
								beforeImpl
							))
							.<Result<Snippet, String>>map(Result::ok)
							.orElseGet(() -> Result.err("no parent record implements token range!"))
						)
						.orElseGet(() -> Result.err("record implements types but has no implements token!"))
					)
					.orElseGet(() -> Result.err("no parent record token range!"))
				)
				// no implemented types
				.orElseGet(() -> findTypeDeclarationSnippet(parentDeclaration, lineIndexer))
			);
	}

	private static Result<Snippet, String> findEnumConstantSnippet(DecompiledClassSource source, Token target) {
		final LineIndexer lineIndexer = new LineIndexer(source.toString());
		return findDeclaration(source, target, EnumConstantDeclaration.class, lineIndexer)
			.andThen(declaration -> Result.ok(toSnippet(lineIndexer, declaration.getRange().orElseThrow())));
	}

	private static Result<Snippet, String> findRegularFieldSnippet(DecompiledClassSource source, Token target) {
		final LineIndexer lineIndexer = new LineIndexer(source.toString());
		return findDeclaration(source, target, FieldDeclaration.class, lineIndexer).andThen(declaration -> declaration
			.getTokenRange()
			.map(tokenRange -> {
				final Range range = declaration.getRange().orElseThrow();
				return declaration.getVariables().stream()
					.filter(variable -> rangeContains(lineIndexer, variable, target))
					.findFirst()
					.map(variable -> toDeclaratorSnippet(range, variable, lineIndexer))
					.orElseGet(() -> Result.err("no matching field declarator!"));
			})
			.orElseGet(() -> Result.err(NO_TOKEN_RANGE))
		);
	}

	private Result<Snippet, String> findLocalSnippet(
			DecompiledClassSource source, Token parentToken, Token targetToken
	) {
		final LineIndexer lineIndexer = new LineIndexer(source.toString());

		return findDeclaration(source, parentToken, MethodDeclaration.class, lineIndexer)
			.andThen(declaration -> declaration
				.getBody()
				.<Result<BlockStmt, String>>map(Result::ok)
				.orElseGet(() -> Result.err("no method body!"))
				.andThen(parentBody -> findLocalSnippet(targetToken, parentBody, lineIndexer, METHOD))
			);
	}

	private static Result<Snippet, String> findLocalSnippet(
			Token target, BlockStmt parentBody, LineIndexer lineIndexer, String parentType
	) {
		return parentBody
			.getStatements()
			.stream()
			.map(Statement::toExpressionStmt)
			.flatMap(Optional::stream)
			.map(ExpressionStmt::getExpression)
			.map(Expression::toVariableDeclarationExpr)
			.flatMap(Optional::stream)
			.filter(variableExpr -> rangeContains(lineIndexer, variableExpr, target))
			.max(depthComparatorOf(lineIndexer))
			.map(variableExpr -> findVariableExpressionSnippet(target, variableExpr, lineIndexer))
			.orElseGet(() -> Result.err("failed to find local in parent %s!".formatted(parentType)));
	}

	private static Result<Snippet, String> findVariableExpressionSnippet(
			Token targetToken, VariableDeclarationExpr variableExpr, LineIndexer lineIndexer
	) {
		return variableExpr
			.getVariables()
			.stream()
			.filter(variable -> rangeContains(lineIndexer, variable, targetToken))
			.findFirst()
			.map(targetVariable ->
				toDeclaratorSnippet(variableExpr.getRange().orElseThrow(), targetVariable, lineIndexer)
			)
			.orElseGet(() -> Result.err("failed to find local in variable expression!"));
	}

	/**
	 * @return an {@linkplain Result#ok(Object) ok result} containing the declaration representing the passed
	 * {@code token}, or an {@linkplain Result#err(Object) error result} if it could not be found;
	 * found declarations always {@linkplain TypeDeclaration#hasRange() have a range}
	 */
	private static <D extends BodyDeclaration<?>> Result<D, String> findDeclaration(
			DecompiledClassSource source, Token target, Class<D> nodeType, LineIndexer lineIndexer
	) {
		return parse(source).andThen(unit -> unit
			.findAll(nodeType, declaration -> rangeContains(lineIndexer, declaration, target))
			.stream()
			.max(depthComparatorOf(lineIndexer))
			.<Result<D, String>>map(Result::ok)
			.orElseGet(() -> Result.err("not found in parsed source!"))
		);
	}

	private static Comparator<NodeWithRange<?>> depthComparatorOf(LineIndexer lineIndexer) {
		return comparingInt(declaration -> lineIndexer.getIndex(declaration.getRange().orElseThrow().begin));
	}

	private static Result<CompilationUnit, String> parse(DecompiledClassSource source) {
		final ParserConfiguration config = new ParserConfiguration()
				.setStoreTokens(true)
				.setLanguageLevel(ParserConfiguration.LanguageLevel.RAW);

		final ParseResult<CompilationUnit> parseResult = new JavaParser(config).parse(source.toString());
		return parseResult
				.getResult()
				.<Result<CompilationUnit, String>>map(Result::ok)
				.orElseGet(() -> Result.err("failed to parse source: " + parseResult.getProblems()));
	}

	private static Result<Snippet, String> toDeclaratorSnippet(
			Range outerRange, VariableDeclarator variable, LineIndexer lineIndexer
	) {
		if (outerRange.begin.line == outerRange.end.line) {
			return Result.ok(toSnippet(lineIndexer, outerRange));
		} else {
			return variable
				.getTokenRange()
				// if it's not all on one line, try excluding assignment
				.map(variableRange -> findFirstToken(variableRange, "=")
					.map(assignment -> assignment.getRange().orElseThrow().begin)
					.<Result<Position, String>>map(Result::ok)
					// no assignment
					.orElse(Result.ok(outerRange.end))
				)
				.orElseGet(() -> Result.err("no variable token range!"))
				.map(end -> toSnippet(lineIndexer, outerRange.begin, end));
		}
	}

	private static Optional<JavaToken> findFirstToken(TokenRange range, String token) {
		return findFirstToken(range, javaToken -> javaToken.asString().equals(token));
	}

	private static Optional<JavaToken> findFirstToken(TokenRange range, Predicate<JavaToken> predicate) {
		for (final JavaToken token : range) {
			if (predicate.test(token)) {
				return Optional.of(token);
			}
		}

		return Optional.empty();
	}

	private static <N extends NodeWithRange<?>> boolean rangeContains(LineIndexer lineIndexer, N node, Token token) {
		if (node.hasRange()) {
			final Range range = node.getRange().orElseThrow();

			return lineIndexer.getIndex(range.begin) <= token.start
				// subtract one because Token.end is exclusive
				&& lineIndexer.getIndex(range.end) >= token.end - 1;
		} else {
			return false;
		}
	}

	private static Snippet toSnippet(LineIndexer lineIndexer, Range range) {
		return toSnippet(lineIndexer, range.begin, range.end);
	}

	private static Snippet toSnippet(LineIndexer lineIndexer, Position startPos, Position endPos) {
		final int start = lineIndexer.getIndex(startPos);
		int end = lineIndexer.getIndex(endPos);
		while (Character.isWhitespace(lineIndexer.getString().charAt(end))) {
			end--;
		}

		return new Snippet(start, end + 1);
	}
}
