package org.quiltmc.enigma.source;

import org.quiltmc.enigma.source.bytecode.BytecodeDecompiler;
import org.quiltmc.enigma.source.cfr.CfrDecompiler;
import org.quiltmc.enigma.source.procyon.ProcyonDecompiler;
import org.quiltmc.enigma.source.vineflower.VineflowerDecompiler;

public class Decompilers {
	public static final DecompilerService VINEFLOWER = VineflowerDecompiler::new;
	public static final DecompilerService PROCYON = ProcyonDecompiler::new;
	public static final DecompilerService CFR = CfrDecompiler::new;
	public static final DecompilerService BYTECODE = BytecodeDecompiler::new;
}
