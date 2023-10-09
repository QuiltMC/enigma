package org.quiltmc.enigma.api.service;

import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.objectweb.asm.ClassVisitor;

import java.util.Set;

public interface JarIndexerService extends EnigmaService {
	EnigmaServiceType<JarIndexerService> TYPE = EnigmaServiceType.create("jar_indexer");

	void acceptJar(Set<String> scope, ClassProvider classProvider, JarIndex jarIndex);

	static JarIndexerService fromVisitor(ClassVisitor visitor) {
		return (scope, classProvider, jarIndex) -> {
			for (String className : scope) {
				classProvider.get(className).accept(visitor);
			}
		};
	}
}
