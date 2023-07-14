package cuchaz.enigma.source.vineflower;

import cuchaz.enigma.source.SourceIndex;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.util.jar.Manifest;

public class EnigmaResultSaver implements IResultSaver {
	private final SourceIndex index;

	public EnigmaResultSaver(SourceIndex index) {
		this.index = index;
	}

	@Override
	public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
		this.index.setSource(content);
	}

	@Override
	public void saveFolder(String path) {
	}

	@Override
	public void copyFile(String source, String path, String entryName) {
	}

	@Override
	public void createArchive(String path, String archiveName, Manifest manifest) {
	}

	@Override
	public void saveDirEntry(String path, String archiveName, String entryName) {
	}

	@Override
	public void copyEntry(String source, String path, String archiveName, String entry) {
	}

	@Override
	public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
	}

	@Override
	public void closeArchive(String path, String archiveName) {
	}
}
