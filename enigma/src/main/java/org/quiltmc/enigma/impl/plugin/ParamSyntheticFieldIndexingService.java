package org.quiltmc.enigma.impl.plugin;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
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
import org.quiltmc.enigma.util.LocalVariableInterpreter;
import org.quiltmc.enigma.util.LocalVariableValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class ParamSyntheticFieldIndexingService implements JarIndexerService, Opcodes {
	public static final String ID = "enigma:param_synthetic_field_indexer";

	private final ParamSyntheticFieldIndexingVisitor visitor;
	private final BiMap<LocalVariableEntry, FieldEntry> linkedFieldsByParam = HashBiMap.create();
	private final BiMap<LocalVariableEntry, LocalVariableEntry> linkedFakeLocalsByParam = HashBiMap.create();

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

		final Map<MethodNode, Map<LocalVariableEntry, FieldEntry>> syntheticFieldsByParamByConstructor =
			new HashMap<>();

		final Map<LocalVariableEntry, LocalVariableEntry> invokedByInvokerParams = new HashMap<>();

		this.visitor.localTypeInstructionsByMethodByOwner.forEach((owner, typeInstructions) -> typeInstructions
				.forEach((method, typeInstructionIndex) -> {
					// population of invokedByInvokerParams based on QEP's DelegateParametersIndex by IotaBread

					if (method.parameters == null || method.parameters.isEmpty()) {
						// no parameter info
						return;
					}

					final Frame<LocalVariableValue>[] frames;
					try {
						frames = new Analyzer<>(LocalVariableInterpreter.INSTANCE).analyze(owner, method);
					} catch (AnalyzerException e) {
						throw new RuntimeException(e);
					}

					final MethodEntry methodEntry = MethodEntry.parse(owner, method.name, method.desc);

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
							final MethodNode constructor = this.visitor.localConstructorsByDescByOwner
									.getOrDefault(invocation.owner, Map.of())
									.get(invocation.desc);
							if (constructor != null) {
								final MethodEntry constructorEntry = MethodEntry
										.parse(invocation.owner, invocation.name, invocation.desc);

								invokedByInvokerParams.putAll(collectLinkedParams(
										methodEntry, constructorEntry,
										invocation, frames[instructionindex]
								));

								final Map<LocalVariableEntry, FieldEntry> syntheticFieldsByParam =
										syntheticFieldsByParamByConstructor.computeIfAbsent(constructor, c ->
											this.buildSyntheticFieldsByConstructorParam(c, constructorEntry)
										);

								invokedByInvokerParams.forEach((invokerParam, invokedParam) -> {
									// TODO decompilers hide the actual field and treat them as out-of-param-bounds locals instead
									//  try to match compiler's fake locals in addition to storing the fields
									final FieldEntry field = syntheticFieldsByParam.get(invokedParam);
									if (field != null) {
										this.linkedFieldsByParam.put(invokerParam, field);

										this.visitor.localSyntheticFieldOffsetsByGettersByOwner
												.getOrDefault(invocation.owner, Map.of())
												.entrySet()
												.stream()
												.filter(entry -> {
													final FieldNode fieldNode = entry.getValue().field();
													return fieldNode.name.equals(field.getName())
															&& fieldNode.desc.equals(field.getDesc().toString());
												})
												.findAny()
												.ifPresent(entry -> {
													final MethodNode getter = entry.getKey();

													this.linkedFakeLocalsByParam.put(
															invokerParam,
															new LocalVariableEntry(
																new MethodEntry(
																	field.getParent(), getter.name,
																	new MethodDescriptor(getter.desc)
																),
																// TODO see if maxLocals is always the right place to start
																getter.maxLocals + entry.getValue().indexOffset()
															)
													);
												});
									}
								});
							}

							break;
						}
					}
				})
		);
	}

	private static Map<LocalVariableEntry, LocalVariableEntry> collectLinkedParams(
			MethodEntry invokerEntry, MethodEntry invokedEntry,
			MethodInsnNode invocation, Frame<LocalVariableValue> frame
	) {
		final boolean isStatic = invocation.getOpcode() == INVOKESTATIC;
		final Type invokedDesc = Type.getMethodType(invocation.desc);
		int invokedLocalIndex = invokedDesc.getArgumentsAndReturnSizes() >> 2;
		if (isStatic) {
			invokedLocalIndex--;
		}

		final Map<LocalVariableEntry, LocalVariableEntry> invokedByInvokerParams = new HashMap<>();
		// Check each of the arguments passed to the invocation
		for (int invokeArgIndex = invokedDesc.getArgumentCount() - 1;
				invokeArgIndex >= 0;
				invokeArgIndex--
		) {
			final LocalVariableValue invokerValue = frame.pop();
			invokedLocalIndex -= invokerValue.getSize();

			// If one of the passed arguments is a parameter of the original method, save it
			if (invokerValue.parameter()) {
				invokedByInvokerParams.put(
					new LocalVariableEntry(invokerEntry, invokerValue.local()),
					new LocalVariableEntry(invokedEntry, invokedLocalIndex)
				);
			}
		}

		return invokedByInvokerParams;
	}

	private Map<LocalVariableEntry, FieldEntry> buildSyntheticFieldsByConstructorParam(
			MethodNode constructor, MethodEntry constructorEntry
	) {
		AbstractInsnNode constructorInstruction = constructor.instructions.getLast();
		FieldInsnNode fieldPut = null;
		final Map<LocalVariableEntry, FieldEntry> paramsBySyntheticField = new HashMap<>();
		while (constructorInstruction != null) {
			if (
					constructorInstruction
						instanceof FieldInsnNode fieldInstruction
						&& fieldInstruction.getOpcode() == PUTFIELD
						&& this.visitor.localSyntheticFieldsByDescByNameByOwner
							.getOrDefault(fieldInstruction.owner, Map.of())
							.getOrDefault(fieldInstruction.name, Map.of())
							.containsKey(fieldInstruction.desc)
			) {
				fieldPut = fieldInstruction;
			} else if (fieldPut != null) {
				if (
						constructorInstruction instanceof VarInsnNode varInstruction
							&& varInstruction.getOpcode() >= ILOAD
							&& varInstruction.getOpcode() <= SALOAD
							// +1 to size for this
							// TODO double-size params?
							&& varInstruction.var < constructor.parameters.size() + 1
				) {
					paramsBySyntheticField.put(
						new LocalVariableEntry(constructorEntry, varInstruction.var),
						new FieldEntry(new ClassEntry(fieldPut.owner), fieldPut.name, new TypeDescriptor(fieldPut.desc))
					);
				}

				fieldPut = null;
			}

			constructorInstruction = constructorInstruction.getPrevious();
		}

		return paramsBySyntheticField;
	}

	@Override
	public String getId() {
		return ID;
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

	public Stream<Map.Entry<LocalVariableEntry, LocalVariableEntry>> streamFakeLocalLinkedParams() {
		return this.linkedFakeLocalsByParam.entrySet().stream();
	}

	@Nullable
	public LocalVariableEntry getFakeLocal(LocalVariableEntry param) {
		return this.linkedFakeLocalsByParam.get(param);
	}

	@Nullable
	public LocalVariableEntry getLinkedParam(LocalVariableEntry fakeLocal) {
		return this.linkedFakeLocalsByParam.inverse().get(fakeLocal);
	}
}
