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

public class MarkdownCoreInfoTest {
	
	@Test
	public void testReadingProperties() {
		assertNotNull(MarkdownCoreInfo.getVersion());
		assertNotNull(MarkdownCoreInfo.getPlantUmlVersion());
		assertNotNull(MarkdownCoreInfo.getFlexmarkVersion());
		assertFalse(MarkdownCoreInfo.getVersion().isBlank());
		assertFalse(MarkdownCoreInfo.getPlantUmlVersion().isBlank());
		assertFalse(MarkdownCoreInfo.getFlexmarkVersion().isBlank());
		assertNotEquals("${project.version}", MarkdownCoreInfo.getVersion());
		assertNotEquals("${plantuml-lib-version}", MarkdownCoreInfo.getPlantUmlVersion());
		assertNotEquals("${flexmark-version}", MarkdownCoreInfo.getFlexmarkVersion());
	}
	
	@Test
	public void testReadingSecurityProfile() {
		assertEquals("LEGACY", MarkdownCoreInfo.getPlantUmlSecurityProfile());
	}
	
	@Test
	public void testReadingUrlAllowList() {
		assertEquals("", MarkdownCoreInfo.getPlantUmlUrlAllowList());

		String allowedUrls = "https://plantuml.com/;https://my.domain.com/some-path/";
		System.setProperty("plantuml.allowlist.url", allowedUrls);
		
		assertEquals(allowedUrls, MarkdownCoreInfo.getPlantUmlUrlAllowList());
	}
	
	@Test
	public void testReadingGraphvizVersion() {
		assertNotNull(MarkdownCoreInfo.getGraphvizVersion());
		assertFalse(MarkdownCoreInfo.getGraphvizVersion().isBlank());
		assertTrue(MarkdownCoreInfo.getGraphvizVersion().matches("\\d+\\.\\d+\\.\\d+.*"));
	}

}
