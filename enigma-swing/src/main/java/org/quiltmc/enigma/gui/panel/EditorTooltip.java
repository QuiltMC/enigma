package org.quiltmc.enigma.gui.panel;

import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.class_handle.ClassHandle;
import org.quiltmc.enigma.api.translation.representation.AccessFlags;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;

import javax.annotation.Nullable;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JWindow;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.MouseInfo;
import java.awt.Window;
import java.util.Optional;
import java.util.function.Consumer;

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

		if (this.declarationSnippet != null) {
			this.declarationSnippet.classHandler.removeListener();
			this.declarationSnippet = null;
		}

		this.addRow(new JLabel("From: "), new JLabel(this.getParentName(target).orElse("un-packaged class")));

		final String javadoc = this.gui.getController().getProject().getRemapper().getMapping(target).javadoc();
		if (javadoc != null) {
			this.add(new JSeparator());

			final JTextArea javadocComponent = new JTextArea(javadoc);
			javadocComponent.setLineWrap(true);
			javadocComponent.setWrapStyleWord(true);
			javadocComponent.setForeground(Config.getCurrentSyntaxPaneColors().comment.value());
			javadocComponent.setFont(Config.currentFonts().editor.value().deriveFont(Font.ITALIC));
			javadocComponent.setBackground(new Color(0, 0, 0, 0));

			this.addRow(javadocComponent);
		}

		{
			final ClassHandle targetTopClassHandle = this.gui.getController().getClassHandleProvider()
					.openClass(target.getTopLevelClass());

			final Component sourceInfo;
			if (targetTopClassHandle != null) {
				this.declarationSnippet = new DeclarationSnippetPanel(this.gui, target, targetTopClassHandle);

				// TODO create method that packs and adjusts position as necessary
				this.declarationSnippet.addSourceSetListener(source -> this.pack());

				sourceInfo = this.declarationSnippet.ui;
			} else {
				sourceInfo = new JLabel("No source available");
			}

			this.addRow(sourceInfo);
		}

		// TODO offset from cursor slightly + ensure on-screen
		this.setLocation(MouseInfo.getPointerInfo().getLocation());

		// TODO clamp size
		// TODO create method that packs and adjusts position as necessary
		this.pack();

		this.setVisible(true);
	}

	private void addRow(Component... components) {
		this.addRow(row -> {
			for (final Component component : components) {
				row.add(component);
			}
		});
	}

	private void addRow(Consumer<Box> rowInitializer) {
		final Box row = Box.createHorizontalBox();
		rowInitializer.accept(row);
		row.add(Box.createHorizontalGlue());
		this.add(row);
	}

	public void close() {
		this.setVisible(false);
		this.content.removeAll();

		if (this.declarationSnippet != null) {
			this.declarationSnippet.classHandler.removeListener();
		}
	}

	private Optional<String> getParentName(Entry<?> entry) {
		final var builder = new StringBuilder();

		Entry<?> parent = entry.getParent();
		if (parent != null) {
			while (true) {
				if (!builder.isEmpty()) {
					builder.insert(0, '.');
				}

				builder.insert(0, this.getSimpleName(parent));

				final Entry<?> nextParent = parent.getParent();
				if (nextParent == null) {
					if (parent instanceof ClassEntry parentClass) {
						final String parentPackage = parentClass.getPackageName();
						if (parentPackage != null) {
							final String dotPackage = parentPackage.replace('/', '.');
							builder.insert(0, dotPackage);
						}
					}

					break;
				} else {
					parent = nextParent;
				}
			}
		}

		return builder.isEmpty() ? Optional.empty() : Optional.of(builder.toString());
	}

	private String getSimpleName(Entry<?> entry) {
		final EnigmaProject project = this.gui.getController().getProject();

		final String simpleObfName = entry.getSimpleName();
		if (!simpleObfName.isEmpty() && Character.isJavaIdentifierStart(simpleObfName.charAt(0))) {
			final AccessFlags access = project.getJarIndex().getIndex(EntryIndex.class).getEntryAccess(entry);
			if (access == null || !(access.isSynthetic())) {
				return project.getRemapper().deobfuscate(entry).getSimpleName();
			} else {
				return "<synthetic>";
			}
		}

		return "<anonymous>";
	}
}
