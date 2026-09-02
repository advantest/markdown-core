/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2026 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.markdown.resources;

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
 * Tests for {@link LocalFileSystemResourceResolver} and the resources it creates,
 * {@link LocalFileSystemResource} and {@link UnresolvedResource}.
 */
class LocalFileSystemResourceResolverTest {

	@TempDir
	private Path tempDir;

	private final LocalFileSystemResourceResolver resolver = new LocalFileSystemResourceResolver();

	private Resource document;

	@BeforeEach
	void createDocument() throws IOException {
		Path documentPath = this.tempDir.resolve("docs").resolve("readme.md");
		Files.createDirectories(documentPath.getParent());
		Files.writeString(documentPath, "# Title");
		this.document = LocalFileSystemResource.of(documentPath);
	}

	@Test
	void resolvesAnExistingFileNextToTheDocument() throws IOException {
		Path neighbour = this.tempDir.resolve("docs").resolve("other.md");
		Files.writeString(neighbour, "# Other");

		Resource resolved = this.resolver.resolve("other.md", this.document);

		assertTrue(resolved.exists());
		assertTrue(resolved.isFile());
		assertFalse(resolved.isDirectory());
		assertEquals(ResourceKind.FILE, resolved.kind().orElseThrow());
		assertEquals(neighbour.toString(), resolved.resolvedPath());
	}

	@Test
	void resolvesAnExistingDirectory() throws IOException {
		Path directory = this.tempDir.resolve("images");
		Files.createDirectories(directory);

		Resource resolved = this.resolver.resolve("../images", this.document);

		assertTrue(resolved.exists());
		assertTrue(resolved.isDirectory());
		assertFalse(resolved.isFile());
		assertEquals(directory.toString(), resolved.resolvedPath());
	}

	@Test
	void resolvesADirectoryWrittenWithATrailingSeparator() throws IOException {
		Path directory = this.tempDir.resolve("images");
		Files.createDirectories(directory);

		Resource resolved = this.resolver.resolve("../images/", this.document);

		assertTrue(resolved.isDirectory());
		assertEquals(directory.toString(), resolved.resolvedPath());
	}

	@Test
	void namesAMissingTargetByItsResolvedPath() {
		Resource resolved = this.resolver.resolve("missing.md", this.document);

		assertFalse(resolved.exists());
		assertTrue(resolved.kind().isEmpty());
		assertEquals(this.tempDir.resolve("docs").resolve("missing.md").toString(), resolved.resolvedPath());
	}

	@Test
	void keepsAnAbsoluteTargetAsItIs() {
		Path absoluteTarget = this.tempDir.resolve("elsewhere.md").toAbsolutePath();

		Resource resolved = this.resolver.resolve(absoluteTarget.toString(), this.document);

		assertEquals(absoluteTarget.toString(), resolved.resolvedPath());
	}

	@Test
	void dropsPathTraversalReachingBeyondTheRoot() {
		Resource resolved = this.resolver.resolve("../../../../../../../../../../nowhere.md", this.document);

		assertFalse(resolved.exists());
		assertTrue(resolved.resolvedPath().endsWith("nowhere.md"));
		assertFalse(resolved.resolvedPath().contains(".."));
	}

	@Test
	void reportsATargetOfADocumentWithoutLocationAsUnresolved() {
		Resource resolved = this.resolver.resolve("other.md", UnresolvedResource.UNKNOWN_DOCUMENT);

		assertFalse(resolved.exists());
		assertTrue(resolved.kind().isEmpty());
		assertEquals("other.md", resolved.resolvedPath());
		assertEquals(new UnresolvedResource("other.md"), resolved);
	}

	@Test
	void reportsATargetOfAForeignResourceAsUnresolved() {
		Resource foreignDocument = new UnresolvedResource("projects/EXAMPLE/repos/docs/readme.md");

		Resource resolved = this.resolver.resolve("other.md", foreignDocument);

		assertFalse(resolved.exists());
		assertEquals("other.md", resolved.resolvedPath());
	}

	@Test
	void rejectsMissingArguments() {
		assertThrows(IllegalArgumentException.class, () -> this.resolver.resolve(null, this.document));
		assertThrows(IllegalArgumentException.class, () -> this.resolver.resolve("  ", this.document));
		assertThrows(IllegalArgumentException.class, () -> this.resolver.resolve("other.md", null));
		assertThrows(IllegalArgumentException.class, () -> new UnresolvedResource(null));
		assertThrows(IllegalArgumentException.class, () -> LocalFileSystemResource.of((Path) null));
		assertThrows(IllegalArgumentException.class, () -> LocalFileSystemResource.of((java.io.File) null));
	}

	@Test
	void treatsEqualPathsAsEqualResources() {
		Resource oneWay = this.resolver.resolve("other.md", this.document);
		Resource anotherWay = this.resolver.resolve("./sub/../other.md", this.document);

		assertEquals(oneWay, anotherWay);
		assertEquals(oneWay.hashCode(), anotherWay.hashCode());
		assertEquals(oneWay.resolvedPath(), oneWay.toString());
	}

}
