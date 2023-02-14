package cuchaz.enigma.gui.docker.component;

import cuchaz.enigma.gui.config.UiConfig;
import cuchaz.enigma.gui.docker.Docker;

import javax.swing.JComponent;
import javax.swing.JToggleButton;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;

public class DockerButton extends JToggleButton implements Draggable {
	private final Docker docker;

	private JComponent initialParent;
	private Object constraints;
	private boolean cancelEvents;
	private boolean mousePressed;
	private String text;
	private Docker.Side side;

	public DockerButton(Docker docker, String text, Docker.Side side) {
		super("");
		this.docker = docker;
		this.text = text;
		this.side = side;

		this.addMouseListener(this.getMouseListener());
		this.addMouseMotionListener(this.getMouseMotionListener());
	}

	public void setSide(Docker.Side side) {
		this.side = side;
		this.repaint();
	}

	public Docker getDocker() {
		return this.docker;
	}

	@Override
	public boolean mousePressed() {
		return this.mousePressed;
	}

	@Override
	public void setMousePressed(boolean mousePressed) {
		this.mousePressed = mousePressed;
	}

	@Override
	public boolean cancelEvents() {
		return this.cancelEvents;
	}

	@Override
	public void setCancelEvents(boolean cancelEvents) {
		this.cancelEvents = cancelEvents;
	}

	@Override
	public JComponent getInitialParent() {
		return this.initialParent;
	}

	@Override
	public void setInitialParent(JComponent parent) {
		this.initialParent = parent;
	}

	@Override
	public Object getConstraints() {
		return this.constraints;
	}

	@Override
	public void setConstraints(Object constraints) {
		this.constraints = constraints;
	}

	@Override
	public JComponent get() {
		return this;
	}

	@Override
	public int getDragDelay() {
		return 500;
	}

	@Override
	public boolean drop(MouseEvent e) {
		return DockerSelector.Util.dropButton(this, e);
	}

	@Override
	public void broadcastMouseEvent(MouseEvent e) {
		DockerSelector.Util.receiveMouseEvent(e);
	}

	@Override
	public void setText(String text) {
		this.text = text;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		Graphics2D g2d = (Graphics2D) g;

		// rotate
		g2d.rotate(-(this.side == Docker.Side.RIGHT ? 3 : 1) * (Math.PI * 0.5));

		// setup text
		Font font = UiConfig.getDefault2Font();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setFont(font);

		// position
		int textSize = (int) font.createGlyphVector(g2d.getFontRenderContext(), text).getVisualBounds().getWidth() + 20;
		this.setPreferredSize(new Dimension(this.getPreferredSize().width, textSize));
		int x = this.side == Docker.Side.RIGHT ? 10 : -textSize + 10;
		int y = this.side == Docker.Side.RIGHT ? -10 : 20;

		g2d.drawString(this.text, x, y);
		this.setSize(this.getPreferredSize());
	}
}
