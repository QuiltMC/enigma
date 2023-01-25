package cuchaz.enigma;

public interface ProgressListener {
	static ProgressListener none() {
		return new ProgressListener() {
			@Override
			public void init(int totalWork, String title) {
				// none
			}

			@Override
			public void step(int numDone, String message) {
				// none
			}
		};
	}

	void init(int totalWork, String title);

	void step(int numDone, String message);
}
