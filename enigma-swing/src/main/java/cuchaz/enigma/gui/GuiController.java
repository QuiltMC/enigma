/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.gui;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.google.common.collect.Lists;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.EnigmaProfile;
import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.analysis.*;
import cuchaz.enigma.api.service.ObfuscationTestService;
import cuchaz.enigma.classhandle.ClassHandle;
import cuchaz.enigma.classhandle.ClassHandleProvider;
import cuchaz.enigma.classprovider.ClasspathClassProvider;
import cuchaz.enigma.gui.config.NetConfig;
import cuchaz.enigma.gui.config.UiConfig;
import cuchaz.enigma.gui.dialog.ProgressDialog;
import cuchaz.enigma.gui.newabstraction.EntryValidation;
import cuchaz.enigma.gui.stats.StatsGenerator;
import cuchaz.enigma.gui.stats.StatsMember;
import cuchaz.enigma.gui.util.History;
import cuchaz.enigma.network.*;
import cuchaz.enigma.network.packet.EntryChangeC2SPacket;
import cuchaz.enigma.network.packet.LoginC2SPacket;
import cuchaz.enigma.network.packet.Packet;
import cuchaz.enigma.source.DecompiledClassSource;
import cuchaz.enigma.source.DecompilerService;
import cuchaz.enigma.source.SourceIndex;
import cuchaz.enigma.source.Token;
import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.*;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.serde.MappingParseException;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.HashEntryTree;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.Utils;
import cuchaz.enigma.utils.validation.PrintValidatable;
import cuchaz.enigma.utils.validation.ValidationContext;

public class GuiController implements ClientPacketHandler {
	private final Gui gui;
	public final Enigma enigma;

	public EnigmaProject project;
	private IndexTreeBuilder indexTreeBuilder;

	private Path loadedMappingPath;
	private MappingFormat loadedMappingFormat;

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
		return project != null && project.getMapper().isDirty();
	}

	public CompletableFuture<Void> openJar(final Path jarPath) {
		this.gui.onStartOpenJar();

		return ProgressDialog.runOffThread(gui.getFrame(), progress -> {
			project = enigma.openJar(jarPath, new ClasspathClassProvider(), progress);
			indexTreeBuilder = new IndexTreeBuilder(project.getJarIndex());
			chp = new ClassHandleProvider(project, UiConfig.getDecompiler().service);
			SwingUtilities.invokeLater(() -> {
				gui.onFinishOpenJar(jarPath.getFileName().toString());
				refreshClasses();
			});
		});
	}

	public void closeJar() {
		this.chp.destroy();
		this.chp = null;
		this.project = null;
		this.gui.onCloseJar();
	}

	public CompletableFuture<Void> openMappings(MappingFormat format, Path path) {
		if (this.project == null) return CompletableFuture.completedFuture(null);

		this.gui.setMappingsFile(path);

		return ProgressDialog.runOffThread(this.gui.getFrame(), progress -> {
			try {
				MappingSaveParameters saveParameters = this.enigma.getProfile().getMappingSaveParameters();

				EntryTree<EntryMapping> mappings = format.read(path, progress, saveParameters);
				this.project.setMappings(mappings);

				this.loadedMappingFormat = format;
				this.loadedMappingPath = path;

				refreshClasses();
				this.chp.invalidateJavadoc();
			} catch (MappingParseException e) {
				JOptionPane.showMessageDialog(this.gui.getFrame(), e.getMessage());
			}
		});
	}

	@Override
	public void openMappings(EntryTree<EntryMapping> mappings) {
		if (this.project == null) return;

		this.project.setMappings(mappings);
		refreshClasses();
		this.chp.invalidateJavadoc();
	}

	public CompletableFuture<Void> saveMappings(Path path) {
		return saveMappings(path, this.loadedMappingFormat);
	}

	/**
	 * Saves the mappings, with a dialog popping up, showing the progress.
	 *
	 * <p>Notice the returned completable future has to be completed by
	 * {@link SwingUtilities#invokeLater(Runnable)}. Hence, do not try to
	 * join on the future in gui, but rather call {@code thenXxx} methods.
	 *
	 * @param path the path of the save
	 * @param format the format of the save
	 * @return the future of saving
	 */
	public CompletableFuture<Void> saveMappings(Path path, MappingFormat format) {
		if (this.project == null) return CompletableFuture.completedFuture(null);

		return ProgressDialog.runOffThread(this.gui.getFrame(), progress -> {
			EntryRemapper mapper = this.project.getMapper();
			MappingSaveParameters saveParameters = this.enigma.getProfile().getMappingSaveParameters();

			MappingDelta<EntryMapping> delta = mapper.takeMappingDelta();
			boolean saveAll = !path.equals(this.loadedMappingPath);

			this.loadedMappingFormat = format;
			this.loadedMappingPath = path;

			if (saveAll) {
				format.write(mapper.getObfToDeobf(), path, progress, saveParameters);
			} else {
				format.write(mapper.getObfToDeobf(), delta, path, progress, saveParameters);
			}
		});
	}

	public void closeMappings() {
		if (this.project == null) return;

		this.project.setMappings(null);

		this.gui.setMappingsFile(null);
		refreshClasses();
		this.chp.invalidateJavadoc();
	}

	public void reloadAll() {
		Path jarPath = this.project.getJarPath();
		if (jarPath != null) {
			this.closeJar();
			CompletableFuture<Void> f = this.openJar(jarPath);
			if (this.loadedMappingFormat != null && this.loadedMappingPath != null) {
				f.whenComplete((v, t) -> this.openMappings(this.loadedMappingFormat, this.loadedMappingPath));
			}
		}
	}

	public void reloadMappings() {
		if (this.loadedMappingFormat != null && this.loadedMappingPath != null) {
			this.closeMappings();
			this.openMappings(this.loadedMappingFormat, this.loadedMappingPath);
		}
	}

	public CompletableFuture<Void> dropMappings() {
		if (this.project == null) return CompletableFuture.completedFuture(null);

		return ProgressDialog.runOffThread(this.gui.getFrame(), progress -> this.project.dropMappings(progress));
	}

	public CompletableFuture<Void> exportSource(final Path path) {
		if (this.project == null) return CompletableFuture.completedFuture(null);

		return ProgressDialog.runOffThread(this.gui.getFrame(), progress -> {
			EnigmaProject.JarExport jar = this.project.exportRemappedJar(progress);
			jar.decompileStream(progress, this.chp.getDecompilerService(), EnigmaProject.DecompileErrorStrategy.TRACE_AS_SOURCE)
					.forEach(source -> {
						try {
							source.writeTo(source.resolvePath(path));
						} catch (IOException e) {
							e.printStackTrace();
						}
					});
		});
	}

	public CompletableFuture<Void> exportJar(final Path path) {
		if (this.project == null) return CompletableFuture.completedFuture(null);

		return ProgressDialog.runOffThread(this.gui.getFrame(), progress -> {
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
	 * Navigates to the declaration with respect to navigation history
	 *
	 * @param entry the entry whose declaration will be navigated to
	 */
	public void openDeclaration(Entry<?> entry) {
		if (entry == null) {
			throw new IllegalArgumentException("Entry cannot be null!");
		}
		openReference(EntryReference.declaration(entry, entry.getName()));
	}

	/**
	 * Navigates to the reference with respect to navigation history
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
		EntryRemapper mapper = this.project.getMapper();

		SourceIndex index = source.getIndex();
		return mapper.getObfResolver().resolveReference(reference, ResolutionStrategy.RESOLVE_CLOSEST)
				.stream()
				.flatMap(r -> index.getReferenceTokens(r).stream())
				.sorted()
				.toList();
	}

	public void openPreviousReference() {
		if (hasPreviousReference()) {
			this.gui.showReference(this.referenceHistory.goBack());
		}
	}

	public boolean hasPreviousReference() {
		return this.referenceHistory != null && this.referenceHistory.canGoBack();
	}

	public void openNextReference() {
		if (hasNextReference()) {
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
		openDeclaration(entry);
	}

	public void navigateTo(EntryReference<Entry<?>, Entry<?>> reference) {
		if (!this.project.isNavigable(reference.getLocationClassEntry())) {
			return;
		}
		openReference(reference);
	}

	public void refreshClasses() {
		if (this.project == null) return;

		List<ClassEntry> obfClasses = Lists.newArrayList();
		List<ClassEntry> deobfClasses = Lists.newArrayList();
		this.addSeparatedClasses(obfClasses, deobfClasses);
		this.gui.setObfClasses(obfClasses);
		this.gui.setDeobfClasses(deobfClasses);
	}

	public void addSeparatedClasses(List<ClassEntry> obfClasses, List<ClassEntry> deobfClasses) {
		EntryRemapper mapper = this.project.getMapper();

		Collection<ClassEntry> classes = this.project.getJarIndex().getEntryIndex().getClasses();
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
		Translator translator = this.project.getMapper().getDeobfuscator();
		ClassInheritanceTreeNode rootNode = this.indexTreeBuilder.buildClassInheritance(translator, entry);
		return ClassInheritanceTreeNode.findNode(rootNode, entry);
	}

	public ClassImplementationsTreeNode getClassImplementations(ClassEntry entry) {
		Translator translator = this.project.getMapper().getDeobfuscator();
		return this.indexTreeBuilder.buildClassImplementations(translator, entry);
	}

	public MethodInheritanceTreeNode getMethodInheritance(MethodEntry entry) {
		Translator translator = this.project.getMapper().getDeobfuscator();
		MethodInheritanceTreeNode rootNode = indexTreeBuilder.buildMethodInheritance(translator, entry);
		return MethodInheritanceTreeNode.findNode(rootNode, entry);
	}

	public MethodImplementationsTreeNode getMethodImplementations(MethodEntry entry) {
		Translator translator = this.project.getMapper().getDeobfuscator();
		List<MethodImplementationsTreeNode> rootNodes = this.indexTreeBuilder.buildMethodImplementations(translator, entry);
		if (rootNodes.isEmpty()) {
			return null;
		}
		if (rootNodes.size() > 1) {
			System.err.println("WARNING: Method " + entry + " implements multiple interfaces. Only showing first one.");
		}
		return MethodImplementationsTreeNode.findNode(rootNodes.get(0), entry);
	}

	public ClassReferenceTreeNode getClassReferences(ClassEntry entry) {
		Translator deobfuscator = this.project.getMapper().getDeobfuscator();
		ClassReferenceTreeNode rootNode = new ClassReferenceTreeNode(deobfuscator, entry);
		rootNode.load(this.project.getJarIndex(), true);
		return rootNode;
	}

	public FieldReferenceTreeNode getFieldReferences(FieldEntry entry) {
		Translator translator = this.project.getMapper().getDeobfuscator();
		FieldReferenceTreeNode rootNode = new FieldReferenceTreeNode(translator, entry);
		rootNode.load(this.project.getJarIndex(), true);
		return rootNode;
	}

	public MethodReferenceTreeNode getMethodReferences(MethodEntry entry, boolean recursive) {
		Translator translator = this.project.getMapper().getDeobfuscator();
		MethodReferenceTreeNode rootNode = new MethodReferenceTreeNode(translator, entry);
		rootNode.load(this.project.getJarIndex(), true, recursive);
		return rootNode;
	}

	@Override
	public boolean applyChangeFromServer(EntryChange<?> change) {
		ValidationContext vc = new ValidationContext();
		vc.setActiveElement(PrintValidatable.INSTANCE);
		this.applyChange0(vc, change);
		this.gui.updateStructure(this.gui.getActiveEditor());

		return vc.canProceed();
	}

	public void validateChange(ValidationContext vc, EntryChange<?> change) {
		if (change.getDeobfName().isSet()) {
			EntryValidation.validateRename(vc, this.project, change.getTarget(), change.getDeobfName().getNewValue());
		}

		if (change.getJavadoc().isSet()) {
			EntryValidation.validateJavadoc(vc, change.getJavadoc().getNewValue());
		}
	}

	public void applyChange(ValidationContext vc, EntryChange<?> change) {
		this.applyChange0(vc, change);
		this.gui.updateStructure(this.gui.getActiveEditor());
		if (!vc.canProceed()) return;
		this.sendPacket(new EntryChangeC2SPacket(change));
	}

	private void applyChange0(ValidationContext vc, EntryChange<?> change) {
		validateChange(vc, change);
		if (!vc.canProceed()) return;

		Entry<?> target = change.getTarget();
		EntryMapping prev = this.project.getMapper().getDeobfMapping(target);
		EntryMapping mapping = EntryUtil.applyChange(vc, this.project.getMapper(), change);

		boolean renamed = !change.getDeobfName().isUnchanged();

		if (renamed && target instanceof ClassEntry classEntry && !classEntry.isInnerClass()) {
			this.gui.moveClassTree(target, prev.targetName() == null, mapping.targetName() == null);
		}

		if (!Objects.equals(prev.targetName(), mapping.targetName())) {
			this.chp.invalidateMapped();
		}

		if (!Objects.equals(prev.javadoc(), mapping.javadoc())) {
			this.chp.invalidateJavadoc(target.getTopLevelClass());
		}

		this.gui.updateStructure(this.gui.getActiveEditor());
	}

	public void openStats(Set<StatsMember> includedMembers, String topLevelPackage, boolean includeSynthetic) {
		ProgressDialog.runOffThread(this.gui.getFrame(), progress -> {
			String data = new StatsGenerator(this.project).generate(progress, includedMembers, topLevelPackage, includeSynthetic).getTreeJson();

			try {
				File statsFile = File.createTempFile("stats", ".html");

				try (FileWriter w = new FileWriter(statsFile)) {
					w.write(
							Utils.readResourceToString("/stats.html")
									.replace("/*data*/", data)
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

	public void createClient(String username, String ip, int port, char[] password) throws IOException {
		client = new EnigmaClient(this, ip, port);
		client.connect();
		client.sendPacket(new LoginC2SPacket(this.project.getJarChecksum(), password, username));
		gui.setConnectionState(ConnectionState.CONNECTED);
	}

	public void createServer(int port, char[] password) throws IOException {
		this.server = new IntegratedEnigmaServer(this.project.getJarChecksum(), password, EntryRemapper.mapped(this.project.getJarIndex(), new HashEntryTree<>(this.project.getMapper().getObfToDeobf())), port);
		this.server.start();
		this.client = new EnigmaClient(this, "127.0.0.1", port);
		this.client.connect();
		this.client.sendPacket(new LoginC2SPacket(this.project.getJarChecksum(), password, NetConfig.getUsername()));
		this.gui.setConnectionState(ConnectionState.HOSTING);
	}

	@Override
	public synchronized void disconnectIfConnected(String reason) {
		if (this.client == null && this.server == null) {
			return;
		}

		if (this.client != null) {
			this.client.disconnect();
		}
		if (this.server != null) {
			this.server.stop();
		}
		this.client = null;
		this.server = null;
		SwingUtilities.invokeLater(() -> {
			if (reason != null) {
				JOptionPane.showMessageDialog(this.gui.getFrame(), I18n.translate(reason), I18n.translate("disconnect.disconnected"), JOptionPane.INFORMATION_MESSAGE);
			}
			this.gui.setConnectionState(ConnectionState.NOT_CONNECTED);
		});
	}

	@Override
	public void sendPacket(Packet<ServerPacketHandler> packet) {
		if (this.client != null) {
			this.client.sendPacket(packet);
		}
	}

	@Override
	public void addMessage(Message message) {
		this.gui.addMessage(message);
	}

	@Override
	public void updateUserList(List<String> users) {
		this.gui.setUserList(users);
	}
}
