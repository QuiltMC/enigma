package org.quiltmc.enigma.impl.source.vineflower;

import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.objectweb.asm.tree.ClassNode;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.util.AsmUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
		return "class " + this.name;
	}

	private void collectClassNames() {
		if (this.classNames != null) {
			return;
		}

		this.classNames = new ArrayList<>();
		String root = this.name.contains("$") ? this.name.substring(0, this.name.indexOf("$")) : this.name;
		this.classNames.add(root);

		Map<String, Object> options = VineflowerPreferences.getEffectiveOptions();
		if (!options.containsKey(IFernflowerPreferences.DECOMPILE_INNER)
				|| "1".equals(options.get(IFernflowerPreferences.DECOMPILE_INNER))) {
			this.classNames.addAll(this.classProvider.getClasses(root).stream().filter(s -> s.contains("$")).toList());
		}
	}

	@Override
	public Entries getEntries() {
		this.collectClassNames();
		List<Entry> classes = this.classNames.stream()
				.distinct().map(Entry::atBase).toList();

		return new Entries(classes,
				Collections.emptyList(), Collections.emptyList());
	}

	@Override
	public InputStream getInputStream(String resource) {
		ClassNode node = this.classProvider.get(resource.substring(0, resource.lastIndexOf(".")));

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
				if (qualifiedName.equals(EnigmaContextSource.this.name)) {
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
		@Override
		public String getName() {
			return "external classes for " + EnigmaContextSource.this.name;
		}

		@Override
		public Entries getEntries() {
			return Entries.EMPTY;
		}

		@Override
		public boolean isLazy() {
			return true;
		}

		@Override
		public InputStream getInputStream(String resource) {
			return EnigmaContextSource.this.getInputStream(resource);
		}
	}
}
