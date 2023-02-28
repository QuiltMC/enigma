/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.translation.representation.entry;

import cuchaz.enigma.source.RenamableTokenType;
import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.IdentifierValidation;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.utils.validation.ValidationContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class ClassEntry extends ParentedEntry<ClassEntry> implements Comparable<ClassEntry> {

	/**
	 * A class' full name is its internal name, as returned by Class.getName.
	 */
	protected final String fullName;
	/**
	 * A class' simple name is its declared name, as given in the source code.
	 * Note: anonymous classes and array types do not have a declared name, so
	 * for those classes this field is {@code null}.
	 * 
	 * example 1: com/example/ExampleClass: 'ExampleClass'
	 * example 2: com/example/ExampleClass$ExampleInnerClass: 'ExampleInnerClass'
	 */
	protected final String simpleName;

	public ClassEntry(String fullName) {
		this(null, fullName, getSimpleName(fullName), null);
	}

	public ClassEntry(@Nullable ClassEntry parent, String fullName, @Nullable String simpleName) {
		this(parent, fullName, simpleName, null);
	}

	/**
	 * @param parent     the enclosing class of this class
	 * @param fullName   the fully qualified name of this class
	 * @param simpleName the declared name of this class (is {@code null} for anonymous classes and array types)
	 * @param javadocs
	 */
	public ClassEntry(@Nullable ClassEntry parent, String fullName, @Nullable String simpleName, @Nullable String javadocs) {
		super(parent, fullName, javadocs);

		if (parent == null && fullName.indexOf('.') >= 0) {
			throw new IllegalArgumentException("Class name must be in JVM format. ie, path/to/package/class$inner : " + fullName);
		}

		this.fullName = fullName;
		this.simpleName = simpleName;
	}

	protected ClassEntry set(ClassEntry parent, String fullName, @Nullable String simpleName, @Nullable String javadoc) {
		return new ClassEntry(parent, fullName, simpleName, javadoc);
	}

	@Override
	public Class<ClassEntry> getParentType() {
		return ClassEntry.class;
	}

	@Override
	public String getName() {
		return this.fullName;
	}

	@Override
	public String getSimpleName() {
		return this.simpleName;
	}

	@Override
	public String getFullName() {
		return this.fullName;
	}

	@Override
	public String getSourceRemapName() {
		return this.getSimpleName();
	}

	@Override
	public String getContextualName() {
		if (this.hasOuterClass())
			return getSimpleName(this.fullName);
		return this.simpleName;
	}

	@Override
	public TranslateResult<? extends ClassEntry> extendedTranslate(Translator translator, @Nonnull EntryMapping mapping) {
		if (this.fullName.charAt(0) == '[') {
			TranslateResult<TypeDescriptor> translatedName = translator.extendedTranslate(new TypeDescriptor(this.fullName));
			return translatedName.map(desc -> this.withName(desc.toString()));
		}

		String targetName = mapping.targetName();
		String javadoc = mapping.javadoc();

		return TranslateResult.of(
				targetName == null ? RenamableTokenType.OBFUSCATED : RenamableTokenType.DEOBFUSCATED,
				this.withNameAndJavadoc(targetName == null ? this.hasOuterClass() ? this.simpleName : this.fullName : targetName, javadoc)
		);
	}

	@Override
	public ClassEntry getContainingClass() {
		return this;
	}

	@Override
	public int hashCode() {
		return this.fullName.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ClassEntry entry && this.equals(entry);
	}

	public boolean equals(ClassEntry other) {
		return other != null && Objects.equals(this.fullName, other.fullName);
	}

	@Override
	public boolean canConflictWith(Entry<?> entry) {
		return true;
	}

	@Override
	public boolean canShadow(Entry<?> entry) {
		return false;
	}

	@Override
	public void validateName(ValidationContext vc, String name) {
		IdentifierValidation.validateClassName(vc, name, this.hasOuterClass());
	}

	@Override
	public ClassEntry withName(String name) {
		return this.withNameAndJavadoc(name, this.javadocs);
	}

	public ClassEntry withNameAndJavadoc(String name, @Nullable String javadoc) {
		String fullName = this.fullName;
		String simpleName = this.simpleName;

		if (this.parent == null) {
			// not an inner class; given name is full internal name
			fullName = name;
			simpleName = getSimpleName(fullName);
		} else if (simpleName != null) { // anonymous classes cannot be renamed
			// inner class; given name is simple name
			if (fullName.endsWith(simpleName)) {
				// The convention is for the full name of inner classes
				// to have the form OuterClassName$InnerName but this is
				// not required by the JVM spec!
				fullName = fullName.substring(0, fullName.length() - simpleName.length()) + name;
			}
			simpleName = name;
		}

		return this.set(this.parent, fullName, simpleName, javadoc);
	}

	@Override
	public ClassEntry withParent(ClassEntry parent) {
		String fullName = this.fullName;
		String simpleName = this.simpleName;

		if (this.parent == null) {
			// current parent is null, so reset simple name
			simpleName = null;
		} else if (fullName.startsWith(this.parent.fullName)) {
			// The convention is for the full name of inner classes
			// to have the form OuterClassName$InnerName but this is
			// not required by the JVM spec!
			fullName = parent.fullName + fullName.substring(this.parent.fullName.length());
		} else {
			// even if convention is not followed, the full name
			// should adopt the parent class' package, so as to
			// avoid package access errors
			String oldPackage = this.getPackageName();
			String newPackage = parent.getPackageName();

			fullName = newPackage + fullName.substring(oldPackage.length());
		}

		return this.set(parent, fullName, simpleName, this.javadocs);
	}

	@Override
	public String toString() {
		return this.fullName;
	}

	public String getPackageName() {
		return getParentPackage(this.fullName);
	}

	public boolean hasOuterClass() {
		return this.parent != null;
	}

	@Nullable
	public ClassEntry getOuterClass() {
		return this.parent;
	}

	@Nonnull
	public ClassEntry getOutermostClass() {
		return this.hasOuterClass() ? this.getOuterClass().getOutermostClass() : this;
	}

	public String getInnerName() {
		return this.hasOuterClass() ? this.simpleName : null;
	}

	public boolean isJre() {
		String packageName = this.getPackageName();
		return packageName != null && (packageName.startsWith("java/") || packageName.startsWith("javax/"));
	}

	public static String getParentPackage(String name) {
		int pos = name.lastIndexOf('/');
		if (pos > 0) {
			return name.substring(0, pos);
		}
		return null;
	}

	public static String getNameInPackage(String name) {
		int pos = name.lastIndexOf('/');

		if (pos == name.length() - 1) {
			return "(empty)";
		}

		if (pos > 0) {
			return name.substring(pos + 1);
		}

		return name;
	}

	public static String getSimpleName(String name) {
		if (name.charAt(0) == '[') {
			return null;
		}

		int packagePos = name.lastIndexOf('/');
		if (packagePos > 0) {
			return name.substring(packagePos + 1);
		}
		return name;
	}

	public static String stripOuterClassName(String name) {
		if (name.charAt(0) == '[') {
			return null;
		}

		int innerPos = name.lastIndexOf('$');
		if (innerPos > 0) {
			return name.substring(innerPos + 1);
		}
		return name;
	}

	@Override
	public int compareTo(ClassEntry entry) {
		int length = this.fullName.length();
		int otherLength = entry.fullName.length();

		if (length != otherLength) {
			return length - otherLength;
		}

		return this.fullName.compareTo(entry.fullName);
	}
}
