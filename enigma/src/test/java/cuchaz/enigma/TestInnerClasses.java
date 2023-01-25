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
import java.nio.file.Paths;

import static cuchaz.enigma.TestEntryFactory.newClass;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestInnerClasses {
	private static final ClassEntry SimpleOuter = newClass("d");
	private static final ClassEntry SimpleInner = newClass("d$a");
	private static final ClassEntry ConstructorArgsOuter = newClass("c");
	private static final ClassEntry ConstructorArgsInner = newClass("c$a");
	private static final ClassEntry ClassTreeRoot = newClass("f");
	private static final ClassEntry ClassTreeLevel1 = newClass("f$a");
	private static final ClassEntry ClassTreeLevel2 = newClass("f$a$a");
	private static final ClassEntry ClassTreeLevel3 = newClass("f$a$a$a");
	public static final Path JAR = Paths.get("build/test-obf/innerClasses.jar");
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
		decompile(SimpleOuter);
	}

	@Test
	public void constructorArgs() {
		decompile(ConstructorArgsOuter);
	}

	@Test
	public void classTree() {
		// root level
		assertTrue(index.getEntryIndex().hasClass(ClassTreeRoot));

		// level 1
		ClassEntry fullClassEntry = new ClassEntry(ClassTreeRoot.getName()
			+ "$" + ClassTreeLevel1.getSimpleName());
		assertTrue(index.getEntryIndex().hasClass(fullClassEntry));

		// level 2
		fullClassEntry = new ClassEntry(ClassTreeRoot.getName()
			+ "$" + ClassTreeLevel1.getSimpleName()
			+ "$" + ClassTreeLevel2.getSimpleName());
		assertTrue(index.getEntryIndex().hasClass(fullClassEntry));

		// level 3
		fullClassEntry = new ClassEntry(ClassTreeRoot.getName()
			+ "$" + ClassTreeLevel1.getSimpleName()
			+ "$" + ClassTreeLevel2.getSimpleName()
			+ "$" + ClassTreeLevel3.getSimpleName());
		assertTrue(index.getEntryIndex().hasClass(fullClassEntry));
	}

	private void decompile(ClassEntry classEntry) {
		decompiler.getSource(classEntry.getName());
	}
}
