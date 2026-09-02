/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2026 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.markdown.resources;

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

	private final String searchedFor;

	/**
	 * Creates a resource for a target that could not be resolved.
	 * 
	 * @param searchedFor what was searched for, e.g. the unresolvable link target, must not be
	 *                    <code>null</code>
	 * @throws IllegalArgumentException if the given argument is <code>null</code>
	 */
	public UnresolvedResource(String searchedFor) {
		if (searchedFor == null) {
			throw new IllegalArgumentException("Argument must not be null.");
		}
		this.searchedFor = searchedFor;
	}

	@Override
	public String resolvedPath() {
		return this.searchedFor;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public Optional<ResourceKind> kind() {
		return Optional.empty();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof UnresolvedResource)) {
			return false;
		}
		return this.searchedFor.equals(((UnresolvedResource) other).searchedFor);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.searchedFor);
	}

	@Override
	public String toString() {
		return "unresolved: " + this.searchedFor;
	}

}
