package cuchaz.enigma.source.quiltflower;

import cuchaz.enigma.source.SourceIndex;
import cuchaz.enigma.source.Token;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.Pair;
import org.jetbrains.java.decompiler.main.extern.TextTokenVisitor;
import org.jetbrains.java.decompiler.struct.gen.FieldDescriptor;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;
import org.jetbrains.java.decompiler.util.token.TextRange;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class EnigmaTextTokenCollector extends TextTokenVisitor {
    private String content;
    private MethodEntry currentMethod;

    private final Map<Token, Entry<?>> declarations = new HashMap<>();
    private final Map<Token, Pair<Entry<?>, Entry<?>>> references = new HashMap<>();
    private final Map<Token, Boolean> tokens = new LinkedHashMap<>();

    public EnigmaTextTokenCollector(TextTokenVisitor next) {
        super(next);
    }

    private static ClassEntry getClassEntry(String name) {
        return new ClassEntry(name);
    }

    private static FieldEntry getFieldEntry(String className, String name, FieldDescriptor descriptor) {
        return FieldEntry.parse(className, name, descriptor.descriptorString);
    }

    private static MethodEntry getMethodEntry(String className, String name, MethodDescriptor descriptor) {
        return MethodEntry.parse(className, name, descriptor.toString());
    }

    private static LocalVariableEntry getParameterEntry(MethodEntry parent, int index, String name) {
        return new LocalVariableEntry(parent, index, name, true, null);
    }

    private static LocalVariableEntry getVariableEntry(MethodEntry parent, int index, String name) {
        return new LocalVariableEntry(parent, index, name, false, null);
    }

    private Token getToken(TextRange range) {
        return new Token(range.start, range.start + range.length, content.substring(range.start, range.start + range.length));
    }

    private void addDeclaration(Token token, Entry<?> entry) {
        declarations.put(token, entry);
        tokens.put(token, true);
    }

    private void addReference(Token token, Entry<?> entry, Entry<?> context) {
        references.put(token, new Pair<>(entry, context));
        tokens.put(token, false);
    }

    public void addTokensToIndex(SourceIndex index, Function<Token, Token> tokenProcessor) {
        for (Token token : tokens.keySet()) {
            Token newToken = tokenProcessor.apply(token);
            if (newToken == null) {
                continue;
            }

            if (tokens.get(token)) {
                index.addDeclaration(newToken, declarations.get(token));
            } else {
                Pair<Entry<?>, Entry<?>> ref = references.get(token);
                index.addReference(newToken, ref.a, ref.b);
            }
        }
    }

    @Override
    public void start(String content) {
        this.content = content;
        this.currentMethod = null;
    }

    @Override
    public void visitClass(TextRange range, boolean declaration, String name) {
        super.visitClass(range, declaration, name);
        Token token = getToken(range);

        if (declaration) {
            addDeclaration(token, getClassEntry(name));
        } else {
            addReference(token, getClassEntry(name), currentMethod);
        }
    }

    @Override
    public void visitField(TextRange range, boolean declaration, String className, String name, FieldDescriptor descriptor) {
        super.visitField(range, declaration, className, name, descriptor);
        Token token = getToken(range);

        if (declaration) {
            addDeclaration(token, getFieldEntry(className, name, descriptor));
        } else {
            addReference(token, getFieldEntry(className, name, descriptor), currentMethod);
        }
    }

    @Override
    public void visitMethod(TextRange range, boolean declaration, String className, String name, MethodDescriptor descriptor) {
        super.visitMethod(range, declaration, className, name, descriptor);
        Token token = getToken(range);
        MethodEntry entry = getMethodEntry(className, name, descriptor);

        if (token.text.equals("new")) {
            return;
        }

        if (declaration) {
            addDeclaration(token, entry);
            currentMethod = entry;
        } else {
            addReference(token, entry, currentMethod);
        }
    }

    @Override
    public void visitParameter(TextRange range, boolean declaration, String className, String methodName, MethodDescriptor methodDescriptor, int idx, String name) {
        super.visitParameter(range, declaration, className, methodName, methodDescriptor, idx, name);
        Token token = getToken(range);
        MethodEntry parent = getMethodEntry(className, methodName, methodDescriptor);

        if (declaration) {
            addDeclaration(token, getParameterEntry(parent, idx, name));
        } else {
            addReference(token, getParameterEntry(parent, idx, name), currentMethod);
        }
    }

    @Override
    public void visitLocal(TextRange range, boolean declaration, String className, String methodName, MethodDescriptor methodDescriptor, int idx, String name) {
        super.visitLocal(range, declaration, className, methodName, methodDescriptor, idx, name);
        Token token = getToken(range);
        MethodEntry parent = getMethodEntry(className, methodName, methodDescriptor);

        if (declaration) {
            addDeclaration(token, getVariableEntry(parent, idx, name));
        } else {
            addReference(token, getVariableEntry(parent, idx, name), currentMethod);
        }
    }
}
