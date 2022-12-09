package cuchaz.enigma.analysis;

import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

import javax.swing.tree.DefaultMutableTreeNode;

public abstract class AbstractClassTreeNode extends DefaultMutableTreeNode {
    protected final Translator translator;
    protected final ClassEntry entry;

    protected AbstractClassTreeNode(Translator translator, ClassEntry entry) {
        this.translator = translator;
        this.entry = entry;
    }

    /**
     * Returns the class entry represented by this tree node.
     */
    public ClassEntry getClassEntry() {
        return this.entry;
    }

    public String getClassName() {
        return this.entry.getFullName();
    }

    @Override
    public String toString() {
        TranslateResult<ClassEntry> translated = translator.extendedTranslate(entry);
        return translated != null ? translated.getValue().toString() : "invalid class";
    }
}
