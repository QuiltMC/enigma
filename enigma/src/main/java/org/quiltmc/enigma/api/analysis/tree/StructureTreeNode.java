package org.quiltmc.enigma.api.analysis.tree;

import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.translation.TranslateResult;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.AccessFlags;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.DefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.api.translation.representation.entry.ParentedEntry;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class StructureTreeNode extends DefaultMutableTreeNode {
	private final EntryRemapper mapper;
	private final ClassEntry parentEntry;
	private final ParentedEntry<?> entry;

	public StructureTreeNode(EnigmaProject project, ClassEntry parentEntry, ParentedEntry<?> entry) {
		this.mapper = project.getRemapper();
		this.parentEntry = parentEntry;
		this.entry = entry;
	}

	/**
	 * Returns the parented entry represented by this tree node.
	 */
	public ParentedEntry<?> getEntry() {
		return this.entry;
	}

	public void load(EnigmaProject project, StructureTreeOptions options) {
		Stream<ParentedEntry<?>> children = project.getJarIndex().getChildrenByClass().get(this.parentEntry).stream();

		children = switch (options.obfuscationVisibility()) {
			case ALL -> children;
			case OBFUSCATED -> children
					// remove deobfuscated members if only obfuscated, unless it's an inner class
					.filter(e -> (e instanceof ClassEntry) || (project.isObfuscated(e) && project.isRenamable(e)))
					// keep constructor methods if the class is obfuscated
					.filter(e -> !(e instanceof MethodEntry m && m.isConstructor()) || project.isObfuscated(e.getParent()));
			case DEOBFUSCATED -> children.filter(e -> (e instanceof ClassEntry)
					|| (!project.isObfuscated(e) && project.isRenamable(e))
					// keep constructor methods if the class is deobfuscated
					|| (e instanceof MethodEntry m && m.isConstructor()) && !project.isObfuscated(e.getParent()));
		};

		children = switch (options.documentationVisibility()) {
			case ALL -> children;
			// TODO remove EntryRemapper.deobfuscate() calls when javadocs will no longer be tied to deobfuscation
			case DOCUMENTED -> children.filter(e -> (e instanceof ClassEntry) || (project.getRemapper().deobfuscate(e).getJavadocs() != null && !project.getRemapper().deobfuscate(e).getJavadocs().isBlank()));
			case NON_DOCUMENTED -> children.filter(e -> (e instanceof ClassEntry) || (project.getRemapper().deobfuscate(e).getJavadocs() == null || project.getRemapper().deobfuscate(e).getJavadocs().isBlank()));
		};

		children = switch (options.sortingOrder()) {
			case DEFAULT -> children;
			case A_Z -> children.sorted(Comparator.comparing(e -> (e instanceof MethodEntry m && m.isConstructor())
					// compare the class name when the entry is a constructor
					? project.getRemapper().deobfuscate(e.getParent()).getSimpleName().toLowerCase()
					: project.getRemapper().deobfuscate(e).getSimpleName().toLowerCase()));
			case Z_A -> children.sorted(Comparator.comparing(e -> (e instanceof MethodEntry m && m.isConstructor())
					? project.getRemapper().deobfuscate(((ParentedEntry<?>) e).getParent()).getSimpleName().toLowerCase()
					: project.getRemapper().deobfuscate((ParentedEntry<?>) e).getSimpleName().toLowerCase())
					.reversed());
		};

		for (ParentedEntry<?> child : children.toList()) {
			StructureTreeNode childNode = new StructureTreeNode(project, this.parentEntry, child);

			if (child instanceof ClassEntry classEntry) {
				childNode = new StructureTreeNode(project, classEntry, child);
				childNode.load(project, options);
			}

			String search = options.searchString().strip().toLowerCase();
			if ((child instanceof ClassEntry) || (search.isBlank() || childNode.toString().toLowerCase().contains(search))) {
				this.add(childNode);
			}
		}
	}

	@Override
	public String toString() {
		TranslateResult<ParentedEntry<?>> translateResult = this.mapper.extendedDeobfuscate(this.entry);
		String result = translateResult.getValue().getName();

		if (this.entry instanceof FieldDefEntry) {
			FieldDefEntry field = (FieldDefEntry) translateResult.getValue();
			String returnType = this.parseDesc(field.getDesc());

			result = result + ": " + returnType;
		} else if (this.entry instanceof MethodDefEntry) {
			MethodDefEntry method = (MethodDefEntry) translateResult.getValue();
			String args = this.parseArgs(method.getDesc().getTypeDescs());
			String returnType = this.parseDesc(method.getDesc().getReturnDesc());

			if (method.isConstructor()) {
				result = method.getParent().getSimpleName() + args;
			} else {
				result = result + args + ": " + returnType;
			}
		}

		return result;
	}

	public String toHtml() {
		List<String> modifiers = new ArrayList<>();

		if (this.entry instanceof DefEntry<?> defEntry) {
			AccessFlags access = defEntry.getAccess();
			boolean isInterfaceMethod = false;

			if (this.entry instanceof MethodEntry && this.entry.getParent() instanceof ClassDefEntry parent) {
				isInterfaceMethod = parent.getAccess().isInterface();
			}

			if (access.isStatic() && !access.isEnum()) {
				// Static member, but not an enum constant
				modifiers.add("static");
			} else if (isInterfaceMethod && !access.isAbstract()) {
				// Non-static default interface method
				modifiers.add("default");
			}

			if (access.isAbstract() && !access.isInterface() && !isInterfaceMethod && !access.isEnum()) {
				// Abstract, but not an interface, an interface method or an enum class (abstract is the default or meaningless)
				modifiers.add("abstract");
			} else if (access.isFinal() && !access.isEnum()) {
				// Final, but not an enum or an enum constant (they're always final)
				modifiers.add("final");
			}
		}

		return "<i>" + String.join(" ", modifiers) + "</i> " + this;
	}

	private String parseArgs(List<TypeDescriptor> args) {
		if (!args.isEmpty()) {
			StringBuilder result = new StringBuilder("(");

			for (int i = 0; i < args.size(); i++) {
				if (i > 0) {
					result.append(", ");
				}

				result.append(this.parseDesc(args.get(i)));
			}

			return result + ")";
		}

		return "()";
	}

	private String parseDesc(TypeDescriptor desc) {
		if (desc.isVoid()) return "void";
		if (desc.isPrimitive()) return desc.getPrimitive().getKeyword();
		if (desc.isType()) return desc.getTypeEntry().getSimpleName();

		if (desc.isArray()) {
			if (desc.getArrayType().isPrimitive()) return desc.getArrayType().getPrimitive().getKeyword() + "[]";
			if (desc.getArrayType().isType()) return desc.getArrayType().getTypeEntry().getSimpleName() + "[]";
		}

		return null;
	}
}
