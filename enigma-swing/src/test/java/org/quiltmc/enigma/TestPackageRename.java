package org.quiltmc.enigma;

import org.quiltmc.enigma.api.EnigmaProfile;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.gui.EditableType;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.docker.AllClassesDocker;
import org.quiltmc.enigma.gui.element.ClassSelectorPopupMenu;
import org.quiltmc.enigma.gui.util.PackageRenamer;
import org.quiltmc.enigma.api.translation.TranslateResult;
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
	private static EnigmaProject project;

	@Test
	void testRemoveOnePackage() throws InterruptedException {
		renamePackage("a/b/c", "a/c", PackageRenamer.Mode.REFACTOR);

		assertMapping("a", "a/c/A");
		assertMapping("b", "a/c/B");
		assertMapping("c", "a/c/C");
		assertMapping("d", "a/D");
		assertMapping("e", "E");
	}

	@Test
	void testRemoveTwoPackages() throws InterruptedException {
		renamePackage("a/b/c", "a", PackageRenamer.Mode.REFACTOR);

		assertMapping("a", "a/A");
		assertMapping("b", "a/B");
		assertMapping("c", "a/C");
		assertMapping("d", "a/D");
		assertMapping("e", "E");
	}

	@Test
	void testPackageConservation() throws InterruptedException {
		renamePackage("a/b", "a", PackageRenamer.Mode.REFACTOR);

		assertMapping("a", "a/c/A");
		assertMapping("b", "a/c/B");
		assertMapping("c", "a/C");
		assertMapping("d", "a/D");
		assertMapping("e", "E");
	}

	@Test
	void testAppendOnePackage() throws InterruptedException {
		renamePackage("a/b/c", "a/b/c/d", PackageRenamer.Mode.REFACTOR);

		assertMapping("a", "a/b/c/d/A");
		assertMapping("b", "a/b/c/d/B");
		assertMapping("c", "a/b/C");
		assertMapping("d", "a/D");
		assertMapping("e", "E");
	}

	@Test
	void testSimpleRename() throws InterruptedException {
		renamePackage("a/b/c", "a/b/d", PackageRenamer.Mode.REFACTOR);

		assertMapping("a", "a/b/d/A");
		assertMapping("b", "a/b/d/B");
		assertMapping("c", "a/b/C");
		assertMapping("d", "a/D");
		assertMapping("e", "E");
	}

	@Test
	void testFirstPackageRename() throws InterruptedException {
		renamePackage("a", "b", PackageRenamer.Mode.REFACTOR);

		assertMapping("a", "b/b/c/A");
		assertMapping("b", "b/b/c/B");
		assertMapping("c", "b/b/C");
		assertMapping("d", "b/D");
		assertMapping("e", "E");
	}

	@Test
	void testPackageMove() throws InterruptedException {
		renamePackage("a/b/c", "a/c", PackageRenamer.Mode.MOVE);

		assertMapping("a", "a/c/A");
		assertMapping("b", "a/c/B");
		assertMapping("c", "a/b/C");
		assertMapping("d", "a/D");
		assertMapping("e", "E");
	}

	@Test
	void testPackageMerge() throws InterruptedException {
		renamePackage("b", "a", PackageRenamer.Mode.REFACTOR);

		assertMapping("a", "a/b/c/A");
		assertMapping("b", "a/b/c/B");
		assertMapping("c", "a/b/C");
		assertMapping("d", "a/D");
		assertMapping("e", "E");
		assertMapping("f", "a/F");
		assertMapping("g", "a/c/G");
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
		gui.getController().openJar(JAR).thenRun(() -> gui.getController().openMappings(MAPPINGS).thenRun(latch::countDown));
		latch.await();

		project = gui.getController().getProject();
		return gui.getDockerManager().getDocker(AllClassesDocker.class).getPopupMenu();
	}

	private static void assertBaseMappings() {
		// a
		assertMapping("a", "a/b/c/A");
		assertMapping("b", "a/b/c/B");
		assertMapping("c", "a/b/C");
		assertMapping("d", "a/D");

		// lone
		assertMapping("e", "E");

		// b
		assertMapping("f", "b/F");
		assertMapping("g", "b/c/G");
	}

	private static void assertMapping(String obf, String deobf) {
		assertMapping(TestEntryFactory.newClass(obf), TestEntryFactory.newClass(deobf));
	}

	private static void assertMapping(Entry<?> obf, Entry<?> deobf) {
		if (!project.getRemapper().getMappings().contains(obf)) {
			throw new RuntimeException("no mapping for " + obf.getFullName() + " !");
		}

		TranslateResult<? extends Entry<?>> result = project.getRemapper().getDeobfuscator().extendedTranslate(obf);
		assertThat(result, is(notNullValue()));
		assertThat(result.getValue(), is(deobf));

		String deobfName = result.getValue().getName();
		if (deobfName != null) {
			assertThat(deobfName, is(deobf.getName()));
		}
	}
}
