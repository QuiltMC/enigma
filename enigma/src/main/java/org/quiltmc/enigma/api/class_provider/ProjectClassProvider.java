package org.quiltmc.enigma.api.class_provider;

import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nullable;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ProjectClassProvider implements ClassProvider {
	@Nullable
	private final ClassProvider main;
	@Nullable
	private final ClassProvider libraries;
	private final Collection<String> classNames;

	public ProjectClassProvider(@Nullable ClassProvider main, @Nullable ClassProvider libraries) {
		if (main == null && libraries == null) {
			throw new InvalidParameterException("cannot create a project class provider with both main and library providers as null!");
		}

		this.main = main;
		this.libraries = libraries;
		this.classNames = new ArrayList<>();
		if (main != null) {
			this.classNames.addAll(main.getClassNames());
		}

		if (libraries != null) {
			this.classNames.addAll(libraries.getClassNames());
		}
	}

	@Nullable
	@Override
	public ClassNode get(String name) {
		// i hate working with nullability and am a bad programmer. btw
		ClassNode mainNode;
		if (this.main != null) {
			mainNode = this.main.get(name);
			if (mainNode != null) {
				return mainNode;
			}
		}

		if (this.libraries != null) {
			return this.libraries.get(name);
		}

		return null;
	}

	/**
	 * Gets the {@linkplain ClassNode} for a class in the main JAR file. The class provider may return a cached result,
	 * so it's important to not mutate it.
	 *
	 * @param name the internal name of the class
	 * @return the {@linkplain ClassNode} for that class, or {@code null} if it was not found
	 */
	public @Nullable ClassNode getMainClass(String name) {
		return this.main != null ? this.main.get(name) : null;
	}

	/**
	 * Gets the {@linkplain ClassNode} for a class in the provided libraries. The class provider may return a cached result,
	 * so it's important to not mutate it.
	 *
	 * @param name the internal name of the class
	 * @return the {@linkplain ClassNode} for that class, or {@code null} if it was not found
	 */
	public @Nullable ClassNode getLibraryClass(String name) {
		return this.libraries != null ? this.libraries.get(name) : null;
	}

	@Override
	public Collection<String> getClassNames() {
		return this.classNames;
	}

	public Collection<String> getMainClassNames() {
		return this.main != null ? this.main.getClassNames() : Collections.emptySet();
	}

	public Collection<String> getLibraryClassNames() {
		return this.libraries != null ? this.libraries.getClassNames() : Collections.emptySet();
	}
}
