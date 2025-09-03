package org.quiltmc.enigma.util;

import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Utils {
	public static String readStreamToString(InputStream in) throws IOException {
		return CharStreams.toString(new InputStreamReader(in, StandardCharsets.UTF_8));
	}

	public static String readResourceToString(String path) throws IOException {
		InputStream in = Utils.class.getResourceAsStream(path);
		if (in == null) {
			throw new IllegalArgumentException("Resource not found! " + path);
		}

		return readStreamToString(in);
	}

	public static Properties readResourceToProperties(String path) throws IOException {
		InputStream in = Utils.class.getResourceAsStream(path);
		if (in == null) {
			throw new IllegalStateException("Resource not found! " + path);
		}

		Properties properties = new Properties();
		properties.load(in);
		return properties;
	}

	public static void delete(Path path) throws IOException {
		if (Files.exists(path)) {
			for (Path p : Files.walk(path).sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(p);
			}
		}
	}

	public static byte[] zipSha1(Path path) throws IOException {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			// Algorithm guaranteed to be supported
			throw new RuntimeException(e);
		}

		try (ZipFile zip = new ZipFile(path.toFile())) {
			List<? extends ZipEntry> entries = Collections.list(zip.entries());
			// only compare classes (some implementations may not generate directory entries)
			entries.removeIf(entry -> !entry.getName().toLowerCase(Locale.ROOT).endsWith(".class"));
			// different implementations may add zip entries in a different order
			entries.sort(Comparator.comparing(ZipEntry::getName));
			byte[] buffer = new byte[8192];
			for (ZipEntry entry : entries) {
				digest.update(entry.getName().getBytes(StandardCharsets.UTF_8));
				try (InputStream in = zip.getInputStream(entry)) {
					int n;
					while ((n = in.read(buffer)) != -1) {
						digest.update(buffer, 0, n);
					}
				}
			}
		}

		return digest.digest();
	}

	public static void withLock(Lock l, Runnable op) {
		try {
			l.lock();
			op.run();
		} finally {
			l.unlock();
		}
	}

	public static <R> R withLock(Lock l, Supplier<R> op) {
		try {
			l.lock();
			return op.get();
		} finally {
			l.unlock();
		}
	}

	public static String andJoin(String... words) {
		return andJoin(true, words);
	}

	public static String andJoin(boolean oxfordComma, String... words) {
		return andJoin(oxfordComma, Arrays.asList(words));
	}

	public static String andJoin(List<String> words) {
		return andJoin(true, words);
	}

	public static String andJoin(boolean oxfordComma, List<String> words) {
		return naturalJoin(" and ", oxfordComma, words);
	}

	/**
	 * @param finalSeparator the separator used between the last two {@code words}
	 * @param oxfordComma whether to include a comma before the {@code finalSeparator}
	 * 						in cases of three or more {@code words}
	 * @param words the words to join
	 * @return the passed {@code words} joined as an english language list
	 */
	public static String naturalJoin(String finalSeparator, boolean oxfordComma, List<String> words) {
		final int count = words.size();
		return switch (count) {
			case 0 -> "";
			case 1 -> words.get(0);
			case 2 -> words.get(0) + finalSeparator + words.get(1);
			default -> {
				final StringBuilder joined = new StringBuilder(words.get(0));
				final int lastIndex = count - 1;
				for (final String word : words.subList(1, lastIndex)) {
					joined.append(", ").append(word);
				}

				if (oxfordComma) {
					joined.append(",");
				}

				joined.append(finalSeparator).append(words.get(lastIndex));

				yield joined.toString();
			}
		};
	}
}
