package cuchaz.enigma.gui.panels.right;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.utils.I18n;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.function.Supplier;

public abstract class MultiplayerOnlyRightPanel extends AbstractRightPanel {
    private final Gui gui;
    private final JLabel offlineLabel;
    private final JPanel offlinePanel;
    private final Supplier<String> offlineTextProvider = () -> I18n.translate("right_panel.multiplayer.offline_text");

    protected MultiplayerOnlyRightPanel(Gui gui) {
        super(gui);
        this.gui = gui;
        this.offlinePanel = new JPanel();
        this.offlineLabel = new JLabel(offlineTextProvider.get());
        this.offlinePanel.add(this.offlineLabel);
    }

    @Override
    public void retranslateUi() {
        super.retranslateUi();
        this.offlineLabel.setText(offlineTextProvider.get());
    }

    @Override
    public JPanel getPanel() {
        if (gui.isOffline()) {
            return this.offlinePanel;
        } else {
            return this.getOnlinePanel();
        }
    }

    public abstract JPanel getOnlinePanel();
}
