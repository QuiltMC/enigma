/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *	 Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma;

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.index.EntryIndex;
import cuchaz.enigma.analysis.index.InheritanceIndex;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.classprovider.CachingClassProvider;
import cuchaz.enigma.classprovider.JarClassProvider;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.IndexEntryResolver;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import static cuchaz.enigma.TestEntryFactory.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class TestJarIndexInheritanceTree {
	public static final Path JAR = Paths.get("build/test-obf/inheritanceTree.jar");
	private final JarIndex index;

	private final ClassEntry baseClass = newClass("a");
	private final ClassEntry subClassA = newClass("b");
	private final ClassEntry subClassAA = newClass("d");
	private final ClassEntry subClassB = newClass("c");
	private final FieldEntry nameField = newField(this.baseClass, "a", "Ljava/lang/String;");
	private final FieldEntry numThingsField = newField(this.subClassB, "a", "I");

	public TestJarIndexInheritanceTree() throws Exception {
		JarClassProvider jcp = new JarClassProvider(JAR);
		this.index = JarIndex.empty();
		this.index.indexJar(jcp.getClassNames(), new CachingClassProvider(jcp), ProgressListener.none());
	}

	@Test
	public void obfEntries() {
		assertThat(this.index.getEntryIndex().getClasses(), containsInAnyOrder(
				newClass("cuchaz/enigma/inputs/Keep"), this.baseClass, this.subClassA, this.subClassAA, this.subClassB
		));
	}

	@Test
	public void translationIndex() {
		InheritanceIndex index = this.index.getInheritanceIndex();

		// base class
		assertThat(index.getParents(this.baseClass), is(empty()));
		assertThat(index.getAncestors(this.baseClass), is(empty()));
		assertThat(index.getChildren(this.baseClass), containsInAnyOrder(this.subClassA, this.subClassB
		));

		// subclass a
		assertThat(index.getParents(this.subClassA), contains(this.baseClass));
		assertThat(index.getAncestors(this.subClassA), containsInAnyOrder(this.baseClass));
		assertThat(index.getChildren(this.subClassA), contains(this.subClassAA));

		// subclass aa
		assertThat(index.getParents(this.subClassAA), contains(this.subClassA));
		assertThat(index.getAncestors(this.subClassAA), containsInAnyOrder(this.subClassA, this.baseClass));
		assertThat(index.getChildren(this.subClassAA), is(empty()));

		// subclass b
		assertThat(index.getParents(this.subClassB), contains(this.baseClass));
		assertThat(index.getAncestors(this.subClassB), containsInAnyOrder(this.baseClass));
		assertThat(index.getChildren(this.subClassB), is(empty()));
	}

	@Test
	public void access() {
		assertThat(this.index.getEntryIndex().getFieldAccess(this.nameField), is(new AccessFlags(Opcodes.ACC_PRIVATE)));
		assertThat(this.index.getEntryIndex().getFieldAccess(this.numThingsField), is(new AccessFlags(Opcodes.ACC_PRIVATE)));
	}

	@Test
	public void relatedMethodImplementations() {
		Collection<MethodEntry> entries;

		EntryResolver resolver = new IndexEntryResolver(this.index);
		// getName()
		entries = resolver.resolveEquivalentMethods(newMethod(this.baseClass, "a", "()Ljava/lang/String;"));
		assertThat(entries, containsInAnyOrder(
				newMethod(this.baseClass, "a", "()Ljava/lang/String;"),
				newMethod(this.subClassAA, "a", "()Ljava/lang/String;")
		));
		entries = resolver.resolveEquivalentMethods(newMethod(this.subClassAA, "a", "()Ljava/lang/String;"));
		assertThat(entries, containsInAnyOrder(
				newMethod(this.baseClass, "a", "()Ljava/lang/String;"),
				newMethod(this.subClassAA, "a", "()Ljava/lang/String;")
		));

		// doBaseThings()
		entries = resolver.resolveEquivalentMethods(newMethod(this.baseClass, "a", "()V"));
		assertThat(entries, containsInAnyOrder(
				newMethod(this.baseClass, "a", "()V"),
				newMethod(this.subClassAA, "a", "()V"),
				newMethod(this.subClassB, "a", "()V")
		));
		entries = resolver.resolveEquivalentMethods(newMethod(this.subClassAA, "a", "()V"));
		assertThat(entries, containsInAnyOrder(
				newMethod(this.baseClass, "a", "()V"),
				newMethod(this.subClassAA, "a", "()V"),
				newMethod(this.subClassB, "a", "()V")
		));
		entries = resolver.resolveEquivalentMethods(newMethod(this.subClassB, "a", "()V"));
		assertThat(entries, containsInAnyOrder(
				newMethod(this.baseClass, "a", "()V"),
				newMethod(this.subClassAA, "a", "()V"),
				newMethod(this.subClassB, "a", "()V")
		));

		// doBThings
		entries = resolver.resolveEquivalentMethods(newMethod(this.subClassB, "b", "()V"));
		assertThat(entries, containsInAnyOrder(newMethod(this.subClassB, "b", "()V")));
	}

	@Test
	public void fieldReferences() {
		Collection<EntryReference<FieldEntry, MethodDefEntry>> references;

		// name
		references = this.index.getReferenceIndex().getReferencesToField(this.nameField);
		assertThat(references, containsInAnyOrder(
				newFieldReferenceByMethod(this.nameField, this.baseClass.getName(), "<init>", "(Ljava/lang/String;)V"),
				newFieldReferenceByMethod(this.nameField, this.baseClass.getName(), "a", "()Ljava/lang/String;")
		));

		// numThings
		references = this.index.getReferenceIndex().getReferencesToField(this.numThingsField);
		assertThat(references, containsInAnyOrder(
				newFieldReferenceByMethod(this.numThingsField, this.subClassB.getName(), "<init>", "()V"),
				newFieldReferenceByMethod(this.numThingsField, this.subClassB.getName(), "b", "()V")
		));
	}

	@Test
	public void behaviorReferences() {
		MethodEntry source;
		Collection<EntryReference<MethodEntry, MethodDefEntry>> references;

		// baseClass constructor
		source = newMethod(this.baseClass, "<init>", "(Ljava/lang/String;)V");
		references = this.index.getReferenceIndex().getReferencesToMethod(source);
		assertThat(references, containsInAnyOrder(
				newBehaviorReferenceByMethod(source, this.subClassA.getName(), "<init>", "(Ljava/lang/String;)V"),
				newBehaviorReferenceByMethod(source, this.subClassB.getName(), "<init>", "()V")
		));

		// subClassA constructor
		source = newMethod(this.subClassA, "<init>", "(Ljava/lang/String;)V");
		references = this.index.getReferenceIndex().getReferencesToMethod(source);
		assertThat(references, containsInAnyOrder(
				newBehaviorReferenceByMethod(source, this.subClassAA.getName(), "<init>", "()V")
		));

		// baseClass.getName()
		source = newMethod(this.baseClass, "a", "()Ljava/lang/String;");
		references = this.index.getReferenceIndex().getReferencesToMethod(source);
		assertThat(references, containsInAnyOrder(
				newBehaviorReferenceByMethod(source, this.subClassAA.getName(), "a", "()Ljava/lang/String;"),
				newBehaviorReferenceByMethod(source, this.subClassB.getName(), "a", "()V")
		));

		// subclassAA.getName()
		source = newMethod(this.subClassAA, "a", "()Ljava/lang/String;");
		references = this.index.getReferenceIndex().getReferencesToMethod(source);
		assertThat(references, containsInAnyOrder(
				newBehaviorReferenceByMethod(source, this.subClassAA.getName(), "a", "()V")
		));
	}

	@Test
	public void containsEntries() {
		EntryIndex entryIndex = this.index.getEntryIndex();
		// classes
		assertThat(entryIndex.hasClass(this.baseClass), is(true));
		assertThat(entryIndex.hasClass(this.subClassA), is(true));
		assertThat(entryIndex.hasClass(this.subClassAA), is(true));
		assertThat(entryIndex.hasClass(this.subClassB), is(true));

		// fields
		assertThat(entryIndex.hasField(this.nameField), is(true));
		assertThat(entryIndex.hasField(this.numThingsField), is(true));

		// methods
		// getName()
		assertThat(entryIndex.hasMethod(newMethod(this.baseClass, "a", "()Ljava/lang/String;")), is(true));
		assertThat(entryIndex.hasMethod(newMethod(this.subClassA, "a", "()Ljava/lang/String;")), is(false));
		assertThat(entryIndex.hasMethod(newMethod(this.subClassAA, "a", "()Ljava/lang/String;")), is(true));
		assertThat(entryIndex.hasMethod(newMethod(this.subClassB, "a", "()Ljava/lang/String;")), is(false));

		// doBaseThings()
		assertThat(entryIndex.hasMethod(newMethod(this.baseClass, "a", "()V")), is(true));
		assertThat(entryIndex.hasMethod(newMethod(this.subClassA, "a", "()V")), is(false));
		assertThat(entryIndex.hasMethod(newMethod(this.subClassAA, "a", "()V")), is(true));
		assertThat(entryIndex.hasMethod(newMethod(this.subClassB, "a", "()V")), is(true));

		// doBThings()
		assertThat(entryIndex.hasMethod(newMethod(this.baseClass, "b", "()V")), is(false));
		assertThat(entryIndex.hasMethod(newMethod(this.subClassA, "b", "()V")), is(false));
		assertThat(entryIndex.hasMethod(newMethod(this.subClassAA, "b", "()V")), is(false));
		assertThat(entryIndex.hasMethod(newMethod(this.subClassB, "b", "()V")), is(true));

	}
}
