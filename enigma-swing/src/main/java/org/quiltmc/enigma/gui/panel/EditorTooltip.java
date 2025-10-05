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
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Optional;
import java.util.function.Consumer;

public class EditorTooltip extends JWindow {
	private final Gui gui;
	private final Box content;

	@Nullable
	private Point dragStart;

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

		Toolkit.getDefaultToolkit().addAWTEventListener(
				e -> {
					if (e instanceof MouseEvent mouseEvent && mouseEvent.getID() == MouseEvent.MOUSE_RELEASED) {
						EditorTooltip.this.dragStart = null;
					}
				},
				MouseEvent.MOUSE_RELEASED
		);

		// TODO
		//  - update tooltip with clicked entry declaration
		//  - add a "bread crumbs" back button
		//  - open entry tab on ctrl-click or "Got to source" button click
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (Config.editor().tooltip.interactable.value()) {
					EditorTooltip.this.dragStart = e.getButton() == MouseEvent.BUTTON1
							? new Point(e.getX(), e.getY())
							: null;

					e.consume();
				} else {
					EditorTooltip.this.close();
				}
			}
		});

		this.addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				final Point dragStart = EditorTooltip.this.dragStart;
				if (dragStart != null) {
					final Point location = EditorTooltip.this.getLocation();
					location.translate(e.getX() - dragStart.x, e.getY() - dragStart.y);
					EditorTooltip.this.setLocation(location);
				}
			}
		});
	}

	/**
	 * Opens this tooltip and populates it with information about the passed {@code target}.
	 *
	 * @param target the entry whose information will be displayed
	 */
	public void open(Entry<?> target) {
		this.content.removeAll();

		@Nullable
		final MouseAdapter stopInteraction = Config.editor().tooltip.interactable.value() ? null : new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				EditorTooltip.this.close();
				e.consume();
			}
		};

		if (this.declarationSnippet != null) {
			this.declarationSnippet.classHandler.removeListener();
			this.declarationSnippet = null;
		}

		this.addRow(new JLabel("From: "), new JLabel(this.getParentName(target).orElse("un-packaged class")));

		// TODO add param javadocs for methods (and component javadocs for records)
		final String javadoc = this.gui.getController().getProject().getRemapper().getMapping(target).javadoc();
		if (javadoc != null) {
			this.add(new JSeparator());

			final JTextArea javadocComponent = new JTextArea(javadoc);
			javadocComponent.setLineWrap(true);
			javadocComponent.setWrapStyleWord(true);
			javadocComponent.setForeground(Config.getCurrentSyntaxPaneColors().comment.value());
			javadocComponent.setFont(Config.currentFonts().editor.value().deriveFont(Font.ITALIC));
			javadocComponent.setBackground(new Color(0, 0, 0, 0));
			javadocComponent.setCaretColor(new Color(0, 0, 0, 0));
			javadocComponent.getCaret().setSelectionVisible(true);
			if (stopInteraction != null) {
				javadocComponent.addMouseListener(stopInteraction);
			}

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

				if (stopInteraction != null) {
					this.declarationSnippet.editor.addMouseListener(stopInteraction);
				}

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
							builder.insert(0, parentPackage.replace('/', '.'));
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
