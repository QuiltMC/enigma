/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *	 Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma;

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.classprovider.CachingClassProvider;
import cuchaz.enigma.classprovider.ClassProvider;
import cuchaz.enigma.classprovider.JarClassProvider;
import cuchaz.enigma.source.*;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.utils.Pair;
import org.tinylog.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TokenChecker {
	private static final Map<Pair<DecompilerService, Path>, Set<String>> ALL_SHOWN_FILES = new HashMap<>();

	private final Decompiler decompiler;
	private final Set<String> shownFiles;

	protected TokenChecker(Path path, DecompilerService decompilerService) throws IOException {
		this(path, decompilerService, new CachingClassProvider(new JarClassProvider(path)));
	}

	protected TokenChecker(Path path, DecompilerService decompilerService, ClassProvider classProvider) {
		decompiler = decompilerService.create(classProvider, new SourceSettings(false, false));
		shownFiles = ALL_SHOWN_FILES.computeIfAbsent(new Pair<>(decompilerService, path), p -> new HashSet<>());
	}

	protected String getDeclarationToken(Entry<?> entry) {
		// decompile the class
		Source source = decompiler.getSource(entry.getContainingClass().getFullName());
		// DEBUG
		// createDebugFile(source, entry.getContainingClass());
		String string = source.asString();
		SourceIndex index = source.index();

		// get the token value
		Token token = index.getDeclarationToken(entry);
		if (token == null) {
			return null;
		}
		return string.substring(token.start, token.end);
	}

	@SuppressWarnings("unchecked")
	protected Collection<String> getReferenceTokens(EntryReference<? extends Entry<?>, ? extends Entry<?>> reference) {
		// decompile the class
		Source source = decompiler.getSource(reference.context.getContainingClass().getFullName());
		String string = source.asString();
		SourceIndex index = source.index();
		// DEBUG
		// createDebugFile(source, reference.context.getContainingClass());

		// get the token values
		List<String> values = new ArrayList<>();
		for (Token token : index.getReferenceTokens((EntryReference<Entry<?>, Entry<?>>) reference)) {
			values.add(string.substring(token.start, token.end));
		}
		return values;
	}

	private void createDebugFile(Source source, ClassEntry classEntry) {
		if (!shownFiles.add(classEntry.getFullName())) {
			return;
		}

		try {
			String name = classEntry.getContextualName();
			Path path = Files.createTempFile("class-" + name.replace("$", "_") + "-", ".html");
			Files.writeString(path, SourceTestUtil.toHtml(source, name));
			Logger.info("Debug file created: {}", path.toUri());
		} catch (Exception e) {
			Logger.error(e, "Failed to create debug source file for {}", classEntry);
		}
	}
}
