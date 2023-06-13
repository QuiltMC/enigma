package cuchaz.enigma.inputs.enums;

// this enum does not have its `values` or `valueOf` methods obfuscated.
// (see proguard-enums-test.conf)
public enum Color {
	RED(0xff0000),
	YELLOW(0xffff00),
	GREEN(0x00ff00),
	CYAN(0x00ffff),
	BLUE(0x0000ff),
	MAGENTA(0xff00ff);

	public final int rgb;

	Color(int rgb) {
		this.rgb = rgb;
	}

	public int getColor() {
		return this.rgb;
	}
}
