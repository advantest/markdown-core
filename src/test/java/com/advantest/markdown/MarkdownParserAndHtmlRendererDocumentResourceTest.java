/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2026 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.advantest.flexmark.ext.plantuml.PlantUmlExtension;
import com.advantest.resources.LocalFileSystemResource;
import com.advantest.resources.Resource;
import com.advantest.resources.UnresolvedResource;
import com.vladsch.flexmark.util.ast.Document;

/**
 * Tests that a parsed {@link Document} knows where its source code came from, see
 * {@link MarkdownParserAndHtmlRenderer#KEY_DOCUMENT_RESOURCE}.
 */
class MarkdownParserAndHtmlRendererDocumentResourceTest {

	private static final String MARKDOWN = "# Title\n\nSee [other](other.md).\n";

	@TempDir
	private Path tempDir;

	private final MarkdownParserAndHtmlRenderer parserAndRenderer = new MarkdownParserAndHtmlRenderer();

	private Path markdownFile;

	@BeforeEach
	void createMarkdownFile() throws IOException {
		this.markdownFile = this.tempDir.resolve("readme.md");
		Files.writeString(this.markdownFile, MARKDOWN);
	}

	@Test
	void aDocumentOfUnknownOriginHasAResourceThatResolvesNothing() {
		Document document = this.parserAndRenderer.parseMarkdown(MARKDOWN);

		Resource resource = MarkdownParserAndHtmlRenderer.KEY_DOCUMENT_RESOURCE.get(document);

		assertSame(UnresolvedResource.UNKNOWN_DOCUMENT, resource);
		assertNull(PlantUmlExtension.KEY_DOCUMENT_FILE_PATH.get(document));
	}

	@Test
	void aParsedDocumentKnowsTheResourceItCameFrom() {
		Resource expectedResource = LocalFileSystemResource.of(this.markdownFile);

		Document document = this.parserAndRenderer.parseMarkdown(MARKDOWN, expectedResource);

		assertEquals(expectedResource, MarkdownParserAndHtmlRenderer.KEY_DOCUMENT_RESOURCE.get(document));
	}

	@Test
	void aParsedDocumentAlsoFeedsTheLegacyFilePathKey() {
		Document document = this.parserAndRenderer.parseMarkdown(MARKDOWN,
				LocalFileSystemResource.of(this.markdownFile));

		assertEquals(this.markdownFile.toString(), PlantUmlExtension.KEY_DOCUMENT_FILE_PATH.get(document));
	}

	@Test
	void anUnresolvedOriginDoesNotFeedTheLegacyFilePathKey() {
		Document document = this.parserAndRenderer.parseMarkdown(MARKDOWN,
				new UnresolvedResource("nowhere.md"));

		assertEquals("nowhere.md",
				MarkdownParserAndHtmlRenderer.KEY_DOCUMENT_RESOURCE.get(document).getResolvedPath());
		assertNull(PlantUmlExtension.KEY_DOCUMENT_FILE_PATH.get(document));
	}

	@Test
	void parsingAFileRemembersItsResource() throws IOException {
		Document document = this.parserAndRenderer.parseMarkdown(this.markdownFile.toFile());

		assertEquals(LocalFileSystemResource.of(this.markdownFile),
				MarkdownParserAndHtmlRenderer.KEY_DOCUMENT_RESOURCE.get(document));
		assertEquals(this.markdownFile.toString(), PlantUmlExtension.KEY_DOCUMENT_FILE_PATH.get(document));
	}

	@Test
	void aResourceCanBeSetOnADocumentParsedElsewhere() {
		Document document = this.parserAndRenderer.parseMarkdown(MARKDOWN);
		Resource expectedResource = LocalFileSystemResource.of(this.markdownFile);

		MarkdownParserAndHtmlRenderer.setDocumentResource(document, expectedResource);

		assertEquals(expectedResource, MarkdownParserAndHtmlRenderer.KEY_DOCUMENT_RESOURCE.get(document));
	}

	@Test
	void renderingKnowsTheResourceTheSourceCameFrom() {
		String html = this.parserAndRenderer.parseMarkdownAndRenderHtml(MARKDOWN,
				LocalFileSystemResource.of(this.markdownFile));

		assertEquals(this.parserAndRenderer.parseMarkdownAndRenderHtml(MARKDOWN), html);
	}

	@Test
	void rejectsAMissingResource() {
		assertThrows(IllegalArgumentException.class, () -> this.parserAndRenderer.parseMarkdown(MARKDOWN, null));
		assertThrows(IllegalArgumentException.class,
				() -> this.parserAndRenderer.parseMarkdownAndRenderHtml(MARKDOWN, null));
		assertThrows(IllegalArgumentException.class,
				() -> MarkdownParserAndHtmlRenderer.setDocumentResource(null,
						LocalFileSystemResource.of(this.markdownFile)));
	}

}
