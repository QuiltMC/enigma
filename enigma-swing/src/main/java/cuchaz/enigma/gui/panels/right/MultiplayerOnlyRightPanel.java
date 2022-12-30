package cuchaz.enigma.gui.panels.right;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.utils.I18n;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.function.Supplier;

public abstract class MultiplayerOnlyRightPanel extends RightPanel {
	private boolean online;
	private final Gui gui;
    private final JLabel offlineLabel;
    private final JPanel offlinePanel;
	private final Supplier<String> offlineTextProvider = () -> I18n.translate("right_panel.multiplayer.offline_text");

    protected MultiplayerOnlyRightPanel(Gui gui) {
        super(gui);
		this.gui = gui;
        this.offlinePanel = new JPanel(new BorderLayout());
        this.offlineLabel = new JLabel(offlineTextProvider.get());
		JPanel offlineTopPanel = new JPanel(new BorderLayout());

		offlineTopPanel.add(this.title, BorderLayout.NORTH);
        offlineTopPanel.add(this.offlineLabel, BorderLayout.SOUTH);
		this.offlinePanel.add(offlineTopPanel, BorderLayout.NORTH);
    }

    @Override
    public void retranslateUi() {
        super.retranslateUi();
        this.offlineLabel.setText(this.offlineTextProvider.get());
    }

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (this.gui.isOffline() == this.online) {
			this.setUp(!gui.isOffline());
		}
	}

    /**
     * sets up the panel for its offline or online state
     * @param online whether to use the offline or online panel
     */
    public void setUp(boolean online) {
        this.removeAll();
		this.online = online;

        if (online) {
            this.addComponents();
        } else {
            this.add(this.offlinePanel);
        }
    }

    protected void addComponents() {
		this.add(this.title, BorderLayout.NORTH);
	}
}
