package cuchaz.enigma.classprovider;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Wraps a ClassProvider to provide caching and synchronization.
 */
public class CachingClassProvider implements ClassProvider {
	private final ClassProvider classProvider;
	private final Cache<String, Optional<ClassNode>> cache = CacheBuilder.newBuilder()
			.maximumSize(128)
			.expireAfterAccess(1, TimeUnit.MINUTES)
			.concurrencyLevel(1)
			.build();

	public CachingClassProvider(ClassProvider classProvider) {
		this.classProvider = classProvider;
	}

	@Override
	@Nullable
	public ClassNode get(String name) {
		try {
			return this.cache.get(name, () -> Optional.ofNullable(this.classProvider.get(name))).orElse(null);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Collection<String> getClassNames() {
		return this.classProvider.getClassNames();
	}

	@Override
	public Collection<String> getClasses(String className) {
		return this.classProvider.getClasses(className);
	}
}
