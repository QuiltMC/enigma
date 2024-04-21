package org.quiltmc.enigma;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProfile;
import org.quiltmc.enigma.api.service.DecompilerService;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma.api.service.ReadWriteService;

import java.io.Reader;
import java.io.StringReader;

public class ActiveByDefaultTest {
	@Test
	public void testServicesLoaded() {
		Enigma enigma = Enigma.builder().build();
		Assertions.assertFalse(enigma.getServices().get(DecompilerService.TYPE).isEmpty());
		Assertions.assertFalse(enigma.getServices().get(ReadWriteService.TYPE).isEmpty());
		Assertions.assertTrue(enigma.getServices().get(NameProposalService.TYPE).isEmpty());
	}

	@Test
	public void testProfile() {
		Reader r = new StringReader("""
				{
					"services": {
						"decompiler": {
							"id": "enigma:vineflower"
						}
					}
				}""");
		EnigmaProfile profile = EnigmaProfile.parse(r);

		// only vf should load since decompilers are explicitly defined
		Enigma enigma = Enigma.builder().setProfile(profile).build();
		Assertions.assertEquals(1, enigma.getServices().get(DecompilerService.TYPE).size());
		Assertions.assertEquals("enigma:vineflower", enigma.getServices().get(DecompilerService.TYPE).get(0).getId());

		// read write services should all be loaded
		Assertions.assertFalse(enigma.getServices().get(ReadWriteService.TYPE).isEmpty());
	}
}
