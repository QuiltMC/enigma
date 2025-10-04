package org.quiltmc.enigma.gui.panel;

import org.quiltmc.enigma.api.class_handle.ClassHandle;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.ParentedEntry;
import org.quiltmc.enigma.gui.Gui;

import javax.annotation.Nullable;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JWindow;
import java.awt.BorderLayout;
import java.awt.MouseInfo;
import java.awt.Window;

public class EditorTooltip extends JWindow {
	private final Gui gui;
	private final Box content;

	@Nullable
	private DeclarationSnippetPanel declarationSnippet;

	public EditorTooltip(Gui gui) {
		super();

		this.gui = gui;
		this.content = new Box(BoxLayout.PAGE_AXIS);

		this.setAlwaysOnTop(true);
		this.setType(Window.Type.POPUP);
		this.setLayout(new BorderLayout());
		this.setContentPane(this.content);
	}

	/**
	 * Opens this tooltip and populates it with information about the passed {@code target}.
	 *
	 * @param target the entry whose information will be displayed
	 */
	public void open(Entry<?> target) {
		this.content.removeAll();

		final Entry<?> deobfTarget = this.gui.getController().getProject().getRemapper().deobfuscate(target);

		// TODO show parent name instead
		this.content.add(new JLabel(deobfTarget.getFullName()));

		if (this.declarationSnippet != null) {
			this.declarationSnippet.classHandler.removeListener();
			this.declarationSnippet = null;
		}

		if (target instanceof ParentedEntry<?> parentedTarget) {
			final ClassEntry targetTopClass = parentedTarget.getTopLevelClass();

			final ClassHandle targetTopClassHandle = this.gui.getController().getClassHandleProvider()
					.openClass(targetTopClass);

			if (targetTopClassHandle != null) {
				this.declarationSnippet = new DeclarationSnippetPanel(this.gui, target, targetTopClassHandle);

				// TODO create method that packs and adjusts position as necessary
				this.declarationSnippet.addSourceSetListener(source -> this.pack());

				this.content.add(this.declarationSnippet.ui);
			} else {
				this.content.add(new JLabel("No source available"));
			}
		}

		// TODO offset from cursor slightly + ensure on-screen
		this.setLocation(MouseInfo.getPointerInfo().getLocation());

		// TODO clamp size
		// TODO create method that packs and adjusts position as necessary
		this.pack();

		this.setVisible(true);
	}

	public void close() {
		this.setVisible(false);
		this.content.removeAll();

		if (this.declarationSnippet != null) {
			this.declarationSnippet.classHandler.removeListener();
		}
	}
}
