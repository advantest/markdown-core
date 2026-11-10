/*
 * Copyright © 2026 Advantest Europe GmbH
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.advantest.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MarkdownFileExtensions}, the one place saying what this library calls a Markdown file.
 */
class MarkdownFileExtensionsTest {

	@Test
	void acceptsAFileNameCarryingTheMarkdownExtension() {
		assertTrue(MarkdownFileExtensions.DEFAULT.isMarkdownFile("readme.md"));
	}

	@Test
	void acceptsAPathCarryingTheMarkdownExtension() {
		assertTrue(MarkdownFileExtensions.DEFAULT.isMarkdownFile("../docs/guide.md"));
		assertTrue(MarkdownFileExtensions.DEFAULT.isMarkdownFile("C:\\docs\\guide.md"));
	}

	@Test
	void acceptsAFileCarryingTheMarkdownExtension() {
		assertTrue(MarkdownFileExtensions.DEFAULT.isMarkdownFile(new File("readme.md")));
	}

	@Test
	void acceptsTheExtensionHoweverItIsWritten() {
		assertTrue(MarkdownFileExtensions.DEFAULT.isMarkdownFile("readme.MD"));
		assertTrue(MarkdownFileExtensions.DEFAULT.isMarkdownFile("readme.Md"));
	}

	@Test
	void refusesAnotherExtension() {
		assertFalse(MarkdownFileExtensions.DEFAULT.isMarkdownFile("readme.txt"));
		assertFalse(MarkdownFileExtensions.DEFAULT.isMarkdownFile("diagram.puml"));
		assertFalse(MarkdownFileExtensions.DEFAULT.isMarkdownFile("readme.markdown"));
	}

	@Test
	void refusesANameThatOnlyEndsWithTheExtensionsLetters() {
		assertFalse(MarkdownFileExtensions.DEFAULT.isMarkdownFile("readme"));
		assertFalse(MarkdownFileExtensions.DEFAULT.isMarkdownFile("readmemd"));
		assertFalse(MarkdownFileExtensions.DEFAULT.isMarkdownFile("md"));
	}

	@Test
	void refusesNothing() {
		assertFalse(MarkdownFileExtensions.DEFAULT.isMarkdownFile((String) null));
		assertFalse(MarkdownFileExtensions.DEFAULT.isMarkdownFile((File) null));
	}

	@Test
	void acceptsTheExtensionsACallerNames() {
		MarkdownFileExtensions markdownFileExtensions = MarkdownFileExtensions.accepting("md", "mdown", "mkd");

		assertTrue(markdownFileExtensions.isMarkdownFile("readme.md"));
		assertTrue(markdownFileExtensions.isMarkdownFile("readme.mdown"));
		assertTrue(markdownFileExtensions.isMarkdownFile("readme.MKD"));
		assertFalse(markdownFileExtensions.isMarkdownFile("readme.txt"));
	}

	@Test
	void refusesTheDefaultExtensionWhereACallerDidNotNameIt() {
		assertFalse(MarkdownFileExtensions.accepting("mdown").isMarkdownFile("readme.md"));
	}

	@Test
	void readsAnExtensionWithOrWithoutItsDotAndHoweverItIsWritten() {
		assertEquals(Set.of("md"), MarkdownFileExtensions.accepting(".md").getAcceptedFileExtensions());
		assertEquals(Set.of("md"), MarkdownFileExtensions.accepting("MD").getAcceptedFileExtensions());
		assertEquals(Set.of("md"), MarkdownFileExtensions.accepting(" .Md ").getAcceptedFileExtensions());
		assertEquals(Set.of("md", "mkd"), MarkdownFileExtensions.accepting("md", ".MKD").getAcceptedFileExtensions());
	}

	@Test
	void refusesToAcceptNothing() {
		assertThrows(IllegalArgumentException.class, () -> MarkdownFileExtensions.accepting());
		assertThrows(IllegalArgumentException.class, () -> MarkdownFileExtensions.accepting((String[]) null));
		assertThrows(IllegalArgumentException.class, () -> MarkdownFileExtensions.accepting(List.of()));
	}

	@Test
	void refusesAnExtensionThatIsNoneAtAll() {
		assertThrows(IllegalArgumentException.class, () -> MarkdownFileExtensions.accepting("md", ""));
		assertThrows(IllegalArgumentException.class, () -> MarkdownFileExtensions.accepting("md", "  "));
		assertThrows(IllegalArgumentException.class, () -> MarkdownFileExtensions.accepting("md", "."));
		assertThrows(IllegalArgumentException.class, () -> MarkdownFileExtensions.accepting("md", (String) null));
	}

	@Test
	void tellsWhichExtensionsAreAccepted() {
		assertEquals(Set.of("md"), MarkdownFileExtensions.DEFAULT.getAcceptedFileExtensions());
		assertEquals(Set.of("md", "mdown"), MarkdownFileExtensions.accepting("md", "mdown").getAcceptedFileExtensions());
	}

	@Test
	void readsWhatFollowsTheLastDot() {
		assertEquals("md", MarkdownFileExtensions.getFileExtension("readme.md"));
		assertEquals("MD", MarkdownFileExtensions.getFileExtension("readme.MD"));
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
		assertTrue(MarkdownFileExtensions.DEFAULT.isMarkdownFile("../v1.0/guide.md"));
		assertFalse(MarkdownFileExtensions.DEFAULT.isMarkdownFile("../v1.md/guide"));
	}

	@Test
	void refusesToAcceptNoCollectionOfExtensions() {
		assertThrows(IllegalArgumentException.class, () -> MarkdownFileExtensions.accepting((Collection<String>) null));
		assertThrows(IllegalArgumentException.class, () -> MarkdownFileExtensions.accepting(List.of()));
	}

	@Test
	void refusesNoFileAsAMarkdownFile() {
		assertFalse(MarkdownFileExtensions.accepting("md").isMarkdownFile((File) null));
	}

	@Test
	void namesTheAcceptedExtensionsWhenPrinted() {
		assertEquals("Markdown file extensions: [md]", MarkdownFileExtensions.accepting(".MD").toString());
	}

}
