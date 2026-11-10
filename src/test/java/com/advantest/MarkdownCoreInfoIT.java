/*
 * Copyright © 2026 Advantest Europe GmbH
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.advantest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Test;

/**
 * Reads the version from the JAR the build made, which the integration tests run on rather than on
 * the class folder.
 */
class MarkdownCoreInfoIT {
	
	private static String expectedVersion() {
		String expected = System.getProperty("expectedVersion");
		assertNotNull(expected, "The build names the version it expects.");
		return expected;
	}
	
	@Test
	void testReadingTheVersionFromTheJar() {
		URL resource = MarkdownCoreInfo.class.getResource("/" + MarkdownCoreInfo.VERSION_RESOURCE);
		assertNotNull(resource);
		assertEquals("jar", resource.getProtocol());
		
		assertEquals(expectedVersion(), MarkdownCoreInfo.getVersion());
	}
	
	@Test
	void testNamingTheVersionInTheManifestOfTheJar() throws Exception {
		File jarFile = new File(MarkdownCoreInfo.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		
		try (JarFile jar = new JarFile(jarFile)) {
			assertEquals(expectedVersion(),
					jar.getManifest().getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION));
		}
	}
	
}
