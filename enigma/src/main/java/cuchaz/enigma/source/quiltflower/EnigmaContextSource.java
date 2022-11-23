package cuchaz.enigma.source.quiltflower;

import cuchaz.enigma.classprovider.ClassProvider;
import cuchaz.enigma.utils.AsmUtil;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.objectweb.asm.tree.ClassNode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnigmaContextSource implements IContextSource {
    private final ClassProvider classProvider;
    private final String name;

    public EnigmaContextSource(ClassProvider classProvider, String className) {
        this.classProvider = classProvider;
        this.name = className;
    }

    @Override
    public String getName() {
        return "class " + name;
    }

    @Override
    public Entries getEntries() {
        List<String> classNames = new ArrayList<>();
        classNames.add(name);
        classNames.addAll(classProvider.getClasses(name));
        List<Entry> classes = classNames.stream().distinct().map(Entry::atBase).toList();

        return new Entries(classes,
                Collections.emptyList(), Collections.emptyList());
    }

    @Override
    public InputStream getInputStream(String resource) {
        ClassNode node = classProvider.get(resource.substring(0, resource.lastIndexOf(".")));

        if (node == null) {
            return null;
        }

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
