/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2026 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.plantuml;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link PlantUmlToSvgRenderer}, which draws diagrams of a kind that needs no Graphviz,
 * so that they can be drawn on every machine.
 */
class PlantUmlToSvgRendererTest {

	private static final String SEQUENCE_DIAGRAM = "@startuml\nAlice -> Bob: Hello Bob\n@enduml\n";

	@TempDir
	private Path tempDir;

	private final PlantUmlToSvgRenderer renderer = new PlantUmlToSvgRenderer();

	@Test
	void drawsDiagramCodeAsSvgContainingTheNamesInTheDiagram() {
		String svg = this.renderer.plantUmlToSvg(SEQUENCE_DIAGRAM);

		assertTrue(svg.contains("<svg"));
		assertTrue(svg.contains("Alice"));
		assertTrue(svg.contains("Bob"));
		assertTrue(svg.contains("Hello Bob"));
	}

	@Test
	void drawsTheSameDiagramTheSameWayEveryTime() {
		assertEquals(this.renderer.plantUmlToSvg(SEQUENCE_DIAGRAM), this.renderer.plantUmlToSvg(SEQUENCE_DIAGRAM));
	}

	@Test
	void reportsDiagramCodeThatIsNullAsAFailure() {
		RuntimeException failure = assertThrows(RuntimeException.class, () -> this.renderer.plantUmlToSvg((String) null));

		assertNotNull(failure.getCause());
	}

	@Test
	void writesTheSvgOfADiagramFileNextToTheFileByDefault() throws IOException {
		File source = writeDiagramFile("sequence.puml");

		File svgFile = this.renderer.plantUmlToSvg(source);

		assertNotNull(svgFile);
		assertEquals(source.getParentFile(), svgFile.getParentFile());
		assertTrue(svgFile.getName().endsWith(".svg"));
		assertTrue(Files.readString(svgFile.toPath()).contains("Hello Bob"));
	}

	@Test
	void writesTheSvgOfADiagramFileIntoTheDirectoryItIsGiven() throws IOException {
		File source = writeDiagramFile("sequence.puml");
		File targetDirectory = Files.createDirectories(this.tempDir.resolve("out")).toFile();

		File svgFile = this.renderer.plantUmlToSvg(source, targetDirectory);

		assertNotNull(svgFile);
		assertEquals(targetDirectory, svgFile.getParentFile());
		assertTrue(Files.readString(svgFile.toPath()).contains("Hello Bob"));
	}

	@Test
	void answersNothingForAFileHoldingNoDiagram() throws IOException {
		File source = this.tempDir.resolve("empty.puml").toFile();
		Files.writeString(source.toPath(), "no diagram in here\n");

		assertNull(this.renderer.plantUmlToSvg(source));
	}

	@Test
	void refusesADiagramFileThatCannotBeRead() {
		File missingFile = this.tempDir.resolve("missing.puml").toFile();

		IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> this.renderer.plantUmlToSvg(missingFile));

		assertTrue(failure.getMessage().contains(missingFile.toString()));
		assertThrows(IllegalArgumentException.class, () -> this.renderer.plantUmlToSvg((File) null));
	}

	private File writeDiagramFile(String name) throws IOException {
		Path source = this.tempDir.resolve(name);
		Files.writeString(source, SEQUENCE_DIAGRAM);
		return source.toFile();
	}

}