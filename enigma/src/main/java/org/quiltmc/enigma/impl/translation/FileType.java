package org.quiltmc.enigma.impl.translation;

import java.util.List;

/**
 * A file type. It can be either a single file with an extension, or a directory.
 *
 * <p>If a file type has multiple extensions, the default for saving will be the first one.
 */
public interface FileType {
	FileType ENIGMA_MAPPING = new File("mapping", "mappings");
	FileType ENIGMA_DIRECTORY = new Directory((File) ENIGMA_MAPPING);
	FileType ENIGMA_ZIP = new File("zip");
	FileType PROGUARD = new File("txt");
	FileType SRG = new File("tsrg");
	FileType TINY_V2 = new File("tiny");

	/**
	 * Gets all possible extensions for this type of mapping file.
	 * If {@link #isDirectory()} is {@code true}, this will return the types of mapping allowed inside the directory.
	 * @return the file extension options
	 */
	List<String> getExtensions();

	/**
	 * {@return whether this file type is a directory}
	 */
	boolean isDirectory();

	record Directory(File file) implements FileType {
		@Override
		public List<String> getExtensions() {
			return this.file.getExtensions();
		}

		public boolean isDirectory() {
			return true;
		}
	}

	record File(List<String> extensions) implements FileType {
		public File(String... extensions) {
			this(List.of(extensions));
		}

		@Override
		public List<String> getExtensions() {
			return this.extensions;
		}

		public boolean isDirectory() {
			return false;
		}
	}
}
