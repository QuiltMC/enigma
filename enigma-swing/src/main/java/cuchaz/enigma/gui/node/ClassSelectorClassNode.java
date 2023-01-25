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

package cuchaz.enigma.gui.node;

import cuchaz.enigma.translation.representation.entry.ClassEntry;

import javax.swing.tree.DefaultMutableTreeNode;

public class ClassSelectorClassNode extends DefaultMutableTreeNode {
	private final ClassEntry obfEntry;
	private ClassEntry classEntry;

	public ClassSelectorClassNode(ClassEntry obfEntry, ClassEntry classEntry) {
		this.obfEntry = obfEntry;
		this.classEntry = classEntry;
		this.setUserObject(classEntry);
	}

	public ClassEntry getObfEntry() {
		return this.obfEntry;
	}

	public ClassEntry getClassEntry() {
		return this.classEntry;
	}

	@Override
	public String toString() {
		return this.classEntry.getSimpleName();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ClassSelectorClassNode && this.equals((ClassSelectorClassNode) other);
	}

	@Override
	public int hashCode() {
		return 17 + (this.classEntry != null ? this.classEntry.hashCode() : 0);
	}

	@Override
	public Object getUserObject() {
		return this.classEntry;
	}

	@Override
	public void setUserObject(Object userObject) {
		String packageName = "";
		if (this.classEntry.getPackageName() != null)
			packageName = this.classEntry.getPackageName() + "/";
		if (userObject instanceof String)
			this.classEntry = new ClassEntry(packageName + userObject);
		else if (userObject instanceof ClassEntry)
			this.classEntry = (ClassEntry) userObject;
		super.setUserObject(this.classEntry);
	}

	public boolean equals(ClassSelectorClassNode other) {
		return this.classEntry.equals(other.classEntry);
	}
}
