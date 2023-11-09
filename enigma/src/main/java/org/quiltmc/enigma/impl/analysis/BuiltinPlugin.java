package org.quiltmc.enigma.impl.analysis;

import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.analysis.index.jar.BridgeMethodIndex;
import org.quiltmc.enigma.api.EnigmaPlugin;
import org.quiltmc.enigma.api.EnigmaPluginContext;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.service.JarIndexerService;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma.api.source.DecompilerService;
import org.quiltmc.enigma.api.source.Decompilers;
import org.quiltmc.enigma.api.source.RenamableTokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.jetbrains.java.decompiler.util.Pair;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class BuiltinPlugin implements EnigmaPlugin {
	@Override
	public void init(EnigmaPluginContext ctx) {
		this.registerEnumNamingService(ctx);
		this.registerSpecializedMethodNamingService(ctx);
		this.registerDecompilerServices(ctx);
	}

	private void registerEnumNamingService(EnigmaPluginContext ctx) {
		final Map<Entry<?>, String> names = new HashMap<>();
		final EnumFieldNameFindingVisitor visitor = new EnumFieldNameFindingVisitor(names);

		ctx.registerService("enigma:enum_initializer_indexer", JarIndexerService.TYPE, ctx1 -> JarIndexerService.fromVisitor(visitor));

		// todo these might toss javadoc?
		String id = "enigma:enum_name_proposer";
		ctx.registerService(id, NameProposalService.TYPE, ctx1 -> new NameProposalFunction() {
			@Override
			public Map<Entry<?>, EntryMapping> apply(JarIndex jarIndex) {
				Map<Entry<?>, EntryMapping> mappings = new HashMap<>();

				jarIndex.getIndex(EntryIndex.class).getFields().forEach(field -> {
					if (names.containsKey(field)) {
						mappings.put(field, new EntryMapping(names.get(field), null, RenamableTokenType.JAR_PROPOSED, id));
					}
				});

				return mappings;
			}
		});
	}

	private void registerSpecializedMethodNamingService(EnigmaPluginContext ctx) {
		String id = "enigma:specialized_method_name_proposer";

		ctx.registerService(id, NameProposalService.TYPE, ctx1 -> new NameProposalFunction() {
				@Override
				public Map<Entry<?>, EntryMapping> apply(JarIndex jarIndex) {
					BridgeMethodIndex bridgeMethodIndex = jarIndex.getIndex(BridgeMethodIndex.class);
					Map<Entry<?>, EntryMapping> mappings = new HashMap<>();

					bridgeMethodIndex.getSpecializedToBridge().forEach((specialized, bridge) -> {
						EntryMapping mapping = new EntryMapping(bridge.getName(), null, RenamableTokenType.JAR_PROPOSED, id);

						mappings.put(specialized, mapping);
						// IndexEntryResolver#resolveEntry can return the bridge method, so we can just use the name
						mappings.put(bridge, mapping);
					});

					return mappings;
				}
		});
	}

	private void registerDecompilerServices(EnigmaPluginContext ctx) {
		ctx.registerService("enigma:vineflower", DecompilerService.TYPE, ctx1 -> Decompilers.VINEFLOWER);
		ctx.registerService("enigma:procyon", DecompilerService.TYPE, ctx1 -> Decompilers.PROCYON);
		ctx.registerService("enigma:cfr", DecompilerService.TYPE, ctx1 -> Decompilers.CFR);
		ctx.registerService("enigma:bytecode", DecompilerService.TYPE, ctx1 -> Decompilers.BYTECODE);
	}

	private static abstract class NameProposalFunction implements NameProposalService, Function<JarIndex, Map<Entry<?>, EntryMapping>> {
		@Override
		public Map<Entry<?>, EntryMapping> getProposedNames(JarIndex index) {
			return this.apply(index);
		}

		@Override
		public Map<Entry<?>, EntryMapping> getDynamicProposedNames(EntryRemapper remapper, @Nullable Entry<?> obfEntry, @Nullable EntryMapping oldMapping, @Nullable EntryMapping newMapping) {
			return null;
		}
	}

	private static final class EnumFieldNameFindingVisitor extends ClassVisitor {
		private ClassEntry clazz;
		private String className;
		private final Map<Entry<?>, String> mappings;
		private final Set<org.jetbrains.java.decompiler.util.Pair<String, String>> enumFields = new HashSet<>();
		private final List<MethodNode> classInits = new ArrayList<>();

		EnumFieldNameFindingVisitor(Map<Entry<?>, String> mappings) {
			super(Enigma.ASM_VERSION);
			this.mappings = mappings;
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			super.visit(version, access, name, signature, superName, interfaces);
			this.className = name;
			this.clazz = new ClassEntry(name);
			this.enumFields.clear();
			this.classInits.clear();
		}

		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			if ((access & Opcodes.ACC_ENUM) != 0
					&& !this.enumFields.add(Pair.of(name, descriptor))) {
				throw new IllegalArgumentException("Found two enum fields with the same name \"" + name + "\" and desc \"" + descriptor + "\"!");
			}

			return super.visitField(access, name, descriptor, signature, value);
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
			if ("<clinit>".equals(name)) {
				MethodNode node = new MethodNode(this.api, access, name, descriptor, signature, exceptions);
				this.classInits.add(node);
				return node;
			}

			return super.visitMethod(access, name, descriptor, signature, exceptions);
		}

		@Override
		public void visitEnd() {
			super.visitEnd();
			try {
				this.collectResults();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		private void collectResults() throws Exception {
			String owner = this.className;
			Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());

			for (MethodNode mn : this.classInits) {
				Frame<SourceValue>[] frames = analyzer.analyze(this.className, mn);

				InsnList instrs = mn.instructions;
				for (int i = 1; i < instrs.size(); i++) {
					AbstractInsnNode instr1 = instrs.get(i - 1);
					AbstractInsnNode instr2 = instrs.get(i);
					String s = null;

					if (instr2.getOpcode() == Opcodes.PUTSTATIC
							&& ((FieldInsnNode) instr2).owner.equals(owner)
							&& this.enumFields.contains(Pair.of(((FieldInsnNode) instr2).name, ((FieldInsnNode) instr2).desc))
							&& instr1.getOpcode() == Opcodes.INVOKESPECIAL
							&& "<init>".equals(((MethodInsnNode) instr1).name)) {
						for (int j = 0; j < frames[i - 1].getStackSize(); j++) {
							SourceValue sv = frames[i - 1].getStack(j);
							for (AbstractInsnNode ci : sv.insns) {
								if (ci instanceof LdcInsnNode insnNode && insnNode.cst instanceof String && s == null) {
									s = (String) (insnNode.cst);
								}
							}
						}
					}

					if (s != null) {
						this.mappings.put(new FieldEntry(this.clazz, ((FieldInsnNode) instr2).name, new TypeDescriptor(((FieldInsnNode) instr2).desc)), s);
					}

					// report otherwise?
				}
			}
		}
	}
}
