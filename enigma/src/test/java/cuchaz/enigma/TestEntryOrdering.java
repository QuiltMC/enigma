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
		ClassEntry c1  = new ClassEntry("pkg/1665BFCF");
		ClassEntry c2  = new ClassEntry("pkg/2BB46638");
		ClassEntry c3  = new ClassEntry("pkg/2BB46638$AC5B2355");
		ClassEntry c4  = new ClassEntry("pkg/6F88ECF");
		ClassEntry c5  = new ClassEntry("pkg/6F88ECF$57C7176A");
		ClassEntry c6  = new ClassEntry("pkg/FEE829A0");
		ClassEntry c7  = new ClassEntry("pkg/FEE829A0$3A78ECBB");
		ClassEntry c8  = new ClassEntry("pkg/294e9cee/77C137C5");
		ClassEntry c9  = new ClassEntry("pkg/294e9cee/7daa64ce/158D6D20");
		ClassEntry c10 = new ClassEntry("pkg/72e178af/225B1ACE");
		ClassEntry c11 = new ClassEntry("pkg/aaf82493/80c8afa8/2760E349");

		List<ClassEntry> classes = new ArrayList<>(List.of(c7, c6, c1, c10, c5, c9, c3, c11, c8, c4, c2));

		Collections.sort(classes);
		Assertions.assertEquals(List.of(c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11), classes);
	}
}
