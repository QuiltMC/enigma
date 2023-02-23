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
import java.util.Collection;

import static cuchaz.enigma.TestEntryFactory.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestJarIndexInheritanceTree {
	public static final Path JAR = TestUtil.obfJar("inheritanceTree");

	private static final ClassEntry BASE_CLASS = newClass("a");
	private static final ClassEntry SUB_CLASS_A = newClass("b");
	private static final ClassEntry SUB_CLASS_AA = newClass("d");
	private static final ClassEntry SUB_CLASS_B = newClass("c");
	private static final FieldEntry NAME_FIELD = newField(BASE_CLASS, "a", "Ljava/lang/String;");
	private static final FieldEntry NUM_THINGS_FIELD = newField(SUB_CLASS_B, "a", "I");

	private final JarIndex index;

	public TestJarIndexInheritanceTree() throws Exception {
		JarClassProvider jcp = new JarClassProvider(JAR);
		index = JarIndex.empty();
		index.indexJar(jcp.getClassNames(), new CachingClassProvider(jcp), ProgressListener.none());
	}

	@Test
	public void obfEntries() {
		assertThat(index.getEntryIndex().getClasses(), containsInAnyOrder(
				newClass("cuchaz/enigma/inputs/Keep"), BASE_CLASS, SUB_CLASS_A, SUB_CLASS_AA, SUB_CLASS_B
		));
	}

	@Test
	public void translationIndex() {
		InheritanceIndex index = this.index.getInheritanceIndex();

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
		assertThat(index.getEntryIndex().getFieldAccess(NAME_FIELD), is(new AccessFlags(Opcodes.ACC_PRIVATE)));
		assertThat(index.getEntryIndex().getFieldAccess(NUM_THINGS_FIELD), is(new AccessFlags(Opcodes.ACC_PRIVATE)));
	}

	@Test
	public void relatedMethodImplementations() {
		Collection<MethodEntry> entries;

		EntryResolver resolver = new IndexEntryResolver(index);
		// getName()
		entries = resolver.resolveEquivalentMethods(newMethod(BASE_CLASS, "a", "()Ljava/lang/String;"));
		assertThat(entries, containsInAnyOrder(
				newMethod(BASE_CLASS, "a", "()Ljava/lang/String;"),
				newMethod(SUB_CLASS_AA, "a", "()Ljava/lang/String;")
		));
		entries = resolver.resolveEquivalentMethods(newMethod(SUB_CLASS_AA, "a", "()Ljava/lang/String;"));
		assertThat(entries, containsInAnyOrder(
				newMethod(BASE_CLASS, "a", "()Ljava/lang/String;"),
				newMethod(SUB_CLASS_AA, "a", "()Ljava/lang/String;")
		));

		// doBaseThings()
		entries = resolver.resolveEquivalentMethods(newMethod(BASE_CLASS, "a", "()V"));
		assertThat(entries, containsInAnyOrder(
				newMethod(BASE_CLASS, "a", "()V"),
				newMethod(SUB_CLASS_AA, "a", "()V"),
				newMethod(SUB_CLASS_B, "a", "()V")
		));
		entries = resolver.resolveEquivalentMethods(newMethod(SUB_CLASS_AA, "a", "()V"));
		assertThat(entries, containsInAnyOrder(
				newMethod(BASE_CLASS, "a", "()V"),
				newMethod(SUB_CLASS_AA, "a", "()V"),
				newMethod(SUB_CLASS_B, "a", "()V")
		));
		entries = resolver.resolveEquivalentMethods(newMethod(SUB_CLASS_B, "a", "()V"));
		assertThat(entries, containsInAnyOrder(
				newMethod(BASE_CLASS, "a", "()V"),
				newMethod(SUB_CLASS_AA, "a", "()V"),
				newMethod(SUB_CLASS_B, "a", "()V")
		));

		// doBThings
		entries = resolver.resolveEquivalentMethods(newMethod(SUB_CLASS_B, "b", "()V"));
		assertThat(entries, containsInAnyOrder(newMethod(SUB_CLASS_B, "b", "()V")));
	}

	@Test
	public void fieldReferences() {
		Collection<EntryReference<FieldEntry, MethodDefEntry>> references;

		// name
		references = index.getReferenceIndex().getReferencesToField(NAME_FIELD);
		assertThat(references, containsInAnyOrder(
				newFieldReferenceByMethod(NAME_FIELD, BASE_CLASS.getName(), "<init>", "(Ljava/lang/String;)V"),
				newFieldReferenceByMethod(NAME_FIELD, BASE_CLASS.getName(), "a", "()Ljava/lang/String;")
		));

		// numThings
		references = index.getReferenceIndex().getReferencesToField(NUM_THINGS_FIELD);
		assertThat(references, containsInAnyOrder(
				newFieldReferenceByMethod(NUM_THINGS_FIELD, SUB_CLASS_B.getName(), "<init>", "()V"),
				newFieldReferenceByMethod(NUM_THINGS_FIELD, SUB_CLASS_B.getName(), "b", "()V")
		));
	}

	@Test
	public void behaviorReferences() {
		MethodEntry source;
		Collection<EntryReference<MethodEntry, MethodDefEntry>> references;

		// baseClass constructor
		source = newMethod(BASE_CLASS, "<init>", "(Ljava/lang/String;)V");
		references = index.getReferenceIndex().getReferencesToMethod(source);
		assertThat(references, containsInAnyOrder(
				newBehaviorReferenceByMethod(source, SUB_CLASS_A.getName(), "<init>", "(Ljava/lang/String;)V"),
				newBehaviorReferenceByMethod(source, SUB_CLASS_B.getName(), "<init>", "()V")
		));

		// subClassA constructor
		source = newMethod(SUB_CLASS_A, "<init>", "(Ljava/lang/String;)V");
		references = index.getReferenceIndex().getReferencesToMethod(source);
		assertThat(references, containsInAnyOrder(
				newBehaviorReferenceByMethod(source, SUB_CLASS_AA.getName(), "<init>", "()V")
		));

		// baseClass.getName()
		source = newMethod(BASE_CLASS, "a", "()Ljava/lang/String;");
		references = index.getReferenceIndex().getReferencesToMethod(source);
		assertThat(references, containsInAnyOrder(
				newBehaviorReferenceByMethod(source, SUB_CLASS_AA.getName(), "a", "()Ljava/lang/String;"),
				newBehaviorReferenceByMethod(source, SUB_CLASS_B.getName(), "a", "()V")
		));

		// subclassAA.getName()
		source = newMethod(SUB_CLASS_AA, "a", "()Ljava/lang/String;");
		references = index.getReferenceIndex().getReferencesToMethod(source);
		assertThat(references, containsInAnyOrder(
				newBehaviorReferenceByMethod(source, SUB_CLASS_AA.getName(), "a", "()V")
		));
	}

	@Test
	public void containsEntries() {
		EntryIndex entryIndex = index.getEntryIndex();
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
		assertThat(entryIndex.hasMethod(newMethod(BASE_CLASS, "a", "()Ljava/lang/String;")), is(true));
		assertThat(entryIndex.hasMethod(newMethod(SUB_CLASS_A, "a", "()Ljava/lang/String;")), is(false));
		assertThat(entryIndex.hasMethod(newMethod(SUB_CLASS_AA, "a", "()Ljava/lang/String;")), is(true));
		assertThat(entryIndex.hasMethod(newMethod(SUB_CLASS_B, "a", "()Ljava/lang/String;")), is(false));

		// doBaseThings()
		assertThat(entryIndex.hasMethod(newMethod(BASE_CLASS, "a", "()V")), is(true));
		assertThat(entryIndex.hasMethod(newMethod(SUB_CLASS_A, "a", "()V")), is(false));
		assertThat(entryIndex.hasMethod(newMethod(SUB_CLASS_AA, "a", "()V")), is(true));
		assertThat(entryIndex.hasMethod(newMethod(SUB_CLASS_B, "a", "()V")), is(true));

		// doBThings()
		assertThat(entryIndex.hasMethod(newMethod(BASE_CLASS, "b", "()V")), is(false));
		assertThat(entryIndex.hasMethod(newMethod(SUB_CLASS_A, "b", "()V")), is(false));
		assertThat(entryIndex.hasMethod(newMethod(SUB_CLASS_AA, "b", "()V")), is(false));
		assertThat(entryIndex.hasMethod(newMethod(SUB_CLASS_B, "b", "()V")), is(true));

	}
}
