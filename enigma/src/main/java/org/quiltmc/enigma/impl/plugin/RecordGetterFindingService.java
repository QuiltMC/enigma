package org.quiltmc.enigma.impl.plugin;

import com.google.common.collect.BiMap;
import org.objectweb.asm.tree.ClassNode;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.class_provider.ProjectClassProvider;
import org.quiltmc.enigma.api.service.JarIndexerService;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.util.Set;

public class RecordGetterFindingService implements JarIndexerService {
	public static final String ID = "enigma:record_component_indexer";

	private final RecordGetterFindingVisitor visitor;

	RecordGetterFindingService(RecordGetterFindingVisitor visitor) {
		this.visitor = visitor;
	}

	public BiMap<FieldEntry, MethodEntry> getGettersByField() {
		return this.visitor.getGettersByField();
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
