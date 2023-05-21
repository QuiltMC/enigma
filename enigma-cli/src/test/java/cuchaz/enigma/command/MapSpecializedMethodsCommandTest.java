package cuchaz.enigma.command;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.TestUtil;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.serde.MappingFileNameFormat;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MapSpecializedMethodsCommandTest extends CommandTest {
	private static final Path JAR = TestUtil.obfJar("bridge");
	private static final Path MAPPINGS = getResource("/mapSpecializedMethods/");

	private static final ClassEntry BASE_CLASS = new ClassEntry("a");
	private static final MethodEntry BASE_FOO_1 = MethodEntry.parse("a", "d", "()La;");
	private static final MethodEntry BASE_FOO_2 = MethodEntry.parse("a", "a", "(I)La;");
	private static final MethodEntry BASE_FOO_3 = MethodEntry.parse("a", "a", "(II)La;");
	private static final MethodEntry BASE_BAR_1 = MethodEntry.parse("a", "e", "()La;");
	private static final MethodEntry BASE_BAR_2 = MethodEntry.parse("a", "b", "(I)La;");
	private static final MethodEntry BASE_BAZ_1 = MethodEntry.parse("a", "c", "(I)La;");
	private static final MethodEntry BASE_BAZ_2 = MethodEntry.parse("a", "b", "(II)La;");
	private static final ClassEntry OTHER_CLASS = new ClassEntry("b");
	private static final MethodEntry OTHER_GET = MethodEntry.parse("b", "a", "()Ljava/lang/Integer;");
	private static final MethodEntry OTHER_GET_BRIDGE = MethodEntry.parse("b", "get", "()Ljava/lang/Object;");
	private static final MethodEntry OTHER_APPLY = MethodEntry.parse("b", "a", "(Ljava/lang/String;)Ljava/lang/Integer;");
	private static final MethodEntry OTHER_APPLY_BRIDGE = MethodEntry.parse("b", "apply", "(Ljava/lang/Object;)Ljava/lang/Object;");
	private static final ClassEntry SUB_CLASS = new ClassEntry("c");
	private static final MethodEntry SUB_FOO_1 = MethodEntry.parse("c", "f", "()Lc;");
	private static final MethodEntry SUB_FOO_1_BRIDGE = MethodEntry.parse("c", "d", "()La;");
	private static final MethodEntry SUB_FOO_2 = MethodEntry.parse("c", "d", "(I)Lc;");
	private static final MethodEntry SUB_FOO_2_BRIDGE = MethodEntry.parse("c", "a", "(I)La;");
	private static final MethodEntry SUB_FOO_3 = MethodEntry.parse("c", "c", "(II)Lc;");
	private static final MethodEntry SUB_FOO_3_BRIDGE = MethodEntry.parse("c", "a", "(II)La;");
	private static final MethodEntry SUB_BAR_1 = MethodEntry.parse("c", "g", "()Lc;");
	private static final MethodEntry SUB_BAR_1_BRIDGE = MethodEntry.parse("c", "e", "()La;");
	private static final MethodEntry SUB_BAR_2 = MethodEntry.parse("c", "e", "(I)Lc;");
	private static final MethodEntry SUB_BAR_2_BRIDGE = MethodEntry.parse("c", "b", "(I)La;");
	private static final MethodEntry SUB_BAZ_1 = MethodEntry.parse("c", "f", "(I)Lc;");
	private static final MethodEntry SUB_BAZ_1_BRIDGE = MethodEntry.parse("c", "c", "(I)La;");
	private static final MethodEntry SUB_BAZ_2 = MethodEntry.parse("c", "d", "(II)Lc;");
	private static final MethodEntry SUB_BAZ_2_BRIDGE = MethodEntry.parse("c", "b", "(II)La;");
	private static final ClassEntry INNER_SUB_CLASS = new ClassEntry("c$a");
	private static final MethodEntry INNER_SUB_FOO_1_BRIDGE = MethodEntry.parse("c$a", "d", "()La;");
	private static final MethodEntry INNER_SUB_FOO_2_BRIDGE = MethodEntry.parse("c$a", "a", "(I)La;");
	private static final MethodEntry INNER_SUB_FOO_3_BRIDGE = MethodEntry.parse("c$a", "a", "(II)La;");
	private static final MethodEntry INNER_SUB_BAR_1_BRIDGE = MethodEntry.parse("c$a", "e", "()La;");
	private static final MethodEntry INNER_SUB_BAR_2_BRIDGE = MethodEntry.parse("c$a", "b", "(I)La;");
	private static final MethodEntry INNER_SUB_BAZ_1_BRIDGE = MethodEntry.parse("c$a", "c", "(I)La;");
	private static final MethodEntry INNER_SUB_BAZ_2_BRIDGE = MethodEntry.parse("c$a", "b", "(II)La;");

	@Test
	public void test() throws Exception {
		Path resultFile = Files.createTempFile("mapSpecializedMethods", ".mappings");
		MapSpecializedMethodsCommand.run(JAR, MappingFormat.ENIGMA_DIRECTORY.name(), MAPPINGS, MappingFormat.ENIGMA_FILE.name(), resultFile);

		EntryTree<EntryMapping> result = MappingFormat.ENIGMA_FILE.read(resultFile, ProgressListener.none(), new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF));

		assertNotNull(result.findNode(BASE_CLASS));
		assertEquals("foo", getName(result, BASE_FOO_1));
		assertEquals("foo", getName(result, BASE_FOO_2));
		assertEquals("foo", getName(result, BASE_FOO_3));
		assertEquals("bar", getName(result, BASE_BAR_1));
		assertEquals("bar", getName(result, BASE_BAR_2));
		assertEquals("baz", getName(result, BASE_BAZ_1));
		assertEquals("baz", getName(result, BASE_BAZ_2));

		assertNotNull(result.findNode(OTHER_CLASS));
		assertEquals("get", getName(result, OTHER_GET));
		assertNull(getName(result, OTHER_GET_BRIDGE));
		assertEquals("apply", getName(result, OTHER_APPLY));
		assertNull(getName(result, OTHER_APPLY_BRIDGE));

		assertNotNull(result.findNode(SUB_CLASS));
		assertEquals("foo", getName(result, SUB_FOO_1));
		assertNull(getName(result, SUB_FOO_1_BRIDGE));
		assertEquals("foo", getName(result, SUB_FOO_2));
		assertNull(getName(result, SUB_FOO_2_BRIDGE));
		assertEquals("foo", getName(result, SUB_FOO_3));
		assertNull(getName(result, SUB_FOO_3_BRIDGE));
		assertEquals("bar", getName(result, SUB_BAR_1));
		assertNull(getName(result, SUB_BAR_1_BRIDGE));
		assertEquals("bar", getName(result, SUB_BAR_2));
		assertNull(getName(result, SUB_BAR_2_BRIDGE));
		assertEquals("baz", getName(result, SUB_BAZ_1));
		assertNull(getName(result, SUB_BAZ_1_BRIDGE));
		assertEquals("baz", getName(result, SUB_BAZ_2));
		assertNull(getName(result, SUB_BAZ_2_BRIDGE));

		assertNull(getName(result, INNER_SUB_FOO_1_BRIDGE));
		assertNull(getName(result, INNER_SUB_FOO_2_BRIDGE));
		assertNull(getName(result, INNER_SUB_FOO_3_BRIDGE));
		assertNull(getName(result, INNER_SUB_BAR_1_BRIDGE));
		assertNull(getName(result, INNER_SUB_BAR_2_BRIDGE));
		assertNull(getName(result, INNER_SUB_BAZ_1_BRIDGE));
		assertNull(getName(result, INNER_SUB_BAZ_2_BRIDGE));
	}
}
