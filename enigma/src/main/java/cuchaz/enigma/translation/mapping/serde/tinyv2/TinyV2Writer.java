package cuchaz.enigma.translation.mapping.serde.tinyv2;

import com.google.common.base.Strings;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.EntryMap;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.serde.LfPrintWriter;
import cuchaz.enigma.translation.mapping.serde.MappingHelper;
import cuchaz.enigma.translation.mapping.serde.MappingsWriter;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.tinylog.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.StreamSupport;

public final class TinyV2Writer implements MappingsWriter {
	private static final String MINOR_VERSION = "0";
	private final String obfHeader;
	private final String deobfHeader;

	public TinyV2Writer(String obfHeader, String deobfHeader) {
		this.obfHeader = obfHeader;
		this.deobfHeader = deobfHeader;
	}

	private static int getEntryKind(Entry<?> e) {
		// field < method < class
		if (e instanceof FieldEntry) {
			return 0;
		} else if (e instanceof MethodEntry) {
			return 1;
		} else if (e instanceof ClassEntry) {
			return 2;
		}

		return -1;
	}

	private static Comparator<EntryTreeNode<EntryMapping>> mappingComparator() {
		return Comparator.<EntryTreeNode<EntryMapping>>comparingInt(n -> getEntryKind(n.getEntry()))
			.thenComparing(EntryTreeNode::getEntry, (o1, o2) -> {
				if (o1 instanceof FieldEntry f1 && o2 instanceof FieldEntry f2) {
					return f1.compareTo(f2);
				} else if (o1 instanceof MethodEntry m1 && o2 instanceof MethodEntry m2) {
					return m1.compareTo(m2);
				} else if (o1 instanceof ClassEntry c1 && o2 instanceof ClassEntry c2) {
					return c1.compareTo(c2);
				} else if (o1 instanceof LocalVariableEntry v1 && o2 instanceof LocalVariableEntry v2) {
					return v1.compareTo(v2);
				} else {
					Entry<?> p1 = o1.getParent();
					Entry<?> p2 = o2.getParent();
					if (p1 instanceof ClassEntry c1 && p2 instanceof ClassEntry c2) {
						return c1.compareTo(c2);
					} else if (p1 instanceof MethodEntry m1 && p2 instanceof MethodEntry m2) {
						return m1.compareTo(m2);
					}

					return -1;
				}
			});
	}

	@Override
	public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progress, MappingSaveParameters parameters) {
		List<EntryTreeNode<EntryMapping>> classes = StreamSupport.stream(mappings.spliterator(), false)
			.filter(node -> node.getEntry() instanceof ClassEntry)
			.sorted(mappingComparator())
			.toList();

		try (PrintWriter writer = new LfPrintWriter(Files.newBufferedWriter(path))) {
			writer.println("tiny\t2\t" + MINOR_VERSION + "\t" + this.obfHeader + "\t" + this.deobfHeader);

			// no escape names

			for (EntryTreeNode<EntryMapping> node : classes) {
				this.writeClass(writer, node, mappings);
			}
		} catch (IOException ex) {
			Logger.error(ex, "Failed to write mappings to {}", path);
		}
	}

	private void writeClass(PrintWriter writer, EntryTreeNode<EntryMapping> node, EntryMap<EntryMapping> tree) {
		writer.print("c\t");
		ClassEntry classEntry = (ClassEntry) node.getEntry();
		String fullName = classEntry.getFullName();
		writer.print(fullName);
		Deque<String> parts = new LinkedList<>();
		do {
			EntryMapping mapping = tree.get(classEntry);
			if (mapping != null && mapping.targetName() != null) {
				parts.addFirst(mapping.targetName());
			} else {
				parts.addFirst(classEntry.getName());
			}

			classEntry = classEntry.getOuterClass();
		} while (classEntry != null);

		String mappedName = String.join("$", parts);

		writer.print("\t");

		writer.print(mappedName); // todo escaping when we have v2 fixed later

		writer.println();

		this.writeComment(writer, node.getValue(), 1);

		for (EntryTreeNode<EntryMapping> child : node.getChildNodes().stream().sorted(mappingComparator()).toList()) {
			Entry<?> entry = child.getEntry();
			if (entry instanceof FieldEntry) {
				this.writeField(writer, child);
			} else if (entry instanceof MethodEntry) {
				this.writeMethod(writer, child);
			}
		}
	}

	private void writeMethod(PrintWriter writer, EntryTreeNode<EntryMapping> node) {
		writer.print(this.indent(1));
		writer.print("m\t");
		writer.print(((MethodEntry) node.getEntry()).getDesc().toString());
		writer.print("\t");
		writer.print(node.getEntry().getName());
		writer.print("\t");
		EntryMapping mapping = node.getValue();

		if (mapping == null) {
			mapping = EntryMapping.DEFAULT;
		}

		if (mapping.targetName() != null) {
			writer.println(mapping.targetName());
		} else {
			writer.println(node.getEntry().getName()); // todo fix v2 name inference
		}

		this.writeComment(writer, mapping, 2);

		for (EntryTreeNode<EntryMapping> child : node.getChildNodes().stream().sorted(mappingComparator()).toList()) {
			Entry<?> entry = child.getEntry();
			if (entry instanceof LocalVariableEntry) {
				this.writeParameter(writer, child);
			}

			// TODO write actual local variables
		}
	}

	private void writeField(PrintWriter writer, EntryTreeNode<EntryMapping> node) {
		if (node.getValue() == null || node.getValue().equals(EntryMapping.DEFAULT)) {
			return; // Shortcut
		}

		writer.print(this.indent(1));
		writer.print("f\t");
		writer.print(((FieldEntry) node.getEntry()).getDesc().toString());
		writer.print("\t");
		writer.print(node.getEntry().getName());
		writer.print("\t");
		EntryMapping mapping = node.getValue();

		if (mapping == null) {
			mapping = EntryMapping.DEFAULT;
		}

		if (mapping.targetName() != null) {
			writer.println(mapping.targetName());
		} else {
			writer.println(node.getEntry().getName()); // todo fix v2 name inference
		}

		this.writeComment(writer, mapping, 2);
	}

	private void writeParameter(PrintWriter writer, EntryTreeNode<EntryMapping> node) {
		if (node.getValue() == null || node.getValue().equals(EntryMapping.DEFAULT)) {
			return; // Shortcut
		}

		writer.print(this.indent(2));
		writer.print("p\t");
		writer.print(((LocalVariableEntry) node.getEntry()).getIndex());
		writer.print("\t");
		writer.print(node.getEntry().getName());
		writer.print("\t");
		EntryMapping mapping = node.getValue();
		if (mapping == null || mapping.targetName() == null) {
			writer.println(); // todo ???
		} else {
			writer.println(mapping.targetName());

			this.writeComment(writer, mapping, 3);
		}
	}

	private void writeComment(PrintWriter writer, EntryMapping mapping, int indent) {
		if (mapping != null && mapping.javadoc() != null) {
			writer.print(this.indent(indent));
			writer.print("c\t");
			writer.print(MappingHelper.escape(mapping.javadoc()));
			writer.println();
		}
	}

	private String indent(int level) {
		return Strings.repeat("\t", level);
	}
}
