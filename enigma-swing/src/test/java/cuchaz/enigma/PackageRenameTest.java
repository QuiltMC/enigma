package cuchaz.enigma;

import cuchaz.enigma.gui.EditableType;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.docker.AllClassesDocker;
import cuchaz.enigma.gui.docker.Docker;
import cuchaz.enigma.gui.elements.ClassSelectorPopupMenu;
import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.representation.entry.Entry;
import org.junit.jupiter.api.Test;
import org.tinylog.Logger;

import java.awt.HeadlessException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static cuchaz.enigma.TestEntryFactory.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class PackageRenameTest {
	public static final Path JAR = TestUtil.obfJar("complete");
	public static final Path MAPPINGS = Path.of("src/test/resources/test_mappings");
	private static Translator deobfuscator;

	@Test
	void testRemoveOnePackage() throws InterruptedException {
		this.renamePackage("a/b/c", "a/c");
		this.assertMapping(newClass("A"), newClass("a/c/A"));
		this.assertMapping(newClass("B"), newClass("a/c/B"));
		this.assertMapping(newClass("C"), newClass("a/c/C"));
		this.assertMapping(newClass("D"), newClass("a/D"));
		this.assertMapping(newClass("E"), newClass("E"));
	}

	@Test
	void testRemoveTwoPackages() throws InterruptedException {
		this.renamePackage("a/b/c", "a");
		this.assertMapping(newClass("A"), newClass("a/A"));
		this.assertMapping(newClass("B"), newClass("a/B"));
		this.assertMapping(newClass("C"), newClass("a/C"));
		this.assertMapping(newClass("D"), newClass("a/D"));
		this.assertMapping(newClass("E"), newClass("E"));
	}

	@Test
	void testPackageConservation() throws InterruptedException {
		this.renamePackage("a/b", "a");
		this.assertMapping(newClass("A"), newClass("a/c/A"));
		this.assertMapping(newClass("B"), newClass("a/c/B"));
		this.assertMapping(newClass("C"), newClass("a/C"));
		this.assertMapping(newClass("D"), newClass("a/D"));
		this.assertMapping(newClass("E"), newClass("E"));
	}

	@Test
	void testAppendOnePackage() throws InterruptedException {
		this.renamePackage("a/b/c", "a/b/c/d");
		this.assertMapping(newClass("A"), newClass("a/b/c/d/A"));
		this.assertMapping(newClass("B"), newClass("a/b/c/d/B"));
		this.assertMapping(newClass("C"), newClass("a/b/C"));
		this.assertMapping(newClass("D"), newClass("a/D"));
		this.assertMapping(newClass("E"), newClass("E"));
	}

	@Test
	void testSimpleRename() throws InterruptedException {
		this.renamePackage("a/b/c", "a/b/d");
		this.assertMapping(newClass("A"), newClass("a/b/d/A"));
		this.assertMapping(newClass("B"), newClass("a/b/d/B"));
		this.assertMapping(newClass("C"), newClass("a/b/C"));
		this.assertMapping(newClass("D"), newClass("a/D"));
		this.assertMapping(newClass("E"), newClass("E"));
	}

	@Test
	void testFirstPackageRename() throws InterruptedException {
		this.renamePackage("a", "b");
		this.assertMapping(newClass("A"), newClass("b/b/c/A"));
		this.assertMapping(newClass("B"), newClass("b/b/c/B"));
		this.assertMapping(newClass("C"), newClass("b/b/C"));
		this.assertMapping(newClass("D"), newClass("b/D"));
		this.assertMapping(newClass("E"), newClass("E"));
	}

	private void renamePackage(String packageName, String input) throws InterruptedException {
		try {
			ClassSelectorPopupMenu menu = this.setupMenu();

			if (menu != null) {
				this.assertBaseMappings();
				CountDownLatch packageRenameLatch = new CountDownLatch(1);
				menu.renamePackage(packageName, input).thenRun(packageRenameLatch::countDown);
				packageRenameLatch.await();
			}
		} catch (ExceptionInInitializerError ignored) {
			// skip the test in a headless environment without xvfb. it'll be run through github actions
		}
	}

	private ClassSelectorPopupMenu setupMenu() throws InterruptedException {
		Set<EditableType> editables = EnumSet.allOf(EditableType.class);
		editables.addAll(List.of(EditableType.values()));
		Gui gui = new Gui(EnigmaProfile.EMPTY, editables, false);

		CountDownLatch latch = new CountDownLatch(1);
		gui.getController().openJar(JAR).thenRun(() -> gui.getController().openMappings(MappingFormat.ENIGMA_DIRECTORY, MAPPINGS).thenRun(latch::countDown));
		latch.await();

		deobfuscator = gui.getController().getProject().getMapper().getDeobfuscator();
		return Docker.getDocker(AllClassesDocker.class).getPopupMenu();
	}

	private void assertBaseMappings() {
		// assert starting mappings
		this.assertMapping(newClass("A"), newClass("a/b/c/A"));
		this.assertMapping(newClass("B"), newClass("a/b/c/B"));
		this.assertMapping(newClass("C"), newClass("a/b/C"));
		this.assertMapping(newClass("D"), newClass("a/D"));
		this.assertMapping(newClass("E"), newClass("E"));
	}

	private void assertMapping(Entry<?> obf, Entry<?> deobf) {
		TranslateResult<? extends Entry<?>> result = deobfuscator.extendedTranslate(obf);
		assertThat(result, is(notNullValue()));
		assertThat(result.getValue(), is(deobf));

		String deobfName = result.getValue().getName();
		if (deobfName != null) {
			assertThat(deobfName, is(deobf.getName()));
		}
	}
}
