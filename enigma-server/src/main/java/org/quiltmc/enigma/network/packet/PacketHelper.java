package org.quiltmc.enigma.network.packet;

import org.quiltmc.enigma.api.source.RenamableTokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryChange;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.util.TristateChange;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PacketHelper {
	private static final int ENTRY_CLASS = 0;
	private static final int ENTRY_FIELD = 1;
	private static final int ENTRY_METHOD = 2;
	private static final int ENTRY_LOCAL_VAR = 3;
	private static final int MAX_STRING_LENGTH = 65535;

	public static Entry<?> readEntry(DataInput input) throws IOException {
		return readEntry(input, null, true);
	}

	public static Entry<?> readEntry(DataInput input, Entry<?> parent, boolean includeParent) throws IOException {
		int type = input.readUnsignedByte();

		if (includeParent && input.readBoolean()) {
			parent = readEntry(input, null, true);
		}

		String name = readString(input);

		String javadocs = null;
		if (input.readBoolean()) {
			javadocs = readString(input);
		}

		switch (type) {
			case ENTRY_CLASS -> {
				if (parent != null && !(parent instanceof ClassEntry)) {
					throw new IOException("Class requires class parent");
				}

				return new ClassEntry((ClassEntry) parent, name, javadocs);
			}
			case ENTRY_FIELD -> {
				if (!(parent instanceof ClassEntry parentClass)) {
					throw new IOException("Field requires class parent");
				}

				TypeDescriptor desc = new TypeDescriptor(readString(input));
				return new FieldEntry(parentClass, name, desc, javadocs);
			}
			case ENTRY_METHOD -> {
				if (!(parent instanceof ClassEntry parentClass)) {
					throw new IOException("Method requires class parent");
				}

				MethodDescriptor desc = new MethodDescriptor(readString(input));
				return new MethodEntry(parentClass, name, desc, javadocs);
			}
			case ENTRY_LOCAL_VAR -> {
				if (!(parent instanceof MethodEntry parentMethod)) {
					throw new IOException("Local variable requires method parent");
				}

				int index = input.readUnsignedShort();
				boolean parameter = input.readBoolean();
				return new LocalVariableEntry(parentMethod, index, name, parameter, javadocs);
			}
			default -> throw new IOException("Received unknown entry type " + type);
		}
	}

	public static void writeEntry(DataOutput output, Entry<?> entry) throws IOException {
		writeEntry(output, entry, true);
	}

	public static void writeEntry(DataOutput output, Entry<?> entry, boolean includeParent) throws IOException {
		// type
		if (entry instanceof ClassEntry) {
			output.writeByte(ENTRY_CLASS);
		} else if (entry instanceof FieldEntry) {
			output.writeByte(ENTRY_FIELD);
		} else if (entry instanceof MethodEntry) {
			output.writeByte(ENTRY_METHOD);
		} else if (entry instanceof LocalVariableEntry) {
			output.writeByte(ENTRY_LOCAL_VAR);
		} else {
			throw new IOException("Don't know how to serialize entry of type " + entry.getClass().getSimpleName());
		}

		// parent
		if (includeParent) {
			output.writeBoolean(entry.getParent() != null);
			if (entry.getParent() != null) {
				writeEntry(output, entry.getParent(), true);
			}
		}

		// name
		writeString(output, entry.getName());

		// javadocs
		output.writeBoolean(entry.getJavadocs() != null);
		if (entry.getJavadocs() != null) {
			writeString(output, entry.getJavadocs());
		}

		// type-specific stuff
		if (entry instanceof FieldEntry fieldEntry) {
			writeString(output, fieldEntry.getDesc().toString());
		} else if (entry instanceof MethodEntry methodEntry) {
			writeString(output, methodEntry.getDesc().toString());
		} else if (entry instanceof LocalVariableEntry localVar) {
			output.writeShort(localVar.getIndex());
			output.writeBoolean(localVar.isArgument());
		}
	}

	public static String readString(DataInput input) throws IOException {
		int length = input.readUnsignedShort();
		byte[] bytes = new byte[length];
		input.readFully(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	public static void writeString(DataOutput output, String str) throws IOException {
		byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
		if (bytes.length > MAX_STRING_LENGTH) {
			throw new IOException("String too long, was " + bytes.length + " bytes, max " + MAX_STRING_LENGTH + " allowed");
		}

		output.writeShort(bytes.length);
		output.write(bytes);
	}

	public static EntryChange<?> readEntryChange(DataInput input) throws IOException {
		Entry<?> e = readEntry(input);
		EntryChange<?> change = EntryChange.modify(e);

		int flags = input.readUnsignedByte();
		TristateChange.Type deobfNameType = TristateChange.Type.values()[flags & 0x3];
		TristateChange.Type javadocType = TristateChange.Type.values()[flags >> 2 & 0x3];
		TristateChange.Type tokenTypeType = TristateChange.Type.values()[flags >> 2 & 0x3];
		TristateChange.Type pluginIdType = TristateChange.Type.values()[flags >> 2 & 0x3];

		switch (deobfNameType) {
			case RESET -> change = change.clearDeobfName();
			case SET -> change = change.withDeobfName(readString(input));
		}

		change = switch (javadocType) {
			case RESET -> change.clearJavadoc();
			case SET -> change.withJavadoc(readString(input));
			default -> change;
		};

		change = switch (tokenTypeType) {
			case RESET -> throw new RuntimeException("cannot remove token type!");
			case SET -> change.withTokenType(RenamableTokenType.values()[input.readInt()]);
			default -> change;
		};

		change = switch (pluginIdType) {
			case RESET -> change.clearSourcePluginId();
			case SET -> change.withSourcePluginId(readString(input));
			default -> change;
		};

		return change;
	}

	public static void writeEntryChange(DataOutput output, EntryChange<?> change) throws IOException {
		writeEntry(output, change.getTarget());
		int flags = change.getDeobfName().getType().ordinal()
				| change.getJavadoc().getType().ordinal() << 2
				| change.getTokenType().getType().ordinal() << 4
				| change.getSourcePluginId().getType().ordinal() << 6;

		output.writeByte(flags);

		if (change.getDeobfName().isSet()) {
			writeString(output, change.getDeobfName().getNewValue());
		}

		if (change.getJavadoc().isSet()) {
			writeString(output, change.getJavadoc().getNewValue());
		}

		if (change.getTokenType().isSet()) {
			output.writeInt(change.getTokenType().getNewValue().ordinal());
		}

		if (change.getSourcePluginId().isSet()) {
			writeString(output, change.getSourcePluginId().getNewValue());
		}
	}
}
