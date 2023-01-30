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
    private final IContextSource external = new ExternalContextSource();
    private final ClassProvider classProvider;
    private final String name;
    private List<String> classNames;

    public EnigmaContextSource(ClassProvider classProvider, String className) {
        this.classProvider = classProvider;
        this.name = className;
    }

    public IContextSource getExternalSource() {
        return this.external;
    }

    @Override
    public String getName() {
        return "class " + name;
    }

    private void collectClassNames() {
        if (classNames != null) {
            return;
        }

        classNames = new ArrayList<>();
        String root = name.contains("$") ? name.substring(0, name.indexOf("$")) : name;
        classNames.add(root);
        classNames.addAll(classProvider.getClasses(root).stream().filter(s -> s.contains("$")).toList());
    }

    @Override
    public Entries getEntries() {
        collectClassNames();
        List<Entry> classes = classNames.stream()
                .distinct().map(Entry::atBase).toList();

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
				if (qualifiedName.equals(name)) {
					saver.saveClassFile("", qualifiedName, fileName, content, mapping);
				}
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

    public class ExternalContextSource implements IContextSource {
        private List<String> externalClassNames;

        @Override
        public String getName() {
            return "external classes for " + name;
        }

        private void collectExternalClassNames() {
            if (externalClassNames != null) {
                return;
            }

            collectClassNames();
            externalClassNames = new ArrayList<>(classProvider.getClassNames());
            externalClassNames.removeAll(EnigmaContextSource.this.classNames);
        }

        @Override
        public Entries getEntries() {
            collectExternalClassNames();
            List<Entry> classes = externalClassNames.stream()
                    .distinct().map(Entry::atBase).toList();
            return new Entries(classes,
                    Collections.emptyList(), Collections.emptyList());
        }

        @Override
        public InputStream getInputStream(String resource) {
            return EnigmaContextSource.this.getInputStream(resource);
        }
    }
}
