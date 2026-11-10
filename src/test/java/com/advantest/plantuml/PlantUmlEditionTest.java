/*
 * Copyright © 2026 Advantest Europe GmbH
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.advantest.plantuml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Test;

import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.version.License;

/**
 * This library is available under the BSD 3-Clause License, which PlantUML's GPL edition is incompatible
 * with. These tests fail if the GPL edition reaches the classpath, beside the EPL edition or instead of it,
 * or if Maven resolves a library bundled in the EPL edition's JAR in another version than the one bundled.
 */
class PlantUmlEditionTest {


	@Test
	void plantUmlIsTheEplEdition() {
		// PlantUML's build lets toString() name the edition: "EPL", "GPL", "MIT" and so on
		assertEquals("EPL", License.getCurrent().toString());
	}

	@Test
	void onlyOneEditionOfPlantUmlIsOnTheClasspath() throws IOException {
		List<URL> copies = Collections.list(
				getClass().getClassLoader().getResources("net/sourceforge/plantuml/SourceStringReader.class"));

		assertEquals(1, copies.size(), () -> "PlantUML found in " + copies);
		assertTrue(copies.get(0).toString().contains("plantuml-epl"), () -> "PlantUML found in " + copies);
	}

	@Test
	void eachLibraryBundledInPlantUmlResolvesInTheVersionBundled() throws IOException, URISyntaxException {
		ClassLoader classLoader = getClass().getClassLoader();
		URL plantUmlJarLocation = SourceStringReader.class.getProtectionDomain().getCodeSource().getLocation();
		String plantUmlJarUrl = "jar:" + plantUmlJarLocation + "!/";
		List<String> bundled = new ArrayList<>();

		try (JarFile jar = new JarFile(Path.of(plantUmlJarLocation.toURI()).toFile())) {
			jar.stream().map(JarEntry::getName)
					.filter(name -> name.startsWith("META-INF/maven/") && name.endsWith("/pom.properties"))
					.forEach(bundled::add);
		}
		assertFalse(bundled.isEmpty());

		for (String pomProperties : bundled) {
			List<URL> copies = Collections.list(classLoader.getResources(pomProperties));
			List<URL> resolved = copies.stream().filter(copy -> !copy.toString().startsWith(plantUmlJarUrl)).toList();
			URL inPlantUml = copies.stream().filter(copy -> copy.toString().startsWith(plantUmlJarUrl)).findFirst()
					.orElseThrow();

			assertEquals(1, resolved.size(), () -> pomProperties + " found in " + copies);
			assertEquals(versionIn(inPlantUml), versionIn(resolved.get(0)), pomProperties);
		}
	}

	private static String versionIn(URL pomProperties) throws IOException {
		Properties properties = new Properties();
		try (InputStream in = pomProperties.openStream()) {
			properties.load(in);
		}
		return properties.getProperty("version");
	}
}