package org.quiltmc.enigma.gui.config;

import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.dialog.decompiler.VineflowerSettingsDialog;
import org.quiltmc.enigma.source.DecompilerService;
import org.quiltmc.enigma.source.Decompilers;

import java.util.Map;
import java.util.function.BiConsumer;
import javax.swing.JDialog;

public enum Decompiler {
	VINEFLOWER("Vineflower", Decompilers.VINEFLOWER, VineflowerSettingsDialog::new),
	CFR("CFR", Decompilers.CFR),
	PROCYON("Procyon", Decompilers.PROCYON),
	BYTECODE("Bytecode", Decompilers.BYTECODE);

	private static final Map<String, Decompiler> LEGACY_ALIASES = Map.of("QUILTFLOWER", VINEFLOWER);

	public final DecompilerService service;
	public final String name;
	public final BiConsumer<Gui, JDialog> settingsDialog;

	Decompiler(String name, DecompilerService service) {
		this(name, service, null);
	}

	Decompiler(String name, DecompilerService service, BiConsumer<Gui, JDialog> settingsDialog) {
		this.name = name;
		this.service = service;
		this.settingsDialog = settingsDialog;
	}

	public static Decompiler valueOfLegacy(String name) {
		if (LEGACY_ALIASES.containsKey(name)) {
			return LEGACY_ALIASES.get(name);
		}

		return valueOf(name);
	}

	static {
		DecompilerConfig.bootstrap();
	}
}
