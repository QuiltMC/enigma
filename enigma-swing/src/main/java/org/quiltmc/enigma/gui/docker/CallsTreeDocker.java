package org.quiltmc.enigma.gui.docker;

import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.analysis.tree.ReferenceTreeNode;
import org.quiltmc.enigma.api.source.Token;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.renderer.CallsTreeCellRenderer;
import org.quiltmc.enigma.gui.renderer.TokenListCellRenderer;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.gui.util.ScaleUtil;
import org.quiltmc.enigma.gui.util.SingleTreeSelectionModel;

import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Vector;

public class CallsTreeDocker extends Docker {
	private final JTree tree = new JTree();
	private final JList<Token> tokens = new JList<>();

	private final JSplitPane contentPane;

	public CallsTreeDocker(Gui gui) {
		super(gui);
		this.tree.setModel(null);
		this.tree.setCellRenderer(new CallsTreeCellRenderer(gui));
		this.tree.setSelectionModel(new SingleTreeSelectionModel());
		this.tree.setShowsRootHandles(true);
		this.tree.addMouseListener(GuiUtil.onMouseClick(this::onTreeClicked));

		this.tokens.setCellRenderer(new TokenListCellRenderer(gui.getController()));
		this.tokens.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.tokens.setLayoutOrientation(JList.VERTICAL);
		this.tokens.addMouseListener(GuiUtil.onMouseClick(this::onTokenClicked));
		this.tokens.setPreferredSize(ScaleUtil.getDimension(0, 200));
		this.tokens.setMinimumSize(ScaleUtil.getDimension(0, 200));

		this.contentPane = new JSplitPane(
				JSplitPane.VERTICAL_SPLIT,
				true,
				new JScrollPane(this.tree),
				new JScrollPane(this.tokens)
		);

		this.contentPane.setResizeWeight(1); // let the top side take all the slack
		this.contentPane.resetToPreferredSizes();
		this.add(this.contentPane, BorderLayout.CENTER);
	}

	public void showCalls(Entry<?> entry, boolean recurse) {
		TreeNode node = null;

		if (entry instanceof ClassEntry classEntry) {
			node = this.gui.getController().getClassReferences(classEntry);
		} else if (entry instanceof FieldEntry fieldEntry) {
			node = this.gui.getController().getFieldReferences(fieldEntry);
		} else if (entry instanceof MethodEntry methodEntry) {
			node = this.gui.getController().getMethodReferences(methodEntry, recurse);
		}

		this.tree.setModel(new DefaultTreeModel(node));

		this.setVisible(true);
	}

	public void showTokens(Collection<Token> tokens) {
		this.tokens.setListData(new Vector<>(tokens));
		this.tokens.setSelectedIndex(0);
		this.updateContentSize();
	}

	public void clearTokens() {
		this.tokens.setListData(new Vector<>());
		this.updateContentSize();
	}

	public void updateContentSize() {
		int tokenCount = this.tokens.getModel().getSize();
		if (tokenCount == 0) {
			this.contentPane.setResizeWeight(1);
			this.contentPane.resetToPreferredSizes();
		} else {
			this.contentPane.setResizeWeight(0.75);
			this.contentPane.resetToPreferredSizes();
		}
	}

	@SuppressWarnings("unchecked")
	private void onTreeClicked(MouseEvent event) {
		if (event.getClickCount() >= 2 && event.getButton() == MouseEvent.BUTTON1) {
			// get the selected node
			TreePath path = this.tree.getSelectionPath();

			if (path == null) {
				return;
			}

			Object node = path.getLastPathComponent();

			if (node instanceof ReferenceTreeNode<?, ?> referenceNode) {
				if (referenceNode.getReference() != null) {
					this.gui.getController().navigateTo((EntryReference<Entry<?>, Entry<?>>) referenceNode.getReference());
				} else {
					this.gui.getController().navigateTo(referenceNode.getEntry());
				}
			}
		}
	}

	private void onTokenClicked(MouseEvent event) {
		if (event.getClickCount() == 2) {
			Token selected = this.tokens.getSelectedValue();
			if (selected != null) {
				this.gui.openClass(this.gui.getController().getTokenHandle().getRef()).navigateToToken(selected);
			}
		}
	}

	@Override
	public Location getPreferredButtonLocation() {
		return new Location(Side.RIGHT, VerticalLocation.TOP);
	}

	@Override
	public String getId() {
		return "calls";
	}
}
