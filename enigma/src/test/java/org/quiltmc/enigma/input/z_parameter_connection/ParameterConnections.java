package org.quiltmc.enigma.input.z_parameter_connection;

public class ParameterConnections {
	static Object toStringOf(String param) {
		return new Object() {
			@Override
			public String toString() {
				return param;
			}
		};
	}
}
