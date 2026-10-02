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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link FileSchemeUriResolver}, i.e. for a file named with the scheme of its own
 * leading to the same resource as the same file named by its path.
 */
class FileSchemeUriResolverTest {

	@TempDir
	private Path tempDir;

	private final FileSchemeUriResolver resolver = new FileSchemeUriResolver();

	private Resource document;

	private Path neighbour;

	@BeforeEach
	void createDocumentAndNeighbour() throws IOException {
		Path documentPath = this.tempDir.resolve("docs").resolve("readme.md");
		Files.createDirectories(documentPath.getParent());
		Files.writeString(documentPath, "# Title");
		this.document = LocalFileSystemResource.of(documentPath);

		this.neighbour = this.tempDir.resolve("docs").resolve("other.md");
		Files.writeString(this.neighbour, "# Other");
	}

	@Test
	void answersForTheFileScheme() {
		assertTrue(this.resolver.isResponsibleFor(URI.create("file:///home/user/guide.md")));
		assertTrue(this.resolver.isResponsibleFor(URI.create("FILE:///home/user/guide.md")));
		assertFalse(this.resolver.isResponsibleFor(URI.create("https://example.org/guide")));
		assertFalse(this.resolver.isResponsibleFor(URI.create("mailto:someone@example.org")));
		assertFalse(this.resolver.isResponsibleFor(URI.create("guide.md")));
	}

	@Test
	void findsAFileNamedWithItsScheme() {
		Resource resolved = this.resolver.resolve(this.neighbour.toUri(), this.document);

		assertTrue(resolved.exists());
		assertTrue(resolved.isFile());
		assertEquals(this.neighbour.toString(), resolved.getResolvedPath());
	}

	@Test
	void findsAFileNamedWithItsSchemeAndAPlaceInsideIt() {
		URI withFragment = URI.create(this.neighbour.toUri() + "#a-section");

		Resource resolved = this.resolver.resolve(withFragment, this.document);

		assertTrue(resolved.exists());
		assertEquals(this.neighbour.toString(), resolved.getResolvedPath());
	}

	@Test
	void findsAFileNamedRelativelyWithItsScheme() {
		Resource resolved = this.resolver.resolve(URI.create("file:other.md"), this.document);

		assertTrue(resolved.exists());
		assertEquals(this.neighbour.toString(), resolved.getResolvedPath());
	}

	@Test
	void resolvesTheSameFileAsAReferenceWithoutAScheme() {
		Resource namedByPath = new LocalFileSystemResourceResolver().resolve("other.md", this.document);
		Resource namedByUri = this.resolver.resolve(this.neighbour.toUri(), this.document);

		assertEquals(namedByPath, namedByUri);
	}

	@Test
	void resolvesAReferenceGivenAsTextTheSameWay() {
		Resource resolved = this.resolver.resolve(this.neighbour.toUri().toString(), this.document);

		assertTrue(resolved.exists());
		assertEquals(this.neighbour.toString(), resolved.getResolvedPath());
	}

	@Test
	void reportsAReferenceTheUriSyntaxRejectsAsUnresolved() {
		Resource resolved = this.resolver.resolve("file:other file|.md", this.document);

		assertFalse(resolved.exists());
		assertEquals("file:other file|.md", resolved.getResolvedPath());
	}

	@Test
	void rejectsMissingArguments() {
		assertThrows(IllegalArgumentException.class, () -> this.resolver.isResponsibleFor(null));
		assertThrows(IllegalArgumentException.class,
				() -> this.resolver.resolve((URI) null, this.document));
		assertThrows(IllegalArgumentException.class,
				() -> this.resolver.resolve(this.neighbour.toUri(), null));
		assertThrows(IllegalArgumentException.class, () -> this.resolver.resolve((String) null, this.document));
		assertThrows(IllegalArgumentException.class, () -> this.resolver.resolve("  ", this.document));
		assertThrows(IllegalArgumentException.class, () -> new FileSchemeUriResolver(null));
	}

	@Test
	void reportsAnAddressWithoutAPathAsUnresolved() {
		URI withoutPath = URI.create("file://some-host");

		Resource resolved = this.resolver.resolve(withoutPath, this.document);

		assertFalse(resolved.exists());
		assertEquals(new UnresolvedResource("file://some-host"), resolved);
	}

	@Test
	void ignoresTheQueryOfAnAddressTheFileSystemCannotReadAsAWhole() {
		URI withQuery = URI.create("file:///some-directory/guide.md?version=1");

		Resource resolved = this.resolver.resolve(withQuery, this.document);

		assertFalse(resolved.exists());
		assertTrue(resolved.getResolvedPath().endsWith("guide.md"));
	}

}