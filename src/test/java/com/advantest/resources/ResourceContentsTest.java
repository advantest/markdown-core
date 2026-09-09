/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2026 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Resource#readAllContents()} and {@link Resource#readContents()}, the two ways a
 * caller looks into a document it did not parse itself.
 */
class ResourceContentsTest {

	@TempDir
	private Path tempDir;

	@Test
	void readsTheTextOfAFile() throws IOException {
		Path file = this.tempDir.resolve("readme.md");
		Files.writeString(file, "# Title\n\nA paragraph.\n");

		assertEquals("# Title\n\nA paragraph.\n", LocalFileSystemResource.of(file).readAllContents());
	}

	@Test
	void readsAnEmptyFileAsAnEmptyText() throws IOException {
		Path file = this.tempDir.resolve("empty.md");
		Files.writeString(file, "");

		assertEquals("", LocalFileSystemResource.of(file).readAllContents());
	}

	@Test
	void readsTheBytesAsUtf8WhateverTheMachineWouldSay() throws IOException {
		Path file = this.tempDir.resolve("umlauts.md");
		Files.write(file, "# Überschrift mit Größe".getBytes(StandardCharsets.UTF_8));

		assertEquals("# Überschrift mit Größe", LocalFileSystemResource.of(file).readAllContents());
	}

	@Test
	void doesNotReadAFileThatIsNotThere() {
		Resource resource = LocalFileSystemResource.of(this.tempDir.resolve("absent.md"));

		assertThrows(IOException.class, resource::readAllContents);
		assertThrows(IOException.class, resource::readContents);
	}

	@Test
	void doesNotReadADirectoryAndSaysThatItIsOne() throws IOException {
		Path directory = this.tempDir.resolve("images");
		Files.createDirectories(directory);

		Resource resource = LocalFileSystemResource.of(directory);

		IOException failure = assertThrows(IOException.class, resource::readAllContents);
		assertTrue(failure.getMessage().contains("is a directory"), failure.getMessage());
		assertTrue(failure.getMessage().contains(directory.toString()), failure.getMessage());
		assertThrows(IOException.class, resource::readContents);
	}

	@Test
	void doesNotReadBytesThatAreNoText() throws IOException {
		Path file = this.tempDir.resolve("picture.png");
		Files.write(file, new byte[] { (byte) 0xC3, (byte) 0x28, (byte) 0xA0, (byte) 0xA1 });

		Resource resource = LocalFileSystemResource.of(file);

		assertThrows(IOException.class, resource::readAllContents);
	}

	@Test
	void doesNotReadAResourceNobodyResolvedAndNamesWhatWasSearchedFor() {
		Resource resource = new UnresolvedResource("../guide.md");

		IOException failure = assertThrows(IOException.class, resource::readAllContents);
		assertTrue(failure.getMessage().contains("../guide.md"), failure.getMessage());
		assertThrows(IOException.class, resource::readContents);
	}

	@Test
	void doesNotReadTheDocumentWhoseLocationIsUnknown() {
		assertThrows(IOException.class, UnresolvedResource.UNKNOWN_DOCUMENT::readAllContents);
		assertThrows(IOException.class, UnresolvedResource.UNKNOWN_DOCUMENT::readContents);
	}

	@Test
	void readsTheSameFileTwiceAndSeesTheChangeInBetween() throws IOException {
		Path file = this.tempDir.resolve("changing.md");
		Files.writeString(file, "before");

		Resource resource = LocalFileSystemResource.of(file);
		assertEquals("before", resource.readAllContents());

		Files.writeString(file, "after");
		assertEquals("after", resource.readAllContents());
	}

	@Test
	void handsOutAReaderOfTheWholeText() throws IOException {
		Path file = this.tempDir.resolve("readme.md");
		Files.writeString(file, "# Title\n\nA paragraph.\n");

		try (BufferedReader reader = LocalFileSystemResource.of(file).readContents()) {
			assertEquals(List.of("# Title", "", "A paragraph."), reader.lines().toList());
		}
	}

	@Test
	void readsOnlyTheBeginningOfATextTooBigToHold() throws IOException {
		Path file = this.tempDir.resolve("huge.md");
		Files.writeString(file, "# Title\n" + "a line of text\n".repeat(200_000));

		try (BufferedReader reader = LocalFileSystemResource.of(file).readContents()) {
			assertEquals("# Title", reader.readLine());
		}
	}

	@Test
	void tellsAboutBytesThatAreNoTextWhileTheReaderIsRead() throws IOException {
		Path file = this.tempDir.resolve("picture.png");
		Files.write(file, new byte[] { (byte) 0xC3, (byte) 0x28, (byte) 0xA0, (byte) 0xA1 });

		try (BufferedReader reader = LocalFileSystemResource.of(file).readContents()) {
			assertThrows(IOException.class, reader::readLine);
		}
	}

}
