package org.quiltmc.enigma.command;

import java.util.Arrays;
import java.util.stream.Collectors;

final class Argument {
	static final char SEPARATOR = ' ';
	static final char NAME_DELIM = '=';

	private static final String ALTERNATIVES_DELIM = "|";
	private static final String PATH_TYPE = "path";
	private static final String BOOL_TYPE = true + ALTERNATIVES_DELIM + false;

	/**
	 * @return an argument whose {@code typeDescription} is {@value #PATH_TYPE}
	 */
	static Argument ofPath(String name, String explanation) {
		return new Argument(name, PATH_TYPE, explanation);
	}

	/**
	 * @return an argument whose {@code typeDescription} lists its allowed values
	 */
	static Argument ofEnum(String name, Class<? extends Enum<?>> type, String explanation) {
		final String alternatives = Arrays.stream(type.getEnumConstants())
				.map(Object::toString)
				.collect(Collectors.joining(ALTERNATIVES_DELIM));
		return new Argument(name, alternatives, explanation);
	}

	/**
	 * @return an argument whose {@code typeDescription} is {@value #BOOL_TYPE}
	 */
	static Argument ofBool(String name, String explanation) {
		return new Argument(name, BOOL_TYPE, explanation);
	}

	private final String name;
	private final String explanation;
	private final String displayForm;

	/**
	 * @param name the name of the argument; may not contain any space or {@value #NAME_DELIM} characters
	 * @param typeDescription a short description of the type of value to expect; by convention these are in kebab-case,
	 * 							except for enums which should use {@link #ofEnum(String, Class, String)}
	 * @param explanation an extended explanation of what the argument accepts and what it's for
	 *
	 * @see #ofPath(String, String)
	 * @see #ofEnum(String, Class, String)
	 */
	Argument(String name, String typeDescription, String explanation) {
		this.name = name;
		this.explanation = explanation;
		this.displayForm = "[" + this.name + NAME_DELIM + "]" + "<" + typeDescription + ">";
	}

	public String getName() {
		return this.name;
	}

	public String getExplanation() {
		return this.explanation;
	}

	public String getDisplayForm() {
		return this.displayForm;
	}
}
