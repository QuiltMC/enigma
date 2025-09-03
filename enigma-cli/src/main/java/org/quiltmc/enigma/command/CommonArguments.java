package org.quiltmc.enigma.command;

import java.nio.file.Path;

final class CommonArguments {
	private CommonArguments() {
		throw new UnsupportedOperationException();
	}

	static final Argument<Path> INPUT_JAR = Argument.ofReadableFile("input-jar",
			"""
					A path to the .jar file to use in executing the command."""
	);
	static final Argument<Path> INPUT_MAPPINGS = Argument.ofReadablePath("input-mappings",
			"""
					A path to the file or folder to read mappings from."""
	);
	static final Argument<Path> MAPPING_OUTPUT = Argument.ofWritablePath("mapping-output",
			"""
					A path to the file or folder to write mappings to. Will be created if missing."""
	);
	static final Argument<Path> OUTPUT_JAR = Argument.ofWritableFile("output-jar",
			"""
					A path to the .jar file to write output to. Will be created if missing."""
	);
	static final Argument<Path> ENIGMA_PROFILE = Argument.ofReadableFile("enigma-profile",
			"""
					A path to an Enigma profile JSON file, used to apply things like plugins."""
	);
	static final Argument<String> OBFUSCATED_NAMESPACE = Argument.ofString("obfuscated-namespace", "namespace",
			"""
					The namespace to use for obfuscated names when writing mappings. Only used in certain mapping formats."""
	);
	static final Argument<String> DEOBFUSCATED_NAMESPACE = Argument.ofString("deobfuscated-namespace", "namespace",
			"""
					The namespace to use for deobfuscated names when writing mappings. Only used in certain mapping formats."""
	);
}
