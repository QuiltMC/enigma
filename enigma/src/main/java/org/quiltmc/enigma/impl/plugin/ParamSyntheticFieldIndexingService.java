package org.quiltmc.enigma.impl.plugin;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.class_provider.ProjectClassProvider;
import org.quiltmc.enigma.api.service.JarIndexerService;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ParamSyntheticFieldIndexingService implements JarIndexerService {
	public static final String ID = "enigma:param_synthetic_field_indexer";

	private final ParamSyntheticFieldIndexingVisitor visitor;
	private final Map<LocalVariableEntry, FieldEntry> linkedFieldsByParam = new HashMap<>();

	ParamSyntheticFieldIndexingService() {
		this.visitor = new ParamSyntheticFieldIndexingVisitor();
	}

	@Override
	public void acceptJar(Set<String> scope, ProjectClassProvider classProvider, JarIndex jarIndex) {
		for (final String className : scope) {
			final ClassNode node = classProvider.get(className);
			if (node != null) {
				node.accept(this.visitor);
			}
		}

		final Map<MethodNode, Map<FieldInsnNode, VarInsnNode>> constructorParamsBySyntheticFieldByConstructor =
				new HashMap<>();

		this.visitor.localTypeInstructions.forEach((method, typeInstruction) -> {
			final List<VarInsnNode> params = new ArrayList<>();

			AbstractInsnNode postTypeInstruction = typeInstruction.getNext();
			while (postTypeInstruction != null) {
				if (
						postTypeInstruction instanceof MethodInsnNode invocation
							&& invocation.name.equals("<init>")
							&& invocation.owner.equals(typeInstruction.desc)
				) {
					this.visitor.localConstructorsByOwner.get(invocation.owner).stream()
							.filter(constructor ->
								!constructorParamsBySyntheticFieldByConstructor.containsKey(constructor)
									&& constructor.desc.equals(invocation.desc)
							)
							.findAny()
							.ifPresent(constructor -> {
								final Map<FieldInsnNode, VarInsnNode> constructorParamsBySyntheticField =
										constructorParamsBySyntheticFieldByConstructor
											.computeIfAbsent(constructor, c -> {
												AbstractInsnNode constructorInstruction = c.instructions.getLast();
												FieldInsnNode fieldPut = null;
												final Map<FieldInsnNode, VarInsnNode> paramsBySyntheticField =
													new HashMap<>();
												while (constructorInstruction != null) {
													if (
															constructorInstruction
																instanceof FieldInsnNode fieldInstruction
																&& fieldInstruction.getOpcode() == Opcodes.PUTFIELD
																&& this.visitor.localSyntheticFieldsByOwner
																.get(fieldInstruction.owner)
																.stream()
																.anyMatch(field ->
																	field.name.equals(fieldInstruction.name)
																		&& field.desc.equals(fieldInstruction.desc)
																)
													) {
														fieldPut = fieldInstruction;
													} else if (fieldPut != null) {
														if (
																constructorInstruction
																	instanceof VarInsnNode varInstruction
																	&& varInstruction.getOpcode() >= Opcodes.ILOAD
																	&& varInstruction.getOpcode() <= Opcodes.SALOAD
																	// TODO account for non/static
																	//  and double-size params
																	&& varInstruction.var < c.parameters.size()
														) {
															paramsBySyntheticField.put(fieldPut, varInstruction);
														}

														fieldPut = null;
													}

													constructorInstruction = constructorInstruction.getPrevious();
												}

												return paramsBySyntheticField;
											});

								// for (final VarInsnNode param : params) {
								// 	param.
								// }
								//
								// constructorParamsBySyntheticField.forEach((field, constructorParam) -> {
								// 	constructorParam.
								// });
							});

					break;
				} else if (
						postTypeInstruction instanceof VarInsnNode variable
							// TODO account for non/static and double-size params
							&& variable.var < method.parameters.size()
				) {
					// TODO map method params to constructor call params
					//  - are constructor invocations always preceded by a number of loads+invokes equal to their param count?
					//  - check what QEP delegate params does
					params.add(variable);
				}

				postTypeInstruction = postTypeInstruction.getNext();
			}

			// this.linkedFieldsByParam
		});
	}

	@Override
	public String getId() {
		return ID;
	}
}
