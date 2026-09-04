/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2026 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.resources;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves an absolute path of the file system of the machine this code runs on, e.g.
 * <code>/usr/share/doc/guide.md</code> or <code>C:\documents\guide.md</code>.
 * 
 * <p>Such a path names a resource on its own, so the document it is written in is not looked at:
 * wherever that document is, the path leads to the same place. This is what tells it apart from a
 * path without a leading separator, which is resolved by a
 * {@link RelativePathResourceResolver} and means something else in every environment.</p>
 * 
 * <p>A path that is not absolute, that is no valid path of this file system, or that is left empty
 * once its fragment is cut off yields an {@link UnresolvedResource}: this resolver is asked for
 * absolute paths, and anything else is not its business.</p>
 */
public final class AbsoluteLocalPathResolver implements ResourceResolver {

	@Override
	public Resource resolve(String targetResourcePathOrUri, Resource referencingDocument) {
		if (targetResourcePathOrUri == null || targetResourcePathOrUri.isBlank()) {
			throw new IllegalArgumentException("Argument must be a non-blank resource path.");
		}
		if (referencingDocument == null) {
			throw new IllegalArgumentException("Argument must not be null.");
		}

		String resourcePath = withoutFragment(targetResourcePathOrUri);
		if (resourcePath.isBlank()) {
			// the reference names a place inside the document it is written in, not a resource
			return new UnresolvedResource(targetResourcePathOrUri);
		}

		try {
			Path target = Paths.get(resourcePath);

			if (!target.isAbsolute()) {
				return new UnresolvedResource(resourcePath);
			}

			return LocalFileSystemResource.of(target);
		} catch (InvalidPathException e) {
			return new UnresolvedResource(resourcePath);
		}
	}

	/**
	 * Cuts off the fragment of the given reference, i.e. everything from the first <code>#</code>
	 * onwards.
	 */
	private static String withoutFragment(String targetResourcePathOrUri) {
		int indexOfHashTag = targetResourcePathOrUri.indexOf('#');
		return indexOfHashTag > -1
				? targetResourcePathOrUri.substring(0, indexOfHashTag)
				: targetResourcePathOrUri;
	}

}
