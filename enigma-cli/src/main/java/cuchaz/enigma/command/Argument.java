package cuchaz.enigma.command;

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
	OUTPUT_MAPPING_FORMAT("<output-mapping-format>",
		"""
				The mapping format to use when writing output mappings. Allowed values are (case-insensitive):
				- TINY_V2:from_namespace:to_namespace (ex: tiny_v2:intermediary:named)
				- ENIGMA_FILE
				- ENIGMA_DIRECTORY
				- ENIGMA_ZIP
				- SRG_FILE
				- RECAF

				Proguard is not a valid output format, as writing is unsupported."""
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
