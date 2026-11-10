/*
 * Copyright © 2026 Advantest Europe GmbH
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.advantest.resources;

/**
 * Resolves a reference that names no scheme, e.g. <code>../images/logo.png</code>, against the
 * document it is written in.
 * 
 * <p>Such a reference says nothing about where it is looked for. It has a meaning only together
 * with the document carrying it, and what that meaning is depends on where the documents live: on
 * the machine this code runs on it names a file next to the document, while for a renderer reading
 * its documents from the web interface of a version control system it names an address built from
 * the address of the document. A caller registers the one implementation matching the place its
 * documents come from.</p>
 * 
 * <p>There is exactly one such resolver, because a reference without a scheme says nothing that
 * could tell two of them apart. A resolver claiming a scheme implements {@link UriResolver}
 * instead, and several of those can be registered side by side.</p>
 * 
 * @see UriResolver
 */
@FunctionalInterface
public interface RelativePathResourceResolver extends ResourceResolver {

}
