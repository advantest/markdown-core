/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2026 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.resources;

/**
 * Resolves a path a document refers to, e.g. the target of a Markdown link or the file name of an
 * included PlantUML diagram, against the document it is written in.
 * 
 * <p>This is the one place where the environment enters: an Eclipse plug-in resolves against the
 * workspace, a language server against the file system, a web renderer against a repository of a
 * version control system. A caller registers the implementation matching its environment, and it is
 * the same implementation that created the {@link Resource} of the document itself, so no format of
 * a document location has to be agreed on anywhere.</p>
 * 
 * <p>A resolver is asked with a bare path, i.e. without a query string and without an anchor, and it
 * is not asked for web addresses.</p>
 * 
 * @see Resource
 */
@FunctionalInterface
public interface ResourceResolver {

	/**
	 * Resolves the given resource path as it is seen from the given document.
	 * 
	 * <p>Implementations never return <code>null</code> and never throw because a resource does not
	 * exist, its path cannot be interpreted, or the document has no known location. Each of these is
	 * answered with a resource that does not {@link Resource#exists() exist} and that names the
	 * unresolved path, so that a caller can report the problem where it belongs to.</p>
	 * 
	 * @param resourcePath the path to be resolved, usually relative to the given document,
	 *                     must be neither <code>null</code> nor blank
	 * @param document the resource of the document the path is written in, must not be
	 *                 <code>null</code>; a document of unknown location is passed as
	 *                 {@link UnresolvedResource#UNKNOWN_DOCUMENT}
	 * @return the resolved resource, never <code>null</code>
	 * @throws IllegalArgumentException if the resource path is <code>null</code> or blank or if the
	 *                                  document is <code>null</code>
	 */
	Resource resolve(String resourcePath, Resource document);

}
