/*
 * Originally:
 * EnumSwitchRewriterTransform.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is based on Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package cuchaz.enigma.source.procyon.transformers;

import com.strobel.assembler.metadata.BuiltinTypes;
import com.strobel.assembler.metadata.FieldDefinition;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.SafeCloseable;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.AssignmentExpression;
import com.strobel.decompiler.languages.java.ast.AstBuilder;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.CaseLabel;
import com.strobel.decompiler.languages.java.ast.ContextTrackingVisitor;
import com.strobel.decompiler.languages.java.ast.Expression;
import com.strobel.decompiler.languages.java.ast.IdentifierExpression;
import com.strobel.decompiler.languages.java.ast.IndexerExpression;
import com.strobel.decompiler.languages.java.ast.InvocationExpression;
import com.strobel.decompiler.languages.java.ast.Keys;
import com.strobel.decompiler.languages.java.ast.MemberReferenceExpression;
import com.strobel.decompiler.languages.java.ast.PrimitiveExpression;
import com.strobel.decompiler.languages.java.ast.SwitchSection;
import com.strobel.decompiler.languages.java.ast.SwitchStatement;
import com.strobel.decompiler.languages.java.ast.TypeDeclaration;
import com.strobel.decompiler.languages.java.ast.TypeReferenceExpression;
import com.strobel.decompiler.languages.java.ast.transforms.IAstTransform;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Copy of {@link com.strobel.decompiler.languages.java.ast.transforms.EnumSwitchRewriterTransform} modified to:
 * - Not rely on a field containing "$SwitchMap$" (Proguard strips it)
 * - Ignore classes *with* SwitchMap$ names (so the original can handle it)
 * - Ignores inner synthetics that are not package private
 */
@SuppressWarnings("Duplicates")
public class ObfuscatedEnumSwitchRewriterTransform implements IAstTransform {
	private final DecompilerContext context;

	public ObfuscatedEnumSwitchRewriterTransform(final DecompilerContext context) {
		this.context = VerifyArgument.notNull(context, "context");
	}

	@Override
	public void run(final AstNode compilationUnit) {
		compilationUnit.acceptVisitor(new Visitor(this.context), null);
	}

	private static final class Visitor extends ContextTrackingVisitor<Void> {
		private static final class SwitchMapInfo {
			final String enclosingType;
			final Map<String, List<SwitchStatement>> switches = new LinkedHashMap<>();
			final Map<String, Map<Integer, Expression>> mappings = new LinkedHashMap<>();

			TypeDeclaration enclosingTypeDeclaration;

			SwitchMapInfo(final String enclosingType) {
				this.enclosingType = enclosingType;
			}
		}

		private final Map<String, SwitchMapInfo> switchMaps = new LinkedHashMap<>();
		private boolean isSwitchMapWrapper;

		private Visitor(final DecompilerContext context) {
			super(context);
		}

		@Override
		public Void visitTypeDeclarationOverride(final TypeDeclaration typeDeclaration, final Void p) {
			final boolean oldIsSwitchMapWrapper = this.isSwitchMapWrapper;
			final TypeDefinition typeDefinition = typeDeclaration.getUserData(Keys.TYPE_DEFINITION);
			final boolean isSwitchMapWrapper = isSwitchMapWrapper(typeDefinition);

			if (isSwitchMapWrapper) {
				final String internalName = typeDefinition.getInternalName();

				SwitchMapInfo info = this.switchMaps.get(internalName);

				if (info == null) {
					this.switchMaps.put(internalName, info = new SwitchMapInfo(internalName));
				}

				info.enclosingTypeDeclaration = typeDeclaration;
			}

			this.isSwitchMapWrapper = isSwitchMapWrapper;

			try {
				super.visitTypeDeclarationOverride(typeDeclaration, p);
			} finally {
				this.isSwitchMapWrapper = oldIsSwitchMapWrapper;
			}

			this.rewrite();

			return null;
		}

		@Override
		public Void visitSwitchStatement(final SwitchStatement node, final Void data) {
			final Expression test = node.getExpression();

			if (test instanceof final IndexerExpression indexer) {
				final Expression array = indexer.getTarget();
				final Expression argument = indexer.getArgument();

				if (!(array instanceof final MemberReferenceExpression arrayAccess)) {
					return super.visitSwitchStatement(node, data);
				}

				final Expression arrayOwner = arrayAccess.getTarget();
				final String mapName = arrayAccess.getMemberName();

				if (mapName == null || mapName.startsWith("$SwitchMap$") || !(arrayOwner instanceof final TypeReferenceExpression enclosingTypeExpression)) {
					return super.visitSwitchStatement(node, data);
				}

				final TypeReference enclosingType = enclosingTypeExpression.getType().getUserData(Keys.TYPE_REFERENCE);

				if (!isSwitchMapWrapper(enclosingType) || !(argument instanceof final InvocationExpression invocation)) {
					return super.visitSwitchStatement(node, data);
				}

				final Expression invocationTarget = invocation.getTarget();

				if (!(invocationTarget instanceof final MemberReferenceExpression memberReference)) {
					return super.visitSwitchStatement(node, data);
				}

				if (!"ordinal".equals(memberReference.getMemberName())) {
					return super.visitSwitchStatement(node, data);
				}

				final String enclosingTypeName = enclosingType.getInternalName();

				SwitchMapInfo info = this.switchMaps.get(enclosingTypeName);

				if (info == null) {
					this.switchMaps.put(enclosingTypeName, info = new SwitchMapInfo(enclosingTypeName));

					final TypeDefinition resolvedType = enclosingType.resolve();

					if (resolvedType != null) {
						AstBuilder astBuilder = this.context.getUserData(Keys.AST_BUILDER);

						if (astBuilder == null) {
							astBuilder = new AstBuilder(this.context);
						}

						try (SafeCloseable ignored = astBuilder.suppressImports()) {
							final TypeDeclaration declaration = astBuilder.createType(resolvedType);

							declaration.acceptVisitor(this, data);
						}
					}
				}

				List<SwitchStatement> switches = info.switches.computeIfAbsent(mapName, k -> new ArrayList<>());

				switches.add(node);
			}

			return super.visitSwitchStatement(node, data);
		}

		@Override
		public Void visitAssignmentExpression(final AssignmentExpression node, final Void data) {
			final TypeDefinition currentType = this.context.getCurrentType();
			final MethodDefinition currentMethod = this.context.getCurrentMethod();

			if (this.isSwitchMapWrapper
					&& currentType != null
					&& currentMethod != null
					&& currentMethod.isTypeInitializer()) {
				final Expression left = node.getLeft();
				final Expression right = node.getRight();

				if (left instanceof IndexerExpression expression
						&& right instanceof final PrimitiveExpression value) {
					String mapName = null;

					final Expression array = expression.getTarget();
					final Expression argument = expression.getArgument();

					if (array instanceof MemberReferenceExpression referenceExpression) {
						mapName = referenceExpression.getMemberName();
					} else if (array instanceof IdentifierExpression identifierExpression) {
						mapName = identifierExpression.getIdentifier();
					}

					if (mapName == null || mapName.startsWith("$SwitchMap$")) {
						return super.visitAssignmentExpression(node, data);
					}

					if (!(argument instanceof final InvocationExpression invocation)) {
						return super.visitAssignmentExpression(node, data);
					}

					final Expression invocationTarget = invocation.getTarget();

					if (!(invocationTarget instanceof final MemberReferenceExpression memberReference)) {
						return super.visitAssignmentExpression(node, data);
					}

					final Expression memberTarget = memberReference.getTarget();

					if (!(memberTarget instanceof final MemberReferenceExpression outerMemberReference) || !"ordinal".equals(memberReference.getMemberName())) {
						return super.visitAssignmentExpression(node, data);
					}

					final Expression outerMemberTarget = outerMemberReference.getTarget();

					if (!(outerMemberTarget instanceof TypeReferenceExpression)) {
						return super.visitAssignmentExpression(node, data);
					}

					final String enclosingType = currentType.getInternalName();

					SwitchMapInfo info = this.switchMaps.get(enclosingType);

					if (info == null) {
						this.switchMaps.put(enclosingType, info = new SwitchMapInfo(enclosingType));

						AstBuilder astBuilder = this.context.getUserData(Keys.AST_BUILDER);

						if (astBuilder == null) {
							astBuilder = new AstBuilder(this.context);
						}

						info.enclosingTypeDeclaration = astBuilder.createType(currentType);
					}

					assert value.getValue() instanceof Integer;

					Map<Integer, Expression> mapping = info.mappings.computeIfAbsent(mapName, k -> new LinkedHashMap<>());

					final IdentifierExpression enumValue = new IdentifierExpression(Expression.MYSTERY_OFFSET, outerMemberReference.getMemberName());

					enumValue.putUserData(Keys.MEMBER_REFERENCE, outerMemberReference.getUserData(Keys.MEMBER_REFERENCE));

					mapping.put(((Number) value.getValue()).intValue(), enumValue);
				}
			}

			return super.visitAssignmentExpression(node, data);
		}

		private void rewrite() {
			if (this.switchMaps.isEmpty()) {
				return;
			}

			for (final SwitchMapInfo info : this.switchMaps.values()) {
				this.rewrite(info);
			}

		//
		// Remove switch map type wrappers that are no longer referenced.
		//

		outer:
			for (final SwitchMapInfo info : this.switchMaps.values()) {
				for (final String mapName : info.switches.keySet()) {
					final List<SwitchStatement> switches = info.switches.get(mapName);

					if (switches != null && !switches.isEmpty()) {
						continue outer;
					}
				}

				final TypeDeclaration enclosingTypeDeclaration = info.enclosingTypeDeclaration;

				if (enclosingTypeDeclaration != null) {
					enclosingTypeDeclaration.remove();
				}
			}
		}

		private void rewrite(final SwitchMapInfo info) {
			if (info.switches.isEmpty()) {
				return;
			}

			for (final String mapName : info.switches.keySet()) {
				final List<SwitchStatement> switches = info.switches.get(mapName);
				final Map<Integer, Expression> mappings = info.mappings.get(mapName);

				if (switches != null && mappings != null) {
					for (int i = 0; i < switches.size(); i++) {
						if (this.rewriteSwitch(switches.get(i), mappings)) {
							switches.remove(i--);
						}
					}
				}
			}
		}

		private boolean rewriteSwitch(final SwitchStatement s, final Map<Integer, Expression> mappings) {
			final Map<Expression, Expression> replacements = new IdentityHashMap<>();

			for (final SwitchSection section : s.getSwitchSections()) {
				for (final CaseLabel caseLabel : section.getCaseLabels()) {
					final Expression expression = caseLabel.getExpression();

					if (expression.isNull()) {
						continue;
					}

					if (expression instanceof PrimitiveExpression primitiveExpression) {
						final Object value = primitiveExpression.getValue();

						if (value instanceof Integer) {
							final Expression replacement = mappings.get(value);

							if (replacement != null) {
								replacements.put(expression, replacement);
								continue;
							}
						}
					}

					// If we can't rewrite all cases, we abort.

					return false;
				}
			}

			final IndexerExpression indexer = (IndexerExpression) s.getExpression();
			final InvocationExpression argument = (InvocationExpression) indexer.getArgument();
			final MemberReferenceExpression memberReference = (MemberReferenceExpression) argument.getTarget();
			final Expression newTest = memberReference.getTarget();

			newTest.remove();
			indexer.replaceWith(newTest);

			for (final Map.Entry<Expression, Expression> entry : replacements.entrySet()) {
				entry.getKey().replaceWith(entry.getValue().clone());
			}

			return true;
		}

		private static boolean isSwitchMapWrapper(final TypeReference type) {
			if (type == null) {
				return false;
			}

			final TypeDefinition definition = type instanceof TypeDefinition typeDefinition ? typeDefinition : type.resolve();

			if (definition == null || !definition.isSynthetic() || !definition.isInnerClass() || !definition.isPackagePrivate()) {
				return false;
			}

			for (final FieldDefinition field : definition.getDeclaredFields()) {
				if (!field.getName().startsWith("$SwitchMap$")
						&& BuiltinTypes.Integer.makeArrayType().equals(field.getFieldType())) {
					return true;
				}
			}

			return false;
		}
	}
}
