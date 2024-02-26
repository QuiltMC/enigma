package org.quiltmc.enigma;

import org.quiltmc.enigma.api.analysis.index.jar.ReferenceIndex;
import org.quiltmc.enigma.api.analysis.tree.ClassImplementationsTreeNode;
import org.quiltmc.enigma.api.analysis.tree.ClassInheritanceTreeNode;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.impl.analysis.IndexTreeBuilder;
import org.quiltmc.enigma.api.analysis.tree.MethodImplementationsTreeNode;
import org.quiltmc.enigma.api.analysis.tree.MethodInheritanceTreeNode;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.InheritanceIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.class_provider.CachingClassProvider;
import org.quiltmc.enigma.api.class_provider.JarClassProvider;
import org.quiltmc.enigma.api.translation.VoidTranslator;
import org.quiltmc.enigma.api.translation.representation.AccessFlags;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestJarIndexLoneClass {
	public static final Path JAR = TestUtil.obfJar("lone_class");
	private final JarIndex index;

	public TestJarIndexLoneClass() throws Exception {
		JarClassProvider jcp = new JarClassProvider(JAR);
		this.index = JarIndex.empty();
		this.index.indexJar(jcp.getClassNames(), new CachingClassProvider(jcp), ProgressListener.createEmpty());
	}

	@Test
	public void obfEntries() {
		assertThat(this.index.getIndex(EntryIndex.class).getClasses(), Matchers.containsInAnyOrder(
				TestEntryFactory.newClass("org/quiltmc/enigma/input/Keep"),
				TestEntryFactory.newClass("a")
		));
	}

	@Test
	public void translationIndex() {
		InheritanceIndex inheritanceIndex = this.index.getIndex(InheritanceIndex.class);
		assertThat(inheritanceIndex.getParents(new ClassEntry("a")), is(empty()));
		assertThat(inheritanceIndex.getParents(new ClassEntry("org/quiltmc/enigma/input/Keep")), is(empty()));
		assertThat(inheritanceIndex.getAncestors(new ClassEntry("a")), is(empty()));
		assertThat(inheritanceIndex.getAncestors(new ClassEntry("org/quiltmc/enigma/input/Keep")), is(empty()));
		assertThat(inheritanceIndex.getChildren(new ClassEntry("a")), is(empty()));
		assertThat(inheritanceIndex.getChildren(new ClassEntry("org/quiltmc/enigma/input/Keep")), is(empty()));
	}

	@Test
	public void access() {
		EntryIndex entryIndex = this.index.getIndex(EntryIndex.class);
		assertThat(entryIndex.getFieldAccess(TestEntryFactory.newField("a", "a", "Ljava/lang/String;")), is(AccessFlags.PRIVATE));
		assertThat(entryIndex.getMethodAccess(TestEntryFactory.newMethod("a", "a", "()Ljava/lang/String;")), is(AccessFlags.PUBLIC));
		assertThat(entryIndex.getFieldAccess(TestEntryFactory.newField("a", "b", "Ljava/lang/String;")), is(nullValue()));
		assertThat(entryIndex.getFieldAccess(TestEntryFactory.newField("a", "a", "LFoo;")), is(nullValue()));
	}

	@Test
	public void classInheritance() {
		IndexTreeBuilder treeBuilder = new IndexTreeBuilder(this.index);
		ClassInheritanceTreeNode node = treeBuilder.buildClassInheritance(VoidTranslator.INSTANCE, TestEntryFactory.newClass("a"));
		assertThat(node, is(not(nullValue())));
		assertThat(node.getClassName(), is("a"));
		assertThat(node.getChildCount(), is(0));
	}

	@Test
	public void methodInheritance() {
		IndexTreeBuilder treeBuilder = new IndexTreeBuilder(this.index);
		MethodEntry source = TestEntryFactory.newMethod("a", "a", "()Ljava/lang/String;");
		MethodInheritanceTreeNode node = treeBuilder.buildMethodInheritance(VoidTranslator.INSTANCE, source);
		assertThat(node, is(not(nullValue())));
		assertThat(node.getMethodEntry(), is(source));
		assertThat(node.getChildCount(), is(0));
	}

	@Test
	public void classImplementations() {
		IndexTreeBuilder treeBuilder = new IndexTreeBuilder(this.index);
		ClassImplementationsTreeNode node = treeBuilder.buildClassImplementations(VoidTranslator.INSTANCE, TestEntryFactory.newClass("a"));
		assertThat(node, is(nullValue()));
	}

	@Test
	public void methodImplementations() {
		IndexTreeBuilder treeBuilder = new IndexTreeBuilder(this.index);
		MethodEntry source = TestEntryFactory.newMethod("a", "a", "()Ljava/lang/String;");

		List<MethodImplementationsTreeNode> nodes = treeBuilder.buildMethodImplementations(VoidTranslator.INSTANCE, source);
		assertThat(nodes, hasSize(1));
		assertThat(nodes.get(0).getMethodEntry(), is(source));
	}

	@Test
	public void relatedMethodImplementations() {
		Collection<MethodEntry> entries = this.index.getEntryResolver().resolveEquivalentMethods(TestEntryFactory.newMethod("a", "a", "()Ljava/lang/String;"));
		assertThat(entries, containsInAnyOrder(
				TestEntryFactory.newMethod("a", "a", "()Ljava/lang/String;")
		));
	}

	@Test
	public void fieldReferences() {
		FieldEntry source = TestEntryFactory.newField("a", "a", "Ljava/lang/String;");
		Collection<EntryReference<FieldEntry, MethodDefEntry>> references = this.index.getIndex(ReferenceIndex.class).getReferencesToField(source);
		assertThat(references, Matchers.containsInAnyOrder(
				TestEntryFactory.newFieldReferenceByMethod(source, "a", "<init>", "(Ljava/lang/String;)V"),
				TestEntryFactory.newFieldReferenceByMethod(source, "a", "a", "()Ljava/lang/String;")
		));
	}

	@Test
	public void behaviorReferences() {
		assertThat(this.index.getIndex(ReferenceIndex.class).getReferencesToMethod(TestEntryFactory.newMethod("a", "a", "()Ljava/lang/String;")), is(empty()));
	}

	@Test
	public void interfaces() {
		assertThat(this.index.getIndex(InheritanceIndex.class).getParents(new ClassEntry("a")), is(empty()));
	}

	@Test
	public void implementingClasses() {
		assertThat(this.index.getIndex(InheritanceIndex.class).getChildren(new ClassEntry("a")), is(empty()));
	}

	@Test
	public void isInterface() {
		assertThat(this.index.getIndex(InheritanceIndex.class).isParent(new ClassEntry("a")), is(false));
	}

	@Test
	public void testContains() {
		EntryIndex entryIndex = this.index.getIndex(EntryIndex.class);
		assertThat(entryIndex.hasClass(TestEntryFactory.newClass("a")), is(true));
		assertThat(entryIndex.hasClass(TestEntryFactory.newClass("b")), is(false));
		assertThat(entryIndex.hasField(TestEntryFactory.newField("a", "a", "Ljava/lang/String;")), is(true));
		assertThat(entryIndex.hasField(TestEntryFactory.newField("a", "b", "Ljava/lang/String;")), is(false));
		assertThat(entryIndex.hasField(TestEntryFactory.newField("a", "a", "LFoo;")), is(false));
		assertThat(entryIndex.hasMethod(TestEntryFactory.newMethod("a", "a", "()Ljava/lang/String;")), is(true));
		assertThat(entryIndex.hasMethod(TestEntryFactory.newMethod("a", "b", "()Ljava/lang/String;")), is(false));
	}
}
