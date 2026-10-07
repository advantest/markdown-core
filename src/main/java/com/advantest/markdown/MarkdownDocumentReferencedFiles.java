/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2026 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.markdown;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.advantest.flexmark.ext.plantuml.PlantUmlFileLocation;
import com.advantest.flexmark.ext.plantuml.PlantUmlFileLocations;
import com.advantest.resources.RelativePathResourceResolver;
import com.advantest.resources.Resource;
import com.advantest.resources.ResourceContentsReader;

/**
 * The text of everything a document refers to, read when it is asked for, and where each of those
 * targets was looked for.
 * 
 * <p>A renderer working on a parsed document asks for a target the way it is written in that
 * document, e.g. <code>diagrams/classes.puml</code>, and is answered with the text behind it. The
 * target is resolved against the resource the document came from and read through the
 * {@link ResourceContentsReader} this map was given, which asks that resource unless it knows better,
 * so a document of the local file system, one of a repository and one served over a network are all
 * answered the same way and none of them is read here.</p>
 * 
 * <p>Nothing is read before it is asked for: a document that is only parsed or validated reads
 * nothing at all, and a target asked for twice is read once. A target that cannot be resolved or
 * cannot be read is answered with <code>null</code>, which is how a caller says that it has no
 * text, and the reason is logged rather than thrown &ndash; whoever renders a reference decides
 * what to show in its place. Such a renderer may then ask {@link #locationOf(String) where} the
 * target was looked for and tell its reader that place, which is the only way that reader learns
 * which file to go looking for.</p>
 * 
 * <p>What is {@link #put(String, String) put in} wins over reading. A caller that already knows the
 * text of a target, or that hands over text it produced itself rather than the text of a file, says
 * so this way and is never read behind its back. This map therefore answers about targets, not
 * about what it holds: {@link #size()}, {@link #containsKey(Object)} and iteration see what was put
 * in and what was read so far, never what could still be read.</p>
 */
final class MarkdownDocumentReferencedFiles extends AbstractMap<String, String>
		implements PlantUmlFileLocations {

	private static final Logger LOG = LoggerFactory.getLogger(MarkdownDocumentReferencedFiles.class);

	private final Resource documentResource;

	private final RelativePathResourceResolver resolver;

	private final Map<String, Resource> resourcesByTarget;

	private final ResourceContentsReader reader;

	private final Map<String, String> contentsByTarget = new ConcurrentHashMap<>();

	/**
	 * Creates the contents of everything the document of the given resource refers to.
	 * 
	 * @param documentResource the resource the document came from, must not be <code>null</code>
	 * @param resolver what turns a target written in that document into a resource, must not be
	 *                 <code>null</code>
	 * @param resourcesByTarget where a resolved target is remembered, must not be <code>null</code>
	 * @param reader what answers with the contents of a resolved target, must not be
	 *               <code>null</code>
	 * @throws IllegalArgumentException if one of the arguments is <code>null</code>
	 */
	MarkdownDocumentReferencedFiles(Resource documentResource, RelativePathResourceResolver resolver,
			Map<String, Resource> resourcesByTarget, ResourceContentsReader reader) {
		if (documentResource == null || resolver == null || resourcesByTarget == null || reader == null) {
			throw new IllegalArgumentException("Arguments must not be null.");
		}

		this.documentResource = documentResource;
		this.resolver = resolver;
		this.resourcesByTarget = resourcesByTarget;
		this.reader = reader;
	}

	@Override
	public String get(Object target) {
		if (!(target instanceof String targetPath) || targetPath.isBlank()) {
			return null;
		}

		String knownContents = this.contentsByTarget.get(targetPath);
		if (knownContents != null) {
			return knownContents;
		}

		Resource resource = resolvedResourceOf(targetPath);

		try {
			String contents = this.reader.readAllContents(resource);
			this.contentsByTarget.put(targetPath, contents);
			return contents;
		} catch (IOException failure) {
			LOG.debug("The document {} refers to {}, which cannot be read, so nothing is answered"
					+ " for it.", this.documentResource.getResolvedPath(), targetPath, failure);
			return null;
		}
	}

	@Override
	public PlantUmlFileLocation locationOf(String target) {
		if (target == null || target.isBlank()) {
			return null;
		}

		Resource resource = resolvedResourceOf(target);
		return new PlantUmlFileLocation(resource.getResolvedPath(), resource.exists());
	}

	/**
	 * Resolves the given target against the document it is written in, once per target.
	 * 
	 * @param target the target as it is written in the document, must not be blank
	 * @return what that target names, never <code>null</code>, an
	 *         {@link com.advantest.resources.UnresolvedResource} if it names nothing
	 */
	private Resource resolvedResourceOf(String target) {
		return this.resourcesByTarget.computeIfAbsent(target,
				path -> this.resolver.resolve(path, this.documentResource));
	}

	@Override
	public String put(String target, String contents) {
		if (target == null || contents == null) {
			throw new IllegalArgumentException("Arguments must not be null.");
		}

		return this.contentsByTarget.put(target, contents);
	}

	@Override
	public Set<Entry<String, String>> entrySet() {
		return this.contentsByTarget.entrySet();
	}

}
