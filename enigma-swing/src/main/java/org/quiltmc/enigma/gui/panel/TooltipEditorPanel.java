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
import org.quiltmc.enigma.gui.highlight.SelectionHighlightPainter;
import org.quiltmc.enigma.util.LineIndexer;
import org.quiltmc.enigma.util.Result;
import org.quiltmc.syntaxpain.LineNumbersRuler;
import org.tinylog.Logger;

import javax.swing.JViewport;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Comparator.comparingInt;

public class TooltipEditorPanel extends BaseEditorPanel {
	private static final String NO_ENTRY_DEFINITION = "no entry definition!";
	private static final String NO_TOKEN_RANGE = "no token range!";
	// used to compose error messages
	private static final String METHOD = "method";
	private static final String LAMBDA = "lambda";

	public TooltipEditorPanel(Gui gui, Entry<?> target, ClassHandle targetTopClassHandle) {
		super(gui);

		Optional.ofNullable(this.editorScrollPane.getRowHeader())
				.map(JViewport::getView)
				// LineNumbersRuler is installed by syntaxpain
				.map(view -> view instanceof LineNumbersRuler lineNumbers ? lineNumbers : null)
				// TODO offset line numbers instead of removing them once
				//  offsets are implemented in syntaxpain
				.ifPresent(lineNumbers -> lineNumbers.deinstall(this.editor));

		this.addSourceSetListener(source -> {
			final Token declarationToken = source.getIndex().getDeclarationToken(target);
			if (declarationToken != null) {
				// TODO create custom highlighter
				this.navigateToToken(declarationToken, SelectionHighlightPainter.INSTANCE);
			}
		});

		this.getEditor().setEditable(false);
		this.setClassHandle(targetTopClassHandle, source -> this.createTrimmedBounds(source, target));
	}

	private TrimmedBounds createTrimmedBounds(DecompiledClassSource source, Entry<?> target) {
		final Token targetToken = Objects.requireNonNull(
				source.getIndex().getDeclarationToken(target),
				() -> "Error trimming tooltip for '%s': no declaration token!"
					.formatted(this.getFullDeobfuscatedName(target))
		);

		final Result<TrimmedBounds, String> bounds;
		if (target instanceof ClassEntry targetClass) {
			bounds = this.findClassBounds(source, targetToken, targetClass);
		} else if (target instanceof MethodEntry targetMethod) {
			bounds = this.findMethodBounds(source, targetToken, targetMethod);
		} else if (target instanceof FieldEntry targetField) {
			bounds = this.findFieldBounds(source, targetToken, targetField);
		} else if (target instanceof LocalVariableEntry targetLocal) {
			bounds = this.getVariableBounds(source, targetToken, targetLocal);
		} else {
			// this should never be reached
			Logger.error(
					"Error trimming tooltip for '{}': unrecognized target entry type!",
					this.getFullDeobfuscatedName(target)
			);
			return null;
		}

		return bounds.unwrapOrElse(error -> {
			Logger.error(
					"Error finding declaration of '{}' for tooltip: {}",
					this.getFullDeobfuscatedName(target),
					error
			);
			return null;
		});
	}

	private Result<TrimmedBounds, String> getVariableBounds(
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
				return this.findMethodBounds(source, parentToken, parent);
			} else {
				return this.findLocalBounds(source, parentToken, target);
			}
		}
	}

	private Result<TrimmedBounds, String> findLambdaVariable(
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
										.map(bodyRange -> toTrimmedBounds(lineIndexer, parentBegin, bodyRange.begin))
										.<Result<TrimmedBounds, String>>map(Result::ok)
										.orElseGet(() -> Result.err("no parent lambda body range!")))
									.orElseGet(() -> Result.err("no parent lambda begin"));
							} else {
								final Statement parentBody = parentLambda.getBody();
								return parentBody.toBlockStmt()
									.map(parentBlock -> findLocalBounds(target, parentBlock, lineIndexer, LAMBDA))
									.orElseGet(() -> parentBody.asExpressionStmt()
										.getExpression()
										.toVariableDeclarationExpr()
										.map(variableExpr ->
											findVariableExpressionBounds(target, variableExpr, lineIndexer)
										)
										.orElseGet(() -> Result.err("local declared in non-declaration expression!"))
									);
							}
						})
						.orElseGet(() -> Result.err("failed to find local's parent lambda!")));
				} else {
					return Result.<TrimmedBounds, String>err("no parent token for non-synthetic parent!");
				}
			})
			.orElseGet(() -> Result.err("no parent definition!"));
	}

	private String getFullDeobfuscatedName(Entry<?> entry) {
		return this.gui.getController().getProject().getRemapper()
				.deobfuscate(entry)
				.getFullName();
	}

	private Result<TrimmedBounds, String> findClassBounds(
			DecompiledClassSource source, Token target, ClassEntry targetEntry
	) {
		return this.getNodeType(targetEntry).andThen(nodeType -> {
			final LineIndexer lineIndexer = new LineIndexer(source.toString());
			return findDeclaration(source, target, nodeType, lineIndexer).andThen(declaration -> declaration
				.getTokenRange()
				.map(tokenRange -> findFirstToken(tokenRange, token -> token.asString().equals("{"))
					.map(openCurlyBrace -> openCurlyBrace
						.getRange()
						.map(openRange -> toTrimmedBounds(
							lineIndexer, declaration.getRange().orElseThrow().begin, openRange.begin
						))
						.<Result<TrimmedBounds, String>>map(Result::ok)
						.orElseGet(() -> Result.err("no class open curly brace range!")))
					.orElseGet(() -> Result.err("no class open curly brace!"))
				)
				.orElseGet(() -> Result.err(NO_TOKEN_RANGE))
			);
		});
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

	private Result<TrimmedBounds, String> findMethodBounds(
			DecompiledClassSource source, Token target, MethodEntry targetEntry
	) {
		final LineIndexer lineIndexer = new LineIndexer(source.toString());

		final Class<? extends CallableDeclaration<?>> nodeType;
		final Function<CallableDeclaration<?>, Result<BlockStmt, String>> bodyGetter;
		if (targetEntry.isConstructor()) {
			nodeType = ConstructorDeclaration.class;
			bodyGetter = declaration -> Result.ok(((ConstructorDeclaration) declaration).getBody());
		} else {
			nodeType = MethodDeclaration.class;
			bodyGetter = declaration -> getMethodBody((MethodDeclaration) declaration);
		}

		return findDeclaration(source, target, nodeType, lineIndexer).andThen(declaration -> {
			final Range range = declaration.getRange().orElseThrow();

			final Result<BlockStmt, String> methodBody = bodyGetter.apply(declaration);
			return methodBody.isErr()
					// no body: abstract
					? Result.ok(toTrimmedBounds(lineIndexer, range))
					: methodBody
						.andThen(body -> body.getRange()
							.<Result<Range, String>>map(Result::ok)
							.orElseGet(() -> Result.err("no method body range!"))
						)
						.map(bodyRange -> toTrimmedBounds(lineIndexer, range.begin, bodyRange.begin));
		});
	}

	private Result<TrimmedBounds, String> findFieldBounds(
			DecompiledClassSource source, Token target, FieldEntry targetEntry
	) {
		final EntryIndex entryIndex = this.gui.getController().getProject().getJarIndex().getIndex(EntryIndex.class);

		return Optional.ofNullable(entryIndex.getDefinition(targetEntry))
			.map(targetDef -> {
				final LineIndexer lineIndexer = new LineIndexer(source.toString());
				if (targetDef.getAccess().isEnum()) {
					return findEnumConstantBounds(source, target, lineIndexer);
				} else {
					if (targetDef.getAccess().isStatic()) {
						// not a record component if it's static
						return findRegularFieldBounds(source, target, lineIndexer);
					} else {
						return Optional.ofNullable(entryIndex.getDefinition(targetDef.getParent()))
							.map(parent -> {
								if (parent.isRecord()) {
									return this.findRecordComponent(source, target, parent, lineIndexer);
								} else {
									return findRegularFieldBounds(source, target, lineIndexer);
								}
							})
							.orElseGet(() -> Result.err("no field parent definition!"));
					}
				}
			})
			.orElseGet(() -> Result.err(NO_ENTRY_DEFINITION));
	}

	private Result<TrimmedBounds, String> findRecordComponent(
			DecompiledClassSource source, Token target, ClassDefEntry parent, LineIndexer lineIndexer
	) {
		final Token parentToken = source.getIndex().getDeclarationToken(parent);

		return findDeclaration(source, parentToken, RecordDeclaration.class, lineIndexer)
			.andThen(parentDeclaration -> parentDeclaration
				.getParameters().stream()
				.filter(component -> rangeContains(lineIndexer, component, target))
				.findFirst()
				.map(targetComponent -> toTrimmedBounds(lineIndexer, targetComponent.getRange().orElseThrow()))
				.<Result<TrimmedBounds, String>>map(Result::ok)
				.orElseGet(() -> Result.err("could not find record component!"))
			);
	}

	private static Result<TrimmedBounds, String> findEnumConstantBounds(
			DecompiledClassSource source, Token target, LineIndexer lineIndexer
	) {
		return findDeclaration(source, target, EnumConstantDeclaration.class, lineIndexer)
			.andThen(declaration -> Result.ok(toTrimmedBounds(lineIndexer, declaration.getRange().orElseThrow())));
	}

	private static Result<TrimmedBounds, String> findRegularFieldBounds(
			DecompiledClassSource source, Token target, LineIndexer lineIndexer
	) {
		return findDeclaration(source, target, FieldDeclaration.class, lineIndexer).andThen(declaration -> declaration
			.getTokenRange()
			.map(tokenRange -> {
				final Range range = declaration.getRange().orElseThrow();
				return declaration.getVariables().stream()
					.filter(variable -> rangeContains(lineIndexer, variable, target))
					.findFirst()
					.map(variable -> toDeclaratorBounds(range, variable, lineIndexer))
					.orElseGet(() -> Result.err("no matching field declarator!"));
			})
			.orElseGet(() -> Result.err(NO_TOKEN_RANGE))
		);
	}

	private Result<TrimmedBounds, String> findLocalBounds(
			DecompiledClassSource source, Token parentToken, Token targetToken
	) {
		final LineIndexer lineIndexer = new LineIndexer(source.toString());

		return findDeclaration(source, parentToken, MethodDeclaration.class, lineIndexer).andThen(declaration ->
			getMethodBody(declaration)
				.andThen(parentBody -> findLocalBounds(targetToken, parentBody, lineIndexer, METHOD))
		);
	}

	private static Result<TrimmedBounds, String> findLocalBounds(
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
			.map(variableExpr -> findVariableExpressionBounds(target, variableExpr, lineIndexer))
			.orElseGet(() -> Result.err("failed to find local in parent %s!".formatted(parentType)));
	}

	private static Result<TrimmedBounds, String> findVariableExpressionBounds(
			Token targetToken, VariableDeclarationExpr variableExpr, LineIndexer lineIndexer
	) {
		return variableExpr
			.getVariables()
			.stream()
			.filter(variable -> rangeContains(lineIndexer, variable, targetToken))
			.findFirst()
			.map(targetVariable ->
				toDeclaratorBounds(variableExpr.getRange().orElseThrow(), targetVariable, lineIndexer)
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

	private static Result<BlockStmt, String> getMethodBody(MethodDeclaration declaration) {
		return declaration
			.getBody()
			.<Result<BlockStmt, String>>map(Result::ok)
			.orElseGet(() -> Result.err("no method body!"));
	}

	private static Result<TrimmedBounds, String> toDeclaratorBounds(
			Range outerRange, VariableDeclarator variable, LineIndexer lineIndexer
	) {
		if (outerRange.begin.line == outerRange.end.line) {
			return Result.ok(toTrimmedBounds(lineIndexer, outerRange));
		} else {
			return variable
				.getTokenRange()
				.map(variableRange -> findFirstToken(variableRange, token -> token.asString().equals("="))
					.map(assignment -> assignment.getRange().orElseThrow().begin)
					.<Result<Position, String>>map(Result::ok)
					// no assignment
					.orElse(Result.ok(outerRange.end))
				)
				.orElseGet(() -> Result.err("no variable token range!"))
				.map(end -> toTrimmedBounds(lineIndexer, outerRange.begin, end));
		}
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

	private static TrimmedBounds toTrimmedBounds(LineIndexer lineIndexer, Range range) {
		return toTrimmedBounds(lineIndexer, range.begin, range.end);
	}

	private static TrimmedBounds toTrimmedBounds(LineIndexer lineIndexer, Position startPos, Position endPos) {
		final int start = lineIndexer.getIndex(startPos);
		int end = lineIndexer.getIndex(endPos);
		while (Character.isWhitespace(lineIndexer.getString().charAt(end))) {
			end--;
		}

		return new TrimmedBounds(start, end + 1);
	}
}
