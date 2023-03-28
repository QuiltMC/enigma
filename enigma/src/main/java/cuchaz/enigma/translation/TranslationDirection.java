package cuchaz.enigma.translation;

public enum TranslationDirection {
	DEOBFUSCATING {
		@Override
		public <T> T choose(T deobfChoice, T obfChoice) {
			if (deobfChoice == null) {
				return obfChoice;
			}
			return deobfChoice;
		}
	},
	OBFUSCATING {
		@Override
		public <T> T choose(T deobfChoice, T obfChoice) {
			if (obfChoice == null) {
				return deobfChoice;
			}
			return obfChoice;
		}
	};

	public abstract <T> T choose(T deobfChoice, T obfChoice);
}
