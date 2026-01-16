package org.quiltmc.enigma;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.InheritanceIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.analysis.index.jar.MainJarIndex;
import org.quiltmc.enigma.api.analysis.index.jar.ReferenceIndex;
import org.quiltmc.enigma.api.class_provider.CachingClassProvider;
import org.quiltmc.enigma.api.class_provider.JarClassProvider;
import org.quiltmc.enigma.api.class_provider.ProjectClassProvider;
import org.quiltmc.enigma.api.translation.mapping.EntryResolver;
import org.quiltmc.enigma.api.translation.mapping.IndexEntryResolver;
import org.quiltmc.enigma.api.translation.representation.AccessFlags;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

public class TestJarIndexInheritanceTree {
	public static final Path JAR = TestUtil.obfJar("inheritance_tree");

	private static final ClassEntry OBJECT_CLASS = TestEntryFactory.newClass("java/lang/Object");
	private static final ClassEntry BASE_CLASS = TestEntryFactory.newClass("a");
	private static final ClassEntry SUB_CLASS_A = TestEntryFactory.newClass("c");
	private static final ClassEntry SUB_CLASS_AA = TestEntryFactory.newClass("e");
	private static final ClassEntry SUB_CLASS_B = TestEntryFactory.newClass("d");
	private static final FieldEntry NAME_FIELD = TestEntryFactory.newField(BASE_CLASS, "a", "Ljava/lang/String;");
	private static final FieldEntry NUM_THINGS_FIELD = TestEntryFactory.newField(SUB_CLASS_B, "a", "I");

	private final JarIndex index;

	public TestJarIndexInheritanceTree() throws Exception {
		JarClassProvider jcp = new JarClassProvider(JAR);
		this.index = MainJarIndex.empty();
		this.index.indexJar(new ProjectClassProvider(new CachingClassProvider(jcp), null), ProgressListener.createEmpty());
	}

	@Test
	public void obfEntries() {
		assertThat(this.index.getIndex(EntryIndex.class).getClasses(), Matchers.containsInAnyOrder(
				TestEntryFactory.newClass("org/quiltmc/enigma/input/Keep"),
				BASE_CLASS, SUB_CLASS_A, SUB_CLASS_AA, SUB_CLASS_B,
				InterfaceTree.OUTER,
				InterfaceTree.ROOT,
				InterfaceTree.BRANCH_1_2, InterfaceTree.BRANCH_3_4,
				InterfaceTree.LEAF_1, InterfaceTree.LEAF_2, InterfaceTree.LEAF_3, InterfaceTree.LEAF_4
		));
	}

	@Test
	public void translationIndex() {
		InheritanceIndex index = this.index.getIndex(InheritanceIndex.class);

		// base class
		assertThat(index.getParents(BASE_CLASS), containsInAnyOrder(OBJECT_CLASS));
		assertThat(index.getAncestors(BASE_CLASS), containsInAnyOrder(OBJECT_CLASS));
		assertThat(index.getChildren(BASE_CLASS), containsInAnyOrder(SUB_CLASS_A, SUB_CLASS_B));

		// subclass a
		assertThat(index.getParents(SUB_CLASS_A), contains(BASE_CLASS));
		assertThat(index.getAncestors(SUB_CLASS_A), containsInAnyOrder(BASE_CLASS, OBJECT_CLASS));
		assertThat(index.getChildren(SUB_CLASS_A), contains(SUB_CLASS_AA));

		// subclass aa
		assertThat(index.getParents(SUB_CLASS_AA), contains(SUB_CLASS_A));
		assertThat(index.getAncestors(SUB_CLASS_AA), containsInAnyOrder(SUB_CLASS_A, BASE_CLASS, OBJECT_CLASS));
		assertThat(index.getChildren(SUB_CLASS_AA), is(empty()));

		// subclass b
		assertThat(index.getParents(SUB_CLASS_B), contains(BASE_CLASS));
		assertThat(index.getAncestors(SUB_CLASS_B), containsInAnyOrder(BASE_CLASS, OBJECT_CLASS));
		assertThat(index.getChildren(SUB_CLASS_B), is(empty()));
	}

	@Test
	public void access() {
		assertThat(this.index.getIndex(EntryIndex.class).getFieldAccess(NAME_FIELD), is(new AccessFlags(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL)));
		assertThat(this.index.getIndex(EntryIndex.class).getFieldAccess(NUM_THINGS_FIELD), is(new AccessFlags(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL)));
	}

	@Test
	public void relatedMethodImplementations() {
		Collection<MethodEntry> entries;

		EntryResolver resolver = new IndexEntryResolver(this.index);
		// getName()
		entries = resolver.resolveEquivalentMethods(TestEntryFactory.newMethod(BASE_CLASS, "a", "()Ljava/lang/String;"));
		assertThat(entries, Matchers.containsInAnyOrder(
				TestEntryFactory.newMethod(BASE_CLASS, "a", "()Ljava/lang/String;"),
				TestEntryFactory.newMethod(SUB_CLASS_AA, "a", "()Ljava/lang/String;")
		));
		entries = resolver.resolveEquivalentMethods(TestEntryFactory.newMethod(SUB_CLASS_AA, "a", "()Ljava/lang/String;"));
		assertThat(entries, Matchers.containsInAnyOrder(
				TestEntryFactory.newMethod(BASE_CLASS, "a", "()Ljava/lang/String;"),
				TestEntryFactory.newMethod(SUB_CLASS_AA, "a", "()Ljava/lang/String;")
		));

		// doBaseThings()
		entries = resolver.resolveEquivalentMethods(TestEntryFactory.newMethod(BASE_CLASS, "a", "()V"));
		assertThat(entries, Matchers.containsInAnyOrder(
				TestEntryFactory.newMethod(BASE_CLASS, "a", "()V"),
				TestEntryFactory.newMethod(SUB_CLASS_AA, "a", "()V"),
				TestEntryFactory.newMethod(SUB_CLASS_B, "a", "()V")
		));
		entries = resolver.resolveEquivalentMethods(TestEntryFactory.newMethod(SUB_CLASS_AA, "a", "()V"));
		assertThat(entries, Matchers.containsInAnyOrder(
				TestEntryFactory.newMethod(BASE_CLASS, "a", "()V"),
				TestEntryFactory.newMethod(SUB_CLASS_AA, "a", "()V"),
				TestEntryFactory.newMethod(SUB_CLASS_B, "a", "()V")
		));
		entries = resolver.resolveEquivalentMethods(TestEntryFactory.newMethod(SUB_CLASS_B, "a", "()V"));
		assertThat(entries, Matchers.containsInAnyOrder(
				TestEntryFactory.newMethod(BASE_CLASS, "a", "()V"),
				TestEntryFactory.newMethod(SUB_CLASS_AA, "a", "()V"),
				TestEntryFactory.newMethod(SUB_CLASS_B, "a", "()V")
		));

		// doBThings
		entries = resolver.resolveEquivalentMethods(TestEntryFactory.newMethod(SUB_CLASS_B, "b", "()V"));
		assertThat(entries, containsInAnyOrder(TestEntryFactory.newMethod(SUB_CLASS_B, "b", "()V")));
	}

	@Test
	public void fieldReferences() {
		Collection<EntryReference<FieldEntry, MethodDefEntry>> references;

		// name
		references = this.index.getIndex(ReferenceIndex.class).getReferencesToField(NAME_FIELD);
		assertThat(references, Matchers.containsInAnyOrder(
				TestEntryFactory.newFieldReferenceByMethod(NAME_FIELD, BASE_CLASS.getName(), "<init>", "(Ljava/lang/String;)V"),
				TestEntryFactory.newFieldReferenceByMethod(NAME_FIELD, BASE_CLASS.getName(), "a", "()Ljava/lang/String;")
		));

		// numThings
		references = this.index.getIndex(ReferenceIndex.class).getReferencesToField(NUM_THINGS_FIELD);
		assertThat(references, Matchers.containsInAnyOrder(
				TestEntryFactory.newFieldReferenceByMethod(NUM_THINGS_FIELD, SUB_CLASS_B.getName(), "<init>", "()V"),
				TestEntryFactory.newFieldReferenceByMethod(NUM_THINGS_FIELD, SUB_CLASS_B.getName(), "b", "()V")
		));
	}

	@Test
	public void behaviorReferences() {
		MethodEntry source;
		Collection<EntryReference<MethodEntry, MethodDefEntry>> references;

		// baseClass constructor
		source = TestEntryFactory.newMethod(BASE_CLASS, "<init>", "(Ljava/lang/String;)V");
		references = this.index.getIndex(ReferenceIndex.class).getReferencesToMethod(source);
		assertThat(references, Matchers.containsInAnyOrder(
				TestEntryFactory.newBehaviorReferenceByMethod(source, SUB_CLASS_A.getName(), "<init>", "(Ljava/lang/String;)V"),
				TestEntryFactory.newBehaviorReferenceByMethod(source, SUB_CLASS_B.getName(), "<init>", "()V")
		));

		// subClassA constructor
		source = TestEntryFactory.newMethod(SUB_CLASS_A, "<init>", "(Ljava/lang/String;)V");
		references = this.index.getIndex(ReferenceIndex.class).getReferencesToMethod(source);
		assertThat(references, containsInAnyOrder(
				TestEntryFactory.newBehaviorReferenceByMethod(source, SUB_CLASS_AA.getName(), "<init>", "()V")
		));

		// baseClass.getName()
		source = TestEntryFactory.newMethod(BASE_CLASS, "a", "()Ljava/lang/String;");
		references = this.index.getIndex(ReferenceIndex.class).getReferencesToMethod(source);
		assertThat(references, Matchers.containsInAnyOrder(
				TestEntryFactory.newBehaviorReferenceByMethod(source, SUB_CLASS_AA.getName(), "a", "()Ljava/lang/String;"),
				TestEntryFactory.newBehaviorReferenceByMethod(source, SUB_CLASS_B.getName(), "a", "()V")
		));

		// subclassAA.getName()
		source = TestEntryFactory.newMethod(SUB_CLASS_AA, "a", "()Ljava/lang/String;");
		references = this.index.getIndex(ReferenceIndex.class).getReferencesToMethod(source);
		assertThat(references, containsInAnyOrder(
				TestEntryFactory.newBehaviorReferenceByMethod(source, SUB_CLASS_AA.getName(), "a", "()V")
		));
	}

	@Test
	public void containsEntries() {
		EntryIndex entryIndex = this.index.getIndex(EntryIndex.class);
		// classes
		assertThat(entryIndex.hasClass(BASE_CLASS), is(true));
		assertThat(entryIndex.hasClass(SUB_CLASS_A), is(true));
		assertThat(entryIndex.hasClass(SUB_CLASS_AA), is(true));
		assertThat(entryIndex.hasClass(SUB_CLASS_B), is(true));

		// fields
		assertThat(entryIndex.hasField(NAME_FIELD), is(true));
		assertThat(entryIndex.hasField(NUM_THINGS_FIELD), is(true));

		// methods
		// getName()
		assertThat(entryIndex.hasMethod(TestEntryFactory.newMethod(BASE_CLASS, "a", "()Ljava/lang/String;")), is(true));
		assertThat(entryIndex.hasMethod(TestEntryFactory.newMethod(SUB_CLASS_A, "a", "()Ljava/lang/String;")), is(false));
		assertThat(entryIndex.hasMethod(TestEntryFactory.newMethod(SUB_CLASS_AA, "a", "()Ljava/lang/String;")), is(true));
		assertThat(entryIndex.hasMethod(TestEntryFactory.newMethod(SUB_CLASS_B, "a", "()Ljava/lang/String;")), is(false));

		// doBaseThings()
		assertThat(entryIndex.hasMethod(TestEntryFactory.newMethod(BASE_CLASS, "a", "()V")), is(true));
		assertThat(entryIndex.hasMethod(TestEntryFactory.newMethod(SUB_CLASS_A, "a", "()V")), is(false));
		assertThat(entryIndex.hasMethod(TestEntryFactory.newMethod(SUB_CLASS_AA, "a", "()V")), is(true));
		assertThat(entryIndex.hasMethod(TestEntryFactory.newMethod(SUB_CLASS_B, "a", "()V")), is(true));

		// doBThings()
		assertThat(entryIndex.hasMethod(TestEntryFactory.newMethod(BASE_CLASS, "b", "()V")), is(false));
		assertThat(entryIndex.hasMethod(TestEntryFactory.newMethod(SUB_CLASS_A, "b", "()V")), is(false));
		assertThat(entryIndex.hasMethod(TestEntryFactory.newMethod(SUB_CLASS_AA, "b", "()V")), is(false));
		assertThat(entryIndex.hasMethod(TestEntryFactory.newMethod(SUB_CLASS_B, "b", "()V")), is(true));
	}

	@Test
	void streamAncestorsOrder() {
		final List<ClassEntry> ancestors = this.index.getIndex(InheritanceIndex.class)
				.streamAncestors(InterfaceTree.ROOT)
				.toList();

		assertThat(ancestors.size(), is(13));

		// First generation: Object appears once for ROOT
		assertThat(ancestors.subList(0, 3), Matchers.containsInAnyOrder(
				InterfaceTree.BRANCH_1_2, InterfaceTree.BRANCH_3_4, OBJECT_CLASS
		));

		// Second generation: Object appears once for each of the 2 branches
		assertThat(ancestors.subList(3, 9), Matchers.containsInAnyOrder(
				InterfaceTree.LEAF_1, InterfaceTree.LEAF_2, InterfaceTree.LEAF_3, InterfaceTree.LEAF_4,
				OBJECT_CLASS, OBJECT_CLASS
		));

		// Third generation: Object appears once for each of the 4 leaves
		assertThat(ancestors.subList(9, 13), Matchers.containsInAnyOrder(
				OBJECT_CLASS, OBJECT_CLASS, OBJECT_CLASS, OBJECT_CLASS
		));
	}

	@Test
	void getAncestorsOrder() {
		final List<ClassEntry> ancestors = this.index.getIndex(InheritanceIndex.class)
				.getAncestors(InterfaceTree.ROOT)
				.stream()
				.toList();

		assertThat(ancestors.size(), is(7));

		// Only the first appearance of Object (from the root) is included
		assertThat(ancestors.subList(0, 3), Matchers.containsInAnyOrder(
				InterfaceTree.BRANCH_1_2, InterfaceTree.BRANCH_3_4, OBJECT_CLASS
		));

		assertThat(ancestors.subList(3, 7), Matchers.containsInAnyOrder(
				InterfaceTree.LEAF_1, InterfaceTree.LEAF_2, InterfaceTree.LEAF_3, InterfaceTree.LEAF_4
		));
	}

	private interface InterfaceTree {
		String OUTER_NAME = "b";
		String OUTER_PREFIX = OUTER_NAME + "$";

		ClassEntry OUTER = TestEntryFactory.newClass(OUTER_NAME);

		ClassEntry ROOT = TestEntryFactory.newClass(OUTER_PREFIX + "g");

		ClassEntry BRANCH_1_2 = TestEntryFactory.newClass(OUTER_PREFIX + "a");
		ClassEntry BRANCH_3_4 = TestEntryFactory.newClass(OUTER_PREFIX + "b");

		ClassEntry LEAF_1 = TestEntryFactory.newClass(OUTER_PREFIX + "c");
		ClassEntry LEAF_2 = TestEntryFactory.newClass(OUTER_PREFIX + "d");
		ClassEntry LEAF_3 = TestEntryFactory.newClass(OUTER_PREFIX + "e");
		ClassEntry LEAF_4 = TestEntryFactory.newClass(OUTER_PREFIX + "f");
	}
}
