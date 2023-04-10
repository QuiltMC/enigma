package cuchaz.enigma;

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.classprovider.CachingClassProvider;
import cuchaz.enigma.classprovider.JarClassProvider;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collection;

import static cuchaz.enigma.TestEntryFactory.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestJarIndexConstructorReferences {
	public static final Path JAR = TestUtil.obfJar("constructors");

	private static final ClassEntry BASE_CLASS = newClass("a");
	private static final ClassEntry SUB_CLASS = newClass("d");
	private static final ClassEntry SUBSUB_CLASS = newClass("e");
	private static final ClassEntry DEFAULT_CLASS = newClass("c");
	private static final ClassEntry CALLER_CLASS = newClass("b");

	private final JarIndex index;

	public TestJarIndexConstructorReferences() throws Exception {
		JarClassProvider jcp = new JarClassProvider(JAR);
		this.index = JarIndex.empty();
		this.index.indexJar(jcp.getClassNames(), new CachingClassProvider(jcp), ProgressListener.none());
	}

	@Test
	public void obfEntries() {
		assertThat(this.index.getEntryIndex().getClasses(), containsInAnyOrder(newClass("cuchaz/enigma/inputs/Keep"), BASE_CLASS,
				SUB_CLASS, SUBSUB_CLASS, DEFAULT_CLASS, CALLER_CLASS));
	}

	@Test
	public void baseDefault() {
		MethodEntry source = newMethod(BASE_CLASS, "<init>", "()V");
		Collection<EntryReference<MethodEntry, MethodDefEntry>> references = this.index.getReferenceIndex().getReferencesToMethod(source);
		assertThat(references, containsInAnyOrder(
				newBehaviorReferenceByMethod(source, CALLER_CLASS.getName(), "a", "()V"),
				newBehaviorReferenceByMethod(source, SUB_CLASS.getName(), "<init>", "()V"),
				newBehaviorReferenceByMethod(source, SUB_CLASS.getName(), "<init>", "(III)V")
		));
	}

	@Test
	public void baseInt() {
		MethodEntry source = newMethod(BASE_CLASS, "<init>", "(I)V");
		assertThat(this.index.getReferenceIndex().getReferencesToMethod(source), containsInAnyOrder(
				newBehaviorReferenceByMethod(source, CALLER_CLASS.getName(), "b", "()V")
		));
	}

	@Test
	public void subDefault() {
		MethodEntry source = newMethod(SUB_CLASS, "<init>", "()V");
		assertThat(this.index.getReferenceIndex().getReferencesToMethod(source), containsInAnyOrder(
				newBehaviorReferenceByMethod(source, CALLER_CLASS.getName(), "c", "()V"),
				newBehaviorReferenceByMethod(source, SUB_CLASS.getName(), "<init>", "(I)V")
		));
	}

	@Test
	public void subInt() {
		MethodEntry source = newMethod(SUB_CLASS, "<init>", "(I)V");
		assertThat(this.index.getReferenceIndex().getReferencesToMethod(source), containsInAnyOrder(
				newBehaviorReferenceByMethod(source, CALLER_CLASS.getName(), "d", "()V"),
				newBehaviorReferenceByMethod(source, SUB_CLASS.getName(), "<init>", "(II)V"),
				newBehaviorReferenceByMethod(source, SUBSUB_CLASS.getName(), "<init>", "(I)V")
		));
	}

	@Test
	public void subIntInt() {
		MethodEntry source = newMethod(SUB_CLASS, "<init>", "(II)V");
		assertThat(this.index.getReferenceIndex().getReferencesToMethod(source), containsInAnyOrder(
				newBehaviorReferenceByMethod(source, CALLER_CLASS.getName(), "e", "()V")
		));
	}

	@Test
	public void subIntIntInt() {
		MethodEntry source = newMethod(SUB_CLASS, "<init>", "(III)V");
		assertThat(this.index.getReferenceIndex().getReferencesToMethod(source), is(empty()));
	}

	@Test
	public void subsubInt() {
		MethodEntry source = newMethod(SUBSUB_CLASS, "<init>", "(I)V");
		assertThat(this.index.getReferenceIndex().getReferencesToMethod(source), containsInAnyOrder(
				newBehaviorReferenceByMethod(source, CALLER_CLASS.getName(), "f", "()V")
		));
	}

	@Test
	public void defaultConstructable() {
		MethodEntry source = newMethod(DEFAULT_CLASS, "<init>", "()V");
		assertThat(this.index.getReferenceIndex().getReferencesToMethod(source), containsInAnyOrder(
				newBehaviorReferenceByMethod(source, CALLER_CLASS.getName(), "g", "()V")
		));
	}
}
