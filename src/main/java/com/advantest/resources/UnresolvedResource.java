/*
 * Copyright © 2026 Advantest Europe GmbH
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.advantest.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * A resource that could not be resolved: the target does not name anything the resolver
 * understands, or the document it is relative to has no known location.
 * 
 * <p>It keeps what was searched for, so that a message can still name the target. It is also the
 * resource of a document whose location nobody told us, see {@link #UNKNOWN_DOCUMENT}: with that,
 * a missing document location needs no special case anywhere &ndash; every link relative to such a
 * document simply resolves to something that does not exist.</p>
 */
public final class UnresolvedResource implements Resource {

	/** The resource of a document whose location is unknown. */
	public static final UnresolvedResource UNKNOWN_DOCUMENT = new UnresolvedResource("");

	private final String resourcePath;

	/**
	 * Creates a resource for a target that could not be resolved.
	 * 
	 * @param resourcePath the path that could not be resolved, e.g. an unresolvable link target, must not be
	 *                    <code>null</code>
	 * @throws IllegalArgumentException if the given argument is <code>null</code>
	 */
	public UnresolvedResource(String resourcePath) {
		if (resourcePath == null) {
			throw new IllegalArgumentException("Argument must not be null.");
		}
		this.resourcePath = resourcePath;
	}

	@Override
	public String getResolvedPath() {
		return this.resourcePath;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public Optional<ResourceKind> getKind() {
		return Optional.empty();
	}

	/**
	 * Fails, because a resource nobody resolved names nothing that could be read.
	 * 
	 * @return never
	 * @throws IOException always, naming what was searched for
	 */
	@Override
	public BufferedReader readContents() throws IOException {
		throw new IOException(String.format("'%s' could not be resolved and can therefore not be read.",
				this.resourcePath));
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof UnresolvedResource)) {
			return false;
		}
		return this.resourcePath.equals(((UnresolvedResource) other).resourcePath);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.resourcePath);
	}

	@Override
	public String toString() {
		return "unresolved: " + this.resourcePath;
	}

}
