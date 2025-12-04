package org.quiltmc.enigma.api.analysis.index.jar;

import com.google.common.collect.Maps;
import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.translation.representation.AccessFlags;
import org.quiltmc.enigma.api.translation.representation.ArgumentDescriptor;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class BridgeMethodIndexImpl implements BridgeMethodIndex {
	private final EntryIndex entryIndex;
	private final InheritanceIndex inheritanceIndex;
	private final ReferenceIndex referenceIndex;

	private final Map<MethodEntry, MethodEntry> bridgeToSpecialized = Maps.newHashMap();
	private final Map<MethodEntry, MethodEntry> specializedToBridge = Maps.newHashMap();

	BridgeMethodIndexImpl(EntryIndex entryIndex, InheritanceIndex inheritanceIndex, ReferenceIndex referenceIndex) {
		this.entryIndex = entryIndex;
		this.inheritanceIndex = inheritanceIndex;
		this.referenceIndex = referenceIndex;
	}

	@Override
	public void findBridgeMethods() {
		// look for access and bridged methods
		for (MethodEntry methodEntry : this.entryIndex.getMethods()) {
			MethodDefEntry methodDefEntry = (MethodDefEntry) methodEntry;

			AccessFlags access = methodDefEntry.getAccess();
			if (access == null || !access.isSynthetic()) {
				continue;
			}

			this.indexSyntheticMethod(methodDefEntry, access);
		}
	}

	@Override
	public void processIndex(JarIndex index) {
		Map<MethodEntry, MethodEntry> copiedAccessToBridge = new HashMap<>(this.specializedToBridge);

		for (Map.Entry<MethodEntry, MethodEntry> entry : copiedAccessToBridge.entrySet()) {
			MethodEntry specializedEntry = entry.getKey();
			MethodEntry bridgeEntry = entry.getValue();
			if (bridgeEntry.getName().equals(specializedEntry.getName())) {
				continue;
			}

			MethodEntry renamedSpecializedEntry = specializedEntry.withName(bridgeEntry.getName());
			this.specializedToBridge.put(renamedSpecializedEntry, this.specializedToBridge.get(specializedEntry));
		}
	}

	private void indexSyntheticMethod(MethodDefEntry syntheticMethod, AccessFlags access) {
		MethodEntry specializedMethod = this.findSpecializedMethod(syntheticMethod);
		if (specializedMethod == null) {
			return;
		}

		if (access.isBridge() || this.isPotentialBridge(syntheticMethod, specializedMethod)) {
			this.bridgeToSpecialized.put(syntheticMethod, specializedMethod);
			if (this.specializedToBridge.containsKey(specializedMethod)) {
				// we already have a bridge for this method, so we keep the one higher in the hierarchy
				// can happen with a class inheriting from a superclass with one or more bridge method(s)
				MethodEntry bridgeMethod = this.specializedToBridge.get(specializedMethod);
				this.specializedToBridge.put(specializedMethod, this.getHigherMethod(syntheticMethod, bridgeMethod));
			} else {
				this.specializedToBridge.put(specializedMethod, syntheticMethod);
			}
		}
	}

	private MethodEntry findSpecializedMethod(MethodEntry method) {
		// we want to find all compiler-added methods that directly call another with no processing

		// get all the methods that we call
		final Collection<MethodEntry> referencedMethods = this.referenceIndex.getMethodsReferencedBy(method);

		// is there just one?
		if (referencedMethods.size() != 1) {
			return null;
		}

		return referencedMethods.stream().findFirst().orElse(null);
	}

	private boolean isPotentialBridge(MethodDefEntry bridgeMethod, MethodEntry specializedMethod) {
		// Bridge methods only exist for inheritance purposes, if we're private, final, or static, we cannot be inherited
		AccessFlags bridgeAccess = bridgeMethod.getAccess();
		if (bridgeAccess.isPrivate() || bridgeAccess.isFinal() || bridgeAccess.isStatic()) {
			return false;
		}

		MethodDescriptor bridgeDesc = bridgeMethod.getDesc();
		MethodDescriptor specializedDesc = specializedMethod.getDesc();
		List<ArgumentDescriptor> bridgeArguments = bridgeDesc.getArgumentDescs();
		List<ArgumentDescriptor> specializedArguments = specializedDesc.getArgumentDescs();

		// A bridge method will always have the same number of arguments
		if (bridgeArguments.size() != specializedArguments.size()) {
			return false;
		}

		// Check that all argument types are bridge-compatible
		for (int i = 0; i < bridgeArguments.size(); i++) {
			if (!this.areTypesBridgeCompatible(bridgeArguments.get(i), specializedArguments.get(i))) {
				return false;
			}
		}

		// Check that the return type is bridge-compatible
		return this.areTypesBridgeCompatible(bridgeDesc.getReturnDesc(), specializedDesc.getReturnDesc());
	}

	private boolean areTypesBridgeCompatible(TypeDescriptor bridgeDesc, TypeDescriptor specializedDesc) {
		if (bridgeDesc.equals(specializedDesc)) {
			return true;
		}

		// Either the descs will be equal, or they are both types and different through a generic
		if (bridgeDesc.isType() && specializedDesc.isType()) {
			ClassEntry bridgeType = bridgeDesc.getTypeEntry();
			ClassEntry accessedType = specializedDesc.getTypeEntry();

			// If the given types are completely unrelated to each other, this can't be bridge compatible
			InheritanceIndex.Relation relation = this.inheritanceIndex.computeClassRelation(accessedType, bridgeType);
			return relation != InheritanceIndex.Relation.UNRELATED;
		}

		return false;
	}

	// Get the method higher in the hierarchy
	private MethodEntry getHigherMethod(MethodEntry bridgeMethod1, MethodEntry bridgeMethod2) {
		ClassEntry parent1 = bridgeMethod1.getParent();
		ClassEntry parent2 = bridgeMethod2.getParent();
		return this.inheritanceIndex.getDescendants(parent1).contains(parent2) ? bridgeMethod1 : bridgeMethod2;
	}

	@Override
	public boolean isBridgeMethod(MethodEntry entry) {
		return this.bridgeToSpecialized.containsKey(entry);
	}

	@Override
	public boolean isSpecializedMethod(MethodEntry entry) {
		return this.specializedToBridge.containsKey(entry);
	}

	@Override
	public @Nullable MethodEntry getBridgeFromSpecialized(MethodEntry specialized) {
		return this.specializedToBridge.get(specialized);
	}

	@Override
	public MethodEntry getSpecializedFromBridge(MethodEntry bridge) {
		return this.bridgeToSpecialized.get(bridge);
	}

	@Override
	public Map<MethodEntry, MethodEntry> getSpecializedToBridge() {
		return Collections.unmodifiableMap(this.specializedToBridge);
	}

	@Override
	public Map<MethodEntry, MethodEntry> getBridgeToSpecialized() {
		return Collections.unmodifiableMap(this.bridgeToSpecialized);
	}
}
