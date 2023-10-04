package org.quiltmc.enigma;

import org.quiltmc.enigma.gui.EditableType;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.docker.AllClassesDocker;
import org.quiltmc.enigma.gui.element.ClassSelectorPopupMenu;
import org.quiltmc.enigma.gui.util.PackageRenamer;
import org.quiltmc.enigma.api.translation.TranslateResult;
import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.translation.mapping.serde.MappingFormat;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@DisabledIf(value = "java.awt.GraphicsEnvironment#isHeadless", disabledReason = "headless environment")
public class TestPackageRename {
	public static final Path JAR = TestUtil.obfJar("complete");
	public static final Path MAPPINGS = Path.of("src/test/resources/test_mappings");
	private static Translator deobfuscator;

	@Test
	void testRemoveOnePackage() throws InterruptedException {
		renamePackage("a/b/c", "a/c", PackageRenamer.Mode.REFACTOR);

		assertMapping(TestEntryFactory.newClass("A"), TestEntryFactory.newClass("a/c/A"));
		assertMapping(TestEntryFactory.newClass("B"), TestEntryFactory.newClass("a/c/B"));
		assertMapping(TestEntryFactory.newClass("C"), TestEntryFactory.newClass("a/c/C"));
		assertMapping(TestEntryFactory.newClass("D"), TestEntryFactory.newClass("a/D"));
		assertMapping(TestEntryFactory.newClass("E"), TestEntryFactory.newClass("E"));
	}

	@Test
	void testRemoveTwoPackages() throws InterruptedException {
		renamePackage("a/b/c", "a", PackageRenamer.Mode.REFACTOR);

		assertMapping(TestEntryFactory.newClass("A"), TestEntryFactory.newClass("a/A"));
		assertMapping(TestEntryFactory.newClass("B"), TestEntryFactory.newClass("a/B"));
		assertMapping(TestEntryFactory.newClass("C"), TestEntryFactory.newClass("a/C"));
		assertMapping(TestEntryFactory.newClass("D"), TestEntryFactory.newClass("a/D"));
		assertMapping(TestEntryFactory.newClass("E"), TestEntryFactory.newClass("E"));
	}

	@Test
	void testPackageConservation() throws InterruptedException {
		renamePackage("a/b", "a", PackageRenamer.Mode.REFACTOR);

		assertMapping(TestEntryFactory.newClass("A"), TestEntryFactory.newClass("a/c/A"));
		assertMapping(TestEntryFactory.newClass("B"), TestEntryFactory.newClass("a/c/B"));
		assertMapping(TestEntryFactory.newClass("C"), TestEntryFactory.newClass("a/C"));
		assertMapping(TestEntryFactory.newClass("D"), TestEntryFactory.newClass("a/D"));
		assertMapping(TestEntryFactory.newClass("E"), TestEntryFactory.newClass("E"));
	}

	@Test
	void testAppendOnePackage() throws InterruptedException {
		renamePackage("a/b/c", "a/b/c/d", PackageRenamer.Mode.REFACTOR);

		assertMapping(TestEntryFactory.newClass("A"), TestEntryFactory.newClass("a/b/c/d/A"));
		assertMapping(TestEntryFactory.newClass("B"), TestEntryFactory.newClass("a/b/c/d/B"));
		assertMapping(TestEntryFactory.newClass("C"), TestEntryFactory.newClass("a/b/C"));
		assertMapping(TestEntryFactory.newClass("D"), TestEntryFactory.newClass("a/D"));
		assertMapping(TestEntryFactory.newClass("E"), TestEntryFactory.newClass("E"));
	}

	@Test
	void testSimpleRename() throws InterruptedException {
		renamePackage("a/b/c", "a/b/d", PackageRenamer.Mode.REFACTOR);

		assertMapping(TestEntryFactory.newClass("A"), TestEntryFactory.newClass("a/b/d/A"));
		assertMapping(TestEntryFactory.newClass("B"), TestEntryFactory.newClass("a/b/d/B"));
		assertMapping(TestEntryFactory.newClass("C"), TestEntryFactory.newClass("a/b/C"));
		assertMapping(TestEntryFactory.newClass("D"), TestEntryFactory.newClass("a/D"));
		assertMapping(TestEntryFactory.newClass("E"), TestEntryFactory.newClass("E"));
	}

	@Test
	void testFirstPackageRename() throws InterruptedException {
		renamePackage("a", "b", PackageRenamer.Mode.REFACTOR);

		assertMapping(TestEntryFactory.newClass("A"), TestEntryFactory.newClass("b/b/c/A"));
		assertMapping(TestEntryFactory.newClass("B"), TestEntryFactory.newClass("b/b/c/B"));
		assertMapping(TestEntryFactory.newClass("C"), TestEntryFactory.newClass("b/b/C"));
		assertMapping(TestEntryFactory.newClass("D"), TestEntryFactory.newClass("b/D"));
		assertMapping(TestEntryFactory.newClass("E"), TestEntryFactory.newClass("E"));
	}

	@Test
	void testPackageMove() throws InterruptedException {
		renamePackage("a/b/c", "a/c", PackageRenamer.Mode.MOVE);

		assertMapping(TestEntryFactory.newClass("A"), TestEntryFactory.newClass("a/c/A"));
		assertMapping(TestEntryFactory.newClass("B"), TestEntryFactory.newClass("a/c/B"));
		assertMapping(TestEntryFactory.newClass("C"), TestEntryFactory.newClass("a/b/C"));
		assertMapping(TestEntryFactory.newClass("D"), TestEntryFactory.newClass("a/D"));
		assertMapping(TestEntryFactory.newClass("E"), TestEntryFactory.newClass("E"));
	}

	private static void renamePackage(String packageName, String newName, PackageRenamer.Mode mode) throws InterruptedException {
		ClassSelectorPopupMenu menu = setupMenu();
		assertBaseMappings();

		CountDownLatch packageRenameLatch = new CountDownLatch(1);
		menu.createPackageRenamer(mode).renamePackage(packageName, newName).thenRun(packageRenameLatch::countDown);
		packageRenameLatch.await();
	}

	private static ClassSelectorPopupMenu setupMenu() throws InterruptedException {
		Set<EditableType> editables = EnumSet.allOf(EditableType.class);
		editables.addAll(List.of(EditableType.values()));
		Gui gui = new Gui(EnigmaProfile.EMPTY, editables, false);
		gui.setShowsProgressBars(false);

		CountDownLatch latch = new CountDownLatch(1);
		gui.getController().openJar(JAR).thenRun(() -> gui.getController().openMappings(MappingFormat.ENIGMA_DIRECTORY, MAPPINGS).thenRun(latch::countDown));
		latch.await();

		deobfuscator = gui.getController().getProject().getMapper().getDeobfuscator();
		return gui.getDockerManager().getDocker(AllClassesDocker.class).getPopupMenu();
	}

	private static void assertBaseMappings() {
		// assert starting mappings
		assertMapping(TestEntryFactory.newClass("A"), TestEntryFactory.newClass("a/b/c/A"));
		assertMapping(TestEntryFactory.newClass("B"), TestEntryFactory.newClass("a/b/c/B"));
		assertMapping(TestEntryFactory.newClass("C"), TestEntryFactory.newClass("a/b/C"));
		assertMapping(TestEntryFactory.newClass("D"), TestEntryFactory.newClass("a/D"));
		assertMapping(TestEntryFactory.newClass("E"), TestEntryFactory.newClass("E"));
	}

	private static void assertMapping(Entry<?> obf, Entry<?> deobf) {
		TranslateResult<? extends Entry<?>> result = deobfuscator.extendedTranslate(obf);
		assertThat(result, is(notNullValue()));
		assertThat(result.getValue(), is(deobf));

		String deobfName = result.getValue().getName();
		if (deobfName != null) {
			assertThat(deobfName, is(deobf.getName()));
		}
	}
}
