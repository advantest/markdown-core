/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2026 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.markdown;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * What this library calls a Markdown file.
 * 
 * <p>Everybody asking whether something is Markdown asks an instance of this class, so that the
 * answer is written down once. A caller comparing a file extension itself would decide the same
 * question a second time, and the two answers would drift apart the day another extension is
 * accepted.</p>
 * 
 * <p>{@link #DEFAULT} accepts <code>md</code>, which is what a document written for this library
 * carries. A tool knowing better which files its users edit as Markdown says so with
 * {@link #accepting(String...)}, e.g. an editor that also opens <code>mdown</code> and
 * <code>mkd</code>. An instance is immutable and is therefore shared freely; which instance is
 * asked is decided where the parser is configured, not by a setting somewhere in the background
 * that a second caller in the same process would change underneath the first.</p>
 * 
 * <p>The decision is read from the name, not from what a file contains: a caller asks before it
 * reads, and a target of a link is often not a file of this machine at all, so its name is all
 * there is. How the extension is written does not matter, i.e. <code>guide.MD</code> is a Markdown
 * file as much as <code>guide.md</code> is, because a file system that distinguishes case and one
 * that does not would otherwise disagree about the same name.</p>
 */
public final class MarkdownFileExtensions {

	/**
	 * The file extension of a Markdown file, without the separating dot.
	 */
	public static final String FILE_EXTENSION = "md";

	/**
	 * Accepts the {@link #FILE_EXTENSION one extension} this library writes its own documents
	 * with, and nothing else.
	 */
	public static final MarkdownFileExtensions DEFAULT = new MarkdownFileExtensions(Set.of(FILE_EXTENSION));

	private final Set<String> acceptedFileExtensions;

	private MarkdownFileExtensions(Collection<String> acceptedFileExtensions) {
		Set<String> extensions = new LinkedHashSet<>();

		for (String extension : acceptedFileExtensions) {
			extensions.add(normalize(extension));
		}

		this.acceptedFileExtensions = Set.copyOf(extensions);
	}

	/**
	 * Answers for the given file extensions instead of the default one.
	 * 
	 * <p>An extension is given with or without its leading dot and however it is written, e.g.
	 * <code>md</code>, <code>.md</code> and <code>MD</code> all name the same one.</p>
	 * 
	 * @param fileExtensions the file extensions to be accepted as Markdown, must not be
	 *                       <code>null</code>, must not be empty and must not contain an empty
	 *                       entry
	 * @return what accepts exactly those extensions, never <code>null</code>
	 * @throws IllegalArgumentException if no extension, an empty one or <code>null</code> is given
	 */
	public static MarkdownFileExtensions accepting(String... fileExtensions) {
		if (fileExtensions == null) {
			throw new IllegalArgumentException("The file extensions must not be null.");
		}

		return accepting(Arrays.asList(fileExtensions));
	}

	/**
	 * Answers for the given file extensions instead of the default one.
	 * 
	 * @param fileExtensions the file extensions to be accepted as Markdown, must not be
	 *                       <code>null</code>, must not be empty and must not contain an empty
	 *                       entry
	 * @return what accepts exactly those extensions, never <code>null</code>
	 * @throws IllegalArgumentException if no extension, an empty one or <code>null</code> is given
	 * @see #accepting(String...)
	 */
	public static MarkdownFileExtensions accepting(Collection<String> fileExtensions) {
		if (fileExtensions == null || fileExtensions.isEmpty()) {
			throw new IllegalArgumentException("At least one file extension must be given.");
		}

		return new MarkdownFileExtensions(fileExtensions);
	}

	/**
	 * Tells which file extensions are accepted as Markdown, without their separating dot and
	 * written in lower case.
	 * 
	 * @return the accepted extensions, never <code>null</code>, never empty and not modifiable
	 */
	public Set<String> getAcceptedFileExtensions() {
		return this.acceptedFileExtensions;
	}

	/**
	 * Tells whether the given file is named like a Markdown file.
	 * 
	 * @param file the file to be decided about, may be <code>null</code>
	 * @return <code>true</code> if it is not <code>null</code> and carries one of the
	 *         {@link #getAcceptedFileExtensions() accepted file extensions}
	 */
	public boolean isMarkdownFile(File file) {
		return file != null && isMarkdownFile(file.getName());
	}

	/**
	 * Tells whether the given path is named like a Markdown file.
	 * 
	 * <p>The path is taken as it is written, e.g. as it stands in the target of a link, and only
	 * its extension is looked at, however that is written. Whether anything is there under that
	 * path is a different question and is not asked here.</p>
	 * 
	 * @param path the path or file name to be decided about, may be <code>null</code>
	 * @return <code>true</code> if it is not <code>null</code> and carries one of the
	 *         {@link #getAcceptedFileExtensions() accepted file extensions}
	 */
	public boolean isMarkdownFile(String path) {
		String fileExtension = getFileExtension(path);

		return fileExtension != null && this.acceptedFileExtensions.contains(normalize(fileExtension));
	}

	/**
	 * Reads the extension out of the given path or file name.
	 * 
	 * <p>The extension is what follows the last dot. A name without a dot, one ending with a dot
	 * and one that is nothing but a dot have no extension; a name starting with a dot and having
	 * no other, e.g. <code>.gitignore</code>, is a name and not an extension. What an extension is
	 * does not depend on which of them are accepted, so this question is asked of the class and
	 * not of an instance.</p>
	 * 
	 * @param path the path or file name to be read, may be <code>null</code>
	 * @return the extension as it is written, without the separating dot, or <code>null</code> if
	 *         the given path has none
	 */
	public static String getFileExtension(String path) {
		if (path == null) {
			return null;
		}

		String fileName = path.substring(indexAfterLastSeparator(path));
		int indexOfLastDot = fileName.lastIndexOf('.');

		if (indexOfLastDot < 1 || indexOfLastDot + 1 >= fileName.length()) {
			return null;
		}

		return fileName.substring(indexOfLastDot + 1);
	}

	/**
	 * Finds where the name begins in the given path, so that a dot in a directory further up,
	 * e.g. in <code>../v1.0/guide</code>, is not mistaken for the beginning of an extension. Both
	 * separators are looked for, because a path written in a document is not necessarily written
	 * the way this machine writes one.
	 */
	private static int indexAfterLastSeparator(String path) {
		return Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\')) + 1;
	}

	/**
	 * Brings an extension into the one form that is compared, so that a caller may write it as it
	 * appears in a file name and the comparison still holds.
	 */
	private static String normalize(String fileExtension) {
		if (fileExtension == null) {
			throw new IllegalArgumentException("A file extension must not be null.");
		}

		String normalized = fileExtension.trim();
		if (normalized.startsWith(".")) {
			normalized = normalized.substring(1);
		}

		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("A file extension must not be empty.");
		}

		return normalized.toLowerCase(Locale.ROOT);
	}

	@Override
	public String toString() {
		return "Markdown file extensions: " + this.acceptedFileExtensions;
	}

}
