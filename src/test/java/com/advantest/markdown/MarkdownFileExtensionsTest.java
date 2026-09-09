/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2026 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MarkdownFileExtensions}, the one place saying what this library calls a Markdown file.
 */
class MarkdownFileExtensionsTest {

	@Test
	void acceptsAFileNameCarryingTheMarkdownExtension() {
		assertTrue(MarkdownFileExtensions.isMarkdownFile("readme.md"));
	}

	@Test
	void acceptsAPathCarryingTheMarkdownExtension() {
		assertTrue(MarkdownFileExtensions.isMarkdownFile("../docs/guide.md"));
		assertTrue(MarkdownFileExtensions.isMarkdownFile("C:\\docs\\guide.md"));
	}

	@Test
	void acceptsAFileCarryingTheMarkdownExtension() {
		assertTrue(MarkdownFileExtensions.isMarkdownFile(new File("readme.md")));
	}

	@Test
	void refusesAnotherExtension() {
		assertFalse(MarkdownFileExtensions.isMarkdownFile("readme.txt"));
		assertFalse(MarkdownFileExtensions.isMarkdownFile("diagram.puml"));
		assertFalse(MarkdownFileExtensions.isMarkdownFile("readme.markdown"));
	}

	@Test
	void acceptsTheExtensionHoweverItIsWritten() {
		assertTrue(MarkdownFileExtensions.isMarkdownFile("readme.MD"));
		assertTrue(MarkdownFileExtensions.isMarkdownFile("readme.Md"));
	}

	@Test
	void refusesANameThatOnlyEndsWithTheExtensionsLetters() {
		assertFalse(MarkdownFileExtensions.isMarkdownFile("readme"));
		assertFalse(MarkdownFileExtensions.isMarkdownFile("readmemd"));
		assertFalse(MarkdownFileExtensions.isMarkdownFile("md"));
	}

	@Test
	void refusesNothing() {
		assertFalse(MarkdownFileExtensions.isMarkdownFile((String) null));
		assertFalse(MarkdownFileExtensions.isMarkdownFile((File) null));
	}

	@Test
	void readsWhatFollowsTheLastDot() {
		assertEquals("md", MarkdownFileExtensions.getFileExtension("readme.md"));
		assertEquals("gz", MarkdownFileExtensions.getFileExtension("archive.tar.gz"));
	}

	@Test
	void readsNoExtensionWhereThereIsNone() {
		assertNull(MarkdownFileExtensions.getFileExtension("readme"));
		assertNull(MarkdownFileExtensions.getFileExtension("readme."));
		assertNull(MarkdownFileExtensions.getFileExtension(""));
		assertNull(MarkdownFileExtensions.getFileExtension(null));
	}

	@Test
	void readsTheNameOfAHiddenFileAsAName() {
		assertNull(MarkdownFileExtensions.getFileExtension(".gitignore"));
		assertNull(MarkdownFileExtensions.getFileExtension("docs/.gitignore"));
	}

	@Test
	void doesNotMistakeADotOfADirectoryForAnExtension() {
		assertNull(MarkdownFileExtensions.getFileExtension("../v1.0/guide"));
		assertEquals("md", MarkdownFileExtensions.getFileExtension("../v1.0/guide.md"));
		assertTrue(MarkdownFileExtensions.isMarkdownFile("../v1.0/guide.md"));
		assertFalse(MarkdownFileExtensions.isMarkdownFile("../v1.md/guide"));
	}

}
