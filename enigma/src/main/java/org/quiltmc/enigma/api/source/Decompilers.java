package org.quiltmc.enigma.api.source;

import org.quiltmc.enigma.impl.source.bytecode.BytecodeDecompiler;
import org.quiltmc.enigma.impl.source.cfr.CfrDecompiler;
import org.quiltmc.enigma.impl.source.procyon.ProcyonDecompiler;
import org.quiltmc.enigma.impl.source.vineflower.VineflowerDecompiler;

public class Decompilers {
	public static final DecompilerService VINEFLOWER = VineflowerDecompiler::new;
	public static final DecompilerService PROCYON = ProcyonDecompiler::new;
	public static final DecompilerService CFR = CfrDecompiler::new;
	public static final DecompilerService BYTECODE = BytecodeDecompiler::new;
}
