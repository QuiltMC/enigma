package cuchaz.enigma.analysis;

import com.google.common.collect.Sets;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.analysis.index.ReferenceIndex;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class MethodReferenceTreeNode extends DefaultMutableTreeNode implements ReferenceTreeNode<MethodEntry, MethodDefEntry> {
	private final Translator translator;
	private final MethodEntry entry;
	private final EntryReference<MethodEntry, MethodDefEntry> reference;

	public MethodReferenceTreeNode(Translator translator, MethodEntry entry) {
		this.translator = translator;
		this.entry = entry;
		this.reference = null;
	}

	public MethodReferenceTreeNode(Translator translator, EntryReference<MethodEntry, MethodDefEntry> reference) {
		this.translator = translator;
		this.entry = reference.entry;
		this.reference = reference;
	}

	@Override
	public MethodEntry getEntry() {
		return this.entry;
	}

	@Override
	public EntryReference<MethodEntry, MethodDefEntry> getReference() {
		return this.reference;
	}

	@Override
	public String toString() {
		if (this.reference != null) {
			return String.format("%s", this.translator.translate(this.reference.context));
		}
		return this.translator.translate(this.entry).getName();
	}

	public void load(JarIndex index, boolean recurse, boolean recurseMethod) {
		// get all the child nodes
		Collection<EntryReference<MethodEntry, MethodDefEntry>> references = this.getReferences(index, recurseMethod);

		for (EntryReference<MethodEntry, MethodDefEntry> reference : references) {
			this.add(new MethodReferenceTreeNode(this.translator, reference));
		}

		if (recurse && this.children != null) {
			for (Object child : this.children) {
				if (child instanceof MethodReferenceTreeNode node) {
					// don't recurse into ancestor
					Set<Entry<?>> ancestors = Sets.newHashSet();
					TreeNode n = node;
					while (n.getParent() != null) {
						n = n.getParent();
						if (n instanceof MethodReferenceTreeNode) {
							ancestors.add(((MethodReferenceTreeNode) n).getEntry());
						}
					}
					if (ancestors.contains(node.getEntry())) {
						continue;
					}

					node.load(index, true, false);
				}
			}
		}
	}

	private Collection<EntryReference<MethodEntry, MethodDefEntry>> getReferences(JarIndex index, boolean recurseMethod) {
		ReferenceIndex referenceIndex = index.getReferenceIndex();

		if (recurseMethod) {
			Collection<EntryReference<MethodEntry, MethodDefEntry>> references = new ArrayList<>();

			EntryResolver entryResolver = index.getEntryResolver();
			for (MethodEntry methodEntry : entryResolver.resolveEquivalentMethods(this.entry)) {
				references.addAll(referenceIndex.getReferencesToMethod(methodEntry));
			}

			return references;
		} else {
			return referenceIndex.getReferencesToMethod(this.entry);
		}
	}
}
