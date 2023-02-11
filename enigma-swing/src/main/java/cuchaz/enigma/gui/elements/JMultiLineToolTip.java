package cuchaz.enigma.gui.elements;

import java.awt.Dimension;
import java.awt.Graphics;
import java.io.Serial;

import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.JToolTip;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolTipUI;

/**
 * Implements a multi line tooltip for GUI components
 * Copied from <a href="https://web.archive.org/web/20120104225105/http://www.codeguru.com:80/java/articles/122.shtml">this CodeGuru article</a>
 *
 * @author Zafir Anjum
 */
public class JMultiLineToolTip extends JToolTip {
	@Serial
	private static final long serialVersionUID = 7813662474312183098L;

	public JMultiLineToolTip() {
		this.updateUI();
	}

	@Override
	public void updateUI() {
		this.setUI(MultiLineToolTipUI.createUI(this));
	}

	public void setColumns(int columns) {
		this.columns = columns;
		this.fixedwidth = 0;
	}

	public int getColumns() {
		return this.columns;
	}

	public void setFixedWidth(int width) {
		this.fixedwidth = width;
		this.columns = 0;
	}

	public int getFixedWidth() {
		return this.fixedwidth;
	}

	protected int columns = 0;
	protected int fixedwidth = 0;
}

/**
 * UI for multi line tool tip
 */
class MultiLineToolTipUI extends BasicToolTipUI {
	static final MultiLineToolTipUI sharedInstance = new MultiLineToolTipUI();
	static JToolTip tip;
	protected CellRendererPane rendererPane;

	private static JTextArea textArea;

	public static ComponentUI createUI(JComponent c) {
		return sharedInstance;
	}

	public MultiLineToolTipUI() {
		super();
	}

	@Override
	public void installUI(JComponent c) {
		super.installUI(c);
		tip = (JToolTip) c;
		this.rendererPane = new CellRendererPane();
		c.add(this.rendererPane);
	}

	@Override
	public void uninstallUI(JComponent c) {
		super.uninstallUI(c);

		c.remove(this.rendererPane);
		this.rendererPane = null;
	}

	@Override
	public void paint(Graphics g, JComponent c) {
		Dimension size = c.getSize();
		textArea.setBackground(c.getBackground());
		this.rendererPane.paintComponent(g, textArea, c, 1, 1, size.width - 1, size.height - 1, true);
	}

	@Override
	public Dimension getPreferredSize(JComponent c) {
		String tipText = ((JToolTip) c).getTipText();
		if (tipText == null) return new Dimension(0, 0);
		textArea = new JTextArea(tipText);
		this.rendererPane.removeAll();
		this.rendererPane.add(textArea);
		textArea.setWrapStyleWord(true);
		int width = ((JMultiLineToolTip) c).getFixedWidth();
		int columns = ((JMultiLineToolTip) c).getColumns();

		if (columns > 0) {
			textArea.setColumns(columns);
			textArea.setSize(0, 0);
			textArea.setLineWrap(true);
			textArea.setSize(textArea.getPreferredSize());
		} else if (width > 0) {
			textArea.setLineWrap(true);
			Dimension d = textArea.getPreferredSize();
			d.width = width;
			d.height++;
			textArea.setSize(d);
		} else
			textArea.setLineWrap(false);

		Dimension dim = textArea.getPreferredSize();

		dim.height += 1;
		dim.width += 1;
		return dim;
	}

	@Override
	public Dimension getMinimumSize(JComponent c) {
		return this.getPreferredSize(c);
	}

	@Override
	public Dimension getMaximumSize(JComponent c) {
		return this.getPreferredSize(c);
	}
}
