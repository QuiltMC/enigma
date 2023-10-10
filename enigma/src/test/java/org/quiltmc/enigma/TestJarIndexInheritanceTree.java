package org.quiltmc.enigma;

import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.InheritanceIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.analysis.index.jar.ReferenceIndex;
import org.quiltmc.enigma.api.class_provider.CachingClassProvider;
import org.quiltmc.enigma.api.class_provider.JarClassProvider;
import org.quiltmc.enigma.api.translation.mapping.EntryResolver;
import org.quiltmc.enigma.api.translation.mapping.IndexEntryResolver;
import org.quiltmc.enigma.api.translation.representation.AccessFlags;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import java.nio.file.Path;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestJarIndexInheritanceTree {
	public static final Path JAR = TestUtil.obfJar("inheritance_tree");

	private static final ClassEntry BASE_CLASS = TestEntryFactory.newClass("a");
	private static final ClassEntry SUB_CLASS_A = TestEntryFactory.newClass("b");
	private static final ClassEntry SUB_CLASS_AA = TestEntryFactory.newClass("d");
	private static final ClassEntry SUB_CLASS_B = TestEntryFactory.newClass("c");
	private static final FieldEntry NAME_FIELD = TestEntryFactory.newField(BASE_CLASS, "a", "Ljava/lang/String;");
	private static final FieldEntry NUM_THINGS_FIELD = TestEntryFactory.newField(SUB_CLASS_B, "a", "I");

	private final JarIndex index;

	public TestJarIndexInheritanceTree() throws Exception {
		JarClassProvider jcp = new JarClassProvider(JAR);
		this.index = JarIndex.empty();
		this.index.indexJar(jcp.getClassNames(), new CachingClassProvider(jcp), ProgressListener.none());
	}

	@Test
	public void obfEntries() {
		assertThat(this.index.getIndex(EntryIndex.class).getClasses(), Matchers.containsInAnyOrder(
				TestEntryFactory.newClass("org/quiltmc/enigma/input/Keep"), BASE_CLASS, SUB_CLASS_A, SUB_CLASS_AA, SUB_CLASS_B
		));
	}

	@Test
	public void translationIndex() {
		InheritanceIndex index = this.index.getIndex(InheritanceIndex.class);

		// base class
		assertThat(index.getParents(BASE_CLASS), is(empty()));
		assertThat(index.getAncestors(BASE_CLASS), is(empty()));
		assertThat(index.getChildren(BASE_CLASS), containsInAnyOrder(SUB_CLASS_A, SUB_CLASS_B
		));

		// subclass a
		assertThat(index.getParents(SUB_CLASS_A), contains(BASE_CLASS));
		assertThat(index.getAncestors(SUB_CLASS_A), containsInAnyOrder(BASE_CLASS));
		assertThat(index.getChildren(SUB_CLASS_A), contains(SUB_CLASS_AA));

		// subclass aa
		assertThat(index.getParents(SUB_CLASS_AA), contains(SUB_CLASS_A));
		assertThat(index.getAncestors(SUB_CLASS_AA), containsInAnyOrder(SUB_CLASS_A, BASE_CLASS));
		assertThat(index.getChildren(SUB_CLASS_AA), is(empty()));

		// subclass b
		assertThat(index.getParents(SUB_CLASS_B), contains(BASE_CLASS));
		assertThat(index.getAncestors(SUB_CLASS_B), containsInAnyOrder(BASE_CLASS));
		assertThat(index.getChildren(SUB_CLASS_B), is(empty()));
	}

	@Test
	public void access() {
		assertThat(this.index.getIndex(EntryIndex.class).getFieldAccess(NAME_FIELD), is(new AccessFlags(Opcodes.ACC_PRIVATE)));
		assertThat(this.index.getIndex(EntryIndex.class).getFieldAccess(NUM_THINGS_FIELD), is(new AccessFlags(Opcodes.ACC_PRIVATE)));
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
}
