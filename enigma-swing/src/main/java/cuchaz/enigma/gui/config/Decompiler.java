package cuchaz.enigma.gui.config;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.dialog.decompiler.QuiltflowerSettingsDialog;
import cuchaz.enigma.source.DecompilerService;
import cuchaz.enigma.source.Decompilers;

import javax.swing.JDialog;
import java.util.function.BiConsumer;

public enum Decompiler {
	QUILTFLOWER("QuiltFlower", Decompilers.QUILTFLOWER, QuiltflowerSettingsDialog::new),
	CFR("CFR", Decompilers.CFR),
	PROCYON("Procyon", Decompilers.PROCYON),
	BYTECODE("Bytecode", Decompilers.BYTECODE);

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

	static {
		DecompilerConfig.bootstrap();
	}
}
