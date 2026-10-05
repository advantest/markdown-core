/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2026 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.resources;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers what {@link Resource#readAllContents()} answered, so that a resource is read at most
 * once for as long as this cache lives.
 * 
 * <p>A cache is meant to live as long as one run, e.g. one validation of a document or of a
 * directory tree: whoever runs creates it when the run starts and drops it when the run ends. That
 * is why it has neither a bound nor a way to clear it, and why a file changed while a run reads it
 * is seen by that run as it was first read. Reading without a cache is done by asking the resource
 * itself.</p>
 * 
 * <p>What was read is remembered under the resource's {@link Resource#getResolvedPath() resolved
 * path}, which is what tells two resources apart for whoever resolved them. A resource without such
 * a path, and an {@link UnresolvedResource}, whose path is only the target as it was written, are
 * read every time they are asked for: nothing tells them apart from another resource of the same
 * name.</p>
 * 
 * <p>A failure of reading is remembered just as long as the contents would have been, so a resource
 * that cannot be read is not read again for the next one asking, and every one of them is told what
 * went wrong. Several threads may ask at the same time; a resource they ask for at once is read by
 * the first of them, while the others wait for what it read.</p>
 */
public final class ResourceContentsCache {

	private final Map<String, CompletableFuture<String>> contentsByResolvedPath = new ConcurrentHashMap<>();

	/**
	 * Answers the contents of the given resource, reading them only if nobody asked for them before.
	 * 
	 * @param resource the resource to be read, must not be <code>null</code>
	 * @return the resource's contents, never <code>null</code>
	 * @throws IOException if the resource cannot be read, now or when it was read before
	 * @throws IllegalArgumentException if the given resource is <code>null</code>
	 */
	public String readAllContents(Resource resource) throws IOException {
		if (resource == null) {
			throw new IllegalArgumentException("Argument must not be null.");
		}

		String resolvedPath = resource.getResolvedPath();
		if (resource instanceof UnresolvedResource || resolvedPath == null || resolvedPath.isEmpty()) {
			return resource.readAllContents();
		}

		CompletableFuture<String> ownRead = new CompletableFuture<>();
		CompletableFuture<String> rememberedRead = this.contentsByResolvedPath.putIfAbsent(resolvedPath, ownRead);
		if (rememberedRead == null) {
			// read outside the map, so that a slow resource does not block asking for other ones
			try {
				ownRead.complete(resource.readAllContents());
			} catch (Throwable failure) {
				ownRead.completeExceptionally(failure);
			}
			rememberedRead = ownRead;
		}

		return contentsOf(rememberedRead);
	}

	private static String contentsOf(CompletableFuture<String> read) throws IOException {
		try {
			return read.join();
		} catch (CompletionException completion) {
			Throwable failure = completion.getCause();
			if (failure instanceof IOException ioFailure) {
				throw ioFailure;
			}
			if (failure instanceof RuntimeException runtimeFailure) {
				throw runtimeFailure;
			}
			if (failure instanceof Error error) {
				throw error;
			}
			throw new IOException(failure);
		}
	}

}
