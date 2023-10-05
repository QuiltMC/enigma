package org.quiltmc.enigma.api.translation.mapping.serde.recaf;

import com.google.common.collect.Lists;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.MappingDelta;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingSaveParameters;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingsWriter;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTreeNode;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.tinylog.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class RecafMappingsWriter implements MappingsWriter {
	public static final RecafMappingsWriter INSTANCE = new RecafMappingsWriter();

	@Override
	public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progress, MappingSaveParameters saveParameters) {
		try {
			Files.deleteIfExists(path);
			Files.createFile(path);
		} catch (IOException e) {
			Logger.error(e, "Failed to create file {}", path);
		}

		try (BufferedWriter writer = Files.newBufferedWriter(path)) {
			Lists.newArrayList(mappings)
					.stream()
					.map(EntryTreeNode::getEntry)
					.forEach(entry -> this.writeEntry(writer, mappings, entry));
		} catch (IOException e) {
			Logger.error(e, "Failed to write to file {}", path);
		}
	}

	private void writeEntry(Writer writer, EntryTree<EntryMapping> mappings, Entry<?> entry) {
		EntryTreeNode<EntryMapping> node = mappings.findNode(entry);
		if (node == null) {
			return;
		}

		EntryMapping mapping = mappings.get(entry);

		try {
			if (mapping != null && mapping.targetName() != null) {
				if (entry instanceof ClassEntry classEntry) {
					writer.write(classEntry.getFullName());
					writer.write(" ");
					writer.write(mapping.targetName());
				} else if (entry instanceof FieldEntry fieldEntry) {
					writer.write(fieldEntry.getFullName());
					writer.write(" ");
					writer.write(fieldEntry.getDesc().toString());
					writer.write(" ");
					writer.write(mapping.targetName());
				} else if (entry instanceof MethodEntry methodEntry) {
					writer.write(methodEntry.getFullName());
					writer.write(methodEntry.getDesc().toString());
					writer.write(" ");
					writer.write(mapping.targetName());
				}

				writer.write("\n");
			}
		} catch (IOException e) {
			Logger.error(e, "Failed to write to file");
		}

		node.getChildren().forEach(child -> this.writeEntry(writer, mappings, child));
	}
}
