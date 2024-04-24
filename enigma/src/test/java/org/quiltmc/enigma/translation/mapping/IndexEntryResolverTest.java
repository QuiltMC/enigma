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
		Assertions.assertIterableEquals(expected, resolver.resolveEntry(entry, ResolutionStrategy.RESOLVE_ROOT));
	}

	private static <E extends Entry<?>> void assertResolvedClosest(E entry, Collection<E> expected) {
		Assertions.assertIterableEquals(expected, resolver.resolveEntry(entry, ResolutionStrategy.RESOLVE_CLOSEST));
	}

	@Test
	public void testResolveFields() {
		var baseA = TestEntryFactory.newClass("BaseA");
		var sub1A = TestEntryFactory.newClass("Sub1A");

		var entry = TestEntryFactory.newField(sub1A, "field1", "Z");
		var expected = entry;
		assertResolvedRoot(entry, Collections.singleton(expected));
		assertResolvedClosest(entry, Collections.singleton(expected));

		entry = TestEntryFactory.newField(baseA, "field1", "Z");
		expected = entry;
		assertResolvedRoot(entry, Collections.singleton(expected));
		assertResolvedClosest(entry, Collections.singleton(expected));

		entry = TestEntryFactory.newField(sub1A, "field2", "I");
		expected = TestEntryFactory.newField(baseA, "field2", "I");
		assertResolvedRoot(entry, Collections.singleton(expected));
		assertResolvedClosest(entry, Collections.singleton(expected));

		entry = TestEntryFactory.newField(sub1A, "field3", "I");
		expected = entry;
		assertResolvedRoot(entry, Collections.singleton(expected));
		assertResolvedClosest(entry, Collections.singleton(expected));

		// TODO: Non-existing entries should be resolved to empty or the entry singleton??
		// entry = TestEntryFactory.newField(baseA, "field3", "I");

		entry = TestEntryFactory.newField(sub1A, "field4", "J");
		expected = TestEntryFactory.newField(baseA, "field4", "J");
		assertResolvedRoot(entry, Collections.singleton(expected));
		assertResolvedClosest(entry, Collections.singleton(expected));

		entry = TestEntryFactory.newField(sub1A, "field4", "B");
		expected = entry;
		assertResolvedRoot(entry, Collections.singleton(expected));
		assertResolvedClosest(entry, Collections.singleton(expected));
	}

	@Test
	public void testResolveMethods() {
		var baseB = TestEntryFactory.newClass("BaseB");
		var sub1B = TestEntryFactory.newClass("Sub1B");
		var sub1BSub1 = TestEntryFactory.newClass("Sub1BSub1");

		var entry = TestEntryFactory.newMethod(baseB, "foo", "()LBaseB;");
		var expected = entry;
		assertResolvedRoot(entry, Collections.singleton(expected));
		assertResolvedClosest(entry, Collections.singleton(expected));

		// TODO
	}

	private static void clazz(ClassNode classNode) {
		CLASSES.put(classNode.name, classNode);
	}

	static {
		clazz(ClassNodeBuilder.create("BaseA")
				.field("field1", "Z")
				.field("field2", "I")
				.field("field4", "J")
				.superInit()
				.build());
		clazz(ClassNodeBuilder.create("Sub1A", "BaseA")
				.field("field1", "Z")
				.field("field3", "I")
				.field("field4", "B")
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
				.method(MethodNodeBuilder.create("foo", "()LSubB;")
					.aload(0)
					.insn(Opcodes.ARETURN)
					.build())
				.method(MethodNodeBuilder.create(Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PUBLIC, "foo", "()LBaseB;", null, null)
					.aload(0)
					.methodInsn(Opcodes.INVOKEVIRTUAL, "Sub1B", "foo", "()LSubB;", false)
					.insn(Opcodes.ARETURN)
					.build())
				.build());
		clazz(ClassNodeBuilder.create("Sub1BSub1", "Sub1B")
				.superInit()
				.method(MethodNodeBuilder.create(Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PUBLIC, "foo", "()LBaseB;", null, null)
					.aload(0)
					.methodInsn(Opcodes.INVOKESPECIAL, "Sub1B", "foo", "()LSubB;", false)
					.insn(Opcodes.ARETURN)
					.build())
				.build());
	}
}
