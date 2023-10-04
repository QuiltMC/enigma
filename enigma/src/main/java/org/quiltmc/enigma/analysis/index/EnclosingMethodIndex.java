package org.quiltmc.enigma.analysis.index;

import org.quiltmc.enigma.api.analysis.index.JarIndexer;
import org.quiltmc.enigma.api.translation.representation.entry.ClassDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class EnclosingMethodIndex implements JarIndexer {
	private final Map<ClassDefEntry, EnclosingMethodData> enclosingMethodData = new HashMap<>();

	@Override
	public void indexEnclosingMethod(ClassDefEntry classEntry, EnclosingMethodData enclosingMethodData) {
		this.enclosingMethodData.put(classEntry, enclosingMethodData);
	}

	@Nullable
	public EnclosingMethodData getEnclosingMethodData(ClassEntry entry) {
		return this.enclosingMethodData.get(entry);
	}

	public boolean hasEnclosingMethod(ClassEntry entry) {
		return this.getEnclosingMethodData(entry) != null;
	}

	@Override
	public String getTranslationKey() {
		return "progress.jar.indexing.process.enclosing_methods";
	}
}
