package org.quiltmc.enigma.command;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

public class ValidateArgNamesTest {
	@Test
	void validate() {
		Main.getCommands().forEach((cmdName, cmd) -> {
			final Set<String> requiredNames = validateArgs(cmdName, cmd.requiredArguments);
			final Set<String> optionalNames = validateArgs(cmdName, cmd.optionalArguments);
			requiredNames.forEach(required -> Assertions.assertFalse(
					optionalNames.contains(required),
					() -> getDuplicateNameMessage(cmdName, required)
			));
		});
	}

	private static Set<String> validateArgs(String cmdName, ImmutableList<Argument<?>> args) {
		final Set<String> argNames = new HashSet<>();
		for (final Argument<?> arg : args) {
			final String argName = arg.getName();

			Assertions.assertFalse(
					argName.isEmpty(),
					() -> "Command '%s' arg '%s' name must not be empty".formatted(cmdName, arg)
			);

			for (int i = 0; i < argName.length(); i++) {
				final char c = argName.charAt(i);

				assertLegal(cmdName, argName, Argument.SEPARATOR, c);
				assertLegal(cmdName, argName, Argument.NAME_DELIM, c);
			}

			Assertions.assertTrue(
					argNames.add(argName),
					() -> getDuplicateNameMessage(cmdName, argName)
			);
		}

		return argNames;
	}

	private static void assertLegal(String cmdName, String argName, char illegal, char actual) {
		Assertions.assertNotEquals(
				illegal, actual,
				() -> "Command '%s' arg '%s' name must not contain '%s'".formatted(cmdName, argName, illegal)
		);
	}

	private static String getDuplicateNameMessage(String cmdName, String argName) {
		return "Command '%s' duplicates argument name '%s'".formatted(cmdName, argName);
	}
}
