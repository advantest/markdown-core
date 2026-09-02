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
 * Resolves link targets in the file system of the machine this code runs on.
 * 
 * <p>The resolver is stateless and has no root directory: a target is always resolved relative to
 * the document it is written in, i.e. relative to that document's directory. An absolute target is
 * taken as it is. Everything that cannot be resolved &ndash; a target that is no valid path of this
 * file system, or a document without a known location &ndash; yields an
 * {@link UnresolvedResource}.</p>
 */
public final class LocalFileSystemResourceResolver implements ResourceResolver {

	@Override
	public Resource resolve(String resourcePath, Resource document) {
		if (resourcePath == null || resourcePath.isBlank()) {
			throw new IllegalArgumentException("Argument must be a non-blank resource path.");
		}
		if (document == null) {
			throw new IllegalArgumentException("Argument must not be null.");
		}

		try {
			Path target = Paths.get(resourcePath);

			if (target.isAbsolute()) {
				return LocalFileSystemResource.of(target);
			}

			Path documentDirectory = directoryOf(document);
			if (documentDirectory == null) {
				return new UnresolvedResource(resourcePath);
			}

			return LocalFileSystemResource.of(documentDirectory.resolve(target));
		} catch (InvalidPathException e) {
			return new UnresolvedResource(resourcePath);
		}
	}

	private Path directoryOf(Resource document) {
		if (!(document instanceof LocalFileSystemResource)) {
			return null;
		}
		Path documentPath = ((LocalFileSystemResource) document).getResolvedFilePath();
		return documentPath.getParent();
	}

}
