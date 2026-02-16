package org.quiltmc.enigma.impl.plugin;

import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.TestEntryFactory;
import org.quiltmc.enigma.TestUtil;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProfile;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.class_provider.ClasspathClassProvider;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ParamLocalClassLinksTest {
	private static final Path JAR = TestUtil.obfJar("param_local_class_links");

	private static final String PROFILE =
			"""
			{
				"services": {
					"name_proposal": [
						{
							"id": "%s"
						}
					],
					"jar_indexer": [
						{
							"id": "%s"
						}
					]
				}
			}\
			""".formatted(ParamParamLocalClassFieldProposalService.ID, ParamLocalClassLinkIndexingService.ID);

	private static final ClassEntry OUTER_CLASS = TestEntryFactory.newClass("a");
	private static final ClassEntry TO_STRING_OF_ANON_CLASS = TestEntryFactory
			.newInnerClass(OUTER_CLASS, "1");
	private static final ClassEntry WEIRD_TO_STRING_ANON_CLASS = TestEntryFactory
			.newInnerClass(OUTER_CLASS, "2");
	private static final ClassEntry WITH_VISIBLE_PARAM_ANON_CLASS = TestEntryFactory
			.newInnerClass(OUTER_CLASS, "3");
	private static final ClassEntry MORE_LOCALS_TO_STRING_ANON_CLASS = TestEntryFactory
			.newInnerClass(OUTER_CLASS, "4");
	private static final ClassEntry MAX_LOCAL_CLASS = TestEntryFactory
			.newInnerClass(OUTER_CLASS, "a");
	public static final MethodDescriptor TO_STRING_DESC = new MethodDescriptor("()Ljava/lang/String;");

	private static EnigmaProject openProject() {
		try {
			return Enigma.builder()
				.setProfile(EnigmaProfile.parse(new StringReader(PROFILE)))
				.build()
				.openJar(JAR, new ClasspathClassProvider(), ProgressListener.createEmpty());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void toStringOf() {
		final LocalVariableEntry toStringOfParam = new LocalVariableEntry(
				new MethodEntry(OUTER_CLASS, "a", new MethodDescriptor("(Ljava/lang/String;)Ljava/lang/Object;")),
				0
		);

		final LocalVariableEntry fakeLocal = new LocalVariableEntry(
				new MethodEntry(TO_STRING_OF_ANON_CLASS, "toString", TO_STRING_DESC),
				1
		);

		assertEquals(toStringOfParam, openProject().getRepresentative(fakeLocal));
	}

	@Test
	void weirdToString() {
		final LocalVariableEntry weirdToStringParam = new LocalVariableEntry(
				new MethodEntry(OUTER_CLASS, "b", new MethodDescriptor("(Ljava/lang/String;)Ljava/lang/Object;")),
				0
		);

		final LocalVariableEntry fakeLocal = new LocalVariableEntry(
				new MethodEntry(WEIRD_TO_STRING_ANON_CLASS, "toString", TO_STRING_DESC),
				1
		);
	}
}
