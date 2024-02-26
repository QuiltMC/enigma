package org.quiltmc.enigma;

import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.analysis.index.jar.ReferenceIndex;
import org.quiltmc.enigma.api.class_provider.CachingClassProvider;
import org.quiltmc.enigma.api.class_provider.JarClassProvider;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestJarIndexConstructorReferences {
	public static final Path JAR = TestUtil.obfJar("constructors");

	private static final ClassEntry BASE_CLASS = TestEntryFactory.newClass("a");
	private static final ClassEntry SUB_CLASS = TestEntryFactory.newClass("d");
	private static final ClassEntry SUBSUB_CLASS = TestEntryFactory.newClass("e");
	private static final ClassEntry DEFAULT_CLASS = TestEntryFactory.newClass("c");
	private static final ClassEntry CALLER_CLASS = TestEntryFactory.newClass("b");

	private final JarIndex index;

	public TestJarIndexConstructorReferences() throws Exception {
		JarClassProvider jcp = new JarClassProvider(JAR);
		this.index = JarIndex.empty();
		this.index.indexJar(jcp.getClassNames(), new CachingClassProvider(jcp), ProgressListener.createEmpty());
	}

	@Test
	public void obfEntries() {
		assertThat(this.index.getIndex(EntryIndex.class).getClasses(), Matchers.containsInAnyOrder(TestEntryFactory.newClass("org/quiltmc/enigma/input/Keep"), BASE_CLASS,
				SUB_CLASS, SUBSUB_CLASS, DEFAULT_CLASS, CALLER_CLASS));
	}

	@Test
	public void baseDefault() {
		MethodEntry source = TestEntryFactory.newMethod(BASE_CLASS, "<init>", "()V");
		Collection<EntryReference<MethodEntry, MethodDefEntry>> references = this.index.getIndex(ReferenceIndex.class).getReferencesToMethod(source);
		assertThat(references, Matchers.containsInAnyOrder(
				TestEntryFactory.newBehaviorReferenceByMethod(source, CALLER_CLASS.getName(), "a", "()V"),
				TestEntryFactory.newBehaviorReferenceByMethod(source, SUB_CLASS.getName(), "<init>", "()V"),
				TestEntryFactory.newBehaviorReferenceByMethod(source, SUB_CLASS.getName(), "<init>", "(III)V")
		));
	}

	@Test
	public void baseInt() {
		MethodEntry source = TestEntryFactory.newMethod(BASE_CLASS, "<init>", "(I)V");
		assertThat(this.index.getIndex(ReferenceIndex.class).getReferencesToMethod(source), containsInAnyOrder(
				TestEntryFactory.newBehaviorReferenceByMethod(source, CALLER_CLASS.getName(), "b", "()V")
		));
	}

	@Test
	public void subDefault() {
		MethodEntry source = TestEntryFactory.newMethod(SUB_CLASS, "<init>", "()V");
		assertThat(this.index.getIndex(ReferenceIndex.class).getReferencesToMethod(source), Matchers.containsInAnyOrder(
				TestEntryFactory.newBehaviorReferenceByMethod(source, CALLER_CLASS.getName(), "c", "()V"),
				TestEntryFactory.newBehaviorReferenceByMethod(source, SUB_CLASS.getName(), "<init>", "(I)V")
		));
	}

	@Test
	public void subInt() {
		MethodEntry source = TestEntryFactory.newMethod(SUB_CLASS, "<init>", "(I)V");
		assertThat(this.index.getIndex(ReferenceIndex.class).getReferencesToMethod(source), Matchers.containsInAnyOrder(
				TestEntryFactory.newBehaviorReferenceByMethod(source, CALLER_CLASS.getName(), "d", "()V"),
				TestEntryFactory.newBehaviorReferenceByMethod(source, SUB_CLASS.getName(), "<init>", "(II)V"),
				TestEntryFactory.newBehaviorReferenceByMethod(source, SUBSUB_CLASS.getName(), "<init>", "(I)V")
		));
	}

	@Test
	public void subIntInt() {
		MethodEntry source = TestEntryFactory.newMethod(SUB_CLASS, "<init>", "(II)V");
		assertThat(this.index.getIndex(ReferenceIndex.class).getReferencesToMethod(source), containsInAnyOrder(
				TestEntryFactory.newBehaviorReferenceByMethod(source, CALLER_CLASS.getName(), "e", "()V")
		));
	}

	@Test
	public void subIntIntInt() {
		MethodEntry source = TestEntryFactory.newMethod(SUB_CLASS, "<init>", "(III)V");
		assertThat(this.index.getIndex(ReferenceIndex.class).getReferencesToMethod(source), is(empty()));
	}

	@Test
	public void subsubInt() {
		MethodEntry source = TestEntryFactory.newMethod(SUBSUB_CLASS, "<init>", "(I)V");
		assertThat(this.index.getIndex(ReferenceIndex.class).getReferencesToMethod(source), containsInAnyOrder(
				TestEntryFactory.newBehaviorReferenceByMethod(source, CALLER_CLASS.getName(), "f", "()V")
		));
	}

	@Test
	public void defaultConstructable() {
		MethodEntry source = TestEntryFactory.newMethod(DEFAULT_CLASS, "<init>", "()V");
		assertThat(this.index.getIndex(ReferenceIndex.class).getReferencesToMethod(source), containsInAnyOrder(
				TestEntryFactory.newBehaviorReferenceByMethod(source, CALLER_CLASS.getName(), "g", "()V")
		));
	}
}
