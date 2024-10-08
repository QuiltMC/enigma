package org.quiltmc.enigma.impl.source.vineflower;

import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.mapping.ResolutionStrategy;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
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
			EntryMapping mapping = this.remapper.getMapping(getClassEntry(structClass));
			StringBuilder builder = new StringBuilder();

			if (mapping.javadoc() != null) {
				builder.append(mapping.javadoc());
			}

			builder.append('\n');

			if (isRecord(structClass)) {
				for (StructRecordComponent component : structClass.getRecordComponents()) {
					EntryMapping componentMapping = this.remapper.getMapping(getFieldEntry(structClass, component));

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
			EntryMapping mapping = this.remapper.getMapping(getFieldEntry(structClass, structField));
			return mapping.javadoc();
		}

		return null;
	}

	@Override
	public String getMethodDoc(StructClass structClass, StructMethod structMethod) {
		if (this.remapper != null) {
			MethodEntry entry = getMethodEntry(structClass, structMethod);
			EntryMapping mapping = this.remapper.getMapping(entry);
			StringBuilder builder = new StringBuilder();

			if (mapping.javadoc() != null) {
				builder.append(mapping.javadoc());
			}

			builder.append('\n');

			Collection<Entry<?>> children = this.remapper.getObfChildren(entry);

			if (children != null && !children.isEmpty()) {
				for (Entry<?> child : children) {
					if (child instanceof LocalVariableEntry) {
						EntryMapping paramMapping = this.remapper.getMapping(child);

						if (paramMapping.javadoc() != null) {
							// for overridden methods, it's possible that we have no name and need to search for the root
							String name = paramMapping.targetName();
							if (name == null) {
								var root = this.remapper.getObfResolver().resolveFirstEntry(child, ResolutionStrategy.RESOLVE_ROOT);
								EntryMapping rootMapping = this.remapper.getMapping(root); // root entry will never be null
								name = rootMapping.targetName();
							}

							builder.append("\n@param ").append(name).append(' ').append(paramMapping.javadoc());
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
