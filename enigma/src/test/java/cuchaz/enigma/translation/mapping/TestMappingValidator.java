package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.TestUtil;
import cuchaz.enigma.classprovider.ClasspathClassProvider;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.HashEntryTree;
import cuchaz.enigma.utils.validation.Message;
import cuchaz.enigma.utils.validation.ParameterizedMessage;
import cuchaz.enigma.utils.validation.ValidationContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static cuchaz.enigma.TestEntryFactory.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestMappingValidator {
	public static final Path JAR = TestUtil.obfJar("validation");
	private static Enigma enigma;
	private static EnigmaProject project;
	private static EntryTree<EntryMapping> mappings;

	private static EntryRemapper remapper;

	@BeforeAll
	public static void beforeAll() throws Exception {
		enigma = Enigma.create();
		project = enigma.openJar(JAR, new ClasspathClassProvider(), ProgressListener.none());
		remapper = project.getMapper();
	}

	@BeforeEach
	public void beforeEach() {
		mappings = new HashEntryTree<>();
		project.setMappings(mappings);
		remapper = project.getMapper();
	}

	@Test
	public void shadowPrivateFields() {
		// static fields
		remapper.putMapping(newVC(), newField("b", "a", "Ljava/lang/String;"), new EntryMapping("FIELD_00"));

		ValidationContext vc = new ValidationContext(notifier());
		remapper.validatePutMapping(vc, newField("a", "c", "Ljava/lang/String;"), new EntryMapping("FIELD_00"));

		assertValid(vc);

		// final fields
		remapper.putMapping(newVC(), newField("b", "a", "I"), new EntryMapping("field01"));

		vc = new ValidationContext(notifier());
		remapper.validatePutMapping(vc, newField("a", "a", "I"), new EntryMapping("field01"));

		assertValid(vc);

		// instance fields
		remapper.putMapping(newVC(), newField("b", "b", "I"), new EntryMapping("field02"));

		vc = new ValidationContext(notifier());
		remapper.validatePutMapping(vc, newField("a", "b", "I"), new EntryMapping("field02"));

		assertValid(vc);
	}

	@Test
	public void shadowPublicFields() {
		// static fields
		remapper.putMapping(newVC(), newField("b", "b", "Ljava/lang/String;"), new EntryMapping("FIELD_04"));

		ValidationContext vc = new ValidationContext(notifier());
		remapper.validatePutMapping(vc, newField("a", "a", "Ljava/lang/String;"), new EntryMapping("FIELD_04"));

		// TODO: warning
		assertValid(vc);

		// default fields
		remapper.putMapping(newVC(), newField("b", "b", "Z"), new EntryMapping("field05"));

		vc = new ValidationContext(notifier());
		remapper.validatePutMapping(vc, newField("a", "a", "Z"), new EntryMapping("field05"));

		// TODO: warning
		assertValid(vc);
	}

	@Test
	public void shadowMethods() {
		// static methods
		remapper.putMapping(newVC(), newMethod("b", "c", "()V"), new EntryMapping("method01"));

		ValidationContext vc = new ValidationContext(notifier());
		remapper.validatePutMapping(vc, newMethod("a", "a", "()V"), new EntryMapping("method01"));

		assertValid(vc);

		// private methods
		remapper.putMapping(newVC(), newMethod("b", "a", "()V"), new EntryMapping("method02"));

		vc = new ValidationContext(notifier());
		remapper.validatePutMapping(vc, newMethod("a", "d", "()V"), new EntryMapping("method02"));

		// TODO: shouldn't be an error
		// assertValid(vc);
	}

	@Test
	public void nonUniqueFields() {
		remapper.putMapping(newVC(), newField("a", "a", "I"), new EntryMapping("field01"));

		ValidationContext vc = new ValidationContext(notifier());
		remapper.validatePutMapping(vc, newField("a", "b", "I"), new EntryMapping("field01"));

		assertErrorMessages(vc, Message.NONUNIQUE_NAME_CLASS);

		remapper.putMapping(newVC(), newField("a", "c", "Ljava/lang/String;"), new EntryMapping("FIELD_02"));

		vc = new ValidationContext(notifier());
		remapper.validatePutMapping(vc, newField("a", "a", "Ljava/lang/String;"), new EntryMapping("FIELD_02"));

		assertErrorMessages(vc, Message.NONUNIQUE_NAME_CLASS);
	}

	@Test
	public void nonUniqueMethods() {
		remapper.putMapping(newVC(), newMethod("a", "a", "()V"), new EntryMapping("method01"));

		ValidationContext vc = new ValidationContext(notifier());
		remapper.validatePutMapping(vc, newMethod("a", "b", "()V"), new EntryMapping("method01"));

		assertErrorMessages(vc, Message.NONUNIQUE_NAME_CLASS);

		vc = new ValidationContext(notifier());
		remapper.validatePutMapping(vc, newMethod("a", "d", "()V"), new EntryMapping("method01"));

		assertErrorMessages(vc, Message.NONUNIQUE_NAME_CLASS);
	}

	private static void assertValid(ValidationContext vc) {
		assertThat(vc.getMessages().size(), is(0));
		assertThat(vc.canProceed(), is(true));
	}

	/**
	 * Assert that the validation context can't proceed and contain the messages.
	 *
	 * @param vc validation context
	 * @param messages the messages the validation context should contain
	 */
	private static void assertErrorMessages(ValidationContext vc, Message... messages) {
		assertThat(vc.getMessages().size(), is(messages.length));
		for (int i = 0; i < messages.length; i++) {
			ParameterizedMessage msg = vc.getMessages().get(i);
			assertThat(msg.message(), is(messages[i]));
		}
	}

	private static ValidationContext newVC() {
		return new ValidationContext(notifier());
	}

	private static ValidationContext.Notifier notifier() {
		return new ValidationContext.Notifier() {
			@Override
			public void notify(ParameterizedMessage message) {
			}

			@Override
			public boolean verifyWarning(ParameterizedMessage message) {
				return true;
			}
		};
	}
}
