package org.quiltmc.enigma.api.analysis.index.jar;

import org.quiltmc.enigma.api.analysis.ReferenceTargetType;
import org.quiltmc.enigma.api.translation.representation.Lambda;
import org.quiltmc.enigma.api.translation.representation.entry.MethodDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.util.HashMap;
import java.util.Map;

public class LambdaIndex implements JarIndexer {
	private final Map<MethodEntry, MethodDefEntry> callers = new HashMap<>();

	@Override
	public void indexLambda(MethodDefEntry callerEntry, Lambda lambda, ReferenceTargetType targetType) {
		MethodEntry implMethod = (MethodEntry) lambda.implMethod();
		this.callers.put(implMethod, callerEntry);
	}

	public MethodDefEntry getCaller(MethodEntry lambda) {
		return this.callers.get(lambda);
	}

	@Override
	public String getTranslationKey() {
		return "progress.jar.indexing.process.lambdas";
	}
}
