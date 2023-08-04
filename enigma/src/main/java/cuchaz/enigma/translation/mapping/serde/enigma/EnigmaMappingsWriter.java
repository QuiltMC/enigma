package cuchaz.enigma.translation.mapping.serde.enigma;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.MappingTranslator;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.mapping.VoidEntryResolver;
import cuchaz.enigma.translation.mapping.serde.LfPrintWriter;
import cuchaz.enigma.translation.mapping.serde.MappingFileNameFormat;
import cuchaz.enigma.translation.mapping.serde.MappingHelper;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.serde.MappingsWriter;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.I18n;
import org.tinylog.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

public enum EnigmaMappingsWriter implements MappingsWriter {
	FILE {
		@Override
		public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progress, MappingSaveParameters saveParameters) {
			Collection<ClassEntry> classes = mappings.getRootNodes()
					.filter(entry -> entry.getEntry() instanceof ClassEntry)
					.map(entry -> (ClassEntry) entry.getEntry())
					.toList();

			progress.init(classes.size(), I18n.translate("progress.mappings.enigma_file.writing"));

			int steps = 0;
			try (PrintWriter writer = new LfPrintWriter(Files.newBufferedWriter(path))) {
				for (ClassEntry classEntry : classes) {
					progress.step(steps++, classEntry.getFullName());
					this.writeRoot(writer, mappings, classEntry);
				}
			} catch (IOException e) {
				Logger.error(e, "Error while writing mappings to file {}", path);
			}
		}
	},
	DIRECTORY {
		@Override
		public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progress, MappingSaveParameters saveParameters) {
			Collection<ClassEntry> changedClasses = delta.getChangedRoots()
					.filter(ClassEntry.class::isInstance)
					.map(ClassEntry.class::cast)
					.toList();

			this.applyDeletions(path, changedClasses, mappings, delta.getBaseMappings(), saveParameters.fileNameFormat());

			changedClasses = changedClasses.stream().filter(entry -> !this.isClassEmpty(mappings, entry)).toList();

			progress.init(changedClasses.size(), I18n.translate("progress.mappings.enigma_directory.writing"));

			AtomicInteger steps = new AtomicInteger();

			Translator translator = new MappingTranslator(mappings, VoidEntryResolver.INSTANCE);
			changedClasses.parallelStream().forEach(classEntry -> {
				progress.step(steps.getAndIncrement(), classEntry.getFullName());

				try {
					ClassEntry fileEntry = classEntry;
					if (saveParameters.fileNameFormat() == MappingFileNameFormat.BY_DEOBF) {
						fileEntry = translator.translate(fileEntry);
					}

					Path classPath = this.resolve(path, fileEntry);
					Files.createDirectories(classPath.getParent());
					Files.deleteIfExists(classPath);

					try (PrintWriter writer = new LfPrintWriter(Files.newBufferedWriter(classPath))) {
						this.writeRoot(writer, mappings, classEntry);
					}
				} catch (Exception e) {
					Logger.error(e, "Failed to write class '{}'", classEntry.getFullName());
				}
			});
		}

		private void applyDeletions(Path root, Collection<ClassEntry> changedClasses, EntryTree<EntryMapping> mappings, EntryTree<EntryMapping> oldMappings, MappingFileNameFormat fileNameFormat) {
			Translator oldMappingTranslator = new MappingTranslator(oldMappings, VoidEntryResolver.INSTANCE);

			Stream<ClassEntry> deletedClassStream = changedClasses.stream()
					.filter(e -> !Objects.equals(oldMappings.get(e), mappings.get(e)));

			if (fileNameFormat == MappingFileNameFormat.BY_DEOBF) {
				deletedClassStream = deletedClassStream.map(oldMappingTranslator::translate);
			}

			Collection<ClassEntry> deletedClasses = deletedClassStream.toList();

			for (ClassEntry classEntry : deletedClasses) {
				try {
					Files.deleteIfExists(this.resolve(root, classEntry));
				} catch (IOException e) {
					Logger.error(e, "Failed to delete deleted class '{}'", classEntry);
				}
			}

			for (ClassEntry classEntry : deletedClasses) {
				String packageName = classEntry.getPackageName();
				if (packageName != null) {
					Path packagePath = Paths.get(packageName);
					try {
						this.deleteDeadPackages(root, packagePath);
					} catch (IOException e) {
						Logger.error(e, "Failed to delete dead package '{}'", packageName);
					}
				}
			}
		}

		private void deleteDeadPackages(Path root, Path packagePath) throws IOException {
			for (int i = packagePath.getNameCount() - 1; i >= 0; i--) {
				Path subPath = packagePath.subpath(0, i + 1);
				Path packagePart = root.resolve(subPath.toString());
				if (this.isEmpty(packagePart)) {
					Files.deleteIfExists(packagePart);
				}
			}
		}

		private boolean isEmpty(Path path) {
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
				return !stream.iterator().hasNext();
			} catch (IOException e) {
				return false;
			}
		}

		private Path resolve(Path root, ClassEntry classEntry) {
			return root.resolve(classEntry.getFullName() + ".mapping");
		}
	},
	ZIP {
		@Override
		public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path zip, ProgressListener progress, MappingSaveParameters saveParameters) {
			try (FileSystem fs = FileSystems.newFileSystem(new URI("jar:file", null, zip.toUri().getPath(), ""), Collections.singletonMap("create", "true"))) {
				DIRECTORY.write(mappings, delta, fs.getPath("/"), progress, saveParameters);
			} catch (IOException e) {
				Logger.error(e, "Failed to write mappings to zip file '{}'", zip);
			} catch (URISyntaxException e) {
				throw new RuntimeException("Unexpected error creating URI for " + zip, e);
			}
		}
	};

	protected void writeRoot(PrintWriter writer, EntryTree<EntryMapping> mappings, ClassEntry classEntry) {
		Collection<Entry<?>> children = this.groupChildren(mappings.getChildren(classEntry));

		EntryMapping classEntryMapping = mappings.get(classEntry);

		if (classEntryMapping == null) {
			classEntryMapping = EntryMapping.DEFAULT;
		}

		writer.println(this.writeClass(classEntry, classEntryMapping).trim());
		if (classEntryMapping.javadoc() != null) {
			this.writeDocs(writer, classEntryMapping, 0);
		}

		for (Entry<?> child : children) {
			this.writeEntry(writer, mappings, child, 1);
		}
	}

	private void writeDocs(PrintWriter writer, EntryMapping mapping, int depth) {
		String jd = mapping.javadoc();
		if (jd != null) {
			for (String line : jd.split("\\R")) {
				writer.println(this.indent(EnigmaFormat.COMMENT + " " + MappingHelper.escape(line), depth + 1));
			}
		}
	}

	protected void writeEntry(PrintWriter writer, EntryTree<EntryMapping> mappings, Entry<?> entry, int depth) {
		EntryTreeNode<EntryMapping> node = mappings.findNode(entry);
		if (node == null) {
			return;
		}

		EntryMapping mapping = node.getValue();

		if (mapping == null) {
			mapping = EntryMapping.DEFAULT;
		}

		String line = null;
		if (entry instanceof ClassEntry classEntry) {
			line = this.writeClass(classEntry, mapping);
		} else if (entry instanceof MethodEntry methodEntry) {
			line = this.writeMethod(methodEntry, mapping);
		} else if (entry instanceof FieldEntry fieldEntry) {
			line = this.writeField(fieldEntry, mapping);
		} else if (entry instanceof LocalVariableEntry varEntry && mapping.targetName() != null) {
			line = this.writeArgument(varEntry, mapping);
		}

		if (line != null) {
			writer.println(this.indent(line, depth));
		}

		if (mapping.javadoc() != null) {
			this.writeDocs(writer, mapping, depth);
		}

		Collection<Entry<?>> children = this.groupChildren(node.getChildren());
		for (Entry<?> child : children) {
			this.writeEntry(writer, mappings, child, depth + 1);
		}
	}

	private Collection<Entry<?>> groupChildren(Collection<Entry<?>> children) {
		Collection<Entry<?>> result = new ArrayList<>(children.size());

		children.stream().filter(FieldEntry.class::isInstance)
				.map(e -> (FieldEntry) e)
				.sorted()
				.forEach(result::add);

		children.stream().filter(MethodEntry.class::isInstance)
				.map(e -> (MethodEntry) e)
				.sorted()
				.forEach(result::add);

		children.stream().filter(LocalVariableEntry.class::isInstance)
				.map(e -> (LocalVariableEntry) e)
				.sorted()
				.forEach(result::add);

		children.stream().filter(ClassEntry.class::isInstance)
				.map(e -> (ClassEntry) e)
				.sorted()
				.forEach(result::add);

		return result;
	}

	protected String writeClass(ClassEntry entry, @Nonnull EntryMapping mapping) {
		StringBuilder builder = new StringBuilder(EnigmaFormat.CLASS + " ");
		builder.append(entry.getName()).append(' ');
		this.writeMapping(builder, mapping);

		return builder.toString();
	}

	protected String writeMethod(MethodEntry entry, @Nonnull EntryMapping mapping) {
		StringBuilder builder = new StringBuilder(EnigmaFormat.METHOD + " ");
		builder.append(entry.getName()).append(' ');
		this.writeMapping(builder, mapping);

		builder.append(entry.getDesc().toString());

		return builder.toString();
	}

	protected String writeField(FieldEntry entry, @Nonnull EntryMapping mapping) {
		StringBuilder builder = new StringBuilder(EnigmaFormat.FIELD + " ");
		builder.append(entry.getName()).append(' ');
		this.writeMapping(builder, mapping);

		builder.append(entry.getDesc().toString());

		return builder.toString();
	}

	protected String writeArgument(LocalVariableEntry entry, @Nonnull EntryMapping mapping) {
		return EnigmaFormat.PARAMETER + " " + entry.getIndex() + ' ' + mapping.targetName();
	}

	private void writeMapping(StringBuilder builder, EntryMapping mapping) {
		if (mapping.targetName() != null) {
			builder.append(mapping.targetName()).append(' ');
		}
	}

	private String indent(String line, int depth) {
		return "\t".repeat(Math.max(0, depth))
				+ line.trim();
	}

	protected boolean isClassEmpty(EntryTree<EntryMapping> mappings, ClassEntry classEntry) {
		Collection<Entry<?>> children = this.groupChildren(mappings.getChildren(classEntry));

		EntryMapping classEntryMapping = mappings.get(classEntry);
		return children.isEmpty() && (classEntryMapping == null || this.isMappingEmpty(classEntryMapping));
	}

	private boolean isMappingEmpty(EntryMapping mapping) {
		return mapping.targetName() == null && mapping.javadoc() == null;
	}
}
