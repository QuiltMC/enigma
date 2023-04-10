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
