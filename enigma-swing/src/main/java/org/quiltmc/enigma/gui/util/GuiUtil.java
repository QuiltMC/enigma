package org.quiltmc.enigma.gui.util;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.api.stats.ProjectStatsResult;
import org.quiltmc.enigma.api.translation.representation.AccessFlags;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.gui.config.theme.ThemeUtil;
import org.quiltmc.enigma.util.Os;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolTip;
import javax.swing.JTree;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.Timer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

public class GuiUtil {
	public static final Icon CLASS_ICON = loadIcon("class");
	public static final Icon INTERFACE_ICON = loadIcon("interface");
	public static final Icon ENUM_ICON = loadIcon("enum");
	public static final Icon ANNOTATION_ICON = loadIcon("annotation");
	public static final Icon RECORD_ICON = loadIcon("record");
	public static final Icon METHOD_ICON = loadIcon("method");
	public static final Icon FIELD_ICON = loadIcon("field");
	public static final Icon CONSTRUCTOR_ICON = loadIcon("constructor");

	// icons sourced from https://github.com/primer/octicons
	public static final Icon OBFUSCATED_ICON = loadIcon("obfuscated");
	public static final Icon PARTIALLY_DEOBFUSCATED_ICON = loadIcon("partially_deobfuscated");
	public static final Icon DEOBFUSCATED_ICON = loadIcon("deobfuscated");
	public static final Icon PENDING_STATUS_ICON = loadIcon("pending_status");

	public static final Icon CHEVRON_UP_BLACK = loadIcon("chevron-up-black");
	public static final Icon CHEVRON_DOWN_BLACK = loadIcon("chevron-down-black");
	public static final Icon CHEVRON_UP_WHITE = loadIcon("chevron-up-white");
	public static final Icon CHEVRON_DOWN_WHITE = loadIcon("chevron-down-white");

	public static void openUrl(String url) {
		try {
			if (Objects.requireNonNull(Os.getOs()) == Os.LINUX) {
				new ProcessBuilder("/usr/bin/env", "xdg-open", url).start();
			} else {
				if (Desktop.isDesktopSupported()) {
					Desktop desktop = Desktop.getDesktop();
					desktop.browse(new URI(url));
				}
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		} catch (URISyntaxException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	public static JLabel unboldLabel(JLabel label) {
		Font font = label.getFont();
		label.setFont(font.deriveFont(font.getStyle() & ~Font.BOLD));
		return label;
	}

	/**
	 * Puts the provided {@code text} in the system clipboard.
	 */
	public static void copyToClipboard(String text) {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
	}

	public static String getClipboard() {
		try {
			return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
		} catch (UnsupportedFlavorException | IOException e) {
			return "";
		}
	}

	public static void showPopup(JComponent component, String text, int x, int y) {
		// from https://stackoverflow.com/questions/39955015/java-swing-show-tooltip-as-a-message-dialog
		JToolTip tooltip = new JToolTip();
		tooltip.setTipText(text);
		Popup p = PopupFactory.getSharedInstance().getPopup(component, tooltip, x + 10, y);
		p.show();
		Timer t = new Timer(1000, e -> p.hide());
		t.setRepeats(false);
		t.start();
	}

	public static JLabel createLink(String text, Runnable action) {
		JLabel link = new JLabel(text);
		link.setForeground(Color.BLUE.darker());
		link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		@SuppressWarnings("unchecked")
		Map<TextAttribute, Object> attributes = (Map<TextAttribute, Object>) link.getFont().getAttributes();
		attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		link.setFont(link.getFont().deriveFont(attributes));
		link.addMouseListener(onMousePress(e -> action.run()));
		return link;
	}

	public static Icon loadIcon(String name) {
		String path = "icons/" + name + ".svg";

		// Do an eager check for a missing icon since FlatSVGIcon does it later at render time
		if (GuiUtil.class.getResource('/' + path) == null) {
			throw new NoSuchElementException("Missing icon: '" + name + "' at " + path);
		}

		// Note: the width and height are scaled automatically because the FlatLaf UI scale
		// is set in LookAndFeel.setGlobalLAF()
		return new FlatSVGIcon(path, 16, 16, GuiUtil.class.getClassLoader());
	}

	public static Icon getClassIcon(Gui gui, ClassEntry entry) {
		EntryIndex entryIndex = gui.getController().getProject().getJarIndex().getIndex(EntryIndex.class);
		AccessFlags access = entryIndex.getClassAccess(entry);

		if (access != null) {
			if (access.isAnnotation()) {
				return ANNOTATION_ICON;
			} else if (access.isInterface()) {
				return INTERFACE_ICON;
			} else if (access.isEnum()) {
				return ENUM_ICON;
			} else if (entryIndex.getDefinition(entry).isRecord()) {
				return RECORD_ICON;
			}
		}

		return CLASS_ICON;
	}

	public static Icon getFolderIcon(DefaultTreeCellRenderer renderer, JTree tree, DefaultMutableTreeNode node) {
		boolean expanded = tree.isExpanded(new TreePath(node.getPath()));
		return expanded ? renderer.getOpenIcon() : renderer.getClosedIcon();
	}

	public static Icon getDeobfuscationIcon(ProjectStatsResult stats, String packageName) {
		if (stats != null && stats.getPackageStats(packageName) != null) {
			double percentage = stats.getPackageStats(packageName).getPercentage();

			if (percentage == 100d) {
				return DEOBFUSCATED_ICON;
			} else if (percentage > 0) {
				return PARTIALLY_DEOBFUSCATED_ICON;
			}
		}

		return OBFUSCATED_ICON;
	}

	public static Icon getDeobfuscationIcon(ProjectStatsResult stats, ClassEntry obfEntry) {
		if (stats != null && stats.getStats().get(obfEntry) != null) {
			double percentage = stats.getStats().get(obfEntry).getPercentage();

			if (percentage == 100d) {
				return DEOBFUSCATED_ICON;
			} else if (percentage > 0) {
				return PARTIALLY_DEOBFUSCATED_ICON;
			}
		}

		return OBFUSCATED_ICON;
	}

	public static Icon getMethodIcon(MethodEntry entry) {
		if (entry.isConstructor()) {
			return CONSTRUCTOR_ICON;
		}

		return METHOD_ICON;
	}

	public static TreePath getPathToRoot(TreeNode node) {
		List<TreeNode> nodes = new ArrayList<>();
		TreeNode n = node;

		do {
			nodes.add(n);
			n = n.getParent();
		} while (n != null);

		Collections.reverse(nodes);
		return new TreePath(nodes.toArray());
	}

	public static MouseListener onMouseClick(Consumer<MouseEvent> op) {
		return new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				op.accept(e);
			}
		};
	}

	public static MouseListener onMousePress(Consumer<MouseEvent> op) {
		return new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				op.accept(e);
			}
		};
	}

	public static KeyListener onKeyPress(Consumer<KeyEvent> op) {
		return new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				op.accept(e);
			}
		};
	}

	public static WindowListener onWindowClose(Consumer<WindowEvent> op) {
		return new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				op.accept(e);
			}
		};
	}

	/**
	 * A hack method to set up the Swing glass pane, because of course it doesn't actually display anything
	 * unless you do this. Thanks Swing!
	 */
	public static void setUpGlassPane(JPanel glass) {
		glass.setOpaque(false);
		glass.setVisible(true);
		glass.setLayout(null);
		glass.revalidate();
	}

	public static Icon getUpChevron() {
		return ThemeUtil.isDarkLaf() ? CHEVRON_UP_WHITE : CHEVRON_UP_BLACK;
	}

	public static Icon getDownChevron() {
		return ThemeUtil.isDarkLaf() ? CHEVRON_DOWN_WHITE : CHEVRON_DOWN_BLACK;
	}
}
