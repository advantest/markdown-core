/*
 * Copyright © 2026 Advantest Europe GmbH
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.advantest.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Optional;

/**
 * Something a Markdown document refers to, or is itself: a file, a directory, or a target that
 * could not be resolved at all.
 * 
 * <p>Instances are created by a {@link ResourceResolver} and only that resolver knows what a
 * resource really is &ndash; a file of the local file system, a file in a version control system,
 * a document served over HTTP. Everything working on a parsed Markdown document therefore treats a
 * resource as opaque: it asks whether the resource exists, of which {@link ResourceKind} it is, how
 * to name it in a message, and how to read what it contains. No format of
 * {@link #getResolvedPath()} may be assumed.</p>
 * 
 * <p>A resource is a description, not a snapshot. {@link #exists()}, {@link #getKind()} and
 * {@link #readContents()} answer for the moment they are called.</p>
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
	String getResolvedPath();

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
	Optional<ResourceKind> getKind();

	/**
	 * Opens what this resource contains, as text, for reading it piece by piece.
	 * 
	 * <p>Only the {@link ResourceResolver} that created the resource knows where the content comes
	 * from and how bytes become characters, so it is asked rather than told: a caller wanting to
	 * look into a document it did not parse itself, e.g. to find an anchor a link points to, reads
	 * it here instead of assuming that the resource is a file of the machine it runs on.</p>
	 * 
	 * <p>The reader is opened, not read, and the caller closes it &ndash; a resource read this way
	 * may be arbitrarily large or may arrive over a network, and neither is worth holding in memory
	 * as a whole. A caller that does want it as a whole asks {@link #readAllContents()}.</p>
	 * 
	 * <p>A resource that does not {@link #exists() exist}, one that is a {@link ResourceKind#DIRECTORY}
	 * and one that holds no text fail rather than answering with an empty text: not knowing what a
	 * resource contains is different from knowing that it contains nothing. A failure of the content
	 * itself, e.g. bytes that are no text of the encoding the resolver reads, may as well be raised by
	 * the returned reader rather than here.</p>
	 * 
	 * @return a reader of what this resource contains, never <code>null</code>, to be closed by the
	 *         caller
	 * @throws IOException if this resource cannot be read, among them the resource that is not
	 *                     there, the one nobody resolved and the one that holds no text
	 */
	BufferedReader readContents() throws IOException;

	/**
	 * Reads everything this resource contains, as text.
	 * 
	 * <p>Nothing is remembered between two calls: reading answers for the moment of the call, as
	 * {@link #exists()} and {@link #getKind()} do. A caller reading something it cannot bound, or
	 * reading only the beginning of it, uses {@link #readContents()} instead.</p>
	 * 
	 * @return what this resource contains, never <code>null</code>, possibly empty
	 * @throws IOException if this resource cannot be read, see {@link #readContents()}
	 */
	default String readAllContents() throws IOException {
		try (BufferedReader reader = readContents()) {
			StringWriter contents = new StringWriter();
			reader.transferTo(contents);
			return contents.toString();
		}
	}

	/**
	 * Convenience test for {@link ResourceKind#FILE}.
	 * 
	 * @return <code>true</code> if and only if this resource currently is a file
	 */
	default boolean isFile() {
		return getKind().filter(ResourceKind.FILE::equals).isPresent();
	}

	/**
	 * Convenience test for {@link ResourceKind#DIRECTORY}.
	 * 
	 * @return <code>true</code> if and only if this resource currently is a directory
	 */
	default boolean isDirectory() {
		return getKind().filter(ResourceKind.DIRECTORY::equals).isPresent();
	}

}
