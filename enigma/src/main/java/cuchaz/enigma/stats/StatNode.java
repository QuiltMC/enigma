package cuchaz.enigma.stats;

import cuchaz.enigma.translation.representation.entry.ClassEntry;

public record StatNode(String packageName, ClassEntry obfEntry) {
	public StatNode(String packageName) {
		this(packageName, null);
	}

	public StatNode(ClassEntry obfEntry) {
		this(null, obfEntry);
	}

	public boolean isPackage() {
		return this.obfEntry == null;
	}

	public String getPackageName() {
		return this.packageName;
	}

	public ClassEntry getObfEntry() {
		return this.obfEntry;
	}
}
