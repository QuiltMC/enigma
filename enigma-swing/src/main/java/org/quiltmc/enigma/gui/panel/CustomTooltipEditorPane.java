package org.quiltmc.enigma.gui.panel;

import javax.swing.JEditorPane;
import javax.swing.JToolTip;
import java.util.function.Supplier;

public class CustomTooltipEditorPane extends JEditorPane {
	private final Supplier<JToolTip> toolTipFactory;

	public CustomTooltipEditorPane(Supplier<JToolTip> toolTipFactory) {
		this.toolTipFactory = toolTipFactory;
		this.enableTooltip();
	}

	@Override
	public JToolTip createToolTip() {
		final JToolTip toolTip = this.toolTipFactory.get();
		toolTip.setComponent(this);
		return toolTip;
	}

	public void enableTooltip() {
		// ToolTipManager will only create a tooltip if its test is non-null
		// this also ensures this component is registered with the ToolTipManager
		this.setToolTipText("");
	}

	public void disableToolTip() {
		// if tooltip text is null, ToolTipManager will never create a tooltip
		// this also unregisters this component with the ToolTipManager
		this.setToolTipText(null);
	}
}
