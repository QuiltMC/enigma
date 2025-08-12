package org.quiltmc.enigma.command;

final class CommonArguments {
	private CommonArguments() {
		throw new UnsupportedOperationException();
	}

	static final Argument INPUT_JAR = new Argument("<input-jar>",
			"""
					A path to the .jar file to use in executing the command."""
	);
	static final Argument INPUT_MAPPINGS = new Argument("<input-mappings>",
			"""
					A path to the file or folder to read mappings from."""
	);
	static final Argument MAPPING_OUTPUT = new Argument("<mapping-output>",
			"""
					A path to the file or folder to write mappings to. Will be created if missing."""
	);
	static final Argument OUTPUT_JAR = new Argument("<output-jar>",
			"""
					A path to the .jar file to write output to. Will be created if missing."""
	);
	static final Argument ENIGMA_PROFILE = new Argument("<enigma-profile>",
			"""
					A path to an Enigma profile JSON file, used to apply things like plugins."""
	);
	static final Argument OBFUSCATED_NAMESPACE = new Argument("<obfuscated-namespace>",
			"""
					The namespace to use for obfuscated names when writing mappings. Only used in certain mapping formats."""
	);
	static final Argument DEOBFUSCATED_NAMESPACE = new Argument("<deobfuscated-namespace>",
			"""
					The namespace to use for deobfuscated names when writing mappings. Only used in certain mapping formats."""
	);
}
