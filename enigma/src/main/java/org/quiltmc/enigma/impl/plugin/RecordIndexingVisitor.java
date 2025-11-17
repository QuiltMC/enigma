package org.quiltmc.enigma.impl.plugin;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.RecordComponentNode;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

// TODO add tests
// TODO javadoc, including getter uncertainty
final class RecordIndexingVisitor extends ClassVisitor {
	private static final int REQUIRED_GETTER_ACCESS = Opcodes.ACC_PUBLIC;
	private static final int ILLEGAL_GETTER_ACCESS = Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_STATIC;

	private static final ImmutableSet<String> ILLEGAL_GETTER_NAMES = ImmutableSet
			.of("clone", "finalize", "getClass", "hashCode", "notify", "notifyAll", "toString", "wait");

	// visitation state fields; cleared in visitEnd()
	private ClassEntry clazz;
	private final Set<RecordComponentNode> recordComponents = new HashSet<>();
	// this is a multimap because inner classes' fields go in the same map as their outer class's
	private final Multimap<String, FieldNode> fieldsByName = HashMultimap.create();
	private final Multimap<String, MethodNode> methodsByDescriptor = HashMultimap.create();

	// index fields; contents publicly queryable
	private final Multimap<ClassEntry, FieldEntry> componentFieldsByClass = HashMultimap.create();
	// holds methods that are at least probably getters for their field keys; superset of definiteComponentGettersByField
	private final BiMap<FieldEntry, MethodEntry> componentGettersByField = HashBiMap.create();
	// holds methods that are definitely the getters for their field keys
	private final BiMap<FieldEntry, MethodEntry> definiteComponentGettersByField = HashBiMap.create();
	// holds methods that are at least probably getters; superset of definiteComponentGettersByClass
	private final Multimap<ClassEntry, MethodEntry> componentGettersByClass = HashMultimap.create();
	// holds methods that are definitely component getters
	private final Multimap<ClassEntry, MethodEntry> definiteComponentGettersByClass = HashMultimap.create();

	RecordIndexingVisitor() {
		super(Enigma.ASM_VERSION);
	}

	@Nullable
	public MethodEntry getComponentGetter(FieldEntry componentField) {
		return this.componentGettersByField.get(componentField);
	}

	@Nullable
	public FieldEntry getComponentField(MethodEntry componentGetter) {
		return this.componentGettersByField.inverse().get(componentGetter);
	}

	// TODO javadoc, prevent directly naming method (always match field)
	@Nullable
	public MethodEntry getDefiniteComponentGetter(FieldEntry componentField) {
		return this.definiteComponentGettersByField.get(componentField);
	}

	// TODO javadoc
	@Nullable
	public FieldEntry getDefiniteComponentField(MethodEntry componentGetter) {
		return this.definiteComponentGettersByField.inverse().get(componentGetter);
	}

	public Stream<FieldEntry> streamComponentFields(ClassEntry recordEntry) {
		return this.componentFieldsByClass.get(recordEntry).stream();
	}

	public Stream<MethodEntry> streamComponentMethods(ClassEntry recordEntry) {
		return this.componentGettersByClass.get(recordEntry).stream();
	}

	// TODO javadoc
	public Stream<MethodEntry> streamDefiniteComponentMethods(ClassEntry recordEntry) {
		return this.definiteComponentGettersByClass.get(recordEntry).stream();
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		this.clazz = (access & Opcodes.ACC_RECORD) != 0 ? new ClassEntry(name) : null;
	}

	@Override
	public RecordComponentVisitor visitRecordComponent(final String name, final String descriptor, final String signature) {
		this.recordComponents.add(new RecordComponentNode(this.api, name, descriptor, signature));
		return super.visitRecordComponent(name, descriptor, signature);
	}

	@Override
	public FieldVisitor visitField(final int access, final String name, final String descriptor, final String signature, final Object value) {
		if (this.clazz != null && ((access & Opcodes.ACC_PRIVATE) != 0) && this.recordComponents.stream().anyMatch(component -> component.name.equals(name))) {
			final FieldNode node = new FieldNode(this.api, access, name, descriptor, signature, value);
			this.fieldsByName.put(node.name, node);
			return node;
		}

		return super.visitField(access, name, descriptor, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
		if (this.clazz != null && ((access & Opcodes.ACC_PUBLIC) != 0)) {
			final MethodNode node = new MethodNode(this.api, access, name, descriptor, signature, exceptions);
			this.methodsByDescriptor.put(node.desc, node);
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
		} finally {
			this.clazz = null;
			this.recordComponents.clear();
			this.fieldsByName.clear();
			this.methodsByDescriptor.clear();
		}
	}

	private void collectResults() {
		if (this.clazz == null) {
			return;
		}

		this.recordComponents.stream()
				.map(component -> this.fieldsByName.get(component.name).stream()
					.filter(field -> field.desc.equals(component.descriptor))
					.findAny()
					.orElseThrow(() -> new IllegalStateException(
						"Field not found for record component: " + component.name
					))
				)
				.forEach(field -> {
					final List<MethodNode> potentialGetters = this.methodsByDescriptor
							.get("()" + field.desc)
							.stream()
							.filter(method -> (method.access & REQUIRED_GETTER_ACCESS) == REQUIRED_GETTER_ACCESS)
							.filter(method -> (method.access & ILLEGAL_GETTER_ACCESS) == 0)
							.filter(method -> !ILLEGAL_GETTER_NAMES.contains(method.name))
							.toList();

					if (potentialGetters.isEmpty()) {
						throw new IllegalStateException("No potential getters for field: " + field);
					} else {
						final FieldEntry fieldEntry =
							new FieldEntry(this.clazz, field.name, new TypeDescriptor(field.desc));
						// index the field even if a corresponding getter can't be found
						this.componentFieldsByClass.put(this.clazz, fieldEntry);

						if (potentialGetters.size() == 1) {
							this.indexGetter(potentialGetters.get(0), fieldEntry, true);
						} else {
							// If there are multiple methods with the getter's descriptor and access, it's impossible to
							// tell which is the getter because obfuscation can mismatch getter/field names.
							// This matching produces as few false-positives as possible by matching name, descriptor,
							// and the bytecode of a default (non-overriden) getter method.
							// It can still give a false-positive if a non-getter method's obfuscated name matches the
							// field's, and that non-getter the has expected descriptor and bytecode of the getter.
							// It also has false-negatives for getter overrides with non-default bytecode.
							potentialGetters.stream()
									.filter(method -> method.name.equals(field.name))
									// match bytecode to exact expected bytecode for a getter
									// only check important instructions (ignore new frame instructions, etc.)
									.filter(method -> {
										final InsnList instructions = method.instructions;
										return instructions.size() == 6
											&& instructions.get(2).getOpcode() == Opcodes.ALOAD
											&& instructions.get(3) instanceof FieldInsnNode fieldInsn
											&& fieldInsn.getOpcode() == Opcodes.GETFIELD
											&& fieldInsn.owner.equals(this.clazz.getFullName())
											&& fieldInsn.desc.equals(field.desc)
											&& fieldInsn.name.equals(field.name)
											&& instructions.get(4).getOpcode() >= Opcodes.IRETURN
											&& instructions.get(4).getOpcode() <= Opcodes.ARETURN;
									})
									.findAny()
									.ifPresent(getter -> this.indexGetter(getter, fieldEntry, false));
						}
					}
				});
	}

	private void indexGetter(MethodNode getterNode, FieldEntry fieldEntry, boolean definite) {
		final MethodEntry getterEntry =
			new MethodEntry(this.clazz, getterNode.name, new MethodDescriptor(getterNode.desc));

		this.componentGettersByField.put(fieldEntry, getterEntry);
		this.componentGettersByClass.put(this.clazz, getterEntry);

		if (definite) {
			this.definiteComponentGettersByField.put(fieldEntry, getterEntry);
			this.definiteComponentGettersByClass.put(this.clazz, getterEntry);
		}
	}
}

