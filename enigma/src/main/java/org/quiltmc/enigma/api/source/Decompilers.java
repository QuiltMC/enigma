package org.quiltmc.enigma.api.source;

import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.impl.source.bytecode.BytecodeDecompiler;
import org.quiltmc.enigma.impl.source.cfr.CfrDecompiler;
import org.quiltmc.enigma.impl.source.procyon.ProcyonDecompiler;
import org.quiltmc.enigma.impl.source.vineflower.VineflowerDecompiler;

public class Decompilers {
	public static final DecompilerService VINEFLOWER = new DecompilerService() {
		@Override
		public Decompiler create(ClassProvider classProvider, SourceSettings settings) {
			return new VineflowerDecompiler(classProvider, settings);
		}

		@Override
		public String getId() {
			return "enigma:vineflower";
		}
	};
	public static final DecompilerService PROCYON = new DecompilerService() {
		@Override
		public Decompiler create(ClassProvider classProvider, SourceSettings settings) {
			return new ProcyonDecompiler(classProvider, settings);
		}

		@Override
		public String getId() {
			return "enigma:procyon";
		}
	};
	public static final DecompilerService CFR = new DecompilerService() {
		@Override
		public Decompiler create(ClassProvider classProvider, SourceSettings settings) {
			return new CfrDecompiler(classProvider, settings);
		}

		@Override
		public String getId() {
			return null;
		}
	};
	public static final DecompilerService BYTECODE = new DecompilerService() {
		@Override
		public Decompiler create(ClassProvider classProvider, SourceSettings settings) {
			return new BytecodeDecompiler(classProvider, settings);
		}

		@Override
		public String getId() {
			return "enigma:bytecode";
		}
	};
}
