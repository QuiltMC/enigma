package cuchaz.enigma.inputs.translation;

import java.util.List;
import java.util.Map;

public class I_Generics {
	public List<Integer> f1;
	public List<A_Type> f2;
	public Map<A_Type, A_Type> f3;
	public B_Generic<Integer> f5;
	public B_Generic<A_Type> f6;

	public class A_Type {
	}

	public class B_Generic<T> {
		public T f4;

		public T m1() {
			return null;
		}
	}
}
