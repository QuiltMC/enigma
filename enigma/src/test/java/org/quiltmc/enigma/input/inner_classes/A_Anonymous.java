package org.quiltmc.enigma.input.inner_classes;

@SuppressWarnings("Convert2Lambda")
public class A_Anonymous {
	public void foo() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				// don't care
			}
		};
		runnable.run();
	}
}
