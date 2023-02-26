package cuchaz.enigma.command;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.serde.MappingFileNameFormat;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class FillClassMappingsCommandTest extends CommandTest {
	private static final Path JAR = obfJar("innerClasses");
	private static final Path MAPPINGS = getResource("/fillClassMappings/");

	private static final ClassEntry A = new ClassEntry("a");
	private static final MethodEntry A_METHOD = MethodEntry.parse("a", "a", "()V");
	private static final ClassEntry A_ANONYMOUS = new ClassEntry("a$1");
	private static final ClassEntry B = new ClassEntry("b");
	private static final MethodEntry B_METHOD = MethodEntry.parse("b", "a", "(Ld;)V");
	private static final ClassEntry B_ANONYMOUS = new ClassEntry("b$1");
	private static final ClassEntry C = new ClassEntry("c");
	private static final FieldEntry C_FIELD = FieldEntry.parse("c", "a", "c$a");
	private static final ClassEntry C_INNER = new ClassEntry("c$a");
	private static final FieldEntry C_INNER_FIELD = FieldEntry.parse("c$a", "a", "I");
	private static final ClassEntry D = new ClassEntry("d");
	private static final ClassEntry D_INNER = new ClassEntry("d$a");
	private static final ClassEntry E = new ClassEntry("e");
	private static final MethodEntry E_METHOD_1 = MethodEntry.parse("e", "a", "()Ljava/lang/Object;");
	private static final MethodEntry E_METHOD_2 = MethodEntry.parse("e", "a", "()Ljava/lang/String;");
	private static final ClassEntry E_ANONYMOUS = new ClassEntry("e$1");
	private static final ClassEntry F = new ClassEntry("f");
	private static final ClassEntry F_LEVEL_1 = new ClassEntry("f$a");
	private static final FieldEntry F_LEVEL_1_FIELD = FieldEntry.parse("f$a", "a", "I");
	private static final ClassEntry F_LEVEL_2 = new ClassEntry("f$a$a");
	private static final FieldEntry F_LEVEL_2_FIELD = FieldEntry.parse("f$a$a", "a", "I");
	private static final ClassEntry F_LEVEL_3 = new ClassEntry("f$a$a$a");
	private static final FieldEntry F_LEVEL_3_FIELD = FieldEntry.parse("f$a$a$a", "a", "I");

	@Test
	public void test() throws Exception {
		Path resultFile = Files.createTempFile("fillClassMappings", ".mappings");
		FillClassMappingsCommand.run(JAR, MAPPINGS, resultFile, MappingFormat.ENIGMA_FILE.name());

		EntryTree<EntryMapping> result = MappingFormat.ENIGMA_FILE.read(resultFile, ProgressListener.none(), new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF));

		assertEquals("A_Anonymous", getName(result, A));
		assertNotNull(result.findNode(A_ANONYMOUS));
		assertNull(getName(result, A_METHOD));
		assertNull(getName(result, A_ANONYMOUS));

		assertNotNull(result.findNode(B));
		assertNull(getName(result, B));
		assertEquals("foo", getName(result, B_METHOD));
		assertNull(result.findNode(B_ANONYMOUS));

		assertNotNull(result.findNode(C));
		assertNull(getName(result, C));
		assertNull(getName(result, C_FIELD));
		assertNotNull(result.findNode(C_INNER));
		assertNull(getName(result, C_INNER));
		assertEquals("a", getName(result, C_INNER_FIELD));

		assertNotNull(result.findNode(D));
		assertNull(getName(result, D));
		assertEquals("Inner", getName(result, D_INNER));

		assertNotNull(result.findNode(E));
		assertNull(getName(result, E));
		assertNull(getName(result, E_METHOD_1));
		assertEquals("outerMethod", getName(result, E_METHOD_2));
		assertNull(result.findNode(E_ANONYMOUS));

		assertNotNull(result.findNode(F));
		assertNull(getName(result, F));
		assertNotNull(result.findNode(F_LEVEL_1));
		assertNull(getName(result, F_LEVEL_1));
		assertNull(getName(result, F_LEVEL_1_FIELD));
		assertNotNull(result.findNode(F_LEVEL_2));
		assertEquals("Level2", getName(result, F_LEVEL_2));
		assertNull(getName(result, F_LEVEL_2_FIELD));
		assertNotNull(result.findNode(F_LEVEL_3));
		assertNull(getName(result, F_LEVEL_3));
		assertNull(getName(result, F_LEVEL_3_FIELD));
	}
}
