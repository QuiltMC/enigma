package org.quiltmc.enigma.api.analysis.tree;

import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import javax.swing.tree.DefaultMutableTreeNode;

public abstract class AbstractMethodTreeNode extends DefaultMutableTreeNode {
	protected final Translator translator;
	protected final MethodEntry entry;

	protected AbstractMethodTreeNode(Translator translator, MethodEntry entry) {
		this.translator = translator;
		this.entry = entry;
	}

	/**
	 * Returns the method entry represented by this tree node.
	 */
	public MethodEntry getMethodEntry() {
		return this.entry;
	}
}
