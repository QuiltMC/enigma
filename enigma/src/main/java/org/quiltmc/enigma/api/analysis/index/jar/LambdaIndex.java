package org.quiltmc.enigma.api.analysis.index.jar;

import com.google.common.collect.ImmutableListMultimap;
import org.quiltmc.enigma.api.analysis.ReferenceTargetType;
import org.quiltmc.enigma.api.translation.representation.Lambda;
import org.quiltmc.enigma.api.translation.representation.entry.MethodDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LambdaIndex implements JarIndexer {
	private final Map<MethodEntry, MethodDefEntry> callers = new HashMap<>();
	private ImmutableListMultimap<MethodEntry, MethodEntry> lambdas = null;
	private final ImmutableListMultimap.Builder<MethodEntry, MethodEntry> lambdasBuilder = ImmutableListMultimap.builder();

	@Override
	public void indexLambda(MethodDefEntry callerEntry, Lambda lambda, ReferenceTargetType targetType) {
		MethodEntry implMethod = (MethodEntry) lambda.implMethod();
		this.callers.put(implMethod, callerEntry);

		this.lambdasBuilder.put(callerEntry, implMethod);
	}

	@Override
	public void processIndex(JarIndex index) {
		var nestedLambdas = this.lambdasBuilder.build();

		// denest
		ImmutableListMultimap.Builder<MethodEntry, MethodEntry> topLevelLambdasBuilder = ImmutableListMultimap.builder();
		for (var callerMethod : nestedLambdas.keySet()) {
			// if caller method is a lambda itself, find the top level method
			boolean isLambda = nestedLambdas.containsValue(callerMethod);
			MethodEntry topLevel = callerMethod;

			while (isLambda) {
				topLevel = this.callers.get(topLevel);
				isLambda = nestedLambdas.containsValue(topLevel);
			}

			topLevelLambdasBuilder.put(topLevel, callerMethod);
		}

		this.lambdas = topLevelLambdasBuilder.build();
	}

	public MethodDefEntry getCaller(MethodEntry lambda) {
		return this.callers.get(lambda);
	}

	@Nullable
	public List<MethodEntry> getInternalLambdas(MethodEntry caller) {
		return this.lambdas.get(caller);
	}

	@Override
	public String getTranslationKey() {
		return "progress.jar.indexing.process.lambdas";
	}
}
