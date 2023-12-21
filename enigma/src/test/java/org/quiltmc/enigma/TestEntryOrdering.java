package cuchaz.enigma;

import cuchaz.enigma.translation.representation.entry.ClassEntry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestEntryOrdering {
	@Test
	public void testClasses() {
		ClassEntry c01 = new ClassEntry("pkg/1665BFCF");
		ClassEntry c02 = new ClassEntry("pkg/2BB46638");
		ClassEntry c03 = new ClassEntry("pkg/2BB46638$AC5B2355");
		ClassEntry c04 = new ClassEntry("pkg/6F88ECF");
		ClassEntry c05 = new ClassEntry("pkg/6F88ECF$57C7176A");
		ClassEntry c06 = new ClassEntry("pkg/FEE829A0");
		ClassEntry c07 = new ClassEntry("pkg/FEE829A0$3A78ECBB");
		ClassEntry c08 = new ClassEntry("pkg/294e9cee/77C137C5");
		ClassEntry c09 = new ClassEntry("pkg/294e9cee/7daa64ce/158D6D20");
		ClassEntry c10 = new ClassEntry("pkg/72e178af/225B1ACE");
		ClassEntry c11 = new ClassEntry("pkg/aaf82493/80c8afa8/2760E349");

		List<ClassEntry> classes = new ArrayList<>(List.of(c07, c06, c01, c10, c05, c09, c03, c11, c08, c04, c02));

		Collections.sort(classes);
		Assertions.assertEquals(List.of(c01, c02, c03, c04, c05, c06, c07, c08, c09, c10, c11), classes);
	}
}
