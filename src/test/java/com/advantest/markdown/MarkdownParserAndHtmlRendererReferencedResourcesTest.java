/*
 * Copyright © 2026 Advantest Europe GmbH
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.advantest.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.advantest.flexmark.ext.plantuml.PlantUmlExtension;
import com.advantest.flexmark.ext.plantuml.PlantUmlFileLocation;
import com.advantest.flexmark.ext.plantuml.PlantUmlFileLocations;
import com.advantest.resources.LocalFileSystemResource;
import com.advantest.resources.RelativePathResourceResolver;
import com.advantest.resources.Resource;
import com.advantest.resources.ResourceKind;
import com.advantest.resources.UnresolvedResource;
import com.vladsch.flexmark.util.ast.Document;

/**
 * Tests that a parsed {@link Document} answers what the targets written in it say, see
 * {@link MarkdownParserAndHtmlRenderer#KEY_DOCUMENT_REFERENCED_RESOURCES}.
 */
class MarkdownParserAndHtmlRendererReferencedResourcesTest {

	private static final String MARKDOWN = "# Title\n\n![diagram](diagrams/classes.puml)\n";

	private static final String PLANT_UML = "@startuml\nclass Foo\n@enduml\n";

	@TempDir
	private Path tempDir;

	private Path markdownFile;

	private Path diagramFile;

	@BeforeEach
	void createDocumentAndDiagram() throws IOException {
		this.markdownFile = this.tempDir.resolve("readme.md");
		Files.writeString(this.markdownFile, MARKDOWN);

		this.diagramFile = this.tempDir.resolve("diagrams").resolve("classes.puml");
		Files.createDirectories(this.diagramFile.getParent());
		Files.writeString(this.diagramFile, PLANT_UML);
	}

	private Map<String, String> contentsOf(Document document) {
		return PlantUmlExtension.KEY_DOCUMENT_PATH_TO_FILE_CONTENTS_MAP.get(document);
	}

	private PlantUmlFileLocations locationsOf(Document document) {
		return PlantUmlExtension.KEY_DOCUMENT_PLANTUML_FILE_LOCATIONS.get(document);
	}

	@Test
	void aTargetWrittenInTheDocumentIsReadFromTheLocalFileSystem() {
		Document document = parse(new MarkdownParserAndHtmlRenderer());

		assertEquals(PLANT_UML, contentsOf(document).get("diagrams/classes.puml"));
	}

	@Test
	void nothingIsReadBeforeItIsAskedFor() {
		Document document = parse(new MarkdownParserAndHtmlRenderer());

		assertTrue(contentsOf(document).isEmpty());
		assertTrue(MarkdownParserAndHtmlRenderer.KEY_DOCUMENT_REFERENCED_RESOURCES.get(document).isEmpty());
	}

	@Test
	void aTargetAskedForTwiceIsReadOnce() {
		CountingResourceResolver resolver = new CountingResourceResolver();
		Document document = parse(MarkdownParserAndHtmlRenderer.builder()
				.withRelativePathResourceResolver(resolver)
				.build());

		contentsOf(document).get("diagrams/classes.puml");
		contentsOf(document).get("diagrams/classes.puml");

		assertEquals(1, resolver.numberOfCalls);
	}

	@Test
	void aResolvedTargetIsRememberedOnTheDocument() {
		Document document = parse(new MarkdownParserAndHtmlRenderer());

		contentsOf(document).get("diagrams/classes.puml");

		Map<String, Resource> resources =
				MarkdownParserAndHtmlRenderer.KEY_DOCUMENT_REFERENCED_RESOURCES.get(document);
		assertEquals(LocalFileSystemResource.of(this.diagramFile), resources.get("diagrams/classes.puml"));
	}

	@Test
	void aTargetOfADocumentOfUnknownOriginSaysNothing() {
		Document document = new MarkdownParserAndHtmlRenderer().parseMarkdown(MARKDOWN);

		assertNull(contentsOf(document).get("diagrams/classes.puml"));
	}

	@Test
	void aTargetThatIsNotThereSaysNothing() {
		Document document = parse(new MarkdownParserAndHtmlRenderer());

		assertNull(contentsOf(document).get("diagrams/missing.puml"));
		assertFalse(contentsOf(document).containsKey("diagrams/missing.puml"));
	}

	@Test
	void whatWasPutInWinsOverReading() {
		Document document = parse(new MarkdownParserAndHtmlRenderer());

		contentsOf(document).put("diagrams/classes.puml", "@startuml\nclass Prepared\n@enduml\n");

		assertEquals("@startuml\nclass Prepared\n@enduml\n", contentsOf(document).get("diagrams/classes.puml"));
	}

	@Test
	void aConfiguredResolverIsAskedInsteadOfTheLocalFileSystem() {
		Document document = parse(MarkdownParserAndHtmlRenderer.builder()
				.withRelativePathResourceResolver(new CountingResourceResolver())
				.build());

		assertEquals("@startuml\nclass Resolved\n@enduml\n", contentsOf(document).get("diagrams/classes.puml"));
	}

	@Test
	void aTargetThatIsNotThereSaysWhereItWasLookedFor() {
		Document document = parse(new MarkdownParserAndHtmlRenderer());

		PlantUmlFileLocation location = locationsOf(document).locationOf("diagrams/missing.puml");

		assertEquals(this.tempDir.resolve("diagrams").resolve("missing.puml").toString(),
				location.resolvedPath());
		assertFalse(location.exists());
	}

	@Test
	void aTargetThatIsThereSaysThatItIsThere() {
		Document document = parse(new MarkdownParserAndHtmlRenderer());

		PlantUmlFileLocation location = locationsOf(document).locationOf("diagrams/classes.puml");

		assertEquals(this.diagramFile.toString(), location.resolvedPath());
		assertTrue(location.exists());
	}

	@Test
	void aConfiguredResolverSaysHowItNamesWhereItLooked() {
		Document document = parse(MarkdownParserAndHtmlRenderer.builder()
				.withRelativePathResourceResolver(new CountingResourceResolver())
				.build());

		PlantUmlFileLocation location = locationsOf(document).locationOf("diagrams/classes.puml");

		assertEquals("diagrams/classes.puml", location.resolvedPath());
	}

	@Test
	void aPathTraversalIsRemovedFromWhereATargetWasLookedFor() {
		Document document = parse(new MarkdownParserAndHtmlRenderer());

		PlantUmlFileLocation location = locationsOf(document).locationOf("diagrams/../missing.puml");

		assertEquals(this.tempDir.resolve("missing.puml").toString(), location.resolvedPath());
	}

	@Test
	void aMissingDiagramIsRenderedSayingWhereItsFileWasLookedFor() {
		MarkdownParserAndHtmlRenderer parserAndRenderer = new MarkdownParserAndHtmlRenderer();
		Document document = parserAndRenderer.parseMarkdown("![diagram](diagrams/missing.puml)\n",
				LocalFileSystemResource.of(this.markdownFile));

		String html = parserAndRenderer.renderHtml(document);

		assertTrue(html.contains("PlantUML file &quot;diagrams/missing.puml&quot; (resolved path: &quot;"
				+ this.tempDir.resolve("diagrams").resolve("missing.puml") + "&quot;) does not exist."),
				html);
	}

	@Test
	void aDiagramFileThatIsThereIsRenderedIntoTheHtml() {
		MarkdownParserAndHtmlRenderer parserAndRenderer = new MarkdownParserAndHtmlRenderer();
		Document document = parse(parserAndRenderer);

		String html = parserAndRenderer.renderHtml(document);

		assertTrue(containsSvgImage(html), html);
		assertTrue(html.contains("Foo"), html);
	}

	@Test
	void aDocumentIsRenderedAlikeWhetherItsFileOrItsTextIsParsed() throws IOException {
		MarkdownParserAndHtmlRenderer parserAndRenderer = new MarkdownParserAndHtmlRenderer();

		String fromText = parserAndRenderer.renderHtml(parse(parserAndRenderer));
		String fromFile = parserAndRenderer.renderHtml(
				parserAndRenderer.parseMarkdown(this.markdownFile.toFile()));

		assertEquals(fromText, fromFile);
	}

	@Test
	void aDiagramReadThroughAConfiguredResolverIsRenderedIntoTheHtml() {
		MarkdownParserAndHtmlRenderer parserAndRenderer = MarkdownParserAndHtmlRenderer.builder()
				.withRelativePathResourceResolver(new CountingResourceResolver())
				.build();

		String html = parserAndRenderer.renderHtml(parse(parserAndRenderer));

		assertTrue(containsSvgImage(html), html);
		assertTrue(html.contains("Resolved"), html);
	}

	@Test
	void aDiagramIsReadFromItsFileWhereTheBuilderIsGivenNoReader() {
		MarkdownParserAndHtmlRenderer parserAndRenderer = MarkdownParserAndHtmlRenderer.builder().build();

		String html = parserAndRenderer.renderHtml(parse(parserAndRenderer));

		assertTrue(containsSvgImage(html), html);
		assertTrue(html.contains("Foo"), html);
	}

	@Test
	void aDiagramReadThroughAConfiguredReaderIsRenderedIntoTheHtml() {
		MarkdownParserAndHtmlRenderer parserAndRenderer = MarkdownParserAndHtmlRenderer.builder()
				.withResourceContentsReader(resource -> "@startuml\nclass Unsaved\n@enduml\n")
				.build();

		String html = parserAndRenderer.renderHtml(parse(parserAndRenderer));

		assertTrue(containsSvgImage(html), html);
		assertTrue(html.contains("Unsaved"), html);
		assertFalse(html.contains("Foo"), html);
	}

	@Test
	void aDiagramOfADocumentOfUnknownOriginIsRenderedAsAMessageNamingTheTargetAsWritten() {
		MarkdownParserAndHtmlRenderer parserAndRenderer = new MarkdownParserAndHtmlRenderer();
		Document document = parserAndRenderer.parseMarkdown(MARKDOWN);

		String html = parserAndRenderer.renderHtml(document);

		assertFalse(containsSvgImage(html), html);
		assertTrue(html.contains(
				"PlantUML file &quot;diagrams/classes.puml&quot; does not exist."), html);
		assertFalse(html.contains("resolved path"), html);
	}

	private boolean containsSvgImage(String html) {
		return Pattern.compile("<figure[^>]*>\\s*<svg ").matcher(html).find();
	}

	private Document parse(MarkdownParserAndHtmlRenderer parserAndRenderer) {
		return parserAndRenderer.parseMarkdown(MARKDOWN, LocalFileSystemResource.of(this.markdownFile));
	}

	/**
	 * A resolver that counts how often it was asked and answers the same text for every target.
	 */
	private static final class CountingResourceResolver implements RelativePathResourceResolver {

		private int numberOfCalls;

		@Override
		public Resource resolve(String targetResourcePathOrUri, Resource referencingDocument) {
			this.numberOfCalls++;

			if (targetResourcePathOrUri == null || targetResourcePathOrUri.isBlank()) {
				return new UnresolvedResource(targetResourcePathOrUri);
			}

			return new ResolvedText(targetResourcePathOrUri);
		}

	}

	/**
	 * A resource that says the same text wherever it is asked, standing for a resource outside the
	 * local file system.
	 */
	private static final class ResolvedText implements Resource {

		private final String path;

		private ResolvedText(String path) {
			this.path = path;
		}

		@Override
		public String getResolvedPath() {
			return this.path;
		}

		@Override
		public boolean exists() {
			return true;
		}

		@Override
		public Optional<ResourceKind> getKind() {
			return Optional.of(ResourceKind.FILE);
		}

		@Override
		public BufferedReader readContents() throws IOException {
			return new BufferedReader(new StringReader("@startuml\nclass Resolved\n@enduml\n"));
		}

	}

}
