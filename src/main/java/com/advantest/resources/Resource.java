/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2026 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.resources;

import java.util.Optional;

/**
 * Something a Markdown document refers to, or is itself: a file, a directory, or a target that
 * could not be resolved at all.
 * 
 * <p>Instances are created by a {@link ResourceResolver} and only that resolver knows what a
 * resource really is &ndash; a file of the local file system, a file in a version control system,
 * a document served over HTTP. Everything working on a parsed Markdown document therefore treats a
 * resource as opaque: it asks whether the resource exists, of which {@link ResourceKind} it is, and
 * how to name it in a message. No format of {@link #resolvedPath()} may be assumed.</p>
 * 
 * <p>A resource is a description, not a snapshot. {@link #exists()} and {@link #kind()} answer for
 * the moment they are called.</p>
 * 
 * @see ResourceResolver
 */
public interface Resource {

	/**
	 * Returns how this resource is named towards a human reader, e.g. in a validation message.
	 * 
	 * <p>The format is decided by the {@link ResourceResolver} that created this resource, e.g. an
	 * absolute file system path or a URL of a version control system's web interface. A resource
	 * that could not be resolved names what was searched for.</p>
	 * 
	 * @return the resolved name of this resource, never <code>null</code>, possibly empty
	 */
	String resolvedPath();

	/**
	 * Tells whether this resource exists at the moment of the call.
	 * 
	 * @return <code>true</code> if and only if something is found at this resource's location
	 */
	boolean exists();

	/**
	 * Returns the kind of this resource at the moment of the call.
	 * 
	 * @return the kind, or an empty {@link Optional} if this resource does not {@link #exists()} or
	 *         if it is of no kind a Markdown link may point to
	 */
	Optional<ResourceKind> kind();

	/**
	 * Convenience test for {@link ResourceKind#FILE}.
	 * 
	 * @return <code>true</code> if and only if this resource currently is a file
	 */
	default boolean isFile() {
		return kind().filter(ResourceKind.FILE::equals).isPresent();
	}

	/**
	 * Convenience test for {@link ResourceKind#DIRECTORY}.
	 * 
	 * @return <code>true</code> if and only if this resource currently is a directory
	 */
	default boolean isDirectory() {
		return kind().filter(ResourceKind.DIRECTORY::equals).isPresent();
	}

}
