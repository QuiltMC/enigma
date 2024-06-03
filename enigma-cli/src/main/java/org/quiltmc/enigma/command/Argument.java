package org.quiltmc.enigma.command;

public enum Argument {
	INPUT_JAR("<input-jar>",
		"""
				A path to the .jar file to use in executing the command."""
	),
	INPUT_MAPPINGS("<input-mappings>",
		"""
				A path to the file or folder to read mappings from."""
	),
	LEFT_MAPPINGS("<left-mappings>",
		"""
				A path to the left file or folder to read mappings from, used in commands which take two mapping inputs."""
	),
	RIGHT_MAPPINGS("<right-mappings>",
		"""
				A path to the right file or folder to read mappings from, used in commands which take two mapping inputs."""
	),
	MAPPING_OUTPUT("<mapping-output>",
		"""
				A path to the file or folder to write mappings to. Will be created if missing."""
	),
	KEEP_MODE("<keep-mode>",
		"""
				Which mappings should overwrite the others when composing conflicting mappings. Allowed values are "left", "right", and "both"."""
	),
	DECOMPILER("<decompiler>",
		"""
				The decompiler to use when producing output. Allowed values are (case-insensitive):
				- VINEFLOWER
				- CFR
				- PROCYON
				- BYTECODE"""
	),
	OUTPUT_FOLDER("<output-folder>",
		"""
				A path to the file or folder to write output to."""
	),
	OUTPUT_JAR("<output-jar>",
		"""
				A path to the .jar file to write output to. Will be created if missing."""
	),
	FILL_ALL("<fill-all>",
		"""
				Whether to fill all possible mappings. Allowed values are "true" and "false"."""
	),
	ENIGMA_PROFILE("<enigma-profile>",
		"""
				A path to an Enigma profile JSON file, used to apply things like plugins."""
	),
	OBFUSCATED_NAMESPACE("<obfuscated-namespace>",
		"""
				The namespace to use for obfuscated names when writing mappings. Only used in certain mapping formats."""
	),
	DEOBFUSCATED_NAMESPACE("<deobfuscated-namespace>",
		"""
				The namespace to use for deobfuscated names when writing mappings. Only used in certain mapping formats."""
	);

	private final String displayForm;
	private final String explanation;

	Argument(String displayForm, String explanation) {
		this.displayForm = displayForm;
		this.explanation = explanation;
	}

	public String getDisplayForm() {
		return this.displayForm;
	}

	public String getExplanation() {
		return this.explanation;
	}

	public ComposedArgument required() {
		return new ComposedArgument(this, false);
	}

	public ComposedArgument optional() {
		return new ComposedArgument(this, true);
	}
}
