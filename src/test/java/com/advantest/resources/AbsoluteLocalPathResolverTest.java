/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2026 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link AbsoluteLocalPathResolver}, i.e. for a path that leads to the same file
 * wherever the document naming it is.
 */
class AbsoluteLocalPathResolverTest {

	@TempDir
	private Path tempDir;

	private final AbsoluteLocalPathResolver resolver = new AbsoluteLocalPathResolver();

	private Resource document;

	private Path target;

	@BeforeEach
	void createDocumentAndTarget() throws IOException {
		Path documentPath = this.tempDir.resolve("docs").resolve("readme.md");
		Files.createDirectories(documentPath.getParent());
		Files.writeString(documentPath, "# Title");
		this.document = LocalFileSystemResource.of(documentPath);

		this.target = this.tempDir.resolve("guide.md");
		Files.writeString(this.target, "# Guide");
	}

	@Test
	void findsAFileNamedByItsAbsolutePath() {
		Resource resolved = this.resolver.resolve(this.target.toString(), this.document);

		assertTrue(resolved.exists());
		assertTrue(resolved.isFile());
		assertEquals(this.target.toString(), resolved.getResolvedPath());
	}

	@Test
	void findsTheSameFileWhereverTheDocumentIs() {
		Resource resolvedFromKnownDocument = this.resolver.resolve(this.target.toString(), this.document);
		Resource resolvedFromNowhere =
				this.resolver.resolve(this.target.toString(), UnresolvedResource.UNKNOWN_DOCUMENT);

		assertEquals(resolvedFromKnownDocument, resolvedFromNowhere);
	}

	@Test
	void findsAFileNamedByItsAbsolutePathAndAPlaceInsideIt() {
		Resource resolved = this.resolver.resolve(this.target + "#a-section", this.document);

		assertTrue(resolved.exists());
		assertEquals(this.target.toString(), resolved.getResolvedPath());
	}

	@Test
	void reportsAnAbsolutePathOfAFileThatIsNotThereAsMissing() {
		String missing = this.tempDir.resolve("nowhere.md").toString();

		Resource resolved = this.resolver.resolve(missing, this.document);

		assertFalse(resolved.exists());
		assertEquals(missing, resolved.getResolvedPath());
	}

	@Test
	void leavesAPathThatIsNotAbsoluteUnresolved() {
		Resource resolved = this.resolver.resolve("../guide.md", this.document);

		assertFalse(resolved.exists());
		assertEquals("../guide.md", resolved.getResolvedPath());
	}

	@Test
	void leavesAReferenceNamingOnlyAPlaceInsideTheDocumentUnresolved() {
		Resource resolved = this.resolver.resolve("#a-section", this.document);

		assertFalse(resolved.exists());
		assertEquals("#a-section", resolved.getResolvedPath());
	}

	@Test
	void rejectsMissingArguments() {
		assertThrows(IllegalArgumentException.class, () -> this.resolver.resolve(null, this.document));
		assertThrows(IllegalArgumentException.class, () -> this.resolver.resolve("  ", this.document));
		assertThrows(IllegalArgumentException.class,
				() -> this.resolver.resolve(this.target.toString(), null));
	}

}
