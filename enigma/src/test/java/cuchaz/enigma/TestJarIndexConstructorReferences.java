/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/

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
import java.nio.file.Paths;
import java.util.Collection;

import static cuchaz.enigma.TestEntryFactory.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestJarIndexConstructorReferences {

	public static final Path JAR = Paths.get("build/test-obf/constructors.jar");
	private final JarIndex index;

	private final ClassEntry baseClass = newClass("a");
	private final ClassEntry subClass = newClass("d");
	private final ClassEntry subsubClass = newClass("e");
	private final ClassEntry defaultClass = newClass("c");
	private final ClassEntry callerClass = newClass("b");

	public TestJarIndexConstructorReferences() throws Exception {
		JarClassProvider jcp = new JarClassProvider(JAR);
        this.index = JarIndex.empty();
        this.index.indexJar(jcp.getClassNames(), new CachingClassProvider(jcp), ProgressListener.none());
	}

	@Test
	public void obfEntries() {
		assertThat(this.index.getEntryIndex().getClasses(), containsInAnyOrder(newClass("cuchaz/enigma/inputs/Keep"), this.baseClass,
                this.subClass, this.subsubClass, this.defaultClass, this.callerClass));
	}

	@Test
	public void baseDefault() {
		MethodEntry source = newMethod(this.baseClass, "<init>", "()V");
		Collection<EntryReference<MethodEntry, MethodDefEntry>> references = this.index.getReferenceIndex().getReferencesToMethod(source);
		assertThat(references, containsInAnyOrder(
				newBehaviorReferenceByMethod(source, this.callerClass.getName(), "a", "()V"),
				newBehaviorReferenceByMethod(source, this.subClass.getName(), "<init>", "()V"),
				newBehaviorReferenceByMethod(source, this.subClass.getName(), "<init>", "(III)V")
		));
	}

	@Test
	public void baseInt() {
		MethodEntry source = newMethod(this.baseClass, "<init>", "(I)V");
		assertThat(this.index.getReferenceIndex().getReferencesToMethod(source), containsInAnyOrder(
				newBehaviorReferenceByMethod(source, this.callerClass.getName(), "b", "()V")
		));
	}

	@Test
	public void subDefault() {
		MethodEntry source = newMethod(this.subClass, "<init>", "()V");
		assertThat(this.index.getReferenceIndex().getReferencesToMethod(source), containsInAnyOrder(
				newBehaviorReferenceByMethod(source, this.callerClass.getName(), "c", "()V"),
				newBehaviorReferenceByMethod(source, this.subClass.getName(), "<init>", "(I)V")
		));
	}

	@Test
	public void subInt() {
		MethodEntry source = newMethod(this.subClass, "<init>", "(I)V");
		assertThat(this.index.getReferenceIndex().getReferencesToMethod(source), containsInAnyOrder(
				newBehaviorReferenceByMethod(source, this.callerClass.getName(), "d", "()V"),
				newBehaviorReferenceByMethod(source, this.subClass.getName(), "<init>", "(II)V"),
				newBehaviorReferenceByMethod(source, this.subsubClass.getName(), "<init>", "(I)V")
		));
	}

	@Test
	public void subIntInt() {
		MethodEntry source = newMethod(this.subClass, "<init>", "(II)V");
		assertThat(this.index.getReferenceIndex().getReferencesToMethod(source), containsInAnyOrder(
				newBehaviorReferenceByMethod(source, this.callerClass.getName(), "e", "()V")
		));
	}

	@Test
	public void subIntIntInt() {
		MethodEntry source = newMethod(this.subClass, "<init>", "(III)V");
		assertThat(this.index.getReferenceIndex().getReferencesToMethod(source), is(empty()));
	}

	@Test
	public void subsubInt() {
		MethodEntry source = newMethod(this.subsubClass, "<init>", "(I)V");
		assertThat(this.index.getReferenceIndex().getReferencesToMethod(source), containsInAnyOrder(
				newBehaviorReferenceByMethod(source, this.callerClass.getName(), "f", "()V")
		));
	}

	@Test
	public void defaultConstructable() {
		MethodEntry source = newMethod(this.defaultClass, "<init>", "()V");
		assertThat(this.index.getReferenceIndex().getReferencesToMethod(source), containsInAnyOrder(
				newBehaviorReferenceByMethod(source, this.callerClass.getName(), "g", "()V")
		));
	}
}
