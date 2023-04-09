package cuchaz.enigma.classhandle;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.classprovider.CachingClassProvider;
import cuchaz.enigma.classprovider.ObfuscationFixClassProvider;
import cuchaz.enigma.events.ClassHandleListener;
import cuchaz.enigma.events.ClassHandleListener.InvalidationType;
import cuchaz.enigma.source.DecompiledClassSource;
import cuchaz.enigma.source.Decompiler;
import cuchaz.enigma.source.DecompilerService;
import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceIndex;
import cuchaz.enigma.source.SourceSettings;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.utils.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nullable;

import static cuchaz.enigma.utils.Utils.withLock;

public final class ClassHandleProvider {
	private final EnigmaProject project;

	private final ExecutorService pool = Executors.newWorkStealingPool();
	private DecompilerService ds;
	private Decompiler decompiler;

	private final Map<ClassEntry, Entry> handles = new HashMap<>();

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	public ClassHandleProvider(EnigmaProject project, DecompilerService ds) {
		this.project = project;
		this.ds = ds;
		this.decompiler = this.createDecompiler();
	}

	/**
	 * Open a class by entry. Schedules decompilation immediately if this is the
	 * only handle to the class.
	 *
	 * @param entry the entry of the class to open
	 * @return a handle to the class, {@code null} if a class by that name does
	 * not exist
	 */
	@Nullable
	public ClassHandle openClass(ClassEntry entry) {
		if (!this.project.getJarIndex().getEntryIndex().hasClass(entry)) return null;

		return withLock(this.lock.writeLock(), () -> {
			Entry e = this.handles.computeIfAbsent(entry, entry1 -> new Entry(this, entry1));
			return e.createHandle();
		});
	}

	/**
	 * Set the decompiler service to use when decompiling classes. Invalidates
	 * all currently open classes.
	 *
	 * <p>If the current decompiler service equals the old one, no classes will
	 * be invalidated.
	 *
	 * @param ds the decompiler service to use
	 */
	public void setDecompilerService(DecompilerService ds) {
		if (this.ds.equals(ds)) return;

		this.ds = ds;
		this.decompiler = this.createDecompiler();
		withLock(this.lock.readLock(), () -> this.handles.values().forEach(Entry::invalidate));
	}

	/**
	 * Gets the current decompiler service in use.
	 *
	 * @return the current decompiler service
	 */
	public DecompilerService getDecompilerService() {
		return this.ds;
	}

	private Decompiler createDecompiler() {
		return this.ds.create(new CachingClassProvider(new ObfuscationFixClassProvider(this.project.getClassProvider(), this.project.getJarIndex())), new SourceSettings(true, true));
	}

	/**
	 * Invalidates all mappings. This causes all open class handles to be
	 * re-remapped.
	 */
	public void invalidateMapped() {
		withLock(this.lock.readLock(), () -> this.handles.values().forEach(Entry::invalidateMapped));
	}

	/**
	 * Invalidates mappings for a single class. Note that this does not
	 * invalidate any mappings of other classes where this class is used, so
	 * this should not be used to notify that the mapped name for this class has
	 * changed.
	 *
	 * @param entry the class entry to invalidate
	 */
	public void invalidateMapped(ClassEntry entry) {
		withLock(this.lock.readLock(), () -> {
			Entry e = this.handles.get(entry);
			if (e != null) {
				e.invalidateMapped();
			}
		});
	}

	/**
	 * Invalidates all javadoc. This causes all open class handles to be
	 * re-remapped.
	 */
	public void invalidateJavadoc() {
		withLock(this.lock.readLock(), () -> this.handles.values().forEach(Entry::invalidateJavadoc));
	}

	/**
	 * Invalidates javadoc for a single class. This also causes the class to be
	 * remapped again.
	 *
	 * @param entry the class entry to invalidate
	 */
	public void invalidateJavadoc(ClassEntry entry) {
		withLock(this.lock.readLock(), () -> {
			Entry e = this.handles.get(entry);
			if (e != null) {
				e.invalidateJavadoc();
			}

			if (entry.isInnerClass()) {
				this.invalidateJavadoc(entry.getOuterClass());
			}
		});
	}

	private void deleteEntry(Entry entry) {
		withLock(this.lock.writeLock(), () -> this.handles.remove(entry.entry));
	}

	/**
	 * Destroy this class handle provider. The decompiler threads will try to
	 * shutdown cleanly, and then every open class handle will also be deleted.
	 * This causes {@link ClassHandleListener#onDeleted(ClassHandle)} to get
	 * called.
	 *
	 * <p>After this method is called, this class handle provider can no longer
	 * be used.
	 */
	public void destroy() {
		this.pool.shutdown();
		try {
			this.pool.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		withLock(this.lock.writeLock(), () -> {
			this.handles.values().forEach(Entry::destroy);
			this.handles.clear();
		});
	}

	private static final class Entry {
		private final ClassHandleProvider p;
		private final ClassEntry entry;
		private ClassEntry deobfRef;
		private final List<ClassHandleImpl> handles = new ArrayList<>();
		private Result<Source, ClassHandleError> uncommentedSource;
		private Result<DecompiledClassSource, ClassHandleError> source;

		private final List<CompletableFuture<Result<Source, ClassHandleError>>> waitingUncommentedSources = Collections.synchronizedList(new ArrayList<>());
		private final List<CompletableFuture<Result<DecompiledClassSource, ClassHandleError>>> waitingSources = Collections.synchronizedList(new ArrayList<>());

		private final AtomicInteger decompileVersion = new AtomicInteger();
		private final AtomicInteger javadocVersion = new AtomicInteger();
		private final AtomicInteger indexVersion = new AtomicInteger();
		private final AtomicInteger mappedVersion = new AtomicInteger();

		private final ReadWriteLock lock = new ReentrantReadWriteLock();

		private Entry(ClassHandleProvider p, ClassEntry entry) {
			this.p = p;
			this.entry = entry;
			this.deobfRef = p.project.getMapper().deobfuscate(entry);
			this.invalidate();
		}

		public ClassHandleImpl createHandle() {
			ClassHandleImpl handle = new ClassHandleImpl(this);
			withLock(this.lock.writeLock(), () -> this.handles.add(handle));
			return handle;
		}

		@Nullable
		public ClassEntry getDeobfRef() {
			return this.deobfRef;
		}

		private void checkDeobfRefForUpdate() {
			ClassEntry newDeobf = this.p.project.getMapper().deobfuscate(this.entry);
			if (!Objects.equals(this.deobfRef, newDeobf)) {
				this.deobfRef = newDeobf;
				// copy the list so we don't call event listener code with the lock active
				withLock(this.lock.readLock(), () -> new ArrayList<>(this.handles)).forEach(h -> h.onDeobfRefChanged(newDeobf));
			}
		}

		public void invalidate() {
			this.checkDeobfRefForUpdate();
			withLock(this.lock.readLock(), () -> new ArrayList<>(this.handles)).forEach(h -> h.onInvalidate(InvalidationType.FULL));
			this.continueMapSource(this.continueIndexSource(this.continueInsertJavadoc(this.decompile())));
		}

		public void invalidateJavadoc() {
			this.checkDeobfRefForUpdate();
			withLock(this.lock.readLock(), () -> new ArrayList<>(this.handles)).forEach(h -> h.onInvalidate(InvalidationType.JAVADOC));
			this.continueMapSource(this.continueIndexSource(this.continueInsertJavadoc(CompletableFuture.completedFuture(this.uncommentedSource))));
		}

		public void invalidateMapped() {
			this.checkDeobfRefForUpdate();
			withLock(this.lock.readLock(), () -> new ArrayList<>(this.handles)).forEach(h -> h.onInvalidate(InvalidationType.MAPPINGS));
			this.continueMapSource(CompletableFuture.completedFuture(this.source));
		}

		private CompletableFuture<Result<Source, ClassHandleError>> decompile() {
			int v = this.decompileVersion.incrementAndGet();
			return CompletableFuture.supplyAsync(() -> {
				if (this.decompileVersion.get() != v) return null;

				Result<Source, ClassHandleError> uncommentedSource = Result.ok(this.p.decompiler.getSource(this.entry.getFullName()));
				Entry.this.uncommentedSource = uncommentedSource;
				Entry.this.waitingUncommentedSources.forEach(f -> f.complete(uncommentedSource));
				Entry.this.waitingUncommentedSources.clear();
				withLock(this.lock.readLock(), () -> new ArrayList<>(this.handles)).forEach(h -> h.onUncommentedSourceChanged(uncommentedSource));
				return uncommentedSource;
			}, this.p.pool);
		}

		private CompletableFuture<Result<Source, ClassHandleError>> continueInsertJavadoc(CompletableFuture<Result<Source, ClassHandleError>> f) {
			int v = this.javadocVersion.incrementAndGet();
			return f.thenApplyAsync(res -> {
				if (res == null || this.javadocVersion.get() != v) return null;
				Result<Source, ClassHandleError> jdSource = res.map(s -> s.withJavadocs(this.p.project.getMapper()));
				withLock(this.lock.readLock(), () -> new ArrayList<>(this.handles)).forEach(h -> h.onDocsChanged(jdSource));
				return jdSource;
			}, this.p.pool);
		}

		private CompletableFuture<Result<DecompiledClassSource, ClassHandleError>> continueIndexSource(CompletableFuture<Result<Source, ClassHandleError>> f) {
			int v = this.indexVersion.incrementAndGet();
			return f.thenApplyAsync(res -> {
				if (res == null || this.indexVersion.get() != v) return null;
				return res.andThen(jdSource -> {
					SourceIndex index = jdSource.index();
					index.resolveReferences(this.p.project.getMapper().getObfResolver());
					DecompiledClassSource source = new DecompiledClassSource(this.entry, index);
					return Result.ok(source);
				});
			}, this.p.pool).exceptionally(e -> Result.err(ClassHandleError.decompile(e)));
		}

		private void continueMapSource(CompletableFuture<Result<DecompiledClassSource, ClassHandleError>> f) {
			int v = this.mappedVersion.incrementAndGet();
			f.thenApplyAsync(res -> {
				if (res == null || this.mappedVersion.get() != v) return null;
				return res.andThen(source -> Result.ok(source.remapSource(this.p.project, this.p.project.getMapper().getDeobfuscator())));
			}, this.p.pool).whenComplete((res, e) -> {
				if (e != null) res = Result.err(ClassHandleError.remap(e));
				if (res == null) return;
				Entry.this.source = res;
				Entry.this.waitingSources.forEach(s -> s.complete(this.source));
				Entry.this.waitingSources.clear();
				withLock(this.lock.readLock(), () -> new ArrayList<>(this.handles)).forEach(h -> h.onMappedSourceChanged(this.source));
			});
		}

		public void closeHandle(ClassHandleImpl classHandle) {
			classHandle.destroy();
			withLock(this.lock.writeLock(), () -> {
				this.handles.remove(classHandle);
				if (this.handles.isEmpty()) {
					this.p.deleteEntry(this);
				}
			});
		}

		public void destroy() {
			withLock(this.lock.writeLock(), () -> {
				this.handles.forEach(ClassHandleImpl::destroy);
				this.handles.clear();
			});
		}

		public CompletableFuture<Result<Source, ClassHandleError>> getUncommentedSourceAsync() {
			if (this.uncommentedSource != null) {
				return CompletableFuture.completedFuture(this.uncommentedSource);
			} else {
				CompletableFuture<Result<Source, ClassHandleError>> f = new CompletableFuture<>();
				this.waitingUncommentedSources.add(f);
				return f;
			}
		}

		public CompletableFuture<Result<DecompiledClassSource, ClassHandleError>> getSourceAsync() {
			if (this.source != null) {
				return CompletableFuture.completedFuture(this.source);
			} else {
				CompletableFuture<Result<DecompiledClassSource, ClassHandleError>> f = new CompletableFuture<>();
				this.waitingSources.add(f);
				return f;
			}
		}
	}

	private static final class ClassHandleImpl implements ClassHandle {
		private final Entry entry;

		private boolean valid = true;

		private final Set<ClassHandleListener> listeners = new HashSet<>();

		private ClassHandleImpl(Entry entry) {
			this.entry = entry;
		}

		@Override
		public ClassEntry getRef() {
			this.checkValid();
			return this.entry.entry;
		}

		@Nullable
		@Override
		public ClassEntry getDeobfRef() {
			this.checkValid();
			// cache this?
			return this.entry.getDeobfRef();
		}

		@Override
		public CompletableFuture<Result<DecompiledClassSource, ClassHandleError>> getSource() {
			this.checkValid();
			return this.entry.getSourceAsync();
		}

		@Override
		public CompletableFuture<Result<Source, ClassHandleError>> getUncommentedSource() {
			this.checkValid();
			return this.entry.getUncommentedSourceAsync();
		}

		@Override
		public void invalidate() {
			this.checkValid();
			this.entry.invalidate();
		}

		@Override
		public void invalidateMapped() {
			this.checkValid();
			this.entry.invalidateMapped();
		}

		@Override
		public void invalidateJavadoc() {
			this.checkValid();
			this.entry.invalidateJavadoc();
		}

		public void onUncommentedSourceChanged(Result<Source, ClassHandleError> source) {
			this.listeners.forEach(l -> l.onUncommentedSourceChanged(this, source));
		}

		public void onDocsChanged(Result<Source, ClassHandleError> source) {
			this.listeners.forEach(l -> l.onDocsChanged(this, source));
		}

		public void onMappedSourceChanged(Result<DecompiledClassSource, ClassHandleError> source) {
			this.listeners.forEach(l -> l.onMappedSourceChanged(this, source));
		}

		public void onInvalidate(InvalidationType t) {
			this.listeners.forEach(l -> l.onInvalidate(this, t));
		}

		public void onDeobfRefChanged(ClassEntry newDeobf) {
			this.listeners.forEach(l -> l.onDeobfRefChanged(this, newDeobf));
		}

		@Override
		public void addListener(ClassHandleListener listener) {
			this.listeners.add(listener);
		}

		@Override
		public void removeListener(ClassHandleListener listener) {
			this.listeners.remove(listener);
		}

		@Override
		public ClassHandle copy() {
			this.checkValid();
			return this.entry.createHandle();
		}

		@Override
		public void close() {
			if (this.valid) this.entry.closeHandle(this);
		}

		private void checkValid() {
			if (!this.valid) throw new IllegalStateException("Class handle no longer valid");
		}

		public void destroy() {
			this.listeners.forEach(l -> l.onDeleted(this));
			this.valid = false;
		}
	}
}
