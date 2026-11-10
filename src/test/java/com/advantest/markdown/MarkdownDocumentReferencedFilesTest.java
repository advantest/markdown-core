/*
 * Copyright © 2026 Advantest Europe GmbH
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.advantest.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.advantest.flexmark.ext.plantuml.PlantUmlFileLocation;
import com.advantest.resources.LocalFileSystemResource;
import com.advantest.resources.LocalFileSystemResourceResolver;
import com.advantest.resources.Resource;
import com.advantest.resources.ResourceContentsReader;

/**
 * Tests for {@link MarkdownDocumentReferencedFiles}, i.e. for what is answered about the targets a
 * document refers to.
 */
class MarkdownDocumentReferencedFilesTest {

	@TempDir
	private Path tempDir;

	private final Map<String, Resource> resourcesByTarget = new HashMap<>();

	private Resource document;

	private MarkdownDocumentReferencedFiles referencedFiles;

	@BeforeEach
	void createDocumentAndReferencedFiles() throws IOException {
		Path documentPath = this.tempDir.resolve("readme.md");
		Files.writeString(documentPath, "# Title");
		Files.writeString(this.tempDir.resolve("diagram.puml"), "@startuml\n@enduml\n");
		this.document = LocalFileSystemResource.of(documentPath);
		this.referencedFiles = new MarkdownDocumentReferencedFiles(this.document,
				new LocalFileSystemResourceResolver(), this.resourcesByTarget, ResourceContentsReader.FROM_THE_RESOURCE);
	}

	@Test
	void answersTheTextOfAnExistingTarget() {
		assertEquals("@startuml\n@enduml\n", this.referencedFiles.get("diagram.puml"));
	}

	@Test
	void answersNothingForATargetThatCannotBeRead() {
		assertNull(this.referencedFiles.get("missing.puml"));
	}

	@Test
	void answersNothingForATargetNamingADirectory() throws IOException {
		Files.createDirectories(this.tempDir.resolve("images"));

		assertNull(this.referencedFiles.get("images"));
	}

	@Test
	void answersNothingForAKeyThatIsNoTargetAtAll() {
		assertNull(this.referencedFiles.get(null));
		assertNull(this.referencedFiles.get(42));
		assertNull(this.referencedFiles.get("  "));
	}

	@Test
	void readsATargetOnceAndRemembersItsText() throws IOException {
		Path file = this.tempDir.resolve("diagram.puml");
		this.referencedFiles.get("diagram.puml");

		Files.writeString(file, "changed");

		assertEquals("@startuml\n@enduml\n", this.referencedFiles.get("diagram.puml"));
	}

	@Test
	void prefersTheTextThatWasPutInOverTheTextOfTheFile() {
		this.referencedFiles.put("diagram.puml", "put in");

		assertEquals("put in", this.referencedFiles.get("diagram.puml"));
	}

	@Test
	void refusesToPutInNothing() {
		assertThrows(IllegalArgumentException.class, () -> this.referencedFiles.put(null, "text"));
		assertThrows(IllegalArgumentException.class, () -> this.referencedFiles.put("diagram.puml", null));
	}

	@Test
	void tellsWhereAnExistingTargetWasLookedFor() {
		PlantUmlFileLocation location = this.referencedFiles.locationOf("diagram.puml");

		assertEquals(this.tempDir.resolve("diagram.puml").toString(), location.resolvedPath());
		assertTrue(location.exists());
	}

	@Test
	void tellsWhereAMissingTargetWasLookedFor() {
		PlantUmlFileLocation location = this.referencedFiles.locationOf("missing.puml");

		assertEquals(this.tempDir.resolve("missing.puml").toString(), location.resolvedPath());
		assertFalse(location.exists());
	}

	@Test
	void tellsNoLocationForATargetThatIsNoTarget() {
		assertNull(this.referencedFiles.locationOf(null));
		assertNull(this.referencedFiles.locationOf(" "));
	}

	@Test
	void resolvesATargetOnceWhereverItIsAskedFor() {
		this.referencedFiles.locationOf("diagram.puml");
		Resource resolved = this.resourcesByTarget.get("diagram.puml");

		this.referencedFiles.get("diagram.puml");

		assertSame(resolved, this.resourcesByTarget.get("diagram.puml"));
		assertEquals(1, this.resourcesByTarget.size());
	}

	@Test
	void readsATargetThroughTheReaderItWasGiven() {
		List<Resource> asked = new ArrayList<>();
		MarkdownDocumentReferencedFiles readByAnotherReader = new MarkdownDocumentReferencedFiles(this.document,
				new LocalFileSystemResourceResolver(), this.resourcesByTarget, resource -> {
					asked.add(resource);
					return "from the reader";
				});

		assertEquals("from the reader", readByAnotherReader.get("diagram.puml"));
		assertEquals(List.of(this.resourcesByTarget.get("diagram.puml")), asked);
	}

	@Test
	void answersNothingForATargetItsReaderCannotRead() {
		MarkdownDocumentReferencedFiles unreadable = new MarkdownDocumentReferencedFiles(this.document,
				new LocalFileSystemResourceResolver(), this.resourcesByTarget, resource -> {
					throw new IOException("cannot be read");
				});

		assertNull(unreadable.get("diagram.puml"));
	}

	@Test
	void refusesToBeCreatedWithoutItsArguments() {		LocalFileSystemResourceResolver resolver = new LocalFileSystemResourceResolver();
		ResourceContentsReader reader = ResourceContentsReader.FROM_THE_RESOURCE;

		assertThrows(IllegalArgumentException.class,
				() -> new MarkdownDocumentReferencedFiles(null, resolver, this.resourcesByTarget, reader));
		assertThrows(IllegalArgumentException.class,
				() -> new MarkdownDocumentReferencedFiles(this.document, null, this.resourcesByTarget, reader));
		assertThrows(IllegalArgumentException.class,
				() -> new MarkdownDocumentReferencedFiles(this.document, resolver, null, reader));
		assertThrows(IllegalArgumentException.class,
				() -> new MarkdownDocumentReferencedFiles(this.document, resolver, this.resourcesByTarget, null));
	}

}
