package org.quiltmc.enigma.api.analysis.index.jar;

import org.quiltmc.enigma.api.analysis.ReferenceTargetType;
import org.quiltmc.enigma.api.translation.representation.Lambda;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * TODO
 */
public class MemberTypeIndex implements JarIndexer {
	private final EntryIndex entryIndex;

	private final Map<MethodEntry, ClassEntry> methodReturnTypes = new HashMap<>();
	private final Map<FieldEntry, ClassEntry> fieldTypes = new HashMap<>();
	private final Map<LocalVariableDefEntry, ClassEntry> paramTypes = new HashMap<>();

	public MemberTypeIndex(EntryIndex entryIndex) {
		this.entryIndex = entryIndex;
	}

	@Override
	public void indexField(FieldDefEntry fieldEntry) {
		getTypeOrArrayType(fieldEntry.getDesc()).ifPresent(type -> this.fieldTypes.put(fieldEntry, type));
	}

	@Override
	public void indexMethod(MethodDefEntry methodEntry) {
		final MethodDescriptor desc = methodEntry.getDesc();
		getTypeOrArrayType(desc.getReturnDesc()).ifPresent(type -> this.methodReturnTypes.put(methodEntry, type));

		methodEntry.streamParameters(this.entryIndex).forEach(paramEntry -> {
			getTypeOrArrayType(paramEntry.getDesc()).ifPresent(type -> this.paramTypes.put(paramEntry, type));
		});
	}

	@Override
	public void indexLambda(MethodDefEntry callerEntry, Lambda lambda, ReferenceTargetType targetType) {
		// TODO index params
	}

	@Nullable
	public ClassEntry getMethodReturnType(MethodEntry methodEntry) {
		return this.methodReturnTypes.get(methodEntry);
	}

	@Nullable
	public ClassEntry getFieldType(FieldEntry entry) {
		return this.fieldTypes.get(entry);
	}

	@Nullable
	public ClassEntry getParameterType(LocalVariableEntry parameter) {
		return this.paramTypes.get(parameter);
	}

	@Override
	public String getTranslationKey() {
		return "progress.jar.indexing.process.member_types";
	}

	private static Optional<ClassEntry> getTypeOrArrayType(TypeDescriptor desc) {
		if (desc.isType()) {
			return Optional.of(desc.getTypeEntry());
		} else if (desc.isArray()) {
            return getTypeOrArrayType(desc.getArrayType());
		} else {
			return Optional.empty();
		}
	}
}
