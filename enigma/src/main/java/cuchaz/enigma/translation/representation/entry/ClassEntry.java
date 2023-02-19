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
import java.util.List;
import java.util.Objects;

public class ClassEntry extends ParentedEntry<ClassEntry> implements Comparable<ClassEntry> {

	protected final String localPrefix;
	protected final String fullName;

	public ClassEntry(String fullName) {
		this(getOuterClass(fullName), getLocalPrefix(fullName), getInnerName(fullName), null);
	}

	public ClassEntry(@Nullable ClassEntry parent, String localPrefix, String className) {
		this(parent, localPrefix, className, null);
	}

	public ClassEntry(@Nullable ClassEntry parent, String localPrefix, String className, @Nullable String javadocs) {
		super(parent, className, javadocs);

		this.localPrefix = localPrefix;
		if (parent != null) {
			this.fullName = parent.getFullName() + "$" + this.localPrefix + this.name;
		} else {
			this.fullName = this.name;
		}

		if (parent == null && className.indexOf('.') >= 0) {
			throw new IllegalArgumentException("Class name must be in JVM format. ie, path/to/package/class$inner : " + className);
		}
	}

	@Override
	public Class<ClassEntry> getParentType() {
		return ClassEntry.class;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getSimpleName() {
		int packagePos = this.name.lastIndexOf('/');
		if (packagePos > 0) {
			return this.name.substring(packagePos + 1);
		}
		return this.name;
	}

	@Override
	public String getFullName() {
		return this.fullName;
	}

	@Override
	public String getContextualName() {
		if (this.isInnerClass()) {
			return this.parent.getSimpleName() + "$" + this.name;
		}
		return this.getSimpleName();
	}

	@Override
	public TranslateResult<? extends ClassEntry> extendedTranslate(Translator translator, @Nonnull EntryMapping mapping) {
		if (this.name.charAt(0) == '[') {
			TranslateResult<TypeDescriptor> translatedName = translator.extendedTranslate(new TypeDescriptor(this.name));
			return translatedName.map(desc -> new ClassEntry(this.parent, this.localPrefix, desc.toString()));
		}

		String translatedName = mapping.targetName() != null ? mapping.targetName() : this.name;
		String docs = mapping.javadoc();
		return TranslateResult.of(
				mapping.targetName() == null ? RenamableTokenType.OBFUSCATED : RenamableTokenType.DEOBFUSCATED,
				new ClassEntry(this.parent, this.localPrefix, translatedName, docs)
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
		return other != null && Objects.equals(this.parent, other.parent) && this.name.equals(other.name);
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
		IdentifierValidation.validateClassName(vc, name, this.isInnerClass());
	}

	@Override
	public ClassEntry withName(String name) {
		return new ClassEntry(this.parent, this.localPrefix, name, this.javadocs);
	}

	@Override
	public ClassEntry withParent(ClassEntry parent) {
		return new ClassEntry(parent, this.localPrefix, this.name, this.javadocs);
	}

	@Override
	public String toString() {
		return this.getFullName();
	}

	public String getPackageName() {
		return getParentPackage(this.fullName);
	}

	public String getLocalPrefix() {
		return this.localPrefix;
	}

	/**
	 * Returns whether this class entry has a parent, and therefore is an inner class.
	 */
	public boolean isInnerClass() {
		return this.parent != null;
	}

	/**
	 * 
	 * Returns whether this class entry is a local inner class (i.e. an inner class
	 * that is nested inside a method).
	 */
	public boolean isLocalClass() {
		return this.parent != null && !this.localPrefix.isBlank();
	}

	@Nullable
	public ClassEntry getOuterClass() {
		return this.parent;
	}

	@Nonnull
	public ClassEntry getOutermostClass() {
		if (this.parent == null) {
			return this;
		}
		return this.parent.getOutermostClass();
	}

	public ClassEntry buildClassEntry(List<ClassEntry> classChain) {
		assert (classChain.contains(this));
		StringBuilder buf = new StringBuilder();
		for (ClassEntry chainEntry : classChain) {
			if (buf.length() == 0) {
				buf.append(chainEntry.getFullName());
			} else {
				buf.append("$");
				buf.append(chainEntry.getSimpleName());
			}

			if (chainEntry == this) {
				break;
			}
		}
		return new ClassEntry(buf.toString());
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

	@Nullable
	public static ClassEntry getOuterClass(String name) {
		if (name.charAt(0) == '[') {
			return null;
		}

		int index = name.lastIndexOf('$');
		if (index >= 0) {
			return new ClassEntry(name.substring(0, index));
		}
		return null;
	}

	public static String getLocalPrefix(String name) {
		int from = name.lastIndexOf('$') + 1;
		int to = getInnerNameIndex(name);

		return (to < 0) ? "" : name.substring(from, to);
	}

	public static String getInnerName(String name) {
		int index = getInnerNameIndex(name);
		return (index < 0) ? name : name.substring(index);
	}

	/**
	 * Given a class' full name, returns the index within
	 * that name where the inner name starts. Local classes
	 * make this trickier than simply testing for the last
	 * occurrence of $ as they have a number prefix that is
	 * not part of the inner name.
	 * 
	 * @return the index within the string of the class' inner name
	 *         if the class is a nested class, otherwise -1
	 */
	private static int getInnerNameIndex(String name) {
		if (name.charAt(0) == '[') {
			return -1;
		}

		int index = name.lastIndexOf('$');

		if (index < 0) {
			return -1;
		}

		// fallback index if the class is anonymous
		// because then the entire inner name is digits
		int fallback = index + 1;

		// local classes have a number prefix, skip past it
		for (index++; index < name.length(); index++) {
			char chr = name.charAt(index);

			if (!Character.isDigit(chr)) {
				break;
			}
		}

		return (index == name.length()) ? fallback : index;
	}

	@Override
	public String getSourceRemapName() {
		ClassEntry outerClass = this.getOuterClass();
		if (outerClass != null) {
			return outerClass.getSourceRemapName() + "." + this.name;
		}
		return this.getSimpleName();
	}

	@Override
	public int compareTo(ClassEntry entry) {
		String name = this.getFullName();
		String otherFullName = entry.getFullName();

		if (name.length() != otherFullName.length()) {
			return name.length() - otherFullName.length();
		}

		return name.compareTo(otherFullName);
	}
}
