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

package cuchaz.enigma.gui;

public class ReadableToken {
	public final int line;
	public final int startColumn;
	public final int endColumn;

	public ReadableToken(int line, int startColumn, int endColumn) {
		this.line = line;
		this.startColumn = startColumn;
		this.endColumn = endColumn;
	}

	@Override
	public String toString() {
		return "line " + this.line + " columns " + this.startColumn + "-" + this.endColumn;
	}
}
