package org.quiltmc.enigma.api.analysis.tree;

import com.google.common.collect.Sets;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.analysis.index.jar.ReferenceIndex;
import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodDefEntry;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Set;

public class ClassReferenceTreeNode extends DefaultMutableTreeNode implements ReferenceTreeNode<ClassEntry, MethodDefEntry> {
	private final Translator deobfuscatingTranslator;
	private final ClassEntry entry;
	private final EntryReference<ClassEntry, MethodDefEntry> reference;

	public ClassReferenceTreeNode(Translator deobfuscatingTranslator, ClassEntry entry) {
		this.deobfuscatingTranslator = deobfuscatingTranslator;
		this.entry = entry;
		this.reference = null;
	}

	public ClassReferenceTreeNode(Translator deobfuscatingTranslator, EntryReference<ClassEntry, MethodDefEntry> reference) {
		this.deobfuscatingTranslator = deobfuscatingTranslator;
		this.entry = reference.entry;
		this.reference = reference;
	}

	@Override
	public ClassEntry getEntry() {
		return this.entry;
	}

	@Override
	public EntryReference<ClassEntry, MethodDefEntry> getReference() {
		return this.reference;
	}

	@Override
	public String toString() {
		if (this.reference != null) {
			return String.format("%s", this.deobfuscatingTranslator.translate(this.reference.context));
		}

		return this.deobfuscatingTranslator.translate(this.entry).getFullName();
	}

	public void load(JarIndex index, boolean recurse) {
		ReferenceIndex referenceIndex = index.getIndex(ReferenceIndex.class);

		// get all the child nodes
		for (EntryReference<ClassEntry, MethodDefEntry> reference : referenceIndex.getReferencesToClass(this.entry)) {
			this.add(new ClassReferenceTreeNode(this.deobfuscatingTranslator, reference));
		}

		if (recurse && this.children != null) {
			for (Object child : this.children) {
				if (child instanceof ClassReferenceTreeNode node) {
					// don't recurse into ancestor
					Set<Entry<?>> ancestors = Sets.newHashSet();
					TreeNode n = node;
					while (n.getParent() != null) {
						n = n.getParent();
						if (n instanceof ClassReferenceTreeNode treeNode) {
							ancestors.add(treeNode.getEntry());
						}
					}

					if (ancestors.contains(node.getEntry())) {
						continue;
					}

					node.load(index, true);
				}
			}
		}
	}
}
