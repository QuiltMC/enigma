package org.quiltmc.enigma.gui.dialog.stats;

import org.quiltmc.enigma.api.stats.ProjectStatsResult;
import org.quiltmc.enigma.api.stats.StatType;
import org.quiltmc.enigma.gui.util.GridBagConstraintsBuilder;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;

public class StatDisplay extends JPanel {

	private final ProjectStatsResult result;
	private final StatType type;

	public StatDisplay(ProjectStatsResult result, StatType type) {
		this.result = result;
		this.type = type;
		this.init();
	}

	private void init() {
		this.setLayout(new GridBagLayout());
		GridBagConstraintsBuilder cb = GridBagConstraintsBuilder.create().insets(2);
		GridBagConstraintsBuilder labels = cb.anchor(GridBagConstraints.EAST);


		JLabel label = new JLabel(type.getName());
		this.add(label, cb.pos(0, 1).weightX(1.0).anchor(GridBagConstraints.WEST).build());
		this.add(new JLabel(Integer.toString(result.getMapped(type))), labels.pos(1, 1).build());
		this.add(new JLabel("/"), labels.pos(2, 1).build());
		this.add(new JLabel(Integer.toString(result.getMappable(type))), labels.pos(3, 1).build());
		this.add(new JLabel(String.format("%.2f%%", result.getPercentage(type))), labels.pos(4, 1).build());
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.setColor(this.type.getColor());
		g.fillRect(0, 0, (int) (this.getWidth() * (this.result.getPercentage(this.type) / 100)), this.getHeight());
	}
}
