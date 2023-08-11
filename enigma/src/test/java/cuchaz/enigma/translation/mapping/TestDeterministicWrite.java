package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.HashEntryTree;
import cuchaz.enigma.translation.representation.ArgumentDescriptor;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.ParameterAccessFlags;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestDeterministicWrite {
	@Test
	public void testTinyV2() throws Exception {
		Path dir = Files.createTempDirectory("enigmaDeterministicTinyV2-");

		EntryTree<EntryMapping> mappings = randomMappingTree(1L);

		String prev = null;
		for (int i = 0; i < 32; i++) {
			Path file = dir.resolve(i + ".tiny");
			MappingFormat.TINY_V2.write(mappings, file, ProgressListener.none(), null);

			String content = Files.readString(file);
			if (prev != null) Assertions.assertEquals(prev, content, "Iteration " + i + " has a different result from the previous one");
			prev = content;
			mappings = MappingFormat.TINY_V2.read(file, ProgressListener.none(), null);
		}
	}

	public static EntryTree<EntryMapping> randomMappingTree(long seed) {
		EntryTree<EntryMapping> mappings = new HashEntryTree<>();
		Random random = new Random(seed);
		int size = 256;

		List<ClassEntry> classes = new ArrayList<>();
		ClassEntry currentClass = new ClassEntry("pkg/" + hexHash("seed" + seed).toUpperCase());
		classes.add(currentClass);
		Entry<?> current = currentClass;
		insertRandomMapping(mappings, current, random, true);
		for (int i = 1; i < size; i++) {
			int next = random.nextInt(9);

			switch (next) {
				case 0 -> {
					// SubClass
					int val = random.nextInt();
					currentClass = new ClassEntry(currentClass, hexHash("subClass" + val).toUpperCase());
					classes.add(currentClass);
					current = currentClass;
					insertRandomMapping(mappings, current, random, true);
				}
				case 1, 2, 3 -> {
					// Field
					int val = random.nextInt();
					TypeDescriptor type = randomType(classes, random, false);
					current = new FieldEntry(currentClass, hexHash("field" + val), type);
					insertRandomMapping(mappings, current, random);
				}
				case 4, 5, 6 -> {
					// Method
					int val = random.nextInt();
					MethodDescriptor descriptor = randomMethodDescriptor(classes, random);
					current = new MethodEntry(currentClass, hexHash("method" + val), descriptor);
					insertRandomMapping(mappings, current, random);
				}
				case 7, 8 -> {
					// Class in package
					int val = random.nextInt();
					String pkg = randomPackage(currentClass, random);
					currentClass = new ClassEntry(pkg + "/" + hexHash("class" + val).toUpperCase());
					classes.add(currentClass);
					current = currentClass;
					insertRandomMapping(mappings, current, random, true);
				}
			}
		}

		return mappings;
	}

	public static TypeDescriptor randomType(List<ClassEntry> classes, Random random, boolean allowVoid) {
		String desc = randomDescriptor(classes, random, allowVoid);

		return new TypeDescriptor(desc);
	}

	private static String randomDescriptor(List<ClassEntry> classes, Random random, boolean allowVoid) {
		int val = random.nextInt(allowVoid ? 10 : 9);

		return switch (val) {
			case 1 -> "B";
			case 2 -> "C";
			case 3 -> "D";
			case 4 -> "F";
			case 5 -> "I";
			case 6 -> "J";
			case 7 -> "S";
			case 8 -> "Z";
			case 9 -> "V";
			default -> // Class
				"L" + classes.get(random.nextInt(classes.size())).getFullName() + ";";
		};
	}

	public static MethodDescriptor randomMethodDescriptor(List<ClassEntry> classes, Random random) {
		int count = random.nextInt(4);
		List<ArgumentDescriptor> args = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			args.add(new ArgumentDescriptor(randomDescriptor(classes, random, false), ParameterAccessFlags.DEFAULT));
		}

		return new MethodDescriptor(args, randomType(classes, random, true));
	}

	public static String randomPackage(ClassEntry cls, Random random) {
		String pkg = cls.getPackageName();
		int i = random.nextInt(3);
		if (i == 0) {
			return pkg;
		} else if (i == 1) {
			return pkg.contains("/") ? pkg.substring(0, pkg.lastIndexOf('/')) : pkg;
		} else {
			return pkg + "/" + hexHash("pkg" + random.nextInt());
		}
	}

	public static void insertRandomMapping(EntryTree<EntryMapping> mappings, Entry<?> entry, Random random) {
		insertRandomMapping(mappings, entry, random, false);
	}

	public static void insertRandomMapping(EntryTree<EntryMapping> mappings, Entry<?> entry, Random random, boolean uppercase) {
		String name = hexHash("name" + random.nextInt());
		if (uppercase) name = name.toUpperCase();
		if (entry instanceof ClassEntry cls) {
			if (cls.getParent() == null) {
				name = cls.getPackageName() + "/" + name;
			}
		}

		EntryMapping mapping = new EntryMapping(name);
		mappings.insert(entry, mapping);
	}

	public static String hexHash(String s) {
		return Integer.toHexString(s.hashCode());
	}
}
