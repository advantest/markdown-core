/*
 * Copyright © 2026 Advantest Europe GmbH
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.advantest.plantuml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.version.License;

/**
 * This library is available under the BSD 3-Clause License, which PlantUML's GPL edition is incompatible
 * with. These tests fail if the GPL edition reaches the classpath, beside the EPL edition or instead of it,
 * or if a library bundled in the EPL edition's JAR arrives a second time.
 */
class PlantUmlEditionTest {

	private static final String[] CLASSES_OF_BUNDLED_LIBRARIES = {
			"org/eclipse/elk/core/RecursiveGraphLayoutEngine.class",
			"org/eclipse/emf/ecore/EObject.class",
			"org/scilab/forge/jlatexmath/TeXFormula.class",
			"com/google/common/base/Preconditions.class" };

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
	void eachBundledLibraryIsOnTheClasspathOnlyOnceAndComesFromPlantUml() throws IOException {
		URL plantUmlJar = SourceStringReader.class.getProtectionDomain().getCodeSource().getLocation();

		for (String className : CLASSES_OF_BUNDLED_LIBRARIES) {
			List<URL> copies = Collections.list(getClass().getClassLoader().getResources(className));

			assertEquals(1, copies.size(), () -> className + " found in " + copies);
			assertTrue(copies.get(0).toString().startsWith("jar:" + plantUmlJar + "!/"),
					() -> className + " found in " + copies);
		}
	}
}
