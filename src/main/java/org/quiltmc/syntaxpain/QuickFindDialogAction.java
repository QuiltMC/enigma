package org.quiltmc.syntaxpain;

import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;

public final class QuickFindDialogAction extends DefaultSyntaxAction {
	public QuickFindDialogAction() {
		super("quick-find-dialog");
	}

	@Override
	public void actionPerformed(JTextComponent target, SyntaxDocument document, int dot, ActionEvent event) {
		Data data = Data.get(target);
		data.showFindDialog(target);
	}

	private static class Data {
		private static final String KEY = "enigma-find-data";
		private QuickFindDialog findDialog;

		private Data() {
		}

		public static Data get(JTextComponent target) {
			Object o = target.getDocument().getProperty(KEY);
			if (o instanceof Data) {
				return (Data) o;
			}

			Data data = new Data();
			target.getDocument().putProperty(KEY, data);
			return data;
		}

		public void showFindDialog(JTextComponent target) {
			if (SyntaxpainConfiguration.isQuickFindDialogEnabled()) {
				if (this.findDialog == null) {
					this.findDialog = SyntaxpainConfiguration.getQuickFindDialog(target);
				}

				this.findDialog.showFor(target);
			}
		}
	}
}
