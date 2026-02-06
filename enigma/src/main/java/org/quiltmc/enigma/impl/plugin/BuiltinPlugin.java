package org.quiltmc.enigma.impl.plugin;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaPlugin;
import org.quiltmc.enigma.api.EnigmaPluginContext;
import org.quiltmc.enigma.api.analysis.index.jar.BridgeMethodIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.service.DecompilerService;
import org.quiltmc.enigma.api.service.JarIndexerService;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma.api.source.Decompilers;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.util.Version;

import java.util.HashMap;
import java.util.Map;

public final class BuiltinPlugin implements EnigmaPlugin {
	@Override
	public void init(EnigmaPluginContext ctx) {
		registerRecordNamingService(ctx);
		registerParamSyntheticFieldNamingService(ctx);
		registerEnumNamingService(ctx);
		registerSpecializedMethodNamingService(ctx);
		registerDecompilerServices(ctx);
		BuiltinMappingFormats.register(ctx);
	}

	@Override
	public boolean supportsEnigmaVersion(@NonNull Version enigmaVersion) {
		return true;
	}

	private static void registerEnumNamingService(EnigmaPluginContext ctx) {
		final EnumFieldNameFindingVisitor visitor = new EnumFieldNameFindingVisitor();

		ctx.registerService(JarIndexerService.TYPE, ctx1 -> new EnumConstantIndexingService(visitor));
		ctx.registerService(NameProposalService.TYPE, ctx1 -> new EnumConstantProposalService(visitor));
	}

	private static void registerRecordNamingService(EnigmaPluginContext ctx) {
		final RecordIndexingVisitor visitor = new RecordIndexingVisitor();

		ctx.registerService(JarIndexerService.TYPE, ctx1 -> new RecordIndexingService(visitor));
		ctx.registerService(NameProposalService.TYPE, ctx1 -> new RecordComponentProposalService(visitor));
	}

	private static void registerParamSyntheticFieldNamingService(EnigmaPluginContext ctx) {
		final ParamSyntheticFieldIndexingService indexer = new ParamSyntheticFieldIndexingService();
		ctx.registerService(JarIndexerService.TYPE, ctx1 -> indexer);
		ctx.registerService(NameProposalService.TYPE, ctx1 -> new ParamSyntheticFieldProposalService(indexer));
	}

	private static void registerSpecializedMethodNamingService(EnigmaPluginContext ctx) {
		ctx.registerService(NameProposalService.TYPE, ctx1 -> new NameProposalService() {
			@Override
			public Map<Entry<?>, EntryMapping> getProposedNames(Enigma enigma, JarIndex index) {
				BridgeMethodIndex bridgeMethodIndex = index.getIndex(BridgeMethodIndex.class);
				Map<Entry<?>, EntryMapping> mappings = new HashMap<>();

				bridgeMethodIndex.getSpecializedToBridge().forEach((specialized, bridge) -> {
					EntryMapping mapping = this.createMapping(bridge.getName(), TokenType.JAR_PROPOSED);

					mappings.put(specialized, mapping);
					// IndexEntryResolver#resolveEntry can return the bridge method, so we can just use the name
					mappings.put(bridge, mapping);
				});

				return mappings;
			}

			@Override
			public Map<Entry<?>, EntryMapping> getDynamicProposedNames(EntryRemapper remapper, @Nullable Entry<?> obfEntry, @Nullable EntryMapping oldMapping, @Nullable EntryMapping newMapping) {
				return null;
			}

			@Override
			public String getId() {
				return "enigma:specialized_method_name_proposer";
			}
		});
	}

	private static void registerDecompilerServices(EnigmaPluginContext ctx) {
		ctx.registerService(DecompilerService.TYPE, ctx1 -> Decompilers.VINEFLOWER);
		ctx.registerService(DecompilerService.TYPE, ctx1 -> Decompilers.PROCYON);
		ctx.registerService(DecompilerService.TYPE, ctx1 -> Decompilers.CFR);
		ctx.registerService(DecompilerService.TYPE, ctx1 -> Decompilers.BYTECODE);
	}
}
