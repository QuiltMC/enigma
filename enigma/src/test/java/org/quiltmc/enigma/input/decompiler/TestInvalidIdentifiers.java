package org.quiltmc.enigma.input.decompiler;

import java.util.Random;

// a
public class TestInvalidIdentifiers {
	// byte
	byte f1;
	// boolean
	boolean f2;
	// int
	int f3;
	// float
	float f4;
	// double
	double f5;
	// short
	short f6;
	// char
	char f7;
	// long
	long f8;
	// final
	int f9;
	// break
	int f10;
	// for
	int f11;
	// static
	int f12;
	// super
	int f13;
	// private
	int f14;
	// import
	int f15;
	// synchronized
	int f16;
	// $
	int f17;

	// new()V
	public void invokeConstructor() {
		System.out.println(new TestInvalidIdentifiers());
	}

	// assert()V
	public void assertStatement() {
		Random r = new Random();
		assert r.nextBoolean();
	}

	// try()V
	public void tryCatchThrownException() {
		try {
			throw new RuntimeException("meow :3");
		} catch (RuntimeException e) {
			invokeConstructor();
		}
	}

	// switch()V
	public void switchCase() {
		int k = new Random().nextInt(3);
		switch (k) {
			case 0 -> System.out.println(1);
			case 1 -> {
				int j = k << (3 * new Random().nextInt(3));
				System.out.println(j);
			}
			case 2 -> System.out.println(-1);
		}
	}

	// void()V
	public void noop() {
	}

	// throws()V
	public void throwException() throws IllegalStateException {
		String s = "not meow :(";
		throw new IllegalStateException(s);
	}

	// class()V
	public void innerClass() {
		// interface
		interface Interface {
			// throws()V
			void meow();

			// enum()I
			default int purr() {
				return 1;
			}
		}

		// abstract
		abstract class Abstract implements Interface {
			// transient
			private char m;
			// volatile
			private boolean b = false;
			// false
			private boolean c = true;

			Abstract() {
				this.m = 'o';
			}

			public void meow() {
				System.out.println("meow");
			}
		}

		Abstract c = new Abstract() {};
		c.meow();
		System.out.println(c.m);
	}

	// while()V
	public static void loop() {
		int i = 0;
		boolean b = true;
		do {
			for (int j = 0; j < 5; j++) {
				System.out.println(j);
			}

			if (++i > 3) {
				break;
			}

			for (int j = 5; j > 0; j--) {
				System.out.println(j);
			}
		} while (b);
	}

	// native()I
	private static int n() {
		return -1;
	}

	enum Enum {
		Foo;

		final Enum e;

		Enum() {
			this.e = this;
		}
	}
}
