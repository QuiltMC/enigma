package org.quiltmc.enigma.gui.config;

import org.quiltmc.enigma.api.service.DecompilerService;
import org.quiltmc.enigma.api.source.Decompilers;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.dialog.decompiler.VineflowerSettingsDialog;

import javax.swing.JDialog;
import java.util.function.BiConsumer;

public enum Decompiler {
	VINEFLOWER("Vineflower", Decompilers.VINEFLOWER, VineflowerSettingsDialog::new),
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
}
