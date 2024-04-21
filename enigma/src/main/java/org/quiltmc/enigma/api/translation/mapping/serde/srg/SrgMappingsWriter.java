package org.quiltmc.enigma.api.translation.mapping.serde.srg;

import com.google.common.collect.Lists;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.translation.MappingTranslator;
import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.MappingDelta;
import org.quiltmc.enigma.api.translation.mapping.VoidEntryResolver;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingSaveParameters;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingsWriter;
import org.quiltmc.enigma.impl.translation.mapping.serde.LfPrintWriter;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTreeNode;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.util.I18n;
import org.tinylog.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public enum SrgMappingsWriter implements MappingsWriter {
	INSTANCE;

	@Override
	public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progress, MappingSaveParameters saveParameters) {
		EntryTree<EntryMapping> writtenMappings = MappingsWriter.filterMappings(mappings, saveParameters);

		try {
			Files.deleteIfExists(path);
			Files.createFile(path);
		} catch (IOException e) {
			Logger.error(e, "Failed to create file {}", path);
		}

		List<String> classLines = new ArrayList<>();
		List<String> fieldLines = new ArrayList<>();
		List<String> methodLines = new ArrayList<>();

		List<? extends Entry<?>> rootEntries = Lists.newArrayList(writtenMappings).stream()
				.map(EntryTreeNode::getEntry)
				.toList();
		progress.init(rootEntries.size(), I18n.translate("progress.mappings.srg_file.generating"));

		int steps = 0;
		for (Entry<?> entry : this.sorted(rootEntries)) {
			progress.step(steps++, entry.getName());
			this.writeEntry(classLines, fieldLines, methodLines, writtenMappings, entry);
		}

		progress.init(3, I18n.translate("progress.mappings.srg_file.writing"));
		try (PrintWriter writer = new LfPrintWriter(Files.newBufferedWriter(path))) {
			progress.step(0, I18n.translate("type.classes"));
			classLines.forEach(writer::println);
			progress.step(1, I18n.translate("type.fields"));
			fieldLines.forEach(writer::println);
			progress.step(2, I18n.translate("type.methods"));
			methodLines.forEach(writer::println);
		} catch (IOException e) {
			Logger.error(e, "Failed to write to file {}", path);
		}
	}

	private void writeEntry(List<String> classes, List<String> fields, List<String> methods, EntryTree<EntryMapping> mappings, Entry<?> entry) {
		EntryTreeNode<EntryMapping> node = mappings.findNode(entry);
		if (node == null) {
			return;
		}

		Translator translator = new MappingTranslator(mappings, VoidEntryResolver.INSTANCE);
		if (entry instanceof ClassEntry classEntry) {
			classes.add(this.generateClassLine(classEntry, translator));
		} else if (entry instanceof FieldEntry fieldEntry) {
			fields.add(this.generateFieldLine(fieldEntry, translator));
		} else if (entry instanceof MethodEntry methodEntry) {
			methods.add(this.generateMethodLine(methodEntry, translator));
		}

		for (Entry<?> child : this.sorted(node.getChildren())) {
			this.writeEntry(classes, fields, methods, mappings, child);
		}
	}

	private String generateClassLine(ClassEntry sourceEntry, Translator translator) {
		ClassEntry targetEntry = translator.translate(sourceEntry);
		return "CL: " + sourceEntry.getFullName() + " " + targetEntry.getFullName();
	}

	private String generateMethodLine(MethodEntry sourceEntry, Translator translator) {
		MethodEntry targetEntry = translator.translate(sourceEntry);
		return "MD: " + this.describeMethod(sourceEntry) + " " + this.describeMethod(targetEntry);
	}

	private String describeMethod(MethodEntry entry) {
		return entry.getParent().getFullName() + "/" + entry.getName() + " " + entry.getDesc();
	}

	private String generateFieldLine(FieldEntry sourceEntry, Translator translator) {
		FieldEntry targetEntry = translator.translate(sourceEntry);
		return "FD: " + this.describeField(sourceEntry) + " " + this.describeField(targetEntry);
	}

	private String describeField(FieldEntry entry) {
		return entry.getParent().getFullName() + "/" + entry.getName();
	}

	private Collection<Entry<?>> sorted(Iterable<? extends Entry<?>> iterable) {
		ArrayList<Entry<?>> sorted = Lists.newArrayList(iterable);
		sorted.sort(Comparator.comparing(Entry::getName));
		return sorted;
	}
}
