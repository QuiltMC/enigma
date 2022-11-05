/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.analysis;

import cuchaz.enigma.analysis.index.InheritanceIndex;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.List;

public class ClassInheritanceTreeNode extends DefaultMutableTreeNode {
	private final Translator translator;
	private final ClassEntry obfClassEntry;

	public ClassInheritanceTreeNode(Translator translator, String obfClassName) {
		this.translator = translator;
		this.obfClassEntry = new ClassEntry(obfClassName);
	}

	public static ClassInheritanceTreeNode findNode(ClassInheritanceTreeNode node, ClassEntry entry) {
		// is this the node?
		if (node.getObfClassName().equals(entry.getFullName())) {
			return node;
		}

		// recurse
		for (int i = 0; i < node.getChildCount(); i++) {
			ClassInheritanceTreeNode foundNode = findNode((ClassInheritanceTreeNode) node.getChildAt(i), entry);
			if (foundNode != null) {
				return foundNode;
			}
		}
		return null;
	}

	/**
	 * Returns the class entry represented by this tree node.
	 */
	public ClassEntry getClassEntry() {
		return this.obfClassEntry;
	}

	public String getObfClassName() {
		return this.obfClassEntry.getFullName();
	}

	@Override
	public String toString() {
		return translator.translate(obfClassEntry).getFullName();
	}

	public void load(InheritanceIndex ancestries, boolean recurse) {
		// get all the child nodes
		List<ClassInheritanceTreeNode> nodes = new ArrayList<>();
		for (ClassEntry inheritor : ancestries.getChildren(this.obfClassEntry)) {
			nodes.add(new ClassInheritanceTreeNode(translator, inheritor.getFullName()));
		}

		// add them to this node
		nodes.forEach(this::add);

		if (recurse) {
			for (ClassInheritanceTreeNode node : nodes) {
				node.load(ancestries, true);
			}
		}
	}
}
