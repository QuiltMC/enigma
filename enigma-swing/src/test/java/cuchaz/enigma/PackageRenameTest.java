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
import org.junit.jupiter.api.condition.DisabledIf;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import static cuchaz.enigma.TestEntryFactory.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class PackageRenameTest {
	public static final Path JAR = TestUtil.obfJar("complete");
	public static final Path MAPPINGS = Path.of("src/test/resources/test_mappings");
	private static Translator deobfuscator;
	private static ClassSelectorPopupMenu menu;

	@Test
	@DisabledIf(value = "java.awt.GraphicsEnvironment#isHeadless", disabledReason = "headless environment")
	void testRemoveOnePackage() throws InterruptedException {
		runTest(() -> {
			renamePackage("a/b/c", "a/c");

			assertMapping(newClass("A"), newClass("a/c/A"));
			assertMapping(newClass("B"), newClass("a/c/B"));
			assertMapping(newClass("C"), newClass("a/c/C"));
			assertMapping(newClass("D"), newClass("a/D"));
			assertMapping(newClass("E"), newClass("E"));
		});
	}

	@Test
	@DisabledIf(value = "java.awt.GraphicsEnvironment#isHeadless", disabledReason = "headless environment")
	void testRemoveTwoPackages() throws InterruptedException {
		runTest(() -> {
			renamePackage("a/b/c", "a");

			assertMapping(newClass("A"), newClass("a/A"));
			assertMapping(newClass("B"), newClass("a/B"));
			assertMapping(newClass("C"), newClass("a/C"));
			assertMapping(newClass("D"), newClass("a/D"));
			assertMapping(newClass("E"), newClass("E"));
		});
	}

	@Test
	@DisabledIf(value = "java.awt.GraphicsEnvironment#isHeadless", disabledReason = "headless environment")
	void testPackageConservation() throws InterruptedException {
		runTest(() -> {
			renamePackage("a/b", "a");

			assertMapping(newClass("A"), newClass("a/c/A"));
			assertMapping(newClass("B"), newClass("a/c/B"));
			assertMapping(newClass("C"), newClass("a/C"));
			assertMapping(newClass("D"), newClass("a/D"));
			assertMapping(newClass("E"), newClass("E"));
		});
	}

	@Test
	@DisabledIf(value = "java.awt.GraphicsEnvironment#isHeadless", disabledReason = "headless environment")
	void testAppendOnePackage() throws InterruptedException {
		runTest(() -> {
			renamePackage("a/b/c", "a/b/c/d");

			assertMapping(newClass("A"), newClass("a/b/c/d/A"));
			assertMapping(newClass("B"), newClass("a/b/c/d/B"));
			assertMapping(newClass("C"), newClass("a/b/C"));
			assertMapping(newClass("D"), newClass("a/D"));
			assertMapping(newClass("E"), newClass("E"));
		});
	}

	@Test
	@DisabledIf(value = "java.awt.GraphicsEnvironment#isHeadless", disabledReason = "headless environment")
	void testSimpleRename() throws InterruptedException {
		runTest(() -> {
			renamePackage("a/b/c", "a/b/d");

			assertMapping(newClass("A"), newClass("a/b/d/A"));
			assertMapping(newClass("B"), newClass("a/b/d/B"));
			assertMapping(newClass("C"), newClass("a/b/C"));
			assertMapping(newClass("D"), newClass("a/D"));
			assertMapping(newClass("E"), newClass("E"));
		});
	}

	@Test
	@DisabledIf(value = "java.awt.GraphicsEnvironment#isHeadless", disabledReason = "headless environment")
	void testFirstPackageRename() throws InterruptedException {
		runTest(() -> {
			renamePackage("a", "b");

			assertMapping(newClass("A"), newClass("b/b/c/A"));
			assertMapping(newClass("B"), newClass("b/b/c/B"));
			assertMapping(newClass("C"), newClass("b/b/C"));
			assertMapping(newClass("D"), newClass("b/D"));
			assertMapping(newClass("E"), newClass("E"));
		});
	}

	private interface ThrowingRunnable {
		void run() throws InterruptedException;
	}

	private static void runTest(ThrowingRunnable test) throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);

		CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
			try {
				latch.await();
				test.run();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});

		Thread t = new Thread(() -> {
			try {
				menu = setupMenu();
				latch.countDown();
				future.get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		});


		t.start();
		latch.await();
		future.join();
		t.stop();
		throw new RuntimeException("end of method");
	}

	private static void renamePackage(String packageName, String input) throws InterruptedException {
		assertBaseMappings();
		CountDownLatch packageRenameLatch = new CountDownLatch(1);
		menu.renamePackage(packageName, input).thenRun(packageRenameLatch::countDown);
		packageRenameLatch.await();
	}

	private static ClassSelectorPopupMenu setupMenu() throws InterruptedException {
		Set<EditableType> editables = EnumSet.allOf(EditableType.class);
		editables.addAll(List.of(EditableType.values()));
		Gui gui = new Gui(EnigmaProfile.EMPTY, editables, false);

		CountDownLatch latch = new CountDownLatch(1);
		gui.getController().openJar(JAR).thenRun(() -> gui.getController().openMappings(MappingFormat.ENIGMA_DIRECTORY, MAPPINGS).thenRun(latch::countDown));
		latch.await();

		deobfuscator = gui.getController().getProject().getMapper().getDeobfuscator();
		return Docker.getDocker(AllClassesDocker.class).getPopupMenu();
	}

	private static void assertBaseMappings() {
		// assert starting mappings
		assertMapping(newClass("A"), newClass("a/b/c/A"));
		assertMapping(newClass("B"), newClass("a/b/c/B"));
		assertMapping(newClass("C"), newClass("a/b/C"));
		assertMapping(newClass("D"), newClass("a/D"));
		assertMapping(newClass("E"), newClass("E"));
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
