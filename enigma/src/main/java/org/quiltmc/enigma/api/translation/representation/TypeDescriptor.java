package org.quiltmc.enigma.api.translation.representation;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.quiltmc.enigma.api.translation.Translatable;
import org.quiltmc.enigma.api.translation.TranslateResult;
import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.mapping.EntryMap;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryResolver;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;

import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Represents a Java type descriptor.
 * Type descriptors are used to represent the types of elements such as arguments, fields, and method returns.
 * <p>
 *     Type descriptors can be formatted two different ways:
 *     <ul>
 *         <li>
 *             For objects, descriptors are formatted as {@code Lpackage/Class$Inner;}.
 *             The prefix {@code L} denotes the start of an object type, followed by the fully qualified name,
 *             and ending in {@code ;}.
 *         </li>
 *         <li>
 *             For primitives, descriptors are formatted as a single, uppercase letter.
 *             Each primitive has its own designated letter, which is the first letter of its keyword for all but {@code boolean} and {@code long}:
 *             <ul>
 *                 <li>{@code byte}: {@code B}</li>
 *                 <li>{@code char}: {@code C}</li>
 *                 <li>{@code short}: {@code S}</li>
 *                 <li>{@code int}: {@code I}</li>
 *                 <li>{@code long}: {@code J}</li>
 *                 <li>{@code float}: {@code F}</li>
 *                 <li>{@code double}: {@code D}</li>
 *                 <li>{@code boolean}: {@code Z}</li>
 *             </ul>
 *         </li>
 *     </ul>
 * </p>
 * When representing an array type in a type descriptor, the descriptor is prefixed by {@code [}, such as {@code [I} or {@code [Lpackage/Class;}.
 */
public class TypeDescriptor implements Translatable {
	protected final String desc;

	public TypeDescriptor(String desc) {
		Preconditions.checkNotNull(desc, "Desc cannot be null");

		// don't deal with generics
		// this is just for raw jvm types
		if ((desc.charAt(0) == 'T' && readClass(desc) != null) || desc.indexOf('<') >= 0 || desc.indexOf('>') >= 0) {
			throw new IllegalArgumentException("don't use with generic types or templates: " + desc);
		}

		this.desc = desc;
	}

	public static String parseFirst(String in) {
		if (in == null || in.length() <= 0) {
			throw new IllegalArgumentException("No desc to parse, input is empty!");
		}

		// read one desc from the input

		char c = in.charAt(0);

		// first check for void
		if (c == 'V') {
			return "V";
		}

		// then check for primitives
		Primitive primitive = Primitive.get(c);
		if (primitive != null) {
			return in.substring(0, 1);
		}

		// then check for classes
		if (c == 'L') {
			return readClass(in);
		}

		// then check for templates
		if (c == 'T') {
			return readClass(in);
		}

		// then check for arrays
		int dim = countArrayDimension(in);
		if (dim > 0) {
			String arrayType = TypeDescriptor.parseFirst(in.substring(dim));
			return in.substring(0, dim + arrayType.length());
		}

		throw new IllegalArgumentException("don't know how to parse: " + in);
	}

	private static int countArrayDimension(String in) {
		int i = 0;
		while (i < in.length() && in.charAt(i) == '[') {
			i++;
		}

		return i;
	}

	private static String readClass(String in) {
		// read all the characters in the buffer until we hit a ';'
		// include the parameters too
		StringBuilder buf = new StringBuilder();
		int depth = 0;
		for (int i = 0; i < in.length(); i++) {
			char c = in.charAt(i);
			buf.append(c);

			if (c == '<') {
				depth++;
			} else if (c == '>') {
				depth--;
			} else if (depth == 0 && c == ';') {
				return buf.toString();
			}
		}

		return null;
	}

	public static TypeDescriptor of(String name) {
		return new TypeDescriptor("L" + name + ";");
	}

	@Override
	public String toString() {
		return this.desc;
	}

	public boolean isVoid() {
		return this.desc.length() == 1 && this.desc.charAt(0) == 'V';
	}

	public boolean isPrimitive() {
		return this.desc.length() == 1 && Primitive.get(this.desc.charAt(0)) != null;
	}

	public Primitive getPrimitive() {
		if (!this.isPrimitive()) {
			throw new IllegalStateException("not a primitive");
		}

		return Primitive.get(this.desc.charAt(0));
	}

	public boolean isType() {
		return this.desc.charAt(0) == 'L' && this.desc.charAt(this.desc.length() - 1) == ';';
	}

	public ClassEntry getTypeEntry() {
		if (this.isType()) {
			String name = this.desc.substring(1, this.desc.length() - 1);

			int pos = name.indexOf('<');
			if (pos >= 0) {
				// remove the parameters from the class name
				name = name.substring(0, pos);
			}

			return new ClassEntry(name);
		} else if (this.isArray() && this.getArrayType().isType()) {
			return this.getArrayType().getTypeEntry();
		} else {
			throw new IllegalStateException("desc doesn't have a class");
		}
	}

	public boolean isArray() {
		return this.desc.charAt(0) == '[';
	}

	public int getArrayDimension() {
		if (!this.isArray()) {
			throw new IllegalStateException("not an array");
		}

		return countArrayDimension(this.desc);
	}

	public TypeDescriptor getArrayType() {
		if (!this.isArray()) {
			throw new IllegalStateException("not an array");
		}

		return new TypeDescriptor(this.desc.substring(this.getArrayDimension()));
	}

	public boolean containsType() {
		return this.isType() || (this.isArray() && this.getArrayType().containsType());
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof TypeDescriptor descriptor && this.equals(descriptor);
	}

	public boolean equals(TypeDescriptor other) {
		return this.desc.equals(other.desc);
	}

	@Override
	public int hashCode() {
		return this.desc.hashCode();
	}

	public TypeDescriptor remap(UnaryOperator<String> remapper) {
		String desc = this.desc;
		if (this.isType() || (this.isArray() && this.containsType())) {
			String replacedName = remapper.apply(this.getTypeEntry().getFullName());
			if (replacedName != null) {
				if (this.isType()) {
					desc = "L" + replacedName + ";";
				} else {
					desc = getArrayPrefix(this.getArrayDimension()) + "L" + replacedName + ";";
				}
			}
		}

		return new TypeDescriptor(desc);
	}

	private static String getArrayPrefix(int dimension) {
		return "[".repeat(Math.max(0, dimension));
	}

	public int getSize() {
		switch (this.desc.charAt(0)) {
			case 'J', 'D' -> {
				if (this.desc.length() == 1) {
					return 2;
				} else {
					return 1;
				}
			}
			default -> {
				return 1;
			}
		}
	}

	@Override
	public TranslateResult<TypeDescriptor> extendedTranslate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		return TranslateResult.ungrouped(this.remap(name -> translator.translate(new ClassEntry(name)).getFullName()));
	}

	public enum Primitive {
		BYTE('B', "byte", 1),
		CHARACTER('C', "char", 1),
		SHORT('S', "short", 1),
		INTEGER('I', "int", 1),
		LONG('J', "long", 2),
		FLOAT('F', "float", 1),
		DOUBLE('D', "double", 2),
		BOOLEAN('Z', "boolean", 1);

		private static final Map<Character, Primitive> lookup;

		static {
			lookup = Maps.newTreeMap();
			for (Primitive val : values()) {
				lookup.put(val.getCode(), val);
			}
		}

		private final char code;
		private final String keyword;
		private final int size;

		Primitive(char code, String keyword, int size) {
			this.code = code;
			this.keyword = keyword;
			this.size = size;
		}

		public static Primitive get(char code) {
			return lookup.get(code);
		}

		public char getCode() {
			return this.code;
		}

		/**
		 * @return the amount the primitive will increment parameter indices
		 */
		public int getSize() {
			return this.size;
		}

		/**
		 * Returns the Java keyword corresponding to this primitive.
		 */
		public String getKeyword() {
			return this.keyword;
		}
	}
}
