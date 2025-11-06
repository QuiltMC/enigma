package org.quiltmc.enigma.gui;

public record ReadableToken(int line, int startColumn, int endColumn) {

	@Override
	public String toString() {
		return "line " + this.line + " columns " + this.startColumn + "-" + this.endColumn;
	}
}
