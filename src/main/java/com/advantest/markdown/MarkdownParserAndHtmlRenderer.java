package com.advantest.markdown;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import com.vladsch.flexmark.ext.attributes.AttributesExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.plantuml.PlantUmlExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.data.SharedDataKeys;

/**
 * Markdown source code parser and HTML renderer based on
 * <a href="https://github.com/vsch/flexmark-java">Flexmark</a>
 * with a selection of extensions.
 */
public class MarkdownParserAndHtmlRenderer {
	
	private final MutableDataSet options = createOptions();

    private final Parser markdownParser = createMarkdownParser();

    private final HtmlRenderer htmlRenderer = createHtmlRenderer();

    protected MutableDataSet createOptions() {
        MutableDataSet options = new MutableDataSet();

        options.set(Parser.BLANK_LINES_IN_AST, false);
        options.set(SharedDataKeys.INDENT_SIZE, 2);
        options.set(SharedDataKeys.GENERATE_HEADER_ID, false);

        options.set(HtmlRenderer.RENDER_HEADER_ID, true);
        options.set(HtmlRenderer.GENERATE_HEADER_ID, false);

        options.set(Parser.EXTENSIONS, Arrays.asList(
        		// see https://github.com/vsch/flexmark-java/wiki/Tables-Extension
                TablesExtension.create(),
                
                // see https://github.com/vsch/flexmark-java/wiki/Extensions#gfm-strikethroughsubscript
        		StrikethroughExtension.create(),
        		
        		// see https://github.com/vsch/flexmark-java/wiki/Extensions#autolink
        		AutolinkExtension.create(),
                
                // see https://github.com/vsch/flexmark-java/wiki/Attributes-Extension
        		// needed e.g. for setting custom heading anchor IDs
                AttributesExtension.create(),
                
                // Advantest's PlantUML extension
                PlantUmlExtension.create()
        ));

        return options;
    }
    
    protected MutableDataSet getOptions() {
    	return this.options;
    }
    
    private Parser createMarkdownParser() {
        return Parser.builder(this.options).build();
    }

    private HtmlRenderer createHtmlRenderer() {
        return HtmlRenderer.builder(this.options).build();
    }
    
    protected Parser getMarkdownParser() {
    	return this.markdownParser;
    }
    
    protected HtmlRenderer getHtmlRenderer() {
    	return this.htmlRenderer;
    }
    
    public Document parseMarkdown(String markdownSourceCode) {
        return this.markdownParser.parse(markdownSourceCode);
    }
    
    public Document parseMarkdown(File markdownFile) throws IOException {
    	if (markdownFile == null || !markdownFile.canRead() || "md".equals(getFileExtension(markdownFile))) {
    		throw new IllegalArgumentException("Argument is not a readable Markdown file.");
    	}
   		String textFileContents = readTextFromFile(markdownFile);
   		Document parsedDocument = this.markdownParser.parse(textFileContents);
   		
   		// Set current file path. That's needed to resolve relative paths in PlantUML extension in flexmark.
        parsedDocument.set(PlantUmlExtension.KEY_DOCUMENT_FILE_PATH, markdownFile.getAbsolutePath());
   		
   		return parsedDocument;
    }
    
    public String renderHtml(Node markdownAstNode) {
        return this.htmlRenderer.render(markdownAstNode);
    }
    
    public String parseMarkdownAndRenderHtml(String markdownSourceCode) {
        return this.renderHtml(this.parseMarkdown(markdownSourceCode));
    }
    
    protected String readTextFromFile(File textFile) throws IOException {
		Path path = textFile.toPath();
		byte[] bytes = Files.readAllBytes(path);
		return new String(bytes);
	}
    
    private String getFileExtension(File file) {
    	if (file == null) {
    		return null;
    	}
    	String fileName = file.getName();
    	int indexOfLastDot = fileName.lastIndexOf(".");
    	
    	if (indexOfLastDot < 0) {
    		return null;
    	}
    	
    	return fileName.substring(indexOfLastDot);
    }
    
}
