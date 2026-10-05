/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2026 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link ResourceContentsCache}, which reads a resource at most once for as long as it
 * lives.
 */
class ResourceContentsCacheTest {

	@TempDir
	private Path tempDir;

	private final ResourceContentsCache cache = new ResourceContentsCache();

	@Test
	void readsAResourceAskedForTwiceOnce() throws IOException {
		CountingResource resource = new CountingResource("/docs/guide.md", "# Guide\n");

		assertEquals("# Guide\n", this.cache.readAllContents(resource));
		assertEquals("# Guide\n", this.cache.readAllContents(resource));

		assertEquals(1, resource.reads());
	}

	@Test
	void readsTwoResourcesNamedAlikeOnceForBoth() throws IOException {
		CountingResource first = new CountingResource("/docs/guide.md", "# Guide\n");
		CountingResource second = new CountingResource("/docs/guide.md", "# Another guide\n");

		this.cache.readAllContents(first);

		assertEquals("# Guide\n", this.cache.readAllContents(second));
		assertEquals(0, second.reads());
	}

	@Test
	void readsResourcesOfDifferentPathsEach() throws IOException {
		CountingResource guide = new CountingResource("/docs/guide.md", "# Guide\n");
		CountingResource notes = new CountingResource("/docs/notes.md", "# Notes\n");

		assertEquals("# Guide\n", this.cache.readAllContents(guide));
		assertEquals("# Notes\n", this.cache.readAllContents(notes));

		assertEquals(1, guide.reads());
		assertEquals(1, notes.reads());
	}

	@Test
	void keepsAnsweringWhatAFileSaidWhenItWasFirstRead() throws IOException {
		Path file = this.tempDir.resolve("guide.md");
		Files.writeString(file, "# Before\n");
		Resource resource = LocalFileSystemResource.of(file);

		this.cache.readAllContents(resource);
		Files.writeString(file, "# After\n");

		assertEquals("# Before\n", this.cache.readAllContents(resource));
		assertEquals("# After\n", new ResourceContentsCache().readAllContents(resource));
	}

	@Test
	void remembersThatAResourceCannotBeReadAndTellsEveryoneAsking() {
		CountingResource resource = CountingResource.failing("/docs/absent.md",
				new IOException("'/docs/absent.md' does not exist."));

		IOException first = assertThrows(IOException.class, () -> this.cache.readAllContents(resource));
		IOException second = assertThrows(IOException.class, () -> this.cache.readAllContents(resource));

		assertEquals("'/docs/absent.md' does not exist.", first.getMessage());
		assertSame(first, second);
		assertEquals(1, resource.reads());
	}

	@Test
	void remembersAnUnexpectedFailureOfReadingAsWell() {
		CountingResource resource = CountingResource.failing("/docs/broken.md",
				new IllegalStateException("The reader broke."));

		assertThrows(IllegalStateException.class, () -> this.cache.readAllContents(resource));
		assertThrows(IllegalStateException.class, () -> this.cache.readAllContents(resource));

		assertEquals(1, resource.reads());
	}

	@Test
	void remembersAnErrorOfReadingAsWell() {
		CountingResource resource = CountingResource.failing("/docs/broken.md", new LinkageError("A class is missing."));

		assertThrows(LinkageError.class, () -> this.cache.readAllContents(resource));
		assertThrows(LinkageError.class, () -> this.cache.readAllContents(resource));

		assertEquals(1, resource.reads());
	}

	@Test
	void saysThatAResourceCannotBeReadWhereItFailedWithAnUndeclaredCheckedException() {
		Exception undeclared = new Exception("Something nobody declared.");
		CountingResource resource = CountingResource.failing("/docs/odd.md", undeclared);

		IOException failure = assertThrows(IOException.class, () -> this.cache.readAllContents(resource));

		assertSame(undeclared, failure.getCause());
		assertEquals(1, resource.reads());
	}

	@Test
	void readsAResourceWithoutAPathEveryTime() throws IOException {
		CountingResource resource = new CountingResource("", "# Untitled\n");

		this.cache.readAllContents(resource);
		this.cache.readAllContents(resource);

		assertEquals(2, resource.reads());
	}

	@Test
	void readsAResourceWithoutAnyPathEveryTime() throws IOException {
		CountingResource resource = new CountingResource(null, "# Untitled\n");

		this.cache.readAllContents(resource);
		this.cache.readAllContents(resource);

		assertEquals(2, resource.reads());
	}

	@Test
	void remembersNothingForAResourceNobodyResolved() {
		Resource first = new UnresolvedResource("missing.md");

		IOException failure = assertThrows(IOException.class, () -> this.cache.readAllContents(first));

		assertEquals("'missing.md' could not be resolved and can therefore not be read.", failure.getMessage());
		assertThrows(IOException.class, () -> this.cache.readAllContents(first));
	}

	@Test
	void readsAResourceSeveralThreadsAskForAtOnceOnce() throws Exception {
		int threads = 8;
		CountDownLatch everybodyAsked = new CountDownLatch(threads);
		CountingResource resource = new CountingResource("/docs/guide.md", "# Guide\n") {
			@Override
			public BufferedReader readContents() throws IOException {
				awaitQuietly(everybodyAsked);
				return super.readContents();
			}
		};

		ExecutorService askers = Executors.newFixedThreadPool(threads);
		try {
			List<Future<String>> answers = new ArrayList<>();
			for (int i = 0; i < threads; i++) {
				answers.add(askers.submit(() -> {
					everybodyAsked.countDown();
					return this.cache.readAllContents(resource);
				}));
			}

			for (Future<String> answer : answers) {
				assertEquals("# Guide\n", answer.get(10, TimeUnit.SECONDS));
			}
		} finally {
			askers.shutdownNow();
		}

		assertEquals(1, resource.reads());
	}

	@Test
	void refusesToReadNothing() {
		assertThrows(IllegalArgumentException.class, () -> this.cache.readAllContents(null));
	}

	private static void awaitQuietly(CountDownLatch latch) {
		try {
			latch.await(5, TimeUnit.SECONDS);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * A resource counting how often it is read, answering a text or failing.
	 */
	private static class CountingResource implements Resource {

		private final String resolvedPath;

		private final String contents;

		private final Throwable failure;

		private final AtomicInteger reads = new AtomicInteger();

		CountingResource(String resolvedPath, String contents) {
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
			return this.reads.get();
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
			return Optional.of(ResourceKind.FILE);
		}

		@Override
		public BufferedReader readContents() throws IOException {
			this.reads.incrementAndGet();
			if (this.failure != null) {
				throw CountingResource.<RuntimeException>asUnchecked(this.failure);
			}
			return new BufferedReader(new StringReader(this.contents));
		}

		/**
		 * Lets a failure of any kind pass a method declaring only an {@link IOException}.
		 */
		@SuppressWarnings("unchecked")
		private static <T extends Throwable> T asUnchecked(Throwable failure) throws T {
			throw (T) failure;
		}

	}

}
