/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2022-2024 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.advantest.MarkdownCoreInfo;

import net.sourceforge.plantuml.dot.GraphvizRuntimeEnvironment;

class MarkdownCoreInfoTest {
	
	@AfterEach
	void leaveTheSettingsAsTheyWere() {
		System.clearProperty("plantuml.allowlist.url");
	}
	
	@Test
	void testReadingTheVersion() {
		String expected = System.getProperty("expectedVersion");
		assertNotNull(expected, "The build names the version it expects.");
		
		assertEquals(expected, MarkdownCoreInfo.getVersion());
		assertFalse(MarkdownCoreInfo.getVersion().contains("${"));
	}
	
	@Test
	void testReadingThePlantUmlVersion() {
		assertNotNull(MarkdownCoreInfo.getPlantUmlVersion());
		assertFalse(MarkdownCoreInfo.getPlantUmlVersion().isBlank());
	}
	
	@Test
	@SuppressWarnings("deprecation")
	void testReadingTheVersionsOfTheFlexmarkLibrariesThatRun() {
		String expectedFlexmark = System.getProperty("expectedFlexmarkVersion");
		String expectedExtensions = System.getProperty("expectedFlexmarkExtensionsVersion");
		assertNotNull(expectedFlexmark, "The build names the flexmark version it expects.");
		assertNotNull(expectedExtensions, "The build names the flexmark-extensions version it expects.");
		
		assertEquals(expectedFlexmark, MarkdownCoreInfo.getFlexmarkVersion());
		assertEquals(expectedExtensions, MarkdownCoreInfo.getFlexmarkExtensionsVersion());
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
	
	@Test
	void testReadingTheGraphvizExecutableNamedLast() throws IOException {
		String executableBefore = MarkdownCoreInfo.getGraphvizExecutable();
		File anotherExecutable = File.createTempFile("dot-of-a-test", ".exe");
		anotherExecutable.deleteOnExit();
		
		try {
			GraphvizRuntimeEnvironment.getInstance().setDotExecutable(anotherExecutable.getAbsolutePath());
			
			assertEquals(anotherExecutable.getAbsolutePath(), MarkdownCoreInfo.getGraphvizExecutable());
		} finally {
			GraphvizRuntimeEnvironment.getInstance().setDotExecutable(null);
		}
		
		assertEquals(executableBefore, MarkdownCoreInfo.getGraphvizExecutable());
	}

	@Test
	void testReadingTheGraphvizVersionOfAnExecutableThatCannotBeRun() throws IOException {
		File notRunnable = File.createTempFile("dot-of-a-test", ".exe");
		notRunnable.deleteOnExit();
		
		try {
			GraphvizRuntimeEnvironment.getInstance().setDotExecutable(notRunnable.getAbsolutePath());
			
			assertEquals("", MarkdownCoreInfo.getGraphvizVersion());
		} finally {
			GraphvizRuntimeEnvironment.getInstance().setDotExecutable(null);
		}
	}

}
