package com.advantest.markdown;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import com.vladsch.flexmark.ast.HtmlCommentBlock;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.collection.iteration.ReversiblePeekingIterator;

public class MarkdownParserAndHtmlRendererExtensionsTest {
	
	private MarkdownParserAndHtmlRenderer parserRenderer;
	
	@BeforeEach
	public void setUp() {
		parserRenderer = new MarkdownParserAndHtmlRenderer();
	}
	
	@AfterEach
	public void tearDown() {
		parserRenderer = null;
	}
	
	@Test
	public void hiddenCommentsParsedButNotRenderedInHtml() throws Exception {
		String testFilePath = "src/test/resources/markdown/extensions/hidden-comments.md";
		File mdFile = new File(testFilePath);
		
		Document document = parserRenderer.parseMarkdown(mdFile);
		String htmlResult = parserRenderer.renderHtml(document);
		
		assertNotNull(document);
		List<HtmlCommentBlock> commentNodes = new ArrayList<>();
		ReversiblePeekingIterator<Node> iterator = document.getDescendants().iterator();
		while(iterator.hasNext()) {
			Node node = iterator.next();
			if (node instanceof HtmlCommentBlock) {
				commentNodes.add((HtmlCommentBlock) node);
			}
		}
		assertEquals(4, commentNodes.size());
		assertEquals("<!-- Some usual one-line comment, not hidden -->\n", commentNodes.get(0).getChars().toString());
		assertEquals("<!-- Comments can be spanned\nover \nseveral lines -->\n", commentNodes.get(1).getChars().toString());
		assertEquals("<!--- Hidden comment --->\n", commentNodes.get(2).getChars().toString());
		assertEquals("<!---\nHidden\nmulti-line\ncomment\n--->\n", commentNodes.get(3).getChars().toString());
		
		assertNotNull(htmlResult);
		assertFalse(htmlResult.isBlank());
		String regex = "<!--\\sSome usual one-line comment, not hidden\\s-->";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(htmlResult);
		assertTrue(matcher.find());
		
		regex = "<!--\\sComments can be spanned\\nover \\nseveral lines\\s-->";
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

}
