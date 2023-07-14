package cuchaz.enigma.source.vineflower;

import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import net.fabricmc.fernflower.api.IFabricJavadocProvider;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructField;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.StructRecordComponent;
import org.objectweb.asm.Opcodes;

import java.util.Collection;

public class EnigmaJavadocProvider implements IFabricJavadocProvider {
	private final EntryRemapper remapper;

	public EnigmaJavadocProvider(EntryRemapper remapper) {
		this.remapper = remapper;
	}

	private static ClassEntry getClassEntry(StructClass structClass) {
		return new ClassEntry(structClass.qualifiedName);
	}

	private static FieldEntry getFieldEntry(StructClass structClass, StructField field) {
		return FieldEntry.parse(structClass.qualifiedName, field.getName(), field.getDescriptor());
	}

	private static MethodEntry getMethodEntry(StructClass structClass, StructMethod method) {
		return MethodEntry.parse(structClass.qualifiedName, method.getName(), method.getDescriptor());
	}

	private static boolean isRecord(StructClass structClass) {
		return structClass.getRecordComponents() != null;
	}

	@Override
	public String getClassDoc(StructClass structClass) {
		if (this.remapper != null) {
			EntryMapping mapping = this.remapper.getDeobfMapping(getClassEntry(structClass));
			StringBuilder builder = new StringBuilder();

			if (mapping.javadoc() != null) {
				builder.append(mapping.javadoc());
			}

			builder.append('\n');

			if (isRecord(structClass)) {
				for (StructRecordComponent component : structClass.getRecordComponents()) {
					EntryMapping componentMapping = this.remapper.getDeobfMapping(getFieldEntry(structClass, component));

					if (componentMapping.javadoc() != null) {
						builder.append("\n@param ").append(mapping.targetName()).append(' ').append(componentMapping.javadoc());
					}
				}
			}

			String javadoc = builder.toString();
			if (!javadoc.isBlank()) {
				return javadoc.trim();
			}
		}

		return null;
	}

	@Override
	public String getFieldDoc(StructClass structClass, StructField structField) {
		boolean component = isRecord(structClass) && !structField.hasModifier(Opcodes.ACC_STATIC);
		if (this.remapper != null && !component) {
			EntryMapping mapping = this.remapper.getDeobfMapping(getFieldEntry(structClass, structField));
			return mapping.javadoc();
		}

		return null;
	}

	@Override
	public String getMethodDoc(StructClass structClass, StructMethod structMethod) {
		if (this.remapper != null) {
			MethodEntry entry = getMethodEntry(structClass, structMethod);
			EntryMapping mapping = this.remapper.getDeobfMapping(entry);
			StringBuilder builder = new StringBuilder();

			if (mapping.javadoc() != null) {
				builder.append(mapping.javadoc());
			}

			builder.append('\n');

			Collection<Entry<?>> children = this.remapper.getObfChildren(entry);

			if (children != null && !children.isEmpty()) {
				for (Entry<?> child : children) {
					if (child instanceof LocalVariableEntry) {
						EntryMapping paramMapping = this.remapper.getDeobfMapping(child);

						if (paramMapping.javadoc() != null) {
							builder.append("\n@param ").append(paramMapping.targetName()).append(' ').append(paramMapping.javadoc());
						}
					}
				}
			}

			String javadoc = builder.toString();
			if (!javadoc.isBlank()) {
				return javadoc.trim();
			}
		}

		return null;
	}
}
