package cuchaz.enigma.source.quiltflower;

import cuchaz.enigma.utils.AsmUtil;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.objectweb.asm.tree.ClassNode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;

public class EnigmaContextSource implements IContextSource {
    private final ClassNode node;
    private final String name;

    public EnigmaContextSource(ClassNode node) {
        this.node = node;
        this.name = node.name;
    }

    @Override
    public String getName() {
        return "class " + name;
    }

    @Override
    public Entries getEntries() {
        return new Entries(Collections.singletonList(Entry.atBase(name)),
                Collections.emptyList(), Collections.emptyList());
    }

    @Override
    public InputStream getInputStream(String resource) {
        return new ByteArrayInputStream(AsmUtil.nodeToBytes(node));
    }

    @Override
    public IOutputSink createOutputSink(IResultSaver saver) {
        return new IOutputSink() {
            @Override
            public void begin() {
            }

            @Override
            public void acceptClass(String qualifiedName, String fileName, String content, int[] mapping) {
                saver.saveClassFile("", qualifiedName, fileName, content, mapping);
            }

            @Override
            public void acceptDirectory(String directory) {
            }

            @Override
            public void acceptOther(String path) {
            }

            @Override
            public void close() {
            }
        };
    }
}
