package org.quiltmc.enigma;

import org.quiltmc.enigma.api.analysis.index.JarIndex;
import org.quiltmc.enigma.api.analysis.index.PackageVisibilityIndex;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.class_provider.JarClassProvider;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class PackageVisibilityIndexTest {
	public static final Path JAR = TestUtil.obfJar("package_access");
	private static final ClassEntry KEEP = TestEntryFactory.newClass("org/quiltmc/enigma/input/Keep");
	private static final ClassEntry BASE = TestEntryFactory.newClass("a");
	private static final ClassEntry SAME_PACKAGE_CHILD = TestEntryFactory.newClass("b");
	private static final ClassEntry SAME_PACKAGE_CHILD_INNER = TestEntryFactory.newClass("b$a");
	private static final ClassEntry OTHER_PACKAGE_CHILD = TestEntryFactory.newClass("c");
	private static final ClassEntry OTHER_PACKAGE_CHILD_INNER = TestEntryFactory.newClass("c$a");
	private final JarIndex jarIndex;

	public PackageVisibilityIndexTest() throws Exception {
		JarClassProvider jcp = new JarClassProvider(JAR);
		this.jarIndex = JarIndex.empty();
		this.jarIndex.indexJar(jcp.getClassNames(), jcp, ProgressListener.none());
	}

	@Test
	public void test() {
		PackageVisibilityIndex visibilityIndex = this.jarIndex.getPackageVisibilityIndex();
		assertThat(visibilityIndex.getPartition(BASE), containsInAnyOrder(BASE, SAME_PACKAGE_CHILD, SAME_PACKAGE_CHILD_INNER));
		System.out.println(visibilityIndex.getPartitions());
		assertThat(visibilityIndex.getPartitions(), containsInAnyOrder(
				containsInAnyOrder(BASE, SAME_PACKAGE_CHILD, SAME_PACKAGE_CHILD_INNER),
				containsInAnyOrder(OTHER_PACKAGE_CHILD, OTHER_PACKAGE_CHILD_INNER),
				contains(KEEP)
		));
	}
}
