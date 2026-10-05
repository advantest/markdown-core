/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2026 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.advantest.resources.Resource;
import com.advantest.resources.ResourceContentsCache;
import com.advantest.resources.ResourceKind;
import com.advantest.resources.UnresolvedResource;
import com.vladsch.flexmark.util.ast.Document;

/**
 * Checks what a run remembers about the documents it parsed.
 */
class ParsedMarkdownDocumentsCacheTest {

	/** A resource answering with a fixed text, or failing, and counting how often it was read. */
	private static final class CountingResource implements Resource {

		private final String resolvedPath;

		private final String contents;

		private final Throwable failure;

		private final AtomicInteger readCount = new AtomicInteger();

		private CountingResource(String resolvedPath, String contents) {
			this(resolvedPath, contents, null);
		}

		private CountingResource(String resolvedPath, String contents, Throwable failure) {
			this.resolvedPath = resolvedPath;
			this.contents = contents;
			this.failure = failure;
		}

		static CountingResource failing(String resolvedPath, Throwable failure) {
			return new CountingResource(resolvedPath, null, failure);
		}

		int reads() {
			return this.readCount.get();
		}

		@Override
		public String getResolvedPath() {
			return this.resolvedPath;
		}

		@Override
		public boolean exists() {
			return this.contents != null;
		}

		@Override
		public Optional<ResourceKind> getKind() {
			return exists() ? Optional.of(ResourceKind.FILE) : Optional.empty();
		}

		@Override
		public BufferedReader readContents() throws IOException {
			this.readCount.incrementAndGet();
			if (this.failure instanceof IOException ioFailure) {
				throw ioFailure;
			}
			if (this.failure instanceof Error error) {
				throw error;
			}
			return new BufferedReader(new StringReader(this.contents));
		}
	}

	private final MarkdownParserAndHtmlRenderer parserAndRenderer = new MarkdownParserAndHtmlRenderer();

	private final ResourceContentsCache contentsCache = new ResourceContentsCache();

	private final ParsedMarkdownDocumentsCache cache =
			new ParsedMarkdownDocumentsCache(this.parserAndRenderer, this.contentsCache);

	@Test
	void parsesAResourceAskedForTwiceOnce() throws IOException {
		CountingResource resource = new CountingResource("/docs/guide.md", "# Guide\n");

		Document firstAnswer = this.cache.parseMarkdown(resource);
		Document secondAnswer = this.cache.parseMarkdown(resource);

		assertSame(firstAnswer, secondAnswer, "The document parsed first is expected again.");
		assertEquals(1, resource.reads(), "A document is expected to be read once.");
	}

	@Test
	void readsAResourceWhoseContentsWereReadBeforeOnce() throws IOException {
		CountingResource resource = new CountingResource("/docs/guide.md", "# Guide\n");

		this.contentsCache.readAllContents(resource);
		this.cache.parseMarkdown(resource);

		assertEquals(1, resource.reads(), "Reading and parsing are expected to share one read.");
	}

	@Test
	void tellsTwoResourcesApartByTheirResolvedPath() throws IOException {
		CountingResource guide = new CountingResource("/docs/guide.md", "# Guide\n");
		CountingResource notes = new CountingResource("/docs/notes.md", "# Notes\n");

		Document parsedGuide = this.cache.parseMarkdown(guide);
		Document parsedNotes = this.cache.parseMarkdown(notes);

		assertEquals("# Guide\n", parsedGuide.getChars().toString());
		assertEquals("# Notes\n", parsedNotes.getChars().toString());
	}

	@Test
	void doesNotReadAResourceThatCannotBeReadAgain() {
		CountingResource missing = CountingResource.failing("/docs/gone.md", new IOException("Gone."));

		IOException firstFailure = assertThrows(IOException.class, () -> this.cache.parseMarkdown(missing));
		IOException secondFailure = assertThrows(IOException.class, () -> this.cache.parseMarkdown(missing));

		assertSame(firstFailure, secondFailure, "Every caller is expected to be told what went wrong.");
		assertEquals(1, missing.reads(), "A failing document is expected to be read once.");
	}

	@Test
	void remembersAFailureOfParsing() {
		MarkdownParserAndHtmlRenderer failingParser = new MarkdownParserAndHtmlRenderer() {
			@Override
			public Document parseMarkdown(String markdownSourceCode, Resource documentResource) {
				throw new IllegalStateException("The parser broke.");
			}
		};
		ParsedMarkdownDocumentsCache cacheParsingWithFailures =
				new ParsedMarkdownDocumentsCache(failingParser, this.contentsCache);
		CountingResource resource = new CountingResource("/docs/guide.md", "# Guide\n");

		IllegalStateException firstFailure = assertThrows(IllegalStateException.class,
				() -> cacheParsingWithFailures.parseMarkdown(resource));
		IllegalStateException secondFailure = assertThrows(IllegalStateException.class,
				() -> cacheParsingWithFailures.parseMarkdown(resource));

		assertSame(firstFailure, secondFailure);
	}

	@Test
	void remembersAnErrorOfReading() {
		CountingResource resource = CountingResource.failing("/docs/broken.md", new LinkageError("Missing."));

		assertThrows(LinkageError.class, () -> this.cache.parseMarkdown(resource));
		assertThrows(LinkageError.class, () -> this.cache.parseMarkdown(resource));

		assertEquals(1, resource.reads());
	}

	@Test
	void letsAParsedDocumentKnowWhereItCameFrom() throws IOException {
		CountingResource resource = new CountingResource("/docs/guide.md", "# Guide\n");

		Document parsedDocument = this.cache.parseMarkdown(resource);

		assertSame(resource, MarkdownParserAndHtmlRenderer.KEY_DOCUMENT_RESOURCE.get(parsedDocument),
				"The parsed document is expected to carry the resource it was read from.");
	}

	@Test
	void doesNotRememberAnUnresolvedResource() {
		UnresolvedResource unresolved = new UnresolvedResource("guide.md");

		IOException firstFailure = assertThrows(IOException.class, () -> this.cache.parseMarkdown(unresolved));
		IOException secondFailure = assertThrows(IOException.class, () -> this.cache.parseMarkdown(unresolved));

		assertNotSame(firstFailure, secondFailure, "An unresolved resource is expected to be read every time.");
	}

	@Test
	void refusesAResourceWithoutAPath() {
		CountingResource unnamed = new CountingResource("", "# Guide\n");
		CountingResource nameless = new CountingResource(null, "# Guide\n");

		assertThrows(IllegalArgumentException.class, () -> this.cache.parseMarkdown(unnamed));
		assertThrows(IllegalArgumentException.class, () -> this.cache.parseMarkdown(nameless));
		assertEquals(0, unnamed.reads() + nameless.reads(), "Without a name nothing says that this is Markdown.");
	}

	@Test
	void refusesAResourceThatIsNoMarkdownFile() {
		CountingResource plainText = new CountingResource("/docs/guide.txt", "# Guide\n");

		assertThrows(IllegalArgumentException.class, () -> this.cache.parseMarkdown(plainText));
		assertEquals(0, plainText.reads(),
				"Reading a document that is no Markdown and parsing it as Markdown answers about nothing.");
	}

	@Test
	void ignoresHowTheMarkdownExtensionIsWritten() throws IOException {
		CountingResource shouted = new CountingResource("/docs/GUIDE.MD", "# Guide\n");

		assertEquals("# Guide\n", this.cache.parseMarkdown(shouted).getChars().toString());
	}

	@Test
	void takesAsMarkdownWhatTheParserAccepts() throws IOException {
		ParsedMarkdownDocumentsCache cacheAcceptingMore = new ParsedMarkdownDocumentsCache(
				MarkdownParserAndHtmlRenderer.builder()
						.withMarkdownFileExtensions(MarkdownFileExtensions.accepting("md", "mkd"))
						.build(),
				this.contentsCache);
		CountingResource resource = new CountingResource("/docs/guide.mkd", "# Guide\n");

		assertEquals("# Guide\n", cacheAcceptingMore.parseMarkdown(resource).getChars().toString());
		assertThrows(IllegalArgumentException.class, () -> this.cache.parseMarkdown(resource),
				"A cache whose parser was not told about that extension is expected to refuse it.");
	}

	@Test
	void parsesAResourceManyThreadsAskForAtOnceOnce() throws Exception {
		CountingResource resource = new CountingResource("/docs/guide.md", "# Guide\n");
		int threadCount = 8;
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Document>> answers = new ArrayList<>();

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		try {
			for (int i = 0; i < threadCount; i++) {
				answers.add(executor.submit(() -> {
					start.await();
					return this.cache.parseMarkdown(resource);
				}));
			}
			start.countDown();

			Document firstAnswer = answers.get(0).get();
			for (Future<Document> answer : answers) {
				assertSame(firstAnswer, answer.get());
			}
		} finally {
			executor.shutdownNow();
		}
		assertEquals(1, resource.reads());
	}

	@Test
	void refusesNoResource() {
		assertThrows(IllegalArgumentException.class, () -> this.cache.parseMarkdown(null));
	}

	@Test
	void cannotBeCreatedWithoutAParserOrAContentsCache() {
		assertThrows(IllegalArgumentException.class,
				() -> new ParsedMarkdownDocumentsCache(null, this.contentsCache));
		assertThrows(IllegalArgumentException.class,
				() -> new ParsedMarkdownDocumentsCache(this.parserAndRenderer, null));
	}

}
