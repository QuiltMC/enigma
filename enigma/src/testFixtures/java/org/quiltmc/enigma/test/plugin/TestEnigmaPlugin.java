package org.quiltmc.enigma.test.plugin;

import org.quiltmc.enigma.api.EnigmaPlugin;
import org.quiltmc.enigma.api.EnigmaPluginContext;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;

import java.util.HashMap;
import java.util.Map;

public class TestEnigmaPlugin implements EnigmaPlugin {
	@Override
	public void init(EnigmaPluginContext ctx) {
		this.registerParameterNamingService(ctx);
	}

	private void registerParameterNamingService(EnigmaPluginContext ctx) {
		ctx.registerService(NameProposalService.TYPE, ctx1 -> new ParameterNameProposalService());
	}

	public static class ParameterNameProposalService implements NameProposalService {
		private static final MethodDescriptor EQUALS_DESC = new MethodDescriptor("(Ljava/lang/Object;)Z");

		@Override
		public String getId() {
			return "test:parameters";
		}

		@Override
		public Map<Entry<?>, EntryMapping> getProposedNames(JarIndex index) {
			EntryIndex entryIndex = index.getIndex(EntryIndex.class);
			Map<Entry<?>, EntryMapping> names = new HashMap<>();
			for (var method : entryIndex.getMethods()) {
				if (method.getName().equals("equals") && method.getDesc().equals(EQUALS_DESC)) {
					var param = method.getParameters(entryIndex).get(0);
					names.put(param, this.createMapping("o", TokenType.JAR_PROPOSED));
				} else {
					for (var param : method.getParameters(entryIndex)) {
						names.put(param, this.createMapping("param" + param.getIndex(), TokenType.JAR_PROPOSED));
					}
				}
			}

			return names;
		}

		@Override
		public Map<Entry<?>, EntryMapping> getDynamicProposedNames(EntryRemapper remapper, Entry<?> obfEntry, EntryMapping oldMapping, EntryMapping newMapping) {
			return null;
		}
	}
}
