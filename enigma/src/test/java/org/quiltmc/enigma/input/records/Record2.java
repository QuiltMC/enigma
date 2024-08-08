package org.quiltmc.enigma.input.records;

public record Record2(String a) {
	@Override
	public boolean equals(Object obj) {
		return obj == this;
	}
}
