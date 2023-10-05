package org.quiltmc.enigma.config;

public interface ConfigStructureVisitor {
	void visitKeyValue(String key, String value);

	void visitSection(String section);

	void jumpToRootSection();
}
