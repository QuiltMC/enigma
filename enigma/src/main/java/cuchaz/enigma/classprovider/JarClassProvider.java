package cuchaz.enigma.classprovider;

import com.google.common.collect.ImmutableSet;
import cuchaz.enigma.utils.AsmUtil;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Provides classes by loading them from a JAR file.
 */
public class JarClassProvider implements AutoCloseable, ClassProvider {
	private final FileSystem fileSystem;
	private final Set<String> classNames;

	public JarClassProvider(Path jarPath) throws IOException {
		this.fileSystem = FileSystems.newFileSystem(jarPath, (ClassLoader) null);
		this.classNames = collectClassNames(this.fileSystem);
	}

	private static ImmutableSet<String> collectClassNames(FileSystem fileSystem) throws IOException {
		ImmutableSet.Builder<String> classNames = ImmutableSet.builder();
		for (Path root : fileSystem.getRootDirectories()) {
			Files.walk(root).map(Path::toString)
					.forEach(path -> {
						if (path.endsWith(".class")) {
							String name = path.substring(1, path.length() - ".class".length());
							classNames.add(name);
						}
					});
		}

		return classNames.build();
	}

	@Override
	public Set<String> getClassNames() {
		return this.classNames;
	}

	@Nullable
	@Override
	public ClassNode get(String name) {
		if (!this.classNames.contains(name)) {
			return null;
		}

		try {
			return AsmUtil.bytesToNode(Files.readAllBytes(this.fileSystem.getPath(name + ".class")));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() throws Exception {
		this.fileSystem.close();
	}
}
