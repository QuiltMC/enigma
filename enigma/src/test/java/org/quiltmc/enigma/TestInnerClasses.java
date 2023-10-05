package org.quiltmc.enigma;

import org.quiltmc.enigma.analysis.index.JarIndex;
import org.quiltmc.enigma.classprovider.CachingClassProvider;
import org.quiltmc.enigma.classprovider.JarClassProvider;
import org.quiltmc.enigma.source.Decompiler;
import org.quiltmc.enigma.source.Decompilers;
import org.quiltmc.enigma.source.SourceSettings;
import org.quiltmc.enigma.translation.representation.entry.ClassEntry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestInnerClasses {
	private static final ClassEntry SIMPLE_OUTER = TestEntryFactory.newClass("d");
	private static final ClassEntry SIMPLE_INNER = TestEntryFactory.newClass("d$a");
	private static final ClassEntry CONSTRUCTOR_ARGS_OUTER = TestEntryFactory.newClass("c");
	private static final ClassEntry CONSTRUCTOR_ARGS_INNER = TestEntryFactory.newClass("c$a");
	private static final ClassEntry CLASS_TREE_ROOT = TestEntryFactory.newClass("f");
	private static final ClassEntry CLASS_TREE_LEVEL_1 = TestEntryFactory.newClass("f$a");
	private static final ClassEntry CLASS_TREE_LEVEL_2 = TestEntryFactory.newClass("f$a$a");
	private static final ClassEntry CLASS_TREE_LEVEL_3 = TestEntryFactory.newClass("f$a$a$a");

	public static final Path JAR = TestUtil.obfJar("inner_classes");
	private final JarIndex index;
	private final Decompiler decompiler;

	public TestInnerClasses() throws Exception {
		JarClassProvider jcp = new JarClassProvider(JAR);
		CachingClassProvider classProvider = new CachingClassProvider(jcp);
		this.index = JarIndex.empty();
		this.index.indexJar(jcp.getClassNames(), classProvider, ProgressListener.none());
		this.decompiler = Decompilers.CFR.create(classProvider, new SourceSettings(false, false));
	}

	@Test
	public void simple() {
		this.decompile(SIMPLE_OUTER);
	}

	@Test
	public void constructorArgs() {
		this.decompile(CONSTRUCTOR_ARGS_OUTER);
	}

	@Test
	public void classTree() {
		// root level
		assertTrue(this.index.getEntryIndex().hasClass(CLASS_TREE_ROOT));

		// level 1
		ClassEntry fullClassEntry = new ClassEntry(CLASS_TREE_ROOT.getName()
				+ "$" + CLASS_TREE_LEVEL_1.getSimpleName());
		assertTrue(this.index.getEntryIndex().hasClass(fullClassEntry));

		// level 2
		fullClassEntry = new ClassEntry(CLASS_TREE_ROOT.getName()
			+ "$" + CLASS_TREE_LEVEL_1.getSimpleName()
			+ "$" + CLASS_TREE_LEVEL_2.getSimpleName());
		assertTrue(this.index.getEntryIndex().hasClass(fullClassEntry));

		// level 3
		fullClassEntry = new ClassEntry(CLASS_TREE_ROOT.getName()
			+ "$" + CLASS_TREE_LEVEL_1.getSimpleName()
			+ "$" + CLASS_TREE_LEVEL_2.getSimpleName()
			+ "$" + CLASS_TREE_LEVEL_3.getSimpleName());
		assertTrue(this.index.getEntryIndex().hasClass(fullClassEntry));
	}

	private void decompile(ClassEntry classEntry) {
		this.decompiler.getSource(classEntry.getName(), null);
	}
}
