package org.quiltmc.enigma;

import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

public final class TestEntryFactory {
	private TestEntryFactory() {
	}

	public static ClassEntry newClass(String name) {
		return new ClassEntry(name);
	}

	public static FieldEntry newField(String className, String fieldName, String fieldType) {
		return newField(newClass(className), fieldName, fieldType);
	}

	public static FieldEntry newField(ClassEntry classEntry, String fieldName, String fieldType) {
		return new FieldEntry(classEntry, fieldName, new TypeDescriptor(fieldType));
	}

	public static MethodEntry newMethod(String className, String methodName, String methodSignature) {
		return newMethod(newClass(className), methodName, methodSignature);
	}

	public static MethodEntry newMethod(ClassEntry classEntry, String methodName, String methodSignature) {
		return new MethodEntry(classEntry, methodName, new MethodDescriptor(methodSignature));
	}

	public static LocalVariableEntry newParameter(MethodEntry parent, int index) {
		return new LocalVariableEntry(parent, index);
	}

	public static EntryReference<FieldEntry, MethodEntry> newFieldReferenceByMethod(FieldEntry fieldEntry, String callerClassName, String callerName, String callerSignature) {
		return new EntryReference<>(fieldEntry, "", newMethod(callerClassName, callerName, callerSignature));
	}

	public static EntryReference<MethodEntry, MethodEntry> newBehaviorReferenceByMethod(MethodEntry methodEntry, String callerClassName, String callerName, String callerSignature) {
		return new EntryReference<>(methodEntry, "", newMethod(callerClassName, callerName, callerSignature));
	}
}
