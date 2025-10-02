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
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
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
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.Comparator.comparingInt;

public class TooltipEditorPanel extends BaseEditorPanel {
	private static final Pattern CLASS_PUNCTUATION = Pattern.compile("[/\\$]");
	private static final String NO_ENTRY_DEFINITION = "No entry definition!";
	private static final String NO_TOKEN_RANGE = "No token range!";

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
		final Entry<?> deobfTarget = this.gui.getController().getProject().getRemapper().deobfuscate(target);
		this.setClassHandle(targetTopClassHandle, source -> this.createTrimmedBounds(source, target, deobfTarget));
	}

	private TrimmedBounds createTrimmedBounds(DecompiledClassSource source, Entry<?> target, Entry<?> deobfTarget) {
		final String targetDotName = CLASS_PUNCTUATION.matcher(deobfTarget.getFullName()).replaceAll(".");

		if (target instanceof ClassEntry targetClass) {
			return unwrapTooltipBoundsOrNull(this.findClassBounds(source, targetClass), targetDotName);
		} else if (target instanceof MethodEntry targetMethod) {
			return unwrapTooltipBoundsOrNull(this.findMethodBounds(source, targetMethod), targetDotName);
		} else if (target instanceof FieldEntry targetField) {
			return unwrapTooltipBoundsOrNull(this.findFieldBounds(source, targetField), targetDotName);
		} else if (target instanceof LocalVariableEntry targetLocal) {
			if (targetLocal.isArgument()) {
				// TODO show method declaration
				return null;
			} else {
				// TODO show local declaration
				return null;
			}
		} else {
			// TODO use same message formatting as unwrapOrNull
			// this should never be reached
			Logger.error("Unrecognized target entry type: {}!", target);
			return null;
		}
	}

	private Result<TrimmedBounds, String> findClassBounds(DecompiledClassSource source, ClassEntry target) {
		return this.getNodeType(target).andThen(nodeType -> {
			final LineIndexer lineIndexer = new LineIndexer(source.toString());
			final Token targetToken = source.getIndex().getDeclarationToken(target);
			return findDeclaration(source, targetToken, nodeType, lineIndexer).andThen(declaration -> declaration
				.getTokenRange()
				.map(tokenRange -> findFirstToken(tokenRange, token -> token.asString().equals("{"))
					.map(openCurlyBrace -> openCurlyBrace
						.getRange()
						.map(openRange -> toTrimmedBounds(
							lineIndexer, declaration.getRange().orElseThrow().begin, openRange.begin
						))
						.<Result<TrimmedBounds, String>>map(Result::ok)
						.orElseGet(() -> Result.err("No class open curly brace range!")))
					.orElseGet(() -> Result.err("No class open curly brace!"))
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

	private Result<TrimmedBounds, String> findMethodBounds(DecompiledClassSource source, MethodEntry target) {
		final LineIndexer lineIndexer = new LineIndexer(source.toString());
		final Token targetToken = source.getIndex().getDeclarationToken(target);
		return findDeclaration(source, targetToken, MethodDeclaration.class, lineIndexer)
			.andThen(declaration -> {
				final Range range = declaration.getRange().orElseThrow();

				return declaration
					.getBody()
					.map(body -> body.getRange()
						.map(bodyRange -> toTrimmedBounds(lineIndexer, range.begin, bodyRange.begin))
						.<Result<TrimmedBounds, String>>map(Result::ok)
						.orElseGet(() -> Result.err("No method body range!"))
					)
					// no body: abstract
					.orElseGet(() -> Result.ok(toTrimmedBounds(lineIndexer, range)));
			});
	}

	private Result<TrimmedBounds, String> findFieldBounds(DecompiledClassSource source, FieldEntry target) {
		final Token targetToken = source.getIndex().getDeclarationToken(target);

		final EntryIndex entryIndex = this.gui.getController().getProject().getJarIndex().getIndex(EntryIndex.class);

		return Optional.ofNullable(entryIndex.getDefinition(target))
			.map(targetDef -> {
				final LineIndexer lineIndexer = new LineIndexer(source.toString());
				if (targetDef.getAccess().isEnum()) {
					return findEnumConstantBounds(source, targetToken, lineIndexer);
				} else {
					if (targetDef.getAccess().isStatic()) {
						// don't check whether it's a record component if it's static
						return findRegularFieldBounds(source, targetToken, lineIndexer);
					} else {
						return Optional.ofNullable(entryIndex.getDefinition(targetDef.getParent()))
							.map(parent -> {
								if (parent.isRecord()) {
									return this.findRecordComponent(source, targetToken, parent, lineIndexer);
								} else {
									return findRegularFieldBounds(source, targetToken, lineIndexer);
								}
							})
							.orElseGet(() -> Result.err("No field parent definition!"));
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
				.orElseGet(() -> Result.err("Could not find record component!"))
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
		return findDeclaration(source, target, FieldDeclaration.class, lineIndexer)
			.andThen(declaration -> declaration
				.getTokenRange()
				.map(tokenRange -> {
					final Range range = declaration.getRange().orElseThrow();
					return declaration.getVariables().stream()
						.filter(variable -> rangeContains(lineIndexer, variable, target))
						.findFirst()
						.map(variable -> findFirstToken(tokenRange, token -> token.asString().equals("="))
							.map(terminator -> toTrimmedBounds(
								lineIndexer, range.begin, terminator.getRange().orElseThrow().begin
							))
							.<Result<TrimmedBounds, String>>map(Result::ok)
							// no assignment in field declaration
							.orElseGet(() -> Result.ok(toTrimmedBounds(lineIndexer, range)))
						)
						.orElseGet(() -> Result.err("No matching variable declarator!"));
				})
				.orElseGet(() -> Result.err(NO_TOKEN_RANGE))
			);
	}

	/**
	 * @return an {@linkplain Result#ok(Object) ok result} containing the declaration representing the passed
	 * {@code token}, or an {@linkplain Result#err(Object) error result} if it could not be found;
	 * found declarations always {@linkplain TypeDeclaration#hasRange() have a range}
	 */
	private static <D extends BodyDeclaration<?>> Result<D, String> findDeclaration(
			DecompiledClassSource source, Token target, Class<D> nodeType, LineIndexer lineIndexer
	) {
		final ParseResult<CompilationUnit> parseResult = parse(source.toString());
		return parseResult
			.getResult()
			.map(unit -> unit
				.findAll(nodeType, declaration -> rangeContains(lineIndexer, declaration, target))
				.stream()
				// deepest
				.min(comparingInt(declaration -> lineIndexer.getIndex(declaration.getRange().orElseThrow().end)))
				.<Result<D, String>>map(Result::ok)
				.orElseGet(() -> Result.err("Not found in parsed source!")))
			.orElseGet(() -> Result.err("Failed to parse source: " + parseResult.getProblems()));
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

	private static TrimmedBounds unwrapTooltipBoundsOrNull(Result<TrimmedBounds, String> bounds, String targetName) {
		return bounds.unwrapOrElse(error -> {
			Logger.error("Error finding declaration of '{}' for tooltip: {}", targetName, error);
			return null;
		});
	}

	private static ParseResult<CompilationUnit> parse(String source) {
		final ParserConfiguration config = new ParserConfiguration()
				.setStoreTokens(true)
				.setLanguageLevel(ParserConfiguration.LanguageLevel.RAW);

		return new JavaParser(config).parse(source);
	}
}
