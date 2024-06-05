/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2022-2024 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.markdown.renderer.html;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.advantest.markdown.MarkdownParserAndHtmlRenderer;


public class MarkdownToHtmlRendererTest {
	
	private static File markdownSourceFile;
	private MarkdownParserAndHtmlRenderer renderer;
	
	@BeforeAll
	public static void setUpBeforeAll() throws Exception {
		String path = "src/test/resources/feature-overview.md";
		markdownSourceFile = new File(path);
	}
	
	@BeforeEach
	public void setUp() {
		renderer = new MarkdownParserAndHtmlRenderer();
	}
	
	@AfterEach
	public void tearDown() {
		renderer = null;
	}
	
	@Test
	public void rendering_common_elements() throws Exception {
		String htmlSourceCode = renderer.renderHtml(renderer.parseMarkdown(markdownSourceFile));
		
		Assertions.assertNotNull(htmlSourceCode);
		Assertions.assertFalse(htmlSourceCode.isBlank());
	}

}
