package org.quiltmc.enigma.api.source;

import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.impl.source.bytecode.BytecodeDecompiler;
import org.quiltmc.enigma.impl.source.cfr.CfrDecompiler;
import org.quiltmc.enigma.impl.source.procyon.ProcyonDecompiler;
import org.quiltmc.enigma.impl.source.vineflower.VineflowerDecompiler;

import java.util.function.BiFunction;

public class Decompilers {
	public static final DecompilerService VINEFLOWER = create("enigma:vineflower", VineflowerDecompiler::new);
	public static final DecompilerService PROCYON = create("enigma:procyon", ProcyonDecompiler::new);
	public static final DecompilerService CFR = create("enigma:cfr", CfrDecompiler::new);
	public static final DecompilerService BYTECODE = create("enigma:bytecode", BytecodeDecompiler::new);

	private static DecompilerService create(String id, BiFunction<ClassProvider, SourceSettings, Decompiler> factory) {
		return new DecompilerService() {
			@Override
			public Decompiler create(ClassProvider classProvider, SourceSettings settings) {
				return factory.apply(classProvider, settings);
			}

			@Override
			public String getId() {
				return id;
			}
		};
	}
}
