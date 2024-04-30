package org.quiltmc.enigma.translation.mapping;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.TestEntryFactory;
import org.quiltmc.enigma.TestUtil;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.class_provider.ClasspathClassProvider;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.util.validation.Message;
import org.quiltmc.enigma.util.validation.ValidationContext;

import java.nio.file.Path;

public class TestMappingValidatorParameters {
	public static final Path JAR = TestUtil.obfJar("inner_classes");
	private static EntryRemapper remapper;

	@BeforeAll
	public static void beforeAll() throws Exception {
		Enigma enigma = Enigma.create();
		EnigmaProject project = enigma.openJar(JAR, new ClasspathClassProvider(), ProgressListener.createEmpty());
		remapper = project.getRemapper();
	}

	@Test
	public void testLambdaParameterNames() {
		MethodEntry parent = TestEntryFactory.newMethod("g", "a", "(Ljava/lang/String;)V");
		LocalVariableEntry parentParam = TestEntryFactory.newParameter(parent, 0);
		MethodEntry firstLambda = TestEntryFactory.newMethod("g", "b", "(Ljava/lang/String;)V");
		LocalVariableEntry firstLambdaParam = TestEntryFactory.newParameter(firstLambda, 0);
		MethodEntry secondLambda = TestEntryFactory.newMethod("g", "a", "(Ljava/lang/String;Ljava/lang/String;)V");
		LocalVariableEntry secondLambdaParam = TestEntryFactory.newParameter(secondLambda, 1);

		// validate conflict with base method
		remapper.putMapping(TestUtil.newVC(), parentParam, new EntryMapping("LEVEL_0"));

		ValidationContext vc = TestUtil.newVC();
		remapper.validatePutMapping(vc, firstLambdaParam, new EntryMapping("LEVEL_0"));
		TestMappingValidator.assertMessages(vc, Message.NON_UNIQUE_NAME_CLASS);

		ValidationContext vc2 = TestUtil.newVC();
		remapper.validatePutMapping(vc2, secondLambdaParam, new EntryMapping("LEVEL_0"));
		TestMappingValidator.assertMessages(vc2, Message.NON_UNIQUE_NAME_CLASS);

		// validate nested lambda conflict with top level lambda
		remapper.putMapping(TestUtil.newVC(), firstLambdaParam, new EntryMapping("LEVEL_1"));

		ValidationContext vc3 = TestUtil.newVC();
		remapper.validatePutMapping(vc3, secondLambdaParam, new EntryMapping("LEVEL_1"));
		TestMappingValidator.assertMessages(vc3, Message.NON_UNIQUE_NAME_CLASS);

		// validate parent parameter name conflict with top level lambda and nested lambda
		remapper.putMapping(TestUtil.newVC(), secondLambdaParam, new EntryMapping("LEVEL_2"));

		ValidationContext vc4 = TestUtil.newVC();
		remapper.validatePutMapping(vc4, parentParam, new EntryMapping("LEVEL_2"));
		TestMappingValidator.assertMessages(vc4, Message.NON_UNIQUE_NAME_CLASS);

		ValidationContext vc5 = TestUtil.newVC();
		remapper.validatePutMapping(vc5, parentParam, new EntryMapping("LEVEL_1"));
		TestMappingValidator.assertMessages(vc5, Message.NON_UNIQUE_NAME_CLASS);

		// todo validate nested lambda name conflict with nested-er lambda
	}

	@Test
	public void testParameterNames() {
		// TODO test normal param conflicts
	}
}
