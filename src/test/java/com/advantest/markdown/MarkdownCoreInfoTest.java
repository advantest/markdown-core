/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2022-2024 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.advantest.MarkdownCoreInfo;

class MarkdownCoreInfoTest {
	
	@Test
	void testReadingProperties() {
		assertNotNull(MarkdownCoreInfo.getVersion());
		assertNotNull(MarkdownCoreInfo.getPlantUmlVersion());
		assertNotNull(MarkdownCoreInfo.getFlexmarkVersion());
		assertNotNull(MarkdownCoreInfo.getFlexmarkExtensionsVersion());
		assertFalse(MarkdownCoreInfo.getVersion().isBlank());
		assertFalse(MarkdownCoreInfo.getPlantUmlVersion().isBlank());
		assertFalse(MarkdownCoreInfo.getFlexmarkVersion().isBlank());
		assertFalse(MarkdownCoreInfo.getFlexmarkExtensionsVersion().isBlank());
		assertNotEquals("${project.version}", MarkdownCoreInfo.getVersion());
		assertNotEquals("${flexmark-version}", MarkdownCoreInfo.getFlexmarkVersion());
		assertNotEquals("${flexmark-extensions-version}", MarkdownCoreInfo.getFlexmarkVersion());
	}
	
	@Test
	void testReadingSecurityProfile() {
		assertEquals("LEGACY", MarkdownCoreInfo.getPlantUmlSecurityProfile());
	}
	
	@Test
	void testReadingUrlAllowList() {
		assertEquals("", MarkdownCoreInfo.getPlantUmlUrlAllowList());

		String allowedUrls = "https://plantuml.com/;https://my.domain.com/some-path/";
		System.setProperty("plantuml.allowlist.url", allowedUrls);
		
		assertEquals(allowedUrls, MarkdownCoreInfo.getPlantUmlUrlAllowList());
	}
	
	@Test
	void testReadingGraphvizVersion() {
		String version = MarkdownCoreInfo.getGraphvizVersion();
		assertNotNull(version);
		assertFalse(version.isBlank());
		assertTrue(version.matches("\\d+\\.\\d+\\.\\d+.*"));
	}
	
	@Test
	void testReadingGraphvizExecutable() {
		String executable = MarkdownCoreInfo.getGraphvizExecutable();
		assertNotNull(executable);
		assertFalse(executable.isBlank());
		assertTrue(executable.endsWith("dot") || executable.endsWith("dot.exe"));
	}

}
