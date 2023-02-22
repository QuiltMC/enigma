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

import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.classprovider.CachingClassProvider;
import cuchaz.enigma.classprovider.JarClassProvider;
import cuchaz.enigma.source.Decompiler;
import cuchaz.enigma.source.Decompilers;
import cuchaz.enigma.source.SourceSettings;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static cuchaz.enigma.TestEntryFactory.newClass;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestInnerClasses {
	private static final ClassEntry SIMPLE_OUTER = newClass("d");
	private static final ClassEntry SIMPLE_INNER = newClass("d$a");
	private static final ClassEntry CONSTRUCTOR_ARGS_OUTER = newClass("c");
	private static final ClassEntry CONSTRUCTOR_ARGS_INNER = newClass("c$a");
	private static final ClassEntry CLASS_TREE_ROOT = newClass("f");
	private static final ClassEntry CLASS_TREE_LEVEL_1 = newClass("f$a");
	private static final ClassEntry CLASS_TREE_LEVEL_2 = newClass("f$a$a");
	private static final ClassEntry CLASS_TREE_LEVEL_3 = newClass("f$a$a$a");

	public static final Path JAR = TestUtil.obfJar("innerClasses");
	private final JarIndex index;
	private final Decompiler decompiler;

	public TestInnerClasses() throws Exception {
		JarClassProvider jcp = new JarClassProvider(JAR);
		CachingClassProvider classProvider = new CachingClassProvider(jcp);
		index = JarIndex.empty();
		index.indexJar(jcp.getClassNames(), classProvider, ProgressListener.none());
		decompiler = Decompilers.CFR.create(classProvider, new SourceSettings(false, false));
	}

	@Test
	public void simple() {
		decompile(SIMPLE_OUTER);
	}

	@Test
	public void constructorArgs() {
		decompile(CONSTRUCTOR_ARGS_OUTER);
	}

	@Test
	public void classTree() {
		// root level
		assertTrue(index.getEntryIndex().hasClass(CLASS_TREE_ROOT));

		// level 1
		ClassEntry fullClassEntry = new ClassEntry(CLASS_TREE_ROOT.getName()
			+ "$" + CLASS_TREE_LEVEL_1.getSimpleName());
		assertTrue(index.getEntryIndex().hasClass(fullClassEntry));

		// level 2
		fullClassEntry = new ClassEntry(CLASS_TREE_ROOT.getName()
			+ "$" + CLASS_TREE_LEVEL_1.getSimpleName()
			+ "$" + CLASS_TREE_LEVEL_2.getSimpleName());
		assertTrue(index.getEntryIndex().hasClass(fullClassEntry));

		// level 3
		fullClassEntry = new ClassEntry(CLASS_TREE_ROOT.getName()
			+ "$" + CLASS_TREE_LEVEL_1.getSimpleName()
			+ "$" + CLASS_TREE_LEVEL_2.getSimpleName()
			+ "$" + CLASS_TREE_LEVEL_3.getSimpleName());
		assertTrue(index.getEntryIndex().hasClass(fullClassEntry));
	}

	private void decompile(ClassEntry classEntry) {
		decompiler.getSource(classEntry.getName());
	}
}
