package org.quiltmc.enigma.impl.plugin;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.class_provider.ProjectClassProvider;
import org.quiltmc.enigma.api.service.JarIndexerService;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.util.AsmUtil;
import org.quiltmc.enigma.util.LocalVariableInterpreter;
import org.quiltmc.enigma.util.LocalVariableValue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class ParamSyntheticFieldIndexingService implements JarIndexerService, Opcodes {
	public static final String ID = "enigma:param_synthetic_field_indexer";

	private final ParamSyntheticFieldIndexingVisitor visitor;
	private final BiMap<LocalVariableEntry, FieldEntry> linkedFieldsByParam = HashBiMap.create();

	/**
	 * invoker param -> invoked param
	 */
	private final Map<LocalVariableEntry, LocalVariableEntry> linkedParameters = new HashMap<>();
	// Parameters used more than once
	private final Set<LocalVariableEntry> invalidParameters = new HashSet<>();

	ParamSyntheticFieldIndexingService() {
		this.visitor = new ParamSyntheticFieldIndexingVisitor();
	}

	static boolean isSameMethod(ClassNode owner, MethodNode node, MethodInsnNode methodInstruction) {
		return node.name.equals(methodInstruction.name)
				&& node.desc.equals(methodInstruction.desc) && owner.name.equals(methodInstruction.owner);
	}

	@Override
	public void acceptJar(Set<String> scope, ProjectClassProvider classProvider, JarIndex jarIndex) {
		for (final String className : scope) {
			final ClassNode node = classProvider.get(className);
			if (node != null) {
				node.accept(this.visitor);
			}
		}

		final var syntheticFieldsByConstructorParamByConstructor =
			new HashMap<MethodNode, Map<LocalVariableEntry, FieldEntry>>();

		this.visitor.localTypeInstructionsByMethodByOwner
				.forEach((owner, localTypeInstructions) -> localTypeInstructions
					.forEach((method, typeInstructionIndex) -> {
						// population of linkedParameters based on QEP's DelegateParametersIndex by IotaBread

						if (method.parameters == null || method.parameters.isEmpty()) {
							// no parameter info
							return;
						}

						final Frame<LocalVariableValue>[] frames;
						try {
							frames = new Analyzer<>(new LocalVariableInterpreter())
									.analyze(owner, method);
						} catch (AnalyzerException e) {
							throw new RuntimeException(e);
						}

						final MethodEntry methodEntry = MethodEntry.parse(owner, method.name, method.desc);

						final var paramsByTarget = new HashMap<LocalVariableEntry, LocalVariableEntry>();

						for (int instructionindex = typeInstructionIndex.index() + 1;
								instructionindex < method.instructions.size();
								instructionindex++
						) {
							final AbstractInsnNode postTypeInstruction = method.instructions.get(instructionindex);
							if (
									postTypeInstruction instanceof MethodInsnNode invocation
										&& invocation.name.equals("<init>")
										&& invocation.owner.equals(typeInstructionIndex.typeInstruction().desc)
							) {
								final MethodEntry invokedEntry = MethodEntry
										.parse(invocation.owner, invocation.name, invocation.desc);

								final Frame<LocalVariableValue> frame =
										frames[instructionindex];
								final boolean isStatic = invocation.getOpcode() == INVOKESTATIC;
								final Type invokedDesc = Type.getMethodType(invocation.desc);
								int localIndex = invokedDesc.getArgumentsAndReturnSizes() >> 2;
								if (isStatic) {
									localIndex--;
								}

								// Check each of the arguments passed to the invocation
								for (int invokeArgIndex = invokedDesc.getArgumentCount() - 1;
										invokeArgIndex >= 0;
										invokeArgIndex--
								) {
									final LocalVariableValue value = frame.pop();
									localIndex -= value.getSize();

									// If one of the passed arguments is a parameter of the original method, save it
									if (value.parameter()) {
										// Skip synthetic parameters
										final int index = org.quiltmc.enigma.util.AsmUtil.getLocalIndex(method, value.local());
										if (AsmUtil.matchAccess(method.parameters.get(index), ACC_SYNTHETIC)) {
											continue;
										}

										// Skip invalid parameters
										final var paramEntry = new LocalVariableEntry(methodEntry, value.local());
										if (this.invalidParameters.contains(paramEntry)) {
											continue;
										}

										// If another entry was linked to the same one inside this method,
										// remove it and skip this one
										final var targetEntry = new LocalVariableEntry(invokedEntry, localIndex);
										if (paramsByTarget.containsKey(targetEntry)) {
											final LocalVariableEntry otherParam = paramsByTarget.get(targetEntry);

											if (otherParam != null && !paramEntry.equals(otherParam)) {
												paramsByTarget.put(targetEntry, null);
												this.linkedParameters.remove(otherParam);
											}

											continue;
										}

										if (this.tryLink(paramEntry, targetEntry)) {
											paramsByTarget.put(targetEntry, paramEntry);
										}
									}
								}

								this.visitor.localConstructorsByOwner.get(invocation.owner).stream()
										.filter(constructor ->
											!syntheticFieldsByConstructorParamByConstructor.containsKey(constructor)
												&& constructor.desc.equals(invocation.desc)
										)
										.findAny()
										.ifPresent(constructor -> {
											final Map<LocalVariableEntry, FieldEntry> syntheticFieldsByConstructorParams =
													syntheticFieldsByConstructorParamByConstructor
														.computeIfAbsent(constructor, c -> {
															AbstractInsnNode constructorInstruction =
																	c.instructions.getLast();
															FieldInsnNode fieldPut = null;
															final Map<LocalVariableEntry, FieldEntry>
																	paramsBySyntheticField = new HashMap<>();
															while (constructorInstruction != null) {
																if (
																		constructorInstruction
																			instanceof FieldInsnNode fieldInstruction
																			&& fieldInstruction.getOpcode() == PUTFIELD
																			&& this.visitor.localSyntheticFieldsByOwner
																			.get(fieldInstruction.owner)
																			.stream()
																			.anyMatch(field ->
																				field.name.equals(fieldInstruction.name)
																					&& field.desc
																						.equals(fieldInstruction.desc)
																			)
																) {
																	fieldPut = fieldInstruction;
																} else if (fieldPut != null) {
																	if (
																			constructorInstruction
																				instanceof VarInsnNode varInstruction
																				&& varInstruction.getOpcode() >= ILOAD
																				&& varInstruction.getOpcode() <= SALOAD
																				// TODO account for non/static
																				//  and double-size params
																				&& varInstruction.var
																					< c.parameters.size()
																	) {
																		paramsBySyntheticField.put(
																			// TODO +1 for this??
																			new LocalVariableEntry(new MethodEntry(new ClassEntry(invocation.owner), constructor.name, new MethodDescriptor(constructor.desc)), varInstruction.var + 1),
																			new FieldEntry(new ClassEntry(fieldPut.owner), fieldPut.name, new TypeDescriptor(fieldPut.desc))
																		);
																	}

																	fieldPut = null;
																}

																constructorInstruction =
																	constructorInstruction.getPrevious();
															}

															return paramsBySyntheticField;
														});

											this.linkedParameters.forEach((invokerParam, invokedParam) -> {
												final FieldEntry field = syntheticFieldsByConstructorParams.get(invokedParam);
												if (field != null) {
													this.linkedFieldsByParam.put(invokerParam, field);
												}
											});
										});

								break;
							}
						}
					})
		);
	}

	@Override
	public String getId() {
		return ID;
	}

	public void forEachSyntheticFieldLinkedParam(BiConsumer<LocalVariableEntry, FieldEntry> action) {
		this.linkedFieldsByParam.forEach(action);
	}

	public Stream<Map.Entry<LocalVariableEntry, FieldEntry>> streamSyntheticFieldLinkedParams() {
		return this.linkedFieldsByParam.entrySet().stream();
	}

	@Nullable
	public FieldEntry getLinkedSyntheticField(LocalVariableEntry local) {
		return this.linkedFieldsByParam.get(local);
	}

	@Nullable
	public LocalVariableEntry getLinkedParam(FieldEntry syntheticField) {
		return this.linkedFieldsByParam.inverse().get(syntheticField);
	}

	private boolean tryLink(LocalVariableEntry paramEntry, LocalVariableEntry targetEntry) {
		if (paramEntry.equals(targetEntry)) {
			throw new IllegalArgumentException("Can't link a parameter to itself!");
		}

		if (this.linkedParameters.containsKey(paramEntry)) {
			// If the argument passed was already used somewhere else, invalidate it
			if (this.linkedParameters.get(paramEntry) != targetEntry) {
				this.invalidate(paramEntry);
			}

			return false;
		}

		this.linkedParameters.put(paramEntry, targetEntry);
		return true;
	}

	private void invalidate(LocalVariableEntry paramEntry) {
		this.invalidParameters.add(paramEntry);

		this.linkedParameters.remove(paramEntry);
	}
}
