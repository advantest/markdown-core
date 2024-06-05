/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2022-2024 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.markdown;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import com.vladsch.flexmark.ext.attributes.AttributesExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.figures.FiguresExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.math.MathExtension;
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
        options.set(HtmlRenderer.FENCED_CODE_LANGUAGE_CLASS_PREFIX, "");

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
                PlantUmlExtension.create(),
                
                // Advantest's extension for rendering images in figure tags and with figcaption
                FiguresExtension.create(),
                
                // Advantest's extension for parsing and rendering math formulas
                MathExtension.create()
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
    
    /**
     * Reads the given Markdown source code and parses it, i.e. creates the source code's
     * abstract syntax tree representation, a so called {@link Document}.
     * 
     * <bold>Warning!</bold> This method does not know anything about file names,
     * what might be needed to resolve paths to referenced files.
     * If the source code has file references, then use {@link #parseMarkdown(File)} instead
     * or explicitly add path information to the parsed {@link Document} by
     * setting the variable {@link PlantUmlExtension#KEY_DOCUMENT_FILE_PATH} to the
     * parsed file's absolute path.
     * 
     * @param markdownSourceCode the Markdown source code to be parsed
     * @return the parsed abstract syntax tree's root, never <code>null</code>
     */
    public Document parseMarkdown(String markdownSourceCode) {
        return this.markdownParser.parse(markdownSourceCode);
    }
    
    /**
     * Reads the given Markdown source code and parses it, i.e. creates the source code's
     * abstract syntax tree representation, a so called {@link Document}.
     * Since this method gets a file, not just a string, it also considers the file's path,
     * what is needed to resolve relative paths to other files (Markdown or PlantUML file)
     * referenced from the given source code.
     * 
     * @param markdownFile file to be parsed, must have file extension .md
     * @return the parsed abstract syntax tree, never <code>null</code>
     * @throws IOException if reading the file fails
     * @throws IllegalArgumentException if the given file is not a readable Markdown file with file extension .md
     */
    public Document parseMarkdown(File markdownFile) throws IOException {
    	if (markdownFile == null || !markdownFile.canRead() || !"md".equals(getFileExtension(markdownFile))) {
    		throw new IllegalArgumentException("Argument is not a readable Markdown file.");
    	}
   		String textFileContents = readTextFromFile(markdownFile);
   		Document parsedDocument = this.markdownParser.parse(textFileContents);
   		
   		// Set current file path. That's needed to resolve relative paths in PlantUML extension in flexmark.
        parsedDocument.set(PlantUmlExtension.KEY_DOCUMENT_FILE_PATH, markdownFile.getAbsolutePath());
   		
   		return parsedDocument;
    }
    
    /**
     * Translates the given abstract syntax tree (with the given {@link Node} as root)
     * to HTML source code. The given {@link Node} might be a {@link Document} parsed
     * by {@link #parseMarkdown(String)} or {@link #parseMarkdown(File)}.
     * 
     * @param markdownAstNode the root of the abstract syntax tree to be translated to HTML code
     * @return the resulting HTML source code
     */
    public String renderHtml(Node markdownAstNode) {
        return this.htmlRenderer.render(markdownAstNode);
    }
    
    /**
     * Convenience method for parsing Markdown source code and then translating it to HTML.
     * Please consider the hints of {@link #parseMarkdown(String)} method. It might be useful
     * to use {@link #parseMarkdown(File)} instead and then {@link #renderHtml(Node)}.
     * 
     * @param markdownSourceCode the Markdown source code to be parsed and translated to HTML
     * @return the resulting HTML source code
     */
    public String parseMarkdownAndRenderHtml(String markdownSourceCode) {
        return this.renderHtml(this.parseMarkdown(markdownSourceCode));
    }
    
    protected String readTextFromFile(File textFile) throws IOException {
		Path path = textFile.toPath();
		byte[] bytes = Files.readAllBytes(path);
		return new String(bytes);
	}
    
    protected String getFileExtension(File file) {
    	if (file == null) {
    		return null;
    	}
    	String fileName = file.getName();
    	int indexOfLastDot = fileName.lastIndexOf(".");
    	
    	if (indexOfLastDot < 0 || indexOfLastDot + 1 >= fileName.length()) {
    		return null;
    	}
    	
    	return fileName.substring(indexOfLastDot + 1);
    }
    
}
