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

		ImmutableListMultimap.Builder<MethodEntry, MethodEntry> multilevelLambdasBuilder = ImmutableListMultimap.builder();
		for (var callerMethod : nestedLambdas.keySet()) {
			// if caller method is a lambda itself, find the top level method
			boolean isLambda = nestedLambdas.containsValue(callerMethod);
			MethodEntry topLevel = callerMethod;

			if (!isLambda) {
				multilevelLambdasBuilder.put(topLevel, callerMethod);
			} else {
				// travel up the chain until we find the top level method, adding as we go
				while (isLambda) {
					topLevel = this.callers.get(topLevel);
					isLambda = nestedLambdas.containsValue(topLevel);

					multilevelLambdasBuilder.put(topLevel, callerMethod);
				}
			}
		}

		this.lambdas = multilevelLambdasBuilder.build();
	}

	/**
	 * {@return the top-level method that contains the given lambda}
	 * @param lambda the lambda to get the caller for
	 */
	public MethodDefEntry getCaller(MethodEntry lambda) {
		return this.callers.get(lambda);
	}

	/**
	 * {@return all lambda methods nested inside the given method}
	 * @param method the method to get lambdas for
	 */
	@Nullable
	public List<MethodEntry> getInternalLambdas(MethodEntry method) {
		return this.lambdas.get(method);
	}

	@Override
	public String getTranslationKey() {
		return "progress.jar.indexing.process.lambdas";
	}
}
