package org.quiltmc.enigma.translation.mapping;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.quiltmc.enigma.TestEntryFactory;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.translation.mapping.IndexEntryResolver;
import org.quiltmc.enigma.api.translation.mapping.ResolutionStrategy;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.test.bytecode.ClassNodeBuilder;
import org.quiltmc.enigma.test.bytecode.MethodNodeBuilder;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class IndexEntryResolverTest {
	private static final Map<String, ClassNode> CLASSES = new HashMap<>();
	private static final ClassProvider CLASS_PROVIDER = new ClassProvider() {
		@Nullable
		@Override
		public ClassNode get(String name) {
			return CLASSES.get(name);
		}

		@Override
		public Collection<String> getClassNames() {
			return CLASSES.keySet();
		}
	};

	private static JarIndex index;

	private static IndexEntryResolver resolver;
	@BeforeAll
	public static void beforeAll() {
		index = JarIndex.empty();
		index.indexJar(new HashSet<>(CLASS_PROVIDER.getClassNames()), CLASS_PROVIDER, ProgressListener.createEmpty());
		resolver = new IndexEntryResolver(index);
	}

	private static <E extends Entry<?>> void assertResolvedRoot(E entry, Collection<E> expected) {
		Assertions.assertIterableEquals(expected, resolver.resolveEntry(entry, ResolutionStrategy.RESOLVE_ROOT), "wrong root resolution of " + entry);
	}

	private static <E extends Entry<?>> void assertResolvedClosest(E entry, Collection<E> expected) {
		Assertions.assertIterableEquals(expected, resolver.resolveEntry(entry, ResolutionStrategy.RESOLVE_CLOSEST), "wrong closest resolution of " + entry);
	}

	private static <E extends Entry<?>> void assertResolvedSingle(E entry, E expected) {
		assertResolvedRoot(entry, Collections.singleton(expected));
		assertResolvedClosest(entry, Collections.singleton(expected));
	}

	private static <E extends Entry<?>> void assertResolveIdentity(E entry) {
		assertResolvedSingle(entry, entry);
	}

	@Test
	public void testResolveFields() {
		var baseA = TestEntryFactory.newClass("BaseA");
		var sub1A = TestEntryFactory.newClass("Sub1A");

		var entry = TestEntryFactory.newField(sub1A, "field1", "Z");
		// assertResolveIdentity(entry);
		assertResolvedClosest(entry, Collections.singleton(entry));

		entry = TestEntryFactory.newField(baseA, "field1", "Z");
		assertResolveIdentity(entry);

		entry = TestEntryFactory.newField(sub1A, "field2", "I");
		var expected = TestEntryFactory.newField(baseA, "field2", "I");
		assertResolvedSingle(entry, expected);

		entry = TestEntryFactory.newField(sub1A, "field3", "I");
		assertResolveIdentity(entry);

		// TODO: Non-existing entries should be resolved to empty or the entry singleton??
		// entry = TestEntryFactory.newField(baseA, "field3", "I");

		entry = TestEntryFactory.newField(sub1A, "field4", "J");
		expected = TestEntryFactory.newField(baseA, "field4", "J");
		assertResolvedSingle(entry, expected);

		entry = TestEntryFactory.newField(sub1A, "field4", "B");
		assertResolveIdentity(entry);

		entry = TestEntryFactory.newField(sub1A, "FIELD_5", "I");
		assertResolveIdentity(entry);

		entry = TestEntryFactory.newField(sub1A, "FIELD_6", "B");
		// assertResolveIdentity(entry);

		entry = TestEntryFactory.newField(sub1A, "FIELD_7", "C");
		assertResolveIdentity(entry);

		entry = TestEntryFactory.newField(sub1A, "FIELD_8", "D");
		expected = TestEntryFactory.newField(baseA, "FIELD_8", "D");
		assertResolvedSingle(entry, expected);

		entry = TestEntryFactory.newField(sub1A, "FIELD_9", "D");
		assertResolveIdentity(entry);

		entry = TestEntryFactory.newField(sub1A, "field10", "Z");
		assertResolveIdentity(entry);

		entry = TestEntryFactory.newField(sub1A, "field11", "J");
		assertResolveIdentity(entry);

		entry = TestEntryFactory.newField(sub1A, "field12", "Ljava/lang/String;");
		assertResolveIdentity(entry);

		entry = TestEntryFactory.newField(sub1A, "field13", "S");
		expected = TestEntryFactory.newField(baseA, "field13", "S");
		// assertResolvedSingle(entry, expected);

		entry = TestEntryFactory.newField(sub1A, "field14", "S");
		assertResolveIdentity(entry);
	}

	@Test
	public void testResolveMethods() {
		var baseB = TestEntryFactory.newClass("BaseB");
		var sub1B = TestEntryFactory.newClass("Sub1B");
		var sub1BSub1 = TestEntryFactory.newClass("Sub1BSub1");

		var entry = TestEntryFactory.newMethod(baseB, "foo", "()LBaseB;");
		var baseExpected = entry;
		var expected = baseExpected;
		assertResolvedRoot(entry, Collections.singleton(expected));
		assertResolvedClosest(entry, Collections.singleton(expected));

		entry = TestEntryFactory.newMethod(sub1B, "foo", "()LBaseB;");
		expected = entry;
		assertResolvedRoot(entry, Collections.singleton(baseExpected));
		assertResolvedClosest(entry, Collections.singleton(expected));

		entry = TestEntryFactory.newMethod(sub1B, "foo", "()LSub1B;");
		expected = entry;
		assertResolvedRoot(entry, Collections.singleton(baseExpected));
		assertResolvedClosest(entry, Collections.singleton(expected));

		entry = TestEntryFactory.newMethod(sub1BSub1, "foo", "()LSub1B;");
		assertResolvedRoot(entry, Collections.singleton(baseExpected));
		assertResolvedClosest(entry, Collections.singleton(expected));

		entry = TestEntryFactory.newMethod(sub1BSub1, "foo", "()LBaseB;");
		expected = entry;
		assertResolvedRoot(entry, Collections.singleton(baseExpected));
		assertResolvedClosest(entry, Collections.singleton(expected));

		var baseC = TestEntryFactory.newClass("BaseC");
		var sub1C = TestEntryFactory.newClass("Sub1C");
		var sub2C = TestEntryFactory.newClass("Sub2C");

		entry = TestEntryFactory.newMethod(baseC, "priv", "()I");
		expected = entry;
		assertResolvedRoot(entry, Collections.singleton(expected));
		assertResolvedClosest(entry, Collections.singleton(expected));

		entry = TestEntryFactory.newMethod(baseC, "stat", "()LBaseC;");
		expected = entry;
		assertResolvedRoot(entry, Collections.singleton(expected));
		assertResolvedClosest(entry, Collections.singleton(expected));

		entry = TestEntryFactory.newMethod(sub1C, "priv", "()I");
		expected = entry;
		assertResolvedRoot(entry, Collections.singleton(expected));
		assertResolvedClosest(entry, Collections.singleton(expected));

		entry = TestEntryFactory.newMethod(sub1C, "stat", "()LBaseC;");
		expected = entry;
		assertResolvedRoot(entry, Collections.singleton(expected));
		assertResolvedClosest(entry, Collections.singleton(expected));

		entry = TestEntryFactory.newMethod(sub2C, "priv", "()I");
		expected = entry;
		assertResolvedRoot(entry, Collections.singleton(expected));
		assertResolvedClosest(entry, Collections.singleton(expected));

		entry = TestEntryFactory.newMethod(sub2C, "stat", "()LBaseC;");
		expected = entry;
		assertResolvedRoot(entry, Collections.singleton(expected));
		assertResolvedClosest(entry, Collections.singleton(expected));
	}

	private static void clazz(ClassNode classNode) {
		CLASSES.put(classNode.name, classNode);
	}

	static {
		clazz(ClassNodeBuilder.create("BaseA")
				.field("field1", "Z")
				.field("field2", "I")
				.field("field4", "J")
				.field(Opcodes.ACC_STATIC, "FIELD_5", "I", null, null)
				.field(Opcodes.ACC_STATIC, "FIELD_6", "B", null, null)
				.field("FIELD_7", "C")
				.field(Opcodes.ACC_STATIC, "FIELD_8", "D", null, null)
				.field(Opcodes.ACC_PRIVATE, "field10", "Z", null, null)
				.field(Opcodes.ACC_PRIVATE, "field11", "J", null, null)
				.field("field12", "Ljava/lang/String;")
				.field(Opcodes.ACC_PRIVATE, "field13", "S", null, null)
				.superInit()
				.build());
		clazz(ClassNodeBuilder.create("Sub1A", "BaseA")
				.field("field1", "Z")
				.field("field3", "I")
				.field("field4", "B")
				.field(Opcodes.ACC_STATIC, "FIELD_5", "I", null, null)
				.field("FIELD_6", "B")
				.field(Opcodes.ACC_STATIC, "FIELD_7", "C", null, null)
				.field(Opcodes.ACC_STATIC, "FIELD_9", "D", null, null)
				.field(Opcodes.ACC_PRIVATE, "field10", "Z", null, null)
				.field("field11", "J")
				.field(Opcodes.ACC_PRIVATE, "field12", "Ljava/lang/String;", null, null)
				.field(Opcodes.ACC_PRIVATE, "field14", "S", null, null)
				.superInit()
				.build());

		clazz(ClassNodeBuilder.create("BaseB")
				.superInit()
				.method(MethodNodeBuilder.create("foo", "()LBaseB;")
					.aload(0)
					.insn(Opcodes.ARETURN)
					.build())
				.build());
		clazz(ClassNodeBuilder.create("Sub1B", "BaseB")
				.superInit()
				.method(MethodNodeBuilder.create("foo", "()LSub1B;")
					.aload(0)
					.insn(Opcodes.ARETURN)
					.build())
				.method(MethodNodeBuilder.create(Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PUBLIC, "foo", "()LBaseB;", null, null)
					.aload(0)
					.methodInsn(Opcodes.INVOKEVIRTUAL, "Sub1B", "foo", "()LSub1B;", false)
					.insn(Opcodes.ARETURN)
					.build())
				.build());
		clazz(ClassNodeBuilder.create("Sub1BSub1", "Sub1B")
				.superInit()
				.method(MethodNodeBuilder.create(Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PUBLIC, "foo", "()LBaseB;", null, null)
					.aload(0)
					.methodInsn(Opcodes.INVOKESPECIAL, "Sub1B", "foo", "()LSub1B;", false)
					.insn(Opcodes.ARETURN)
					.build())
				.build());

		clazz(ClassNodeBuilder.create("BaseC")
				.superInit()
				.method(MethodNodeBuilder.create(Opcodes.ACC_PRIVATE, "priv", "()I", null, null)
					.iconst_0()
					.insn(Opcodes.IRETURN)
					.maxs(1, 1)
					.build())
				.method(MethodNodeBuilder.create(Opcodes.ACC_STATIC, "stat", "()LBaseC;", null, null)
					.typeInsn(Opcodes.NEW, "BaseC")
					.insn(Opcodes.DUP)
					.methodInsn(Opcodes.INVOKESPECIAL, "BaseC", "<init>", "()V", false)
					.insn(Opcodes.ARETURN)
					.maxs(0, 2)
					.build())
				.build());
		clazz(ClassNodeBuilder.create("Sub1C", "BaseC")
				.superInit()
				.method(MethodNodeBuilder.create(Opcodes.ACC_PRIVATE, "priv", "()I", null, null)
					.iconst_0()
					.insn(Opcodes.IRETURN)
					.maxs(1, 1)
					.build())
				.method(MethodNodeBuilder.create(Opcodes.ACC_STATIC, "stat", "()LBaseC;", null, null)
					.methodInsn(Opcodes.INVOKESTATIC, "BaseC", "stat", "()LBaseC;", false)
					.insn(Opcodes.ARETURN)
					.maxs(1, 1)
					.build())
				.build());
		clazz(ClassNodeBuilder.create("Sub2C", "BaseC")
				.superInit()
				.method(MethodNodeBuilder.create("priv", "()I")
					.iconst_0()
					.insn(Opcodes.IRETURN)
					.maxs(1, 1)
					.build())
				.method(MethodNodeBuilder.create("stat", "()LBaseC;")
					.methodInsn(Opcodes.INVOKESTATIC, "Sub1C", "stat", "()LBaseC;", false)
					.insn(Opcodes.ARETURN)
					.maxs(1, 1)
					.build())
				.build());
	}
}
