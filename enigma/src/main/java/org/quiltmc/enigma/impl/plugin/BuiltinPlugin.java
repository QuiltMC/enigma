package org.quiltmc.enigma.impl.plugin;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.analysis.index.jar.BridgeMethodIndex;
import org.quiltmc.enigma.api.EnigmaPlugin;
import org.quiltmc.enigma.api.EnigmaPluginContext;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.service.JarIndexerService;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma.api.service.DecompilerService;
import org.quiltmc.enigma.api.source.Decompilers;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public final class BuiltinPlugin implements EnigmaPlugin {
	@Override
	public void init(EnigmaPluginContext ctx) {
		registerRecordNamingService(ctx);
		registerEnumNamingService(ctx);
		registerSpecializedMethodNamingService(ctx);
		registerDecompilerServices(ctx);
		BuiltinMappingFormats.register(ctx);
	}

	private static void registerEnumNamingService(EnigmaPluginContext ctx) {
		final Map<Entry<?>, String> names = new HashMap<>();
		final EnumFieldNameFindingVisitor visitor = new EnumFieldNameFindingVisitor(names);

		ctx.registerService(JarIndexerService.TYPE, ctx1 -> JarIndexerService.fromVisitor(visitor, "enigma:enum_initializer_indexer"));

		ctx.registerService(NameProposalService.TYPE, ctx1 -> new NameProposalService() {
			@Override
			public Map<Entry<?>, EntryMapping> getProposedNames(Enigma enigma, JarIndex index) {
				Map<Entry<?>, EntryMapping> mappings = new HashMap<>();

				index.getIndex(EntryIndex.class).getFields().forEach(field -> {
					if (names.containsKey(field)) {
						mappings.put(field, this.createMapping(names.get(field), TokenType.JAR_PROPOSED));
					}
				});

				return mappings;
			}

			@Override
			public Map<Entry<?>, EntryMapping> getDynamicProposedNames(EntryRemapper remapper, @Nullable Entry<?> obfEntry, @Nullable EntryMapping oldMapping, @Nullable EntryMapping newMapping) {
				return null;
			}

			@Override
			public String getId() {
				return "enigma:enum_name_proposer";
			}
		});
	}

	private static void registerRecordNamingService(EnigmaPluginContext ctx) {
		final BiMap<FieldEntry, MethodEntry> gettersByField = HashBiMap.create();
		final RecordGetterFindingVisitor visitor = new RecordGetterFindingVisitor(gettersByField);

		ctx.registerService(JarIndexerService.TYPE, ctx1 -> new RecordGetterFindingService(visitor));
		ctx.registerService(NameProposalService.TYPE, ctx1 -> new RecordComponentProposalService(gettersByField));
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
