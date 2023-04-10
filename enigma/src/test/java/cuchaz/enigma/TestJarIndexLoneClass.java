package cuchaz.enigma;

import cuchaz.enigma.analysis.ClassImplementationsTreeNode;
import cuchaz.enigma.analysis.ClassInheritanceTreeNode;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.IndexTreeBuilder;
import cuchaz.enigma.analysis.MethodImplementationsTreeNode;
import cuchaz.enigma.analysis.MethodInheritanceTreeNode;
import cuchaz.enigma.analysis.index.EntryIndex;
import cuchaz.enigma.analysis.index.InheritanceIndex;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.classprovider.CachingClassProvider;
import cuchaz.enigma.classprovider.JarClassProvider;
import cuchaz.enigma.translation.VoidTranslator;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static cuchaz.enigma.TestEntryFactory.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestJarIndexLoneClass {
	public static final Path JAR = TestUtil.obfJar("loneClass");
	private final JarIndex index;

	public TestJarIndexLoneClass() throws Exception {
		JarClassProvider jcp = new JarClassProvider(JAR);
		this.index = JarIndex.empty();
		this.index.indexJar(jcp.getClassNames(), new CachingClassProvider(jcp), ProgressListener.none());
	}

	@Test
	public void obfEntries() {
		assertThat(this.index.getEntryIndex().getClasses(), containsInAnyOrder(
				newClass("cuchaz/enigma/inputs/Keep"),
				newClass("a")
		));
	}

	@Test
	public void translationIndex() {
		InheritanceIndex inheritanceIndex = this.index.getInheritanceIndex();
		assertThat(inheritanceIndex.getParents(new ClassEntry("a")), is(empty()));
		assertThat(inheritanceIndex.getParents(new ClassEntry("cuchaz/enigma/inputs/Keep")), is(empty()));
		assertThat(inheritanceIndex.getAncestors(new ClassEntry("a")), is(empty()));
		assertThat(inheritanceIndex.getAncestors(new ClassEntry("cuchaz/enigma/inputs/Keep")), is(empty()));
		assertThat(inheritanceIndex.getChildren(new ClassEntry("a")), is(empty()));
		assertThat(inheritanceIndex.getChildren(new ClassEntry("cuchaz/enigma/inputs/Keep")), is(empty()));
	}

	@Test
	public void access() {
		EntryIndex entryIndex = this.index.getEntryIndex();
		assertThat(entryIndex.getFieldAccess(newField("a", "a", "Ljava/lang/String;")), is(AccessFlags.PRIVATE));
		assertThat(entryIndex.getMethodAccess(newMethod("a", "a", "()Ljava/lang/String;")), is(AccessFlags.PUBLIC));
		assertThat(entryIndex.getFieldAccess(newField("a", "b", "Ljava/lang/String;")), is(nullValue()));
		assertThat(entryIndex.getFieldAccess(newField("a", "a", "LFoo;")), is(nullValue()));
	}

	@Test
	public void classInheritance() {
		IndexTreeBuilder treeBuilder = new IndexTreeBuilder(this.index);
		ClassInheritanceTreeNode node = treeBuilder.buildClassInheritance(VoidTranslator.INSTANCE, newClass("a"));
		assertThat(node, is(not(nullValue())));
		assertThat(node.getClassName(), is("a"));
		assertThat(node.getChildCount(), is(0));
	}

	@Test
	public void methodInheritance() {
		IndexTreeBuilder treeBuilder = new IndexTreeBuilder(this.index);
		MethodEntry source = newMethod("a", "a", "()Ljava/lang/String;");
		MethodInheritanceTreeNode node = treeBuilder.buildMethodInheritance(VoidTranslator.INSTANCE, source);
		assertThat(node, is(not(nullValue())));
		assertThat(node.getMethodEntry(), is(source));
		assertThat(node.getChildCount(), is(0));
	}

	@Test
	public void classImplementations() {
		IndexTreeBuilder treeBuilder = new IndexTreeBuilder(this.index);
		ClassImplementationsTreeNode node = treeBuilder.buildClassImplementations(VoidTranslator.INSTANCE, newClass("a"));
		assertThat(node, is(nullValue()));
	}

	@Test
	public void methodImplementations() {
		IndexTreeBuilder treeBuilder = new IndexTreeBuilder(this.index);
		MethodEntry source = newMethod("a", "a", "()Ljava/lang/String;");

		List<MethodImplementationsTreeNode> nodes = treeBuilder.buildMethodImplementations(VoidTranslator.INSTANCE, source);
		assertThat(nodes, hasSize(1));
		assertThat(nodes.get(0).getMethodEntry(), is(source));
	}

	@Test
	public void relatedMethodImplementations() {
		Collection<MethodEntry> entries = this.index.getEntryResolver().resolveEquivalentMethods(newMethod("a", "a", "()Ljava/lang/String;"));
		assertThat(entries, containsInAnyOrder(
				newMethod("a", "a", "()Ljava/lang/String;")
		));
	}

	@Test
	public void fieldReferences() {
		FieldEntry source = newField("a", "a", "Ljava/lang/String;");
		Collection<EntryReference<FieldEntry, MethodDefEntry>> references = this.index.getReferenceIndex().getReferencesToField(source);
		assertThat(references, containsInAnyOrder(
				newFieldReferenceByMethod(source, "a", "<init>", "(Ljava/lang/String;)V"),
				newFieldReferenceByMethod(source, "a", "a", "()Ljava/lang/String;")
		));
	}

	@Test
	public void behaviorReferences() {
		assertThat(this.index.getReferenceIndex().getReferencesToMethod(newMethod("a", "a", "()Ljava/lang/String;")), is(empty()));
	}

	@Test
	public void interfaces() {
		assertThat(this.index.getInheritanceIndex().getParents(new ClassEntry("a")), is(empty()));
	}

	@Test
	public void implementingClasses() {
		assertThat(this.index.getInheritanceIndex().getChildren(new ClassEntry("a")), is(empty()));
	}

	@Test
	public void isInterface() {
		assertThat(this.index.getInheritanceIndex().isParent(new ClassEntry("a")), is(false));
	}

	@Test
	public void testContains() {
		EntryIndex entryIndex = this.index.getEntryIndex();
		assertThat(entryIndex.hasClass(newClass("a")), is(true));
		assertThat(entryIndex.hasClass(newClass("b")), is(false));
		assertThat(entryIndex.hasField(newField("a", "a", "Ljava/lang/String;")), is(true));
		assertThat(entryIndex.hasField(newField("a", "b", "Ljava/lang/String;")), is(false));
		assertThat(entryIndex.hasField(newField("a", "a", "LFoo;")), is(false));
		assertThat(entryIndex.hasMethod(newMethod("a", "a", "()Ljava/lang/String;")), is(true));
		assertThat(entryIndex.hasMethod(newMethod("a", "b", "()Ljava/lang/String;")), is(false));
	}
}
