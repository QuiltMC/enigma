package org.quiltmc.enigma.translation.mapping;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.quiltmc.enigma.TestEntryFactory;
import org.quiltmc.enigma.TestUtil;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.class_provider.ClasspathClassProvider;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.HashEntryTree;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.util.validation.Message;
import org.quiltmc.enigma.util.validation.ParameterizedMessage;
import org.quiltmc.enigma.util.validation.ValidationContext;

import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestMappingValidator {
	private static final String REPEATED_TEST_NAME = RepeatedTest.DISPLAY_NAME_PLACEHOLDER + " :: repetition " + RepeatedTest.CURRENT_REPETITION_PLACEHOLDER + "/" + RepeatedTest.TOTAL_REPETITIONS_PLACEHOLDER;

	public static final Path JAR = TestUtil.obfJar("validation");
	private static EnigmaProject project;
	private static EntryRemapper remapper;

	@BeforeAll
	public static void beforeAll() throws Exception {
		Enigma enigma = Enigma.create();
		project = enigma.openJar(JAR, new ClasspathClassProvider(), ProgressListener.createEmpty());
		remapper = project.getRemapper();
	}

	@BeforeEach
	public void beforeEach(RepetitionInfo repetitionInfo) {
		EntryTree<EntryMapping> mappings = new HashEntryTree<>();
		project.setMappings(mappings, ProgressListener.createEmpty());
		remapper = project.getRemapper();

		// repeat with mapped classes
		if (repetitionInfo.getCurrentRepetition() == 1) {
			remapper.putMapping(TestUtil.newVC(), TestEntryFactory.newClass("a"), new EntryMapping("BaseClass"));
			remapper.putMapping(TestUtil.newVC(), TestEntryFactory.newClass("b"), new EntryMapping("SuperClass"));
		}
	}

	@RepeatedTest(value = 2, name = REPEATED_TEST_NAME)
	public void shadowPrivateFields() {
		// static fields
		remapper.putMapping(TestUtil.newVC(), TestEntryFactory.newField("b", "a", "Ljava/lang/String;"), new EntryMapping("FIELD_00"));

		ValidationContext vc = TestUtil.newVC();
		remapper.validatePutMapping(vc, TestEntryFactory.newField("a", "c", "Ljava/lang/String;"), new EntryMapping("FIELD_00"));

		assertMessages(vc, Message.SHADOWED_NAME_CLASS);

		// final fields
		remapper.putMapping(TestUtil.newVC(), TestEntryFactory.newField("b", "a", "I"), new EntryMapping("field01"));

		vc = TestUtil.newVC();
		remapper.validatePutMapping(vc, TestEntryFactory.newField("a", "a", "I"), new EntryMapping("field01"));

		assertMessages(vc);

		// instance fields
		remapper.putMapping(TestUtil.newVC(), TestEntryFactory.newField("b", "b", "I"), new EntryMapping("field02"));

		vc = TestUtil.newVC();
		remapper.validatePutMapping(vc, TestEntryFactory.newField("a", "b", "I"), new EntryMapping("field02"));

		assertMessages(vc);
	}

	@RepeatedTest(value = 2, name = REPEATED_TEST_NAME)
	public void shadowPublicFields() {
		// static fields
		remapper.putMapping(TestUtil.newVC(), TestEntryFactory.newField("b", "b", "Ljava/lang/String;"), new EntryMapping("FIELD_04"));

		ValidationContext vc = TestUtil.newVC();
		remapper.validatePutMapping(vc, TestEntryFactory.newField("a", "a", "Ljava/lang/String;"), new EntryMapping("FIELD_04"));

		assertMessages(vc, Message.SHADOWED_NAME_CLASS);

		// default fields
		remapper.putMapping(TestUtil.newVC(), TestEntryFactory.newField("b", "b", "Z"), new EntryMapping("field05"));

		vc = TestUtil.newVC();
		remapper.validatePutMapping(vc, TestEntryFactory.newField("a", "a", "Z"), new EntryMapping("field05"));

		assertMessages(vc);
	}

	@RepeatedTest(value = 2, name = REPEATED_TEST_NAME)
	public void shadowMethods() {
		// static methods
		remapper.putMapping(TestUtil.newVC(), TestEntryFactory.newMethod("b", "c", "()V"), new EntryMapping("method01"));

		ValidationContext vc = TestUtil.newVC();
		remapper.validatePutMapping(vc, TestEntryFactory.newMethod("a", "a", "()V"), new EntryMapping("method01"));

		assertMessages(vc, Message.SHADOWED_NAME_CLASS);

		// private methods
		remapper.putMapping(TestUtil.newVC(), TestEntryFactory.newMethod("b", "a", "()V"), new EntryMapping("method02"));

		vc = TestUtil.newVC();
		remapper.validatePutMapping(vc, TestEntryFactory.newMethod("a", "d", "()V"), new EntryMapping("method02"));

		assertMessages(vc);
	}

	@RepeatedTest(value = 2, name = REPEATED_TEST_NAME)
	public void nonUniqueFields() {
		remapper.putMapping(TestUtil.newVC(), TestEntryFactory.newField("a", "a", "I"), new EntryMapping("field01"));

		ValidationContext vc = TestUtil.newVC();
		remapper.validatePutMapping(vc, TestEntryFactory.newField("a", "b", "I"), new EntryMapping("field01"));

		assertMessages(vc, Message.NON_UNIQUE_NAME_CLASS);

		remapper.putMapping(TestUtil.newVC(), TestEntryFactory.newField("a", "c", "Ljava/lang/String;"), new EntryMapping("FIELD_02"));

		vc = TestUtil.newVC();
		remapper.validatePutMapping(vc, TestEntryFactory.newField("a", "a", "Ljava/lang/String;"), new EntryMapping("FIELD_02"));

		assertMessages(vc, Message.NON_UNIQUE_NAME_CLASS);
	}

	@RepeatedTest(value = 2, name = REPEATED_TEST_NAME)
	public void nonUniqueMethods() {
		remapper.putMapping(TestUtil.newVC(), TestEntryFactory.newMethod("a", "a", "()V"), new EntryMapping("method01"));

		ValidationContext vc = TestUtil.newVC();
		remapper.validatePutMapping(vc, TestEntryFactory.newMethod("a", "b", "()V"), new EntryMapping("method01"));

		assertMessages(vc, Message.NON_UNIQUE_NAME_CLASS);

		vc = TestUtil.newVC();
		remapper.validatePutMapping(vc, TestEntryFactory.newMethod("a", "d", "()V"), new EntryMapping("method01"));

		assertMessages(vc, Message.NON_UNIQUE_NAME_CLASS);
	}

	@RepeatedTest(value = 2, name = REPEATED_TEST_NAME)
	public void conflictingMethods() {
		// "overriding" w/different return descriptor
		remapper.putMapping(TestUtil.newVC(), TestEntryFactory.newMethod("b", "a", "()Z"), new EntryMapping("method01"));

		ValidationContext vc = TestUtil.newVC();
		remapper.validatePutMapping(vc, TestEntryFactory.newMethod("a", "b", "()V"), new EntryMapping("method01"));

		assertMessages(vc, Message.NON_UNIQUE_NAME_CLASS);

		// "overriding" a static method
		remapper.putMapping(TestUtil.newVC(), TestEntryFactory.newMethod("b", "c", "()V"), new EntryMapping("method02"));

		vc = TestUtil.newVC();
		remapper.validatePutMapping(vc, TestEntryFactory.newMethod("a", "b", "()V"), new EntryMapping("method02"));

		assertMessages(vc, Message.NON_UNIQUE_NAME_CLASS);

		// "overriding" when the original methods were not related
		remapper.putMapping(TestUtil.newVC(), TestEntryFactory.newMethod("b", "b", "()I"), new EntryMapping("method03"));

		vc = TestUtil.newVC();
		remapper.validatePutMapping(vc, TestEntryFactory.newMethod("a", "a", "()I"), new EntryMapping("method03"));

		assertMessages(vc, Message.NON_UNIQUE_NAME_CLASS);
	}

	@RepeatedTest(value = 2, name = REPEATED_TEST_NAME)
	public void testParameterNames() {
		MethodEntry method = TestEntryFactory.newMethod("a", "a", "(II)I");

		remapper.putMapping(TestUtil.newVC(), TestEntryFactory.newParameter(method, 1), new EntryMapping("param01"));

		ValidationContext vc = TestUtil.newVC();
		remapper.validatePutMapping(vc, TestEntryFactory.newParameter(method, 2), new EntryMapping("param01"));
		assertMessages(vc, Message.NON_UNIQUE_NAME_CLASS);

		remapper.putMapping(TestUtil.newVC(), TestEntryFactory.newParameter(method, 2), new EntryMapping("param02"));

		vc = TestUtil.newVC();
		remapper.validatePutMapping(vc, TestEntryFactory.newParameter(method, 1), new EntryMapping("param02"));
		assertMessages(vc, Message.NON_UNIQUE_NAME_CLASS);
	}

	/**
	 * Assert that the validation context contains the messages.
	 *
	 * @param vc validation context
	 * @param messages the messages the validation context should contain
	 */
	public static void assertMessages(ValidationContext vc, Message... messages) {
		assertThat(vc.getMessages().size(), is(messages.length));
		for (int i = 0; i < messages.length; i++) {
			ParameterizedMessage msg = vc.getMessages().get(i);
			assertThat(msg.message(), is(messages[i]));
		}
	}
}
