package org.quiltmc.enigma.gui.util;

import org.quiltmc.enigma.api.translation.mapping.serde.MappingFormat;
import org.quiltmc.enigma.util.I18n;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

public final class ExtensionFileFilter extends FileFilter {
	private final String formatName;
	private final List<String> extensions;

	/**
	 * Constructs an {@code ExtensionFileFilter}.
	 *
	 * @param formatName the human-readable name of the file format
	 * @param extensions the file extensions with no leading dots
	 */
	public ExtensionFileFilter(String formatName, List<String> extensions) {
		this.formatName = formatName;
		this.extensions = extensions.stream().peek(s -> {
			if (s.contains(".")) {
				throw new IllegalArgumentException("extensions cannot contain dots!");
			}
		}).map(s -> "." + s).toList();
	}

	public List<String> getExtensions() {
		return this.extensions;
	}

	@Override
	public boolean accept(File f) {
		// Always accept directories so the user can see them.
		if (f.isDirectory()) {
			return true;
		}

		for (String extension : this.extensions) {
			if (f.getName().endsWith(extension)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public String getDescription() {
		var joiner = new StringJoiner(", ");

		for (String extension : this.extensions) {
			joiner.add("*" + extension);
		}

		return I18n.translateFormatted("menu.file.mappings.file_filter", this.formatName, joiner.toString());
	}

	/**
	 * Sets up a file chooser with a mapping format. This method resets the choosable filters,
	 * and adds and selects a new filter based on the provided mapping format.
	 *
	 * @param fileChooser the file chooser to set up
	 * @param formats the mapping formats to use. if empty, defaults to {@link MappingFormat#ENIGMA_DIRECTORY}
	 */
	public static void setupFileChooser(JFileChooser fileChooser, MappingFormat... formats) {
		if (formats.length == 0) {
			formats = new MappingFormat[]{MappingFormat.ENIGMA_DIRECTORY};
		}

		// Remove previous custom filters.
		fileChooser.resetChoosableFileFilters();

		for (MappingFormat format : formats) {
			if (format.getFileType().isDirectory()) {
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			} else {
				fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				String formatName = I18n.translate("mapping_format." + format.name().toLowerCase());
				var filter = new ExtensionFileFilter(formatName, format.getFileType().getExtensions());
				// Add our new filter to the list...
				fileChooser.addChoosableFileFilter(filter);
				// ...and choose it as the default.
				fileChooser.setFileFilter(filter);
			}
		}

		if (formats.length > 1) {
			List<String> extensions = Arrays.stream(formats).flatMap(format -> format.getFileType().getExtensions().stream()).distinct().toList();
			var filter = new ExtensionFileFilter(I18n.translate("mapping_format.all_formats"), extensions);
			fileChooser.addChoosableFileFilter(filter);
			fileChooser.setFileFilter(filter);
		}
	}

	/**
	 * Fixes a missing file extension in a save file path when the selected filter
	 * is an {@code ExtensionFileFilter}.
	 *
	 * @param fileChooser the file chooser to check
	 * @return the fixed path
	 */
	public static Path getSavePath(JFileChooser fileChooser) {
		Path savePath = fileChooser.getSelectedFile().toPath();

		if (fileChooser.getFileFilter() instanceof ExtensionFileFilter extensionFilter) {
			// Check that the file name ends with the extension.
			String fileName = savePath.getFileName().toString();
			boolean hasExtension = false;

			for (String extension : extensionFilter.getExtensions()) {
				if (fileName.endsWith(extension)) {
					hasExtension = true;
					break;
				}
			}

			if (!hasExtension) {
				String defaultExtension = extensionFilter.getExtensions().get(0);
				// If not, add the extension.
				savePath = savePath.resolveSibling(fileName + defaultExtension);
				// Store the adjusted file, so that it shows up properly
				// the next time this dialog is used.
				fileChooser.setSelectedFile(savePath.toFile());
			}
		}

		return savePath;
	}
}
