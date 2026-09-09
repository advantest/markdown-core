/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2026 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.markdown;

import java.io.File;

/**
 * What this library calls a Markdown file.
 * 
 * <p>Everybody asking whether something is Markdown asks here, so that the answer is written down
 * once. A caller writing the extension itself would decide the same question a second time, and
 * the two answers would drift apart the day another extension is accepted.</p>
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

	private MarkdownFileExtensions() {
		// This class is a place to ask, not something to create.
	}

	/**
	 * Tells whether the given file is named like a Markdown file.
	 * 
	 * @param file the file to be decided about, may be <code>null</code>
	 * @return <code>true</code> if it is not <code>null</code> and carries the
	 *         {@link #FILE_EXTENSION Markdown file extension}
	 */
	public static boolean isMarkdownFile(File file) {
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
	 * @return <code>true</code> if it is not <code>null</code> and carries the
	 *         {@link #FILE_EXTENSION Markdown file extension}
	 */
	public static boolean isMarkdownFile(String path) {
		return FILE_EXTENSION.equalsIgnoreCase(getFileExtension(path));
	}

	/**
	 * Reads the extension out of the given path or file name.
	 * 
	 * <p>The extension is what follows the last dot. A name without a dot, one ending with a dot
	 * and one that is nothing but a dot have no extension; a name starting with a dot and having
	 * no other, e.g. <code>.gitignore</code>, is a name and not an extension.</p>
	 * 
	 * @param path the path or file name to be read, may be <code>null</code>
	 * @return the extension without the separating dot, or <code>null</code> if the given path has
	 *         none
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

}
