package org.quiltmc.enigma.impl.plugin;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.class_provider.ProjectClassProvider;
import org.quiltmc.enigma.api.service.JarIndexerService;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;

import java.util.Set;

public class EnumConstantIndexingService implements JarIndexerService {
	public static final String ID = "enigma:enum_initializer_indexer";

	private final EnumFieldNameFindingVisitor visitor;

	EnumConstantIndexingService(EnumFieldNameFindingVisitor visitor) {
		this.visitor = visitor;
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

	public boolean isEnumConstant(FieldEntry field) {
		return this.visitor.isEnumConstant(field);
	}

	@Nullable
	public String getEnumConstantName(FieldEntry field) {
		return this.visitor.getEnumConstantName(field);
	}
}
