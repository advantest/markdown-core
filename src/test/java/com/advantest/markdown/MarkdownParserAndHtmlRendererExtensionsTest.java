/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2022-2024 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.markdown;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.w3c.dom.html.HTMLTableRowElement;

import static org.junit.jupiter.api.Assertions.*;

import com.vladsch.flexmark.ast.HtmlCommentBlock;
import com.vladsch.flexmark.ext.plantuml.PlantUmlBlockNode;
import com.vladsch.flexmark.ext.plantuml.PlantUmlImage;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.collection.iteration.ReversiblePeekingIterator;

public class MarkdownParserAndHtmlRendererExtensionsTest {
	
	private static final String TEST_SRC_PATH = "src/test/resources";
	
	private MarkdownParserAndHtmlRenderer parserRenderer;
	
	@BeforeEach
	public void setUp() {
		parserRenderer = new MarkdownParserAndHtmlRenderer();
	}
	
	@AfterEach
	public void tearDown() {
		parserRenderer = null;
	}
	
	@Disabled
	@Test
	public void explicitSectionAnchorsAreCorrectlyRenderedToHtml() {
		fail();
	}
	
	@Test
	public void tablesWithSingleHyphenPerColumnInDelimiterLineCorrectlyRendered() {
		String markdownSource = "|Table header|\n"
				+ "|-|\n"
				+ "|row 1|\n"
				+ "|row 2|";
		
		Document document = parserRenderer.parseMarkdown(markdownSource);
		String htmlResult = parserRenderer.renderHtml(document);
		
		assertNotNull(document);
		assertNotNull(htmlResult);
		assertFalse(htmlResult.isBlank());
		assertTrue(htmlResult.contains("<table>"));
	}
	
	@Test
	public void hiddenCommentsParsedButNotRenderedInHtml() throws Exception {
		String testFilePath = TEST_SRC_PATH + "/markdown/extensions/hidden-comments.md";
		File mdFile = new File(testFilePath);
		
		Document document = parserRenderer.parseMarkdown(mdFile);
		String htmlResult = parserRenderer.renderHtml(document);
		
		assertNotNull(document);
		List<HtmlCommentBlock> commentNodes = collectAstNodes(HtmlCommentBlock.class, document);
		assertEquals(4, commentNodes.size());
		assertEquals("<!-- Some usual one-line comment, not hidden -->\n", commentNodes.get(0).getChars().toString());
		assertEquals("<!-- Comments can be spanned\nover \nseveral lines -->\n", commentNodes.get(1).getChars().toString());
		assertEquals("<!--- Hidden comment --->\n", commentNodes.get(2).getChars().toString());
		assertEquals("<!---\nHidden\nmulti-line\ncomment\n--->\n", commentNodes.get(3).getChars().toString());
		
		assertNotNull(htmlResult);
		assertFalse(htmlResult.isBlank());
		String regex = "<!--\\s+Some usual one-line comment, not hidden\\s+-->";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(htmlResult);
		assertTrue(matcher.find());
		
		regex = "<!--\\s+Comments can be spanned\\nover \\nseveral lines\\s+-->";
		pattern = Pattern.compile(regex);
		matcher = pattern.matcher(htmlResult);
		assertTrue(matcher.find());
		
		// TODO Implement hidden comments flexmark extension and re-activate these checks
//		regex = "Hidden comment";
//		pattern = Pattern.compile(regex);
//		matcher = pattern.matcher(htmlResult);
//		assertFalse(matcher.find(), "Found hidden comment in rendered HTML");
//		
//		regex = "Hidden\\smulti-line\\scomment";
//		pattern = Pattern.compile(regex);
//		matcher = pattern.matcher(htmlResult);
//		assertFalse(matcher.find(), "Found hidden comment in rendered HTML");
	}
	
	@Test
	public void plantUmlIncludesAreRenderedToSvg() throws Exception {
		String testFilePath = TEST_SRC_PATH + "/markdown/extensions/puml-include.md";
		File mdFile = new File(testFilePath);
		
		Document document = parserRenderer.parseMarkdown(mdFile);
		String htmlResult = parserRenderer.renderHtml(document);
		
		assertNotNull(document);
		List<PlantUmlImage> plantUmlImageNodes = collectAstNodes(PlantUmlImage.class, document);
		assertEquals(2, plantUmlImageNodes.size());
		assertEquals("diagrams/plantumlComponents.puml", plantUmlImageNodes.get(0).getUrl().toString());
		assertEquals("../../PlantUML/classes.puml", plantUmlImageNodes.get(1).getUrl().toString());
		
		assertNotNull(htmlResult);
		assertFalse(htmlResult.isBlank());
		String regex = "<figure>\\s*<svg ";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(htmlResult);
		int numberOfRenderedSvgImages = matcher.results().collect(Collectors.toList()).size();
		assertEquals(2, numberOfRenderedSvgImages);
	}
	
	@Test
	public void plantUmlCodeBlocksAreRenderedToSvg() throws Exception {
		String testFilePath = TEST_SRC_PATH + "/markdown/extensions/inline-puml.md";
		File mdFile = new File(testFilePath);
		
		Document document = parserRenderer.parseMarkdown(mdFile);
		String htmlResult = parserRenderer.renderHtml(document);
		
		assertNotNull(document);
		List<PlantUmlBlockNode> plantUmlCodeBlockNodes = collectAstNodes(PlantUmlBlockNode.class, document);
		assertEquals(2, plantUmlCodeBlockNodes.size());
		
		assertNotNull(htmlResult);
		assertFalse(htmlResult.isBlank());
		String regex = "<figure>\\s*<svg ";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(htmlResult);
		assertEquals(2, matcher.results().collect(Collectors.toList()).size());
	}
	
	@SuppressWarnings("unchecked")
	private <T> List<T> collectAstNodes(Class<T> nodeTypeToCollect, Node astRootNode) {
		List<T> commentNodes = new ArrayList<>();
		
		ReversiblePeekingIterator<Node> iterator = astRootNode.getDescendants().iterator();
		
		while(iterator.hasNext()) {
			Node node = iterator.next();
			if (nodeTypeToCollect.isInstance(node)) {
				commentNodes.add((T) node);
			}
		}
		
		return commentNodes;
	}
	

}
