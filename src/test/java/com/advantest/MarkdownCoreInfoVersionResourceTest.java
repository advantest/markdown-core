/*
 * Copyright © 2026 Advantest Europe GmbH
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.advantest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class MarkdownCoreInfoVersionResourceTest {
	
	@Test
	void testReadingTheVersionResourceOfThisLibrary() {
		String expected = System.getProperty("expectedVersion");
		assertNotNull(expected, "The build names the version it expects.");
		
		assertEquals(expected, MarkdownCoreInfo.readVersion(MarkdownCoreInfo.VERSION_RESOURCE));
	}
	
	@Test
	void testAnsweringNoVersionWhereTheResourceIsMissing() {
		assertNull(MarkdownCoreInfo.readVersion("META-INF/com.advantest.markdown/no-such-library/version.properties"));
	}
	
	@Test
	void testAnsweringNoVersionWhereTheResourceNamesNone() {
		assertNull(MarkdownCoreInfo.readVersion("com/advantest/version-resources/no-version.properties"));
	}
	
	@Test
	void testAnsweringNoVersionWhereTheResourceWasNotFilledIn() {
		assertNull(MarkdownCoreInfo.readVersion("com/advantest/version-resources/unfiltered-version.properties"));
	}
	
}
