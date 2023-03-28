package cuchaz.enigma.inputs.innerClasses;

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
