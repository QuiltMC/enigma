package org.quiltmc.enigma.impl.plugin;

import com.google.common.collect.BiMap;
import com.google.common.collect.Multimap;
import org.objectweb.asm.tree.ClassNode;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.class_provider.ProjectClassProvider;
import org.quiltmc.enigma.api.service.JarIndexerService;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.util.Set;

public class RecordIndexingService implements JarIndexerService {
	public static final String ID = "enigma:record_component_indexer";

	private final RecordIndexingVisitor visitor;

	RecordIndexingService(RecordIndexingVisitor visitor) {
		this.visitor = visitor;
	}

	public BiMap<FieldEntry, MethodEntry> getGettersByField() {
		return this.visitor.getGettersByField();
	}

	public Multimap<ClassEntry, FieldEntry> getFieldsByClass() {
		return this.visitor.getFieldsByClass();
	}

	public Multimap<ClassEntry, MethodEntry> getMethodsByClass() {
		return this.visitor.getMethodsByClass();
	}

	@Override
	public void acceptJar(Set<String> scope, ProjectClassProvider classProvider, JarIndex jarIndex) {
		for (String className : scope) {
			ClassNode node = classProvider.get(className);
			if (node != null) {
				node.accept(this.visitor);
			}
		}
	}

	@Override
	public String getId() {
		return ID;
	}
}
