/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2026 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.resources;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

/**
 * Resolves a reference of the <code>file</code> scheme, e.g.
 * <code>file:///home/user/docs/guide.md</code>, in the file system of the machine this code runs
 * on.
 * 
 * <p>A file is the same file however it is named, so a reference naming it with a scheme leads to
 * the same resource as a reference naming it by its path. The scheme says which part of the
 * reference is the path; the path itself is then resolved by the
 * {@link LocalFileSystemResourceResolver}.</p>
 */
public final class FileSchemeUriResolver implements UriResolver {

	private static final String FILE_SCHEME = "file";

	private final ResourceResolver localFileSystemResolver;

	/**
	 * Creates a resolver handing the path of a <code>file</code> URI to a resolver of the local
	 * file system of its own.
	 */
	public FileSchemeUriResolver() {
		this(new LocalFileSystemResourceResolver());
	}

	/**
	 * Creates a resolver handing the path of a <code>file</code> URI to the given resolver.
	 * 
	 * @param localFileSystemResolver the resolver of the local file system, must not be
	 *                                <code>null</code>
	 * @throws IllegalArgumentException if the given resolver is <code>null</code>
	 */
	public FileSchemeUriResolver(ResourceResolver localFileSystemResolver) {
		if (localFileSystemResolver == null) {
			throw new IllegalArgumentException("Argument must not be null.");
		}
		this.localFileSystemResolver = localFileSystemResolver;
	}

	@Override
	public boolean isResponsibleFor(URI targetUri) {
		if (targetUri == null) {
			throw new IllegalArgumentException("Argument must not be null.");
		}
		return FILE_SCHEME.equalsIgnoreCase(targetUri.getScheme());
	}

	@Override
	public Resource resolve(URI targetUri, Resource referencingDocument) {
		if (targetUri == null || referencingDocument == null) {
			throw new IllegalArgumentException("Arguments must not be null.");
		}

		String path = pathOf(targetUri);
		if (path == null || path.isBlank()) {
			return new UnresolvedResource(targetUri.toString());
		}

		return this.localFileSystemResolver.resolve(path, referencingDocument);
	}

	/**
	 * Reads the path a <code>file</code> URI names.
	 * 
	 * <p>A URI naming a whole location, e.g. <code>file:///C:/docs/guide.md</code>, is turned into
	 * the path of this file system, which is what tells a drive letter from the beginning of an
	 * absolute path. Everything else &ndash; a relative reference, or one this file system cannot
	 * read &ndash; keeps the path as it is written.</p>
	 */
	private static String pathOf(URI targetUri) {
		URI withoutFragment = withoutFragment(targetUri);

		try {
			return Path.of(withoutFragment).toString();
		} catch (RuntimeException exception) {
			return withoutFragment.getPath() != null
					? withoutFragment.getPath()
					: withoutFragment.getSchemeSpecificPart();
		}
	}

	/**
	 * Removes the fragment of the given URI, so that the rest of it can be read as a path. A
	 * fragment names a place inside a file, which the file system knows nothing about.
	 */
	private static URI withoutFragment(URI targetUri) {
		if (targetUri.getFragment() == null) {
			return targetUri;
		}

		try {
			return new URI(targetUri.getScheme(), targetUri.getSchemeSpecificPart(), null);
		} catch (URISyntaxException exception) {
			return targetUri;
		}
	}

}
