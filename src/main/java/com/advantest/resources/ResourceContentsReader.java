/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2026 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.resources;

import java.io.IOException;

/**
 * Decides who answers with the contents of a {@link Resource}: the resource itself, or something
 * knowing better what it contains.
 * 
 * <p>A resource reads itself, and most of the time that is what is wanted, which is why
 * {@link #FROM_THE_RESOURCE} is the default wherever a reader can be given. Whoever knows contents
 * the resource cannot know, e.g. the text an editor holds and has not saved yet, gives a reader
 * answering with that text and asking the resource for everything else. A reader decides which
 * source answers, not how a resource reads itself.</p>
 * 
 * <p>A reader is asked from several threads at once, so an implementation has to bear that.</p>
 */
@FunctionalInterface
public interface ResourceContentsReader {

	/** Asks the resource itself, every time, which is what happens where nobody gives a reader. */
	ResourceContentsReader FROM_THE_RESOURCE = Resource::readAllContents;

	/**
	 * Answers what the given resource contains, as text.
	 * 
	 * @param resource the resource to be read, must not be <code>null</code>
	 * @return what the resource contains, never <code>null</code>, possibly empty
	 * @throws IOException if the resource cannot be read, see {@link Resource#readContents()}
	 */
	String readAllContents(Resource resource) throws IOException;

}
