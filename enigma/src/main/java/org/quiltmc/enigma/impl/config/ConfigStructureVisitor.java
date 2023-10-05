package org.quiltmc.enigma.impl.config;

public interface ConfigStructureVisitor {
	void visitKeyValue(String key, String value);

	void visitSection(String section);

	void jumpToRootSection();
}
