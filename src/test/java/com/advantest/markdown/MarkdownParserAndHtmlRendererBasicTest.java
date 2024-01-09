package com.advantest.markdown;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;


public class MarkdownParserAndHtmlRendererBasicTest {
	
	private MarkdownParserAndHtmlRenderer parserRenderer;
	
	@BeforeEach
	public void setUp() {
		parserRenderer = new MarkdownParserAndHtmlRenderer();
	}
	
	@AfterEach
	public void tearDown() {
		parserRenderer = null;
	}
	
	@ParameterizedTest
	@CsvSource({
		"fileName.md, md",
		"otherFile.puml, puml",
		"/usr/local/something/file.x, x",
		"someDirectory, ",
	    "some/path/to/directory, ",
	    "some/path/file., "
	})
	public void readingFileExtensionWorks(String filePath, String fileExtension) {
		File file = new File(filePath);
		
		String returnedFileExtension = parserRenderer.getFileExtension(file);
		
		assertEquals(fileExtension, returnedFileExtension);
	}
	
	@Test
	public void readingTextFileWorks() throws Exception {
		String filePath = "src/test/resources/PlantUML/testGraphviz.puml";
		File file = new File(filePath);
		
		String fileContents = parserRenderer.readTextFromFile(file);
		
		String expected = "@startuml\ntestdot\n@enduml";
		assertEquals(expected, fileContents);
	}
	
	@Test
	public void readingNonExistentFileThrowsException() throws Exception {
		String filePath = "src/test/resources/nonExistentFile.foo";
		File file = new File(filePath);
		
		assertThrows(IOException.class,
				() -> parserRenderer.readTextFromFile(file));
	}
	
}
