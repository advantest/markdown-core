/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2026 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.resources;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * A {@link Resource} in the file system of the machine this code runs on.
 * 
 * <p>Its {@link #getResolvedPath()} is the absolute file system path, i.e. what a user would type into
 * a shell or a file dialog of the same machine.</p>
 * 
 * @see LocalFileSystemResourceResolver
 */
public final class LocalFileSystemResource implements Resource {

	private final Path resolvedPath;

	private LocalFileSystemResource(Path resolvedPath) {
		this.resolvedPath = resolvedPath;
	}

	/**
	 * Creates a resource for the given path. A relative path is made absolute against the working
	 * directory of the running process.
	 * 
	 * @param path the path of the resource, must not be <code>null</code>
	 * @return the resource, never <code>null</code>
	 * @throws IllegalArgumentException if the given path is <code>null</code>
	 */
	public static LocalFileSystemResource of(Path path) {
		if (path == null) {
			throw new IllegalArgumentException("Argument must not be null.");
		}
		return new LocalFileSystemResource(path.toAbsolutePath().normalize());
	}

	/**
	 * Creates a resource for the given file.
	 * 
	 * @param file the file or directory of the resource, must not be <code>null</code>
	 * @return the resource, never <code>null</code>
	 * @throws IllegalArgumentException if the given file is <code>null</code>
	 */
	public static LocalFileSystemResource of(File file) {
		if (file == null) {
			throw new IllegalArgumentException("Argument must not be null.");
		}
		return of(file.toPath());
	}

	/**
	 * Returns the resolved path of this resource as a path of the local file system, i.e.
	 * {@link #getResolvedPath()} as a {@link Path}.
	 * 
	 * @return the absolute, normalized path, never <code>null</code>
	 */
	public Path getResolvedFilePath() {
		return this.resolvedPath;
	}

	@Override
	public String getResolvedPath() {
		return this.resolvedPath.toString();
	}

	@Override
	public boolean exists() {
		return Files.exists(this.resolvedPath);
	}

	@Override
	public Optional<ResourceKind> getKind() {
		if (Files.isDirectory(this.resolvedPath)) {
			return Optional.of(ResourceKind.DIRECTORY);
		}
		if (Files.isRegularFile(this.resolvedPath)) {
			return Optional.of(ResourceKind.FILE);
		}
		return Optional.empty();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof LocalFileSystemResource)) {
			return false;
		}
		return this.resolvedPath.equals(((LocalFileSystemResource) other).resolvedPath);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.resolvedPath);
	}

	@Override
	public String toString() {
		return getResolvedPath();
	}

}
