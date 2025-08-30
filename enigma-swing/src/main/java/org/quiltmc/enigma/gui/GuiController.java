package org.quiltmc.enigma.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProfile;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.InheritanceIndex;
import org.quiltmc.enigma.api.analysis.tree.ClassImplementationsTreeNode;
import org.quiltmc.enigma.api.analysis.tree.ClassInheritanceTreeNode;
import org.quiltmc.enigma.api.analysis.tree.ClassReferenceTreeNode;
import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.analysis.tree.FieldReferenceTreeNode;
import org.quiltmc.enigma.api.service.ReadWriteService;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.gui.dialog.CrashDialog;
import org.quiltmc.enigma.gui.docker.ClassesDocker;
import org.quiltmc.enigma.gui.docker.CollabDocker;
import org.quiltmc.enigma.gui.docker.Docker;
import org.quiltmc.enigma.gui.network.IntegratedEnigmaClient;
import org.quiltmc.enigma.impl.analysis.IndexTreeBuilder;
import org.quiltmc.enigma.api.analysis.tree.MethodImplementationsTreeNode;
import org.quiltmc.enigma.api.analysis.tree.MethodInheritanceTreeNode;
import org.quiltmc.enigma.api.analysis.tree.MethodReferenceTreeNode;
import org.quiltmc.enigma.api.analysis.tree.StructureTreeNode;
import org.quiltmc.enigma.api.analysis.tree.StructureTreeOptions;
import org.quiltmc.enigma.api.service.ObfuscationTestService;
import org.quiltmc.enigma.api.class_handle.ClassHandle;
import org.quiltmc.enigma.api.class_handle.ClassHandleProvider;
import org.quiltmc.enigma.api.class_provider.ClasspathClassProvider;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.dialog.ProgressDialog;
import org.quiltmc.enigma.api.stats.StatType;
import org.quiltmc.enigma.gui.util.History;
import org.quiltmc.enigma.network.ClientPacketHandler;
import org.quiltmc.enigma.network.EnigmaClient;
import org.quiltmc.enigma.network.EnigmaServer;
import org.quiltmc.enigma.gui.network.IntegratedEnigmaServer;
import org.quiltmc.enigma.network.ServerMessage;
import org.quiltmc.enigma.network.ServerPacketHandler;
import org.quiltmc.enigma.network.packet.c2s.EntryChangeC2SPacket;
import org.quiltmc.enigma.network.packet.c2s.LoginC2SPacket;
import org.quiltmc.enigma.network.packet.Packet;
import org.quiltmc.enigma.api.source.DecompiledClassSource;
import org.quiltmc.enigma.api.service.DecompilerService;
import org.quiltmc.enigma.api.source.SourceIndex;
import org.quiltmc.enigma.api.source.Token;
import org.quiltmc.enigma.api.stats.StatsGenerator;
import org.quiltmc.enigma.api.stats.StatsResult;
import org.quiltmc.enigma.api.stats.StatsTree;
import org.quiltmc.enigma.api.translation.TranslateResult;
import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.mapping.EntryChange;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.util.EntryUtil;
import org.quiltmc.enigma.api.translation.mapping.MappingDelta;
import org.quiltmc.enigma.api.translation.mapping.ResolutionStrategy;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingParseException;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingSaveParameters;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.HashEntryTree;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.TristateChange;
import org.quiltmc.enigma.util.Utils;
import org.quiltmc.enigma.util.validation.Message;
import org.quiltmc.enigma.util.validation.ParameterizedMessage;
import org.quiltmc.enigma.util.validation.ValidationContext;
import org.tinylog.Logger;

import javax.annotation.Nullable;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

public class GuiController implements ClientPacketHandler {
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Gui gui;
	private final Enigma enigma;

	private EnigmaProject project;
	private IndexTreeBuilder indexTreeBuilder;
	private StatsGenerator statsGenerator;

	private Path loadedMappingPath;
	private ReadWriteService readWriteService;

	private ClassHandleProvider chp;

	private ClassHandle tokenHandle;

	private EnigmaClient client;
	private EnigmaServer server;

	private History<EntryReference<Entry<?>, Entry<?>>> referenceHistory;

	public GuiController(Gui gui, EnigmaProfile profile) {
		this.gui = gui;
		this.enigma = Enigma.builder()
				.setProfile(profile)
				.build();
	}

	public boolean isDirty() {
		return this.project != null && this.project.getRemapper().isDirty();
	}

	public CompletableFuture<Void> openJar(final Path jarPath) {
		this.gui.onStartOpenJar();

		return ProgressDialog.runOffThread(this.gui, progress -> {
			this.project = this.enigma.openJar(jarPath, new ClasspathClassProvider(), progress);
			this.indexTreeBuilder = new IndexTreeBuilder(this.project.getJarIndex());
			this.chp = new ClassHandleProvider(this.project, Config.decompiler().activeDecompiler.value().service);
			this.statsGenerator = new StatsGenerator(this.project);

			SwingUtilities.invokeLater(() -> {
				this.gui.onFinishOpenJar(jarPath.getFileName().toString());
				this.refreshClasses();
			});
		});
	}

	public void closeJar() {
		this.chp.destroy();
		this.chp = null;
		this.project = null;
		this.statsGenerator = null;
		this.gui.onCloseJar();
	}

	public CompletableFuture<Void> openMappings(Path path) {
		var readWriteService = this.enigma.getReadWriteService(path);
		if (readWriteService.isEmpty() || !readWriteService.get().supportsReading()) {
			Logger.error("Could not open mappings: no reader found for file \"{}\"", path);
			return CompletableFuture.supplyAsync(() -> null);
		}

		return this.openMappings(readWriteService.get(), path);
	}

	public CompletableFuture<Void> openMappings(ReadWriteService readWriteService, Path path) {
		if (this.project == null || !new File(path.toUri()).exists()) {
			return CompletableFuture.supplyAsync(() -> null);
		}

		this.gui.setMappingsFile(path);
		Config.insertRecentProject(this.project.getJarPath().toString(), path.toString());
		this.gui.getMenuBar().getFileMenu().reloadOpenRecentMenu();

		return ProgressDialog.runOffThread(this.gui, progress -> {
			try {
				EntryTree<EntryMapping> mappings = readWriteService.read(path);
				this.project.setMappings(mappings, progress);

				this.readWriteService = readWriteService;
				this.loadedMappingPath = path;

				this.refreshClasses();
				this.chp.invalidateJavadoc();
				this.statsGenerator = new StatsGenerator(this.project);
				new Thread(this::regenerateAndUpdateStatIcons).start();
			} catch (MappingParseException e) {
				JOptionPane.showMessageDialog(this.gui.getFrame(), e.getMessage());
			} catch (Exception e) {
				CrashDialog.show(e);
			}
		});
	}

	@Override
	public void openMappings(EntryTree<EntryMapping> mappings) {
		if (this.project == null) return;

		this.project.setMappings(mappings, new ProgressDialog(this.gui.getFrame()));
		this.refreshClasses();
		this.chp.invalidateJavadoc();
	}

	public void regenerateAndUpdateStatIcons() {
		if (Config.main().features.enableClassTreeStatIcons.value()) {
			ProgressListener progressListener = ProgressListener.createEmpty();
			this.gui.getMainWindow().getStatusBar().syncWith(progressListener);

			var parameters = Config.stats().createIconGenParameters(this.gui.getEditableStatTypes());
			this.statsGenerator.generate(progressListener, parameters);
		}

		// ensure all class tree dockers show the update to the stats icons
		for (Docker docker : this.gui.getDockerManager().getActiveDockers().values()) {
			if (docker instanceof ClassesDocker) {
				docker.repaint();
			}
		}
	}

	public CompletableFuture<Void> saveMappings(Path path) {
		return this.saveMappings(path, false);
	}

	public CompletableFuture<Void> saveMappings(Path path, boolean background) {
		return this.saveMappings(path, this.readWriteService, background);
	}

	/**
	 * Saves the mappings. If {@code background} is false, a dialog will pop up
	 * showing the progress. Otherwise, the progress will be shown in the
	 * status bar.
	 *
	 * <p>Notice the returned completable future has to be completed by
	 * {@link SwingUtilities#invokeLater(Runnable)}. Hence, do not try to
	 * join on the future in gui, but rather call {@code thenXxx} methods.
	 *
	 * @param path the path of the save
	 * @param service the writer for the mapping type
	 * @param background whether the progress should be shown in the status bar
	 * @return the future of saving
	 */
	public CompletableFuture<Void> saveMappings(Path path, ReadWriteService service, boolean background) {
		if (this.project == null) {
			return CompletableFuture.completedFuture(null);
		} else if (!service.supportsWriting()) {
			String nonWriteableMessage = I18n.translateFormatted("menu.file.save.non_writeable", I18n.translate("mapping_format." + service.getId().split(":")[1].toLowerCase()));
			JOptionPane.showMessageDialog(this.gui.getFrame(), nonWriteableMessage, I18n.translate("menu.file.save.cannot_save"), JOptionPane.ERROR_MESSAGE);
			return CompletableFuture.completedFuture(null);
		}

		if (background) {
			return CompletableFuture.supplyAsync(() -> {
				ProgressListener progress = ProgressListener.createEmpty();
				this.gui.getMainWindow().getStatusBar().syncWith(progress);
				this.doSave(path, service, progress);
				return null;
			});
		} else {
			return ProgressDialog.runOffThread(this.gui, progress -> this.doSave(path, service, progress));
		}
	}

	private void doSave(Path path, ReadWriteService service, ProgressListener progress) {
		EntryRemapper mapper = this.project.getRemapper();
		MappingSaveParameters saveParameters = this.enigma.getProfile().getMappingSaveParameters();

		MappingDelta<EntryMapping> delta = mapper.takeMappingDelta();
		boolean saveAll = !path.equals(this.loadedMappingPath);

		this.readWriteService = service;
		this.loadedMappingPath = path;

		if (saveAll) {
			service.write(mapper.getMappings(), path, progress, saveParameters);
		} else {
			service.write(mapper.getMappings(), delta, path, progress, saveParameters);
		}
	}

	public void closeMappings() {
		if (this.project == null) return;

		this.project.setMappings(null, ProgressListener.createEmpty());

		this.gui.setMappingsFile(null);
		this.refreshClasses();
		this.chp.invalidateJavadoc();
	}

	public void reloadAll() {
		Path jarPath = this.project.getJarPath();
		if (jarPath != null) {
			this.closeJar();
			CompletableFuture<Void> f = this.openJar(jarPath);
			if (this.readWriteService != null && this.loadedMappingPath != null) {
				f.whenComplete((v, t) -> this.openMappings(this.readWriteService, this.loadedMappingPath));
			}
		}
	}

	public void reloadMappings() {
		if (this.readWriteService != null && this.loadedMappingPath != null) {
			this.closeMappings();
			this.openMappings(this.readWriteService, this.loadedMappingPath);
		}
	}

	public CompletableFuture<Void> dropMappings() {
		if (this.project == null) return CompletableFuture.completedFuture(null);

		return ProgressDialog.runOffThread(this.gui, progress -> this.project.dropMappings(progress));
	}

	public CompletableFuture<Void> exportSource(final Path path) {
		if (this.project == null) return CompletableFuture.completedFuture(null);

		return ProgressDialog.runOffThread(this.gui, progress -> {
			EnigmaProject.JarExport jar = this.project.exportRemappedJar(progress);
			jar.decompileStream(progress, this.chp.getDecompilerService(), EnigmaProject.DecompileErrorStrategy.TRACE_AS_SOURCE)
					.forEach(source -> {
						try {
							source.writeTo(source.resolvePath(path));
						} catch (IOException e) {
							Logger.error(e);
						}
					});
		});
	}

	public CompletableFuture<Void> exportJar(final Path path) {
		if (this.project == null) return CompletableFuture.completedFuture(null);

		return ProgressDialog.runOffThread(this.gui, progress -> {
			EnigmaProject.JarExport jar = this.project.exportRemappedJar(progress);
			jar.write(path, progress);
		});
	}

	public void setTokenHandle(ClassHandle handle) {
		if (this.tokenHandle != null) {
			this.tokenHandle.close();
		}

		this.tokenHandle = handle;
	}

	public ClassHandle getTokenHandle() {
		return this.tokenHandle;
	}

	public ReadableToken getReadableToken(Token token) {
		if (this.tokenHandle == null) {
			return null;
		}

		try {
			return this.tokenHandle.getSource().get()
					.map(DecompiledClassSource::getIndex)
					.map(index -> new ReadableToken(
							index.getLineNumber(token.start),
							index.getColumnNumber(token.start),
							index.getColumnNumber(token.end)))
					.unwrapOr(null);
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Navigates to the declaration with respect to navigation history.
	 *
	 * @param entry the entry whose declaration will be navigated to
	 */
	public void openDeclaration(Entry<?> entry) {
		if (entry == null) {
			throw new IllegalArgumentException("Entry cannot be null!");
		}

		this.openReference(EntryReference.declaration(entry, entry.getName()));
	}

	/**
	 * Navigates to the reference with respect to navigation history.
	 *
	 * @param reference the reference
	 */
	public void openReference(EntryReference<Entry<?>, Entry<?>> reference) {
		if (reference == null) {
			throw new IllegalArgumentException("Reference cannot be null!");
		}

		if (this.referenceHistory == null) {
			this.referenceHistory = new History<>(reference);
		} else {
			if (!reference.equals(this.referenceHistory.getCurrent())) {
				this.referenceHistory.push(reference);
			}
		}

		this.gui.showReference(reference);
	}

	public List<Token> getTokensForReference(DecompiledClassSource source, EntryReference<Entry<?>, Entry<?>> reference) {
		EntryRemapper mapper = this.project.getRemapper();

		SourceIndex index = source.getIndex();
		return mapper.getObfResolver().resolveReference(reference, ResolutionStrategy.RESOLVE_CLOSEST)
				.stream()
				.flatMap(r -> index.getReferenceTokens(r).stream())
				.sorted()
				.toList();
	}

	public void openPreviousReference() {
		if (this.hasPreviousReference()) {
			this.gui.showReference(this.referenceHistory.goBack());
		}
	}

	public boolean hasPreviousReference() {
		return this.referenceHistory != null && this.referenceHistory.canGoBack();
	}

	public void openNextReference() {
		if (this.hasNextReference()) {
			this.gui.showReference(this.referenceHistory.goForward());
		}
	}

	public boolean hasNextReference() {
		return this.referenceHistory != null && this.referenceHistory.canGoForward();
	}

	public void navigateTo(Entry<?> entry) {
		if (!this.project.isNavigable(entry)) {
			// entry is not in the jar. Ignore it
			return;
		}

		this.openDeclaration(entry);
	}

	public void navigateTo(EntryReference<Entry<?>, Entry<?>> reference) {
		if (!this.project.isNavigable(reference.getNameableEntry())) {
			return;
		}

		this.openReference(reference);
	}

	public void refreshClasses() {
		if (this.project == null) {
			return;
		}

		List<ClassEntry> obfClasses = new ArrayList<>();
		List<ClassEntry> deobfClasses = new ArrayList<>();
		this.addSeparatedClasses(obfClasses, deobfClasses);
		this.gui.setObfClasses(obfClasses);
		this.gui.setDeobfClasses(deobfClasses);
	}

	public void addSeparatedClasses(List<ClassEntry> obfClasses, List<ClassEntry> deobfClasses) {
		EntryRemapper mapper = this.project.getRemapper();

		Collection<ClassEntry> classes = this.project.getJarIndex().getIndex(EntryIndex.class).getClasses();
		Stream<ClassEntry> visibleClasses = classes.stream()
				.filter(entry -> !entry.isInnerClass());

		visibleClasses.forEach(entry -> {
			TranslateResult<ClassEntry> result = mapper.extendedDeobfuscate(entry);
			ClassEntry deobfEntry = result.getValue();

			List<ObfuscationTestService> obfService = this.enigma.getServices().get(ObfuscationTestService.TYPE);
			boolean obfuscated = result.isObfuscated() && deobfEntry.equals(entry);

			if (obfuscated
					&& !obfService.isEmpty()
					&& obfService.stream().anyMatch(service -> service.testDeobfuscated(entry))) {
				obfuscated = false;
			}

			if (obfuscated) {
				obfClasses.add(entry);
			} else {
				deobfClasses.add(entry);
			}
		});
	}

	public StructureTreeNode getClassStructure(ClassEntry entry, StructureTreeOptions options) {
		StructureTreeNode rootNode = new StructureTreeNode(this.project, entry, entry);
		rootNode.load(this.project, options);
		return rootNode;
	}

	public ClassInheritanceTreeNode getClassInheritance(ClassEntry entry) {
		Translator translator = this.project.getRemapper().getDeobfuscator();
		ClassInheritanceTreeNode rootNode = this.indexTreeBuilder.buildClassInheritance(translator, entry);
		return ClassInheritanceTreeNode.findNode(rootNode, entry);
	}

	public ClassImplementationsTreeNode getClassImplementations(ClassEntry entry) {
		Translator translator = this.project.getRemapper().getDeobfuscator();
		return this.indexTreeBuilder.buildClassImplementations(translator, entry);
	}

	public MethodInheritanceTreeNode getMethodInheritance(MethodEntry entry) {
		Translator translator = this.project.getRemapper().getDeobfuscator();
		MethodInheritanceTreeNode rootNode = this.indexTreeBuilder.buildMethodInheritance(translator, entry);
		return MethodInheritanceTreeNode.findNode(rootNode, entry);
	}

	public MethodImplementationsTreeNode getMethodImplementations(MethodEntry entry) {
		Translator translator = this.project.getRemapper().getDeobfuscator();
		List<MethodImplementationsTreeNode> rootNodes = this.indexTreeBuilder.buildMethodImplementations(translator, entry);
		if (rootNodes.isEmpty()) {
			return null;
		}

		if (rootNodes.size() > 1) {
			Logger.warn("Method {} implements multiple interfaces. Only showing first one.", entry);
		}

		return MethodImplementationsTreeNode.findNode(rootNodes.get(0), entry);
	}

	public ClassReferenceTreeNode getClassReferences(ClassEntry entry) {
		Translator deobfuscator = this.project.getRemapper().getDeobfuscator();
		ClassReferenceTreeNode rootNode = new ClassReferenceTreeNode(deobfuscator, entry);
		rootNode.load(this.project.getJarIndex(), true);
		return rootNode;
	}

	public FieldReferenceTreeNode getFieldReferences(FieldEntry entry) {
		Translator translator = this.project.getRemapper().getDeobfuscator();
		FieldReferenceTreeNode rootNode = new FieldReferenceTreeNode(translator, entry);
		rootNode.load(this.project.getJarIndex(), true);
		return rootNode;
	}

	public MethodReferenceTreeNode getMethodReferences(MethodEntry entry, boolean recursive) {
		Translator translator = this.project.getRemapper().getDeobfuscator();
		MethodReferenceTreeNode rootNode = new MethodReferenceTreeNode(translator, entry);
		rootNode.load(this.project.getJarIndex(), true, recursive);
		return rootNode;
	}

	@Override
	public boolean applyChangeFromServer(EntryChange<?> change) {
		ValidationContext vc = new ValidationContext(this.gui.getNotificationManager(), false);
		this.applyChange0(vc, change, true);
		this.gui.updateStructure(this.gui.getActiveEditor());

		return vc.canProceed();
	}

	public void validateJavadocChange(ValidationContext vc, EntryChange<?> change) {
		TristateChange<String> javadoc = change.getJavadoc();

		if (javadoc.isSet() && javadoc.getNewValue().contains("*/")) {
			vc.raise(Message.ILLEGAL_DOC_COMMENT_END);
		}
	}

	public void applyChange(ValidationContext vc, EntryChange<?> change) {
		this.applyChange(vc, change, true);
	}

	public void applyChange(ValidationContext vc, EntryChange<?> change, boolean updateSwingState) {
		this.applyChange0(vc, change, updateSwingState);
		this.gui.updateStructure(this.gui.getActiveEditor());
		if (!vc.canProceed()) {
			return;
		}

		this.sendPacket(new EntryChangeC2SPacket(change));
	}

	private void applyChange0(ValidationContext vc, EntryChange<?> change, boolean updateSwingState) {
		Entry<?> target = change.getTarget();
		EntryMapping prev = this.project.getRemapper().getMapping(target);
		EntryMapping mapping = EntryUtil.applyChange(vc, this.project.getRemapper(), change);

		if (vc.canProceed()) {
			boolean renamed = !change.getDeobfName().isUnchanged();
			this.gui.updateStructure(this.gui.getActiveEditor());
			if (this.gui.getActiveEditor() != null) {
				this.gui.getActiveEditor().onRename(prev.targetName() == null && mapping.targetName() != null);
			}

			if (!Objects.equals(prev.targetName(), mapping.targetName()) || !Objects.equals(prev.tokenType(), mapping.tokenType())) {
				this.chp.invalidateMapped();

				// local variable entries need to be propagated up the tree to update param names in javadoc
				if (target instanceof LocalVariableEntry) {
					this.chp.invalidateJavadoc(target.getTopLevelClass());

					var children = this.project.getJarIndex().getIndex(InheritanceIndex.class).getChildren(target.getContainingClass());
					for (ClassEntry child : children) {
						this.chp.invalidateJavadoc(child.getTopLevelClass());
					}
				}
			}

			if (!Objects.equals(prev.javadoc(), mapping.javadoc())) {
				this.chp.invalidateJavadoc(target.getTopLevelClass());
			}

			if (renamed && target instanceof ClassEntry classEntry && !classEntry.isInnerClass()) {
				boolean isOldOb = prev.targetName() == null;
				boolean isNewOb = mapping.targetName() == null;
				this.gui.moveClassTree(target.getContainingClass(), updateSwingState, isOldOb, isNewOb);
			} else if (updateSwingState) {
				// update stat icons for classes that could have had their mappings changed by this update
				boolean propagate = target instanceof FieldEntry || target instanceof MethodEntry || target instanceof LocalVariableEntry;
				this.gui.reloadStats(change.getTarget().getTopLevelClass(), propagate);
			}
		}
	}

	public void openStatsTree(Set<StatType> includedTypes) {
		ProgressDialog.runOffThread(this.gui, progress -> {
			StatsResult overall = this.getStatsGenerator().getResult(Config.stats().createGenParameters(this.gui.getEditableStatTypes())).getOverall();
			StatsTree<Integer> tree = overall.buildTree(Config.main().stats.lastTopLevelPackage.value(), includedTypes);
			String treeJson = GSON.toJson(tree.root);

			try {
				File statsFile = File.createTempFile("stats", ".html");

				try (FileWriter w = new FileWriter(statsFile)) {
					w.write(
							Utils.readResourceToString("/stats.html")
									.replace("/*data*/", treeJson)
					);
				}

				Desktop.getDesktop().open(statsFile);
			} catch (IOException e) {
				throw new Error(e);
			}
		});
	}

	public void setDecompiler(DecompilerService service) {
		if (this.chp != null) {
			this.chp.setDecompilerService(service);
		}
	}

	public ClassHandleProvider getClassHandleProvider() {
		return this.chp;
	}

	public EnigmaClient getClient() {
		return this.client;
	}

	public EnigmaServer getServer() {
		return this.server;
	}

	public EnigmaProject getProject() {
		return this.project;
	}

	public Enigma getEnigma() {
		return this.enigma;
	}

	@Nullable
	public StatsGenerator getStatsGenerator() {
		return this.statsGenerator;
	}

	@Nullable
	public ReadWriteService getReadWriteService() {
		return this.readWriteService;
	}

	public void createClient(String username, String ip, int port, char[] password) throws IOException {
		this.client = new IntegratedEnigmaClient(this, ip, port);
		this.client.connect();
		this.client.sendPacket(new LoginC2SPacket(this.project.getJarChecksum(), password, username));
		this.gui.setConnectionState(ConnectionState.CONNECTED);
	}

	public void createServer(String username, int port, char[] password) throws IOException {
		this.server = new IntegratedEnigmaServer(this.project.getJarChecksum(), password, EntryRemapper.mapped(this.project.getEnigma(), this.project.getJarIndex(), this.project.getMappingsIndex(), new HashEntryTree<>(this.project.getRemapper().getJarProposedMappings()), new HashEntryTree<>(this.project.getRemapper().getDeobfMappings()), this.project.getEnigma().getNameProposalServices()), port);
		this.server.start();
		this.client = new IntegratedEnigmaClient(this, "127.0.0.1", port);
		this.client.connect();
		this.client.sendPacket(new LoginC2SPacket(this.project.getJarChecksum(), password, username));
		this.gui.setConnectionState(ConnectionState.HOSTING);
	}

	@Override
	public synchronized void disconnectIfConnected(String reason) {
		if (this.client == null && this.server == null) {
			return;
		}

		if (this.client != null) {
			this.client.disconnect();
			this.client = null;
		}

		if (this.server != null) {
			this.server.stop();
			this.server = null;
		}

		SwingUtilities.invokeLater(() -> {
			if (reason != null) {
				JOptionPane.showMessageDialog(this.gui.getFrame(), I18n.translate(reason), I18n.translate("disconnect.disconnected"), JOptionPane.INFORMATION_MESSAGE);
			}

			this.gui.setConnectionState(ConnectionState.NOT_CONNECTED);
		});

		this.gui.setUserList(new ArrayList<>());
		if (Config.main().serverNotificationLevel.value() != NotificationManager.ServerNotificationLevel.NONE) {
			this.gui.getNotificationManager().notify(new ParameterizedMessage(Message.LEFT_SERVER));
		}

		this.gui.getDockerManager().getDocker(CollabDocker.class).setUp();
	}

	@Override
	public void sendPacket(Packet<ServerPacketHandler> packet) {
		if (this.client != null) {
			this.client.sendPacket(packet);
		}
	}

	@Override
	public void addMessage(ServerMessage message) {
		this.gui.addMessage(message);
	}

	@Override
	public void updateUserList(List<String> users) {
		this.gui.setUserList(users);
	}

	public Gui getGui() {
		return this.gui;
	}
}
