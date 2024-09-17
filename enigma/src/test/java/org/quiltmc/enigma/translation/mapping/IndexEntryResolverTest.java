package org.quiltmc.enigma.translation.mapping;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.quiltmc.enigma.TestEntryFactory;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.analysis.index.jar.MainJarIndex;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.class_provider.ProjectClassProvider;
import org.quiltmc.enigma.api.translation.mapping.IndexEntryResolver;
import org.quiltmc.enigma.api.translation.mapping.ResolutionStrategy;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.test.bytecode.ClassNodeBuilder;
import org.quiltmc.enigma.test.bytecode.MethodNodeBuilder;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
		index = MainJarIndex.empty();
		index.indexJar(new ProjectClassProvider(CLASS_PROVIDER, null), ProgressListener.createEmpty());
		resolver = new IndexEntryResolver(index);
	}

	private static <E extends Entry<?>> void assertNotResolved(E entry) {
		Assertions.assertTrue(resolver.resolveEntry(entry, ResolutionStrategy.RESOLVE_ROOT).isEmpty(), "Entry " + entry + " should not be resolved");
		Assertions.assertTrue(resolver.resolveEntry(entry, ResolutionStrategy.RESOLVE_CLOSEST).isEmpty(), "Entry " + entry + " should not be resolved");
	}

	@SafeVarargs
	private static <E extends Entry<?>> void assertResolvedRoot(E entry, E... expected) {
		Assertions.assertIterableEquals(List.of(expected), resolver.resolveEntry(entry, ResolutionStrategy.RESOLVE_ROOT), "wrong root resolution of " + entry);
	}

	@SafeVarargs
	private static <E extends Entry<?>> void assertResolvedClosest(E entry, E... expected) {
		Assertions.assertIterableEquals(List.of(expected), resolver.resolveEntry(entry, ResolutionStrategy.RESOLVE_CLOSEST), "wrong closest resolution of " + entry);
	}

	private static <E extends Entry<?>> void assertResolvedSingle(E entry, E expected) {
		assertResolvedRoot(entry, expected);
		assertResolvedClosest(entry, expected);
	}

	private static <E extends Entry<?>> void assertResolveIdentity(E entry) {
		assertResolvedSingle(entry, entry);
	}

	private static <E extends Entry<?>> void assertExists(E entry) {
		Assertions.assertTrue(index.getIndex(EntryIndex.class).hasEntry(entry), "Entry " + entry + " doesn't exist");
	}

	@Test
	public void testResolveFields() {
		var entryIndex = index.getIndex(EntryIndex.class);

		for (FieldEntry field : entryIndex.getFields()) {
			// If a field exists, it *must* resolve to itself
			assertResolveIdentity(field);
		}
	}

	@Test
	public void testResolveSpecificFields() {
		var baseA = TestEntryFactory.newClass("BaseA");
		var sub1A = TestEntryFactory.newClass("Sub1A");

		var entry = TestEntryFactory.newField(sub1A, "field2", "I");
		var expected = TestEntryFactory.newField(baseA, "field2", "I");
		assertResolvedSingle(entry, expected);

		entry = TestEntryFactory.newField(baseA, "field3", "I");
		assertNotResolved(entry);

		entry = TestEntryFactory.newField(sub1A, "field4", "J");
		expected = TestEntryFactory.newField(baseA, "field4", "J");
		assertResolvedSingle(entry, expected);

		entry = TestEntryFactory.newField(sub1A, "FIELD_8", "D");
		expected = TestEntryFactory.newField(baseA, "FIELD_8", "D");
		assertResolvedSingle(entry, expected);

		entry = TestEntryFactory.newField(baseA, "FIELD_9", "D");
		assertNotResolved(entry);

		entry = TestEntryFactory.newField(sub1A, "field13", "S");
		assertNotResolved(entry);

		entry = TestEntryFactory.newField(baseA, "field14", "F");
		assertNotResolved(entry);
	}

	@Test
	public void testResolveMethods() {
		var entryIndex = index.getIndex(EntryIndex.class);

		for (MethodEntry method : entryIndex.getMethods()) {
			// An existing method *must* resolve to other existing methods
			var resolved = resolver.resolveEntry(method, ResolutionStrategy.RESOLVE_ROOT);
			resolved.forEach(IndexEntryResolverTest::assertExists);
			resolved = resolver.resolveEntry(method, ResolutionStrategy.RESOLVE_CLOSEST);
			resolved.forEach(IndexEntryResolverTest::assertExists);
		}
	}

	@Test
	public void testResolveSpecificMethods() {
		var baseB = TestEntryFactory.newClass("BaseB");
		var sub1B = TestEntryFactory.newClass("Sub1B");
		var sub1BSub1 = TestEntryFactory.newClass("Sub1BSub1");

		var entry = TestEntryFactory.newMethod(baseB, "foo", "()LBaseB;");
		var baseExpected = entry;
		assertResolveIdentity(entry);

		entry = TestEntryFactory.newMethod(sub1B, "foo", "()LBaseB;");
		var expected = entry;
		assertResolvedRoot(entry, baseExpected);
		assertResolvedClosest(entry, expected);

		entry = TestEntryFactory.newMethod(sub1B, "foo", "()LSub1B;");
		expected = entry;
		assertResolvedRoot(entry, baseExpected);
		assertResolvedClosest(entry, expected);

		entry = TestEntryFactory.newMethod(sub1BSub1, "foo", "()LSub1B;");
		assertResolvedRoot(entry, baseExpected);
		assertResolvedClosest(entry, expected);

		entry = TestEntryFactory.newMethod(sub1BSub1, "foo", "()LBaseB;");
		expected = entry;
		assertResolvedRoot(entry, baseExpected);
		assertResolvedClosest(entry, expected);

		entry = TestEntryFactory.newMethod(baseB, "foo", "()LSub1B;");
		assertNotResolved(entry);

		entry = TestEntryFactory.newMethod(baseB, "foo", "()LSub1BSub1;");
		assertNotResolved(entry);

		entry = TestEntryFactory.newMethod(sub1B, "bar", "()V");
		assertNotResolved(entry);

		var baseC = TestEntryFactory.newClass("BaseC");
		var sub1C = TestEntryFactory.newClass("Sub1C");
		var sub2C = TestEntryFactory.newClass("Sub2C");

		entry = TestEntryFactory.newMethod(baseC, "priv", "()I");
		assertResolveIdentity(entry);

		entry = TestEntryFactory.newMethod(baseC, "stat", "()LBaseC;");
		assertResolveIdentity(entry);

		entry = TestEntryFactory.newMethod(sub1C, "priv", "()I");
		assertResolveIdentity(entry);

		entry = TestEntryFactory.newMethod(sub1C, "stat", "()LBaseC;");
		assertResolveIdentity(entry);

		entry = TestEntryFactory.newMethod(sub2C, "priv", "()I");
		assertResolveIdentity(entry);

		entry = TestEntryFactory.newMethod(sub2C, "stat", "()LBaseC;");
		assertResolveIdentity(entry);
	}

	private static void clazz(ClassNode classNode) {
		CLASSES.put(classNode.name, classNode);
	}

	static {
		clazz(ClassNodeBuilder.create("BaseA")
				.field("field1", "Z")
				.field("field2", "I")
				.field("field4", "J")
				.field(Opcodes.ACC_STATIC, "FIELD_5", "I")
				.field(Opcodes.ACC_STATIC, "FIELD_6", "B")
				.field("FIELD_7", "C")
				.field(Opcodes.ACC_STATIC, "FIELD_8", "D")
				.field(Opcodes.ACC_PRIVATE, "field10", "Z")
				.field(Opcodes.ACC_PRIVATE, "field11", "J")
				.field("field12", "Ljava/lang/String;")
				.field(Opcodes.ACC_PRIVATE, "field13", "S")
				.superInit()
				.build());
		clazz(ClassNodeBuilder.create("Sub1A", "BaseA")
				.field("field1", "Z")
				.field("field3", "I")
				.field("field4", "B")
				.field(Opcodes.ACC_STATIC, "FIELD_5", "I")
				.field("FIELD_6", "B")
				.field(Opcodes.ACC_STATIC, "FIELD_7", "C")
				.field(Opcodes.ACC_STATIC, "FIELD_9", "D")
				.field(Opcodes.ACC_PRIVATE, "field10", "Z")
				.field("field11", "J")
				.field(Opcodes.ACC_PRIVATE, "field12", "Ljava/lang/String;")
				.field(Opcodes.ACC_PRIVATE, "field14", "F")
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
				.method(MethodNodeBuilder.create(Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PUBLIC, "foo", "()LBaseB;")
					.aload(0)
					.methodInsn(Opcodes.INVOKEVIRTUAL, "Sub1B", "foo", "()LSub1B;", false)
					.insn(Opcodes.ARETURN)
					.build())
				.build());
		clazz(ClassNodeBuilder.create("Sub1BSub1", "Sub1B")
				.superInit()
				.method(MethodNodeBuilder.create(Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PUBLIC, "foo", "()LBaseB;")
					.aload(0)
					.methodInsn(Opcodes.INVOKESPECIAL, "Sub1B", "foo", "()LSub1B;", false)
					.insn(Opcodes.ARETURN)
					.build())
				.build());

		clazz(ClassNodeBuilder.create("BaseC")
				.superInit()
				.method(MethodNodeBuilder.create(Opcodes.ACC_PRIVATE, "priv", "()I")
					.iconst_0()
					.insn(Opcodes.IRETURN)
					.maxs(1, 1)
					.build())
				.method(MethodNodeBuilder.create(Opcodes.ACC_STATIC, "stat", "()LBaseC;")
					.typeInsn(Opcodes.NEW, "BaseC")
					.insn(Opcodes.DUP)
					.methodInsn(Opcodes.INVOKESPECIAL, "BaseC", "<init>", "()V", false)
					.insn(Opcodes.ARETURN)
					.maxs(0, 2)
					.build())
				.build());
		clazz(ClassNodeBuilder.create("Sub1C", "BaseC")
				.superInit()
				.method(MethodNodeBuilder.create(Opcodes.ACC_PRIVATE, "priv", "()I")
					.iconst_0()
					.insn(Opcodes.IRETURN)
					.maxs(1, 1)
					.build())
				.method(MethodNodeBuilder.create(Opcodes.ACC_STATIC, "stat", "()LBaseC;")
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
