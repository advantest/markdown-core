/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2026 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.resources;

/**
 * The kind of thing a {@link Resource} refers to.
 * 
 * <p>Only kinds a Markdown link may sensibly point to are distinguished. Whether a resource exists
 * at all is not a kind, see {@link Resource#exists()}.</p>
 */
public enum ResourceKind {

	/** A single readable resource, e.g. a file in a file system. */
	FILE,

	/** A container of other resources, e.g. a directory in a file system. */
	DIRECTORY;

}
