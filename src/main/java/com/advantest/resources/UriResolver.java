/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2026 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.resources;

import java.net.URI;

import org.slf4j.LoggerFactory;

/**
 * Resolves references that name their scheme, e.g. <code>https://example.org/guide</code> or
 * <code>file:/home/user/guide.md</code>.
 * 
 * <p>A reference without a scheme names a resource of the environment this code runs in, and there
 * is one {@link ResourceResolver} answering for that environment. A reference with a scheme is a
 * different matter: several resolvers may know the same scheme and tell each other apart by the
 * host or by the beginning of the address, e.g. one answering for the issue tracker of a team and
 * one answering for every other web address. Each resolver is therefore asked whether it is
 * responsible, instead of being looked up by its scheme.</p>
 * 
 * @see ResourceResolver
 */
public interface UriResolver extends ResourceResolver {

	/**
	 * Tells whether this resolver answers for the given reference.
	 * 
	 * <p>A resolver looks at as much of the reference as it needs: the scheme alone, the host, or
	 * the beginning of the whole address. It answers for what it can resolve, not for what it
	 * would like to resolve, because a reference nobody claims is left alone rather than
	 * reported.</p>
	 * 
	 * @param targetUri the reference to be resolved, must not be <code>null</code>
	 * @return <code>true</code> if and only if this resolver can resolve the given reference
	 * @throws IllegalArgumentException if the given reference is <code>null</code>
	 */
	boolean isResponsibleFor(URI targetUri);

	/**
	 * Resolves the given reference as it is seen from the given document.
	 * 
	 * <p>The same promises hold as for {@link #resolve(String, Resource)}: the answer is never
	 * <code>null</code>, and everything that cannot be resolved is a resource that does not
	 * {@link Resource#exists() exist}.</p>
	 * 
	 * @param targetUri the reference to be resolved, must not be <code>null</code>
	 * @param referencingDocument the resource of the document the reference is written in, must not
	 *                            be <code>null</code>; a document of unknown location is passed as
	 *                            {@link UnresolvedResource#UNKNOWN_DOCUMENT}
	 * @return the resolved resource, never <code>null</code>
	 * @throws IllegalArgumentException if one of the arguments is <code>null</code>
	 */
	Resource resolve(URI targetUri, Resource referencingDocument);

	/**
	 * Resolves the given reference by reading it as a URI, so that a resolver of a scheme can be
	 * asked the same way as a resolver of the environment this code runs in.
	 * 
	 * <p>A reference the URI syntax rejects names no scheme and therefore never reaches a resolver
	 * of a scheme; such a reference yields an {@link UnresolvedResource} instead of an error.</p>
	 */
	@Override
	default Resource resolve(String targetResourcePathOrUri, Resource referencingDocument) {
		if (targetResourcePathOrUri == null || targetResourcePathOrUri.isBlank()) {
			throw new IllegalArgumentException("Argument must be a non-blank resource path or URI.");
		}

		URI targetUri;
		try {
			targetUri = URI.create(targetResourcePathOrUri);
		} catch (IllegalArgumentException exception) {
			// the logger is fetched here rather than held in a constant: a field of an interface is
			// public, and this is the only place in it that says anything, on a branch that is rare
			LoggerFactory.getLogger(UriResolver.class).debug(
					"The reference '{}' is no valid URI, so no resolver of a scheme is asked about it.",
					targetResourcePathOrUri, exception);
			return new UnresolvedResource(targetResourcePathOrUri);
		}

		return resolve(targetUri, referencingDocument);
	}

}
