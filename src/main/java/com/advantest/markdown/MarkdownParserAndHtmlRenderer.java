/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2022-2026 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.markdown;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.advantest.flexmark.ext.figures.FiguresExtension;
import com.advantest.flexmark.ext.jira.tickets.JiraTicketExtension;
import com.advantest.flexmark.ext.math.MathExtension;
import com.advantest.flexmark.ext.plantuml.PlantUmlExtension;
import com.advantest.flexmark.ext.sourcetracking.SourcePositionAttributesRendererExtension;
import com.advantest.resources.LocalFileSystemResource;
import com.advantest.resources.LocalFileSystemResourceResolver;
import com.advantest.resources.RelativePathResourceResolver;
import com.advantest.resources.ResourceContentsReader;
import com.advantest.resources.Resource;
import com.advantest.resources.UnresolvedResource;
import com.vladsch.flexmark.ext.attributes.AttributesExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataKey;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.data.NullableDataKey;
import com.vladsch.flexmark.util.data.SharedDataKeys;
import com.vladsch.flexmark.util.misc.Extension;

/**
 * Markdown source code parser and HTML renderer based on
 * <a href="https://github.com/vsch/flexmark-java">Flexmark</a>
 * with a selection of extensions.
 * 
 * <p>An instance with the default configuration can simply be created via {@link #MarkdownParserAndHtmlRenderer()}.
 * If the default configuration has to be adapted, e.g. because additional flexmark extensions are needed or
 * because option values have to be changed, then use {@link #builder()} instead:</p>
 * 
 * <pre>
 * MarkdownParserAndHtmlRenderer renderer = MarkdownParserAndHtmlRenderer.builder()
 *         .withExtension(MyExtension.create())
 *         .withOption(JiraTicketExtension.JIRA_URL, "https://jira.example.com/browse/")
 *         .build();
 * </pre>
 */
public class MarkdownParserAndHtmlRenderer {

	/**
	 * The {@link Resource} of the parsed document itself, i.e. where the Markdown source code came
	 * from. It is what all relative link targets inside that document are resolved against, see
	 * {@link com.advantest.resources.ResourceResolver}.
	 * 
	 * <p>The value is written by {@link #parseMarkdown(String, Resource)} and by
	 * {@link #parseMarkdown(File)}. Its default is {@link UnresolvedResource#UNKNOWN_DOCUMENT}, so
	 * reading it never yields <code>null</code> and a document of unknown location needs no special
	 * case: every relative link of such a document simply resolves to something not existing.</p>
	 */
	public static final DataKey<Resource> KEY_DOCUMENT_RESOURCE =
			new DataKey<>("DOCUMENT_RESOURCE", UnresolvedResource.UNKNOWN_DOCUMENT);

	/**
	 * What a parsed document refers to: a target as it is written in that document, e.g.
	 * <code>diagrams/classes.puml</code>, mapped to the {@link Resource} it names.
	 * 
	 * <p>The map is written by {@link #setDocumentResource(Document, Resource)} and filled while
	 * the document is rendered: a target asked for the first time is resolved against
	 * {@link #KEY_DOCUMENT_RESOURCE} and remembered here. A caller knowing a resource beforehand
	 * may put it in itself, and it is then used instead of resolving the target.</p>
	 */
	public static final DataKey<Map<String, Resource>> KEY_DOCUMENT_REFERENCED_RESOURCES =
			new DataKey<>("DOCUMENT_REFERENCED_RESOURCES", Collections.emptyMap());

	/**
	 * Which files are read as Markdown, see {@link MarkdownFileExtensions}.
	 * 
	 * <p>It is a configuration and not a property of one document, so it is set where the parser is
	 * built, either with {@link Builder#withMarkdownFileExtensions(MarkdownFileExtensions)} or by a
	 * {@link MarkdownCustomization}, and it is read from the parser's options rather than from a
	 * parsed document. A tool knowing which files its users edit as Markdown says so here; the
	 * default accepts <code>md</code>.</p>
	 */
	public static final DataKey<MarkdownFileExtensions> KEY_MARKDOWN_FILE_EXTENSIONS =
			new DataKey<>("MARKDOWN_FILE_EXTENSIONS", MarkdownFileExtensions.DEFAULT);

	private final MutableDataSet options;

	private final RelativePathResourceResolver relativePathResourceResolver;

	private final ResourceContentsReader resourceContentsReader;

	private final Parser markdownParser;

	private final HtmlRenderer htmlRenderer;

	/**
	 * Creates a new parser and renderer with the default configuration.
	 */
	public MarkdownParserAndHtmlRenderer() {
		this(Collections.emptyList());
	}

	/**
	 * Creates a new parser and renderer with the default configuration adapted by the given customizations.
	 * 
	 * <p>The customizations are applied <em>after</em> {@link #createOptions()} returned, in the order in which
	 * they appear in the given list. Hence, they always win over the default configuration and over any
	 * subclass' override of {@link #createOptions()}, and a later customization overrides an earlier one.</p>
	 * 
	 * @param customizations the customizations to be applied, must not be <code>null</code>, but may be empty
	 * @throws IllegalArgumentException if the given list is <code>null</code> or contains <code>null</code> entries
	 */
	protected MarkdownParserAndHtmlRenderer(List<MarkdownCustomization> customizations) {
		this(customizations, new LocalFileSystemResourceResolver());
	}

	/**
	 * Creates a new parser and renderer with the default configuration adapted by the given
	 * customizations, resolving what a document refers to with the given resolver.
	 * 
	 * <p>The customizations are applied as described in
	 * {@link #MarkdownParserAndHtmlRenderer(List)}.</p>
	 * 
	 * @param customizations the customizations to be applied, must not be <code>null</code>, but may be empty
	 * @param relativePathResourceResolver what turns a target written in a document into a
	 *                                     {@link Resource}, must not be <code>null</code>
	 * @throws IllegalArgumentException if the given list is <code>null</code> or contains <code>null</code> entries,
	 *                                  or if the given resolver is <code>null</code>
	 */
	protected MarkdownParserAndHtmlRenderer(List<MarkdownCustomization> customizations,
			RelativePathResourceResolver relativePathResourceResolver) {
		this(customizations, relativePathResourceResolver, ResourceContentsReader.FROM_THE_RESOURCE);
	}

	/**
	 * Creates a new parser and renderer with the default configuration adapted by the given
	 * customizations, resolving what a document refers to with the given resolver and reading it with
	 * the given reader.
	 * 
	 * <p>The customizations are applied as described in
	 * {@link #MarkdownParserAndHtmlRenderer(List)}.</p>
	 * 
	 * @param customizations the customizations to be applied, must not be <code>null</code>, but may be empty
	 * @param relativePathResourceResolver what turns a target written in a document into a
	 *                                     {@link Resource}, must not be <code>null</code>
	 * @param resourceContentsReader what answers with the contents of such a resource while a document
	 *                               is rendered, must not be <code>null</code>
	 * @throws IllegalArgumentException if the given list is <code>null</code> or contains <code>null</code> entries,
	 *                                  or if the given resolver or reader is <code>null</code>
	 */
	protected MarkdownParserAndHtmlRenderer(List<MarkdownCustomization> customizations,
			RelativePathResourceResolver relativePathResourceResolver,
			ResourceContentsReader resourceContentsReader) {
		if (customizations == null) {
			throw new IllegalArgumentException("The list of customizations must not be null.");
		}
		if (relativePathResourceResolver == null) {
			throw new IllegalArgumentException("The resource resolver must not be null.");
		}
		if (resourceContentsReader == null) {
			throw new IllegalArgumentException("The resource contents reader must not be null.");
		}

		MutableDataSet createdOptions = createOptions();

		for (MarkdownCustomization customization : customizations) {
			if (customization == null) {
				throw new IllegalArgumentException("The list of customizations must not contain null entries.");
			}
			customization.customize(createdOptions);
		}

		this.options = createdOptions;
		this.relativePathResourceResolver = relativePathResourceResolver;
		this.resourceContentsReader = resourceContentsReader;
		this.markdownParser = createMarkdownParser();
		this.htmlRenderer = createHtmlRenderer();
	}

	/**
	 * Creates a new {@link Builder} for a customized {@link MarkdownParserAndHtmlRenderer}.
	 * 
	 * @return a new builder instance, never <code>null</code>
	 */
	public static Builder builder() {
		return new Builder();
	}

	protected MutableDataSet createOptions() {
		MutableDataSet options = new MutableDataSet();

		options.set(Parser.BLANK_LINES_IN_AST, false);
		options.set(SharedDataKeys.INDENT_SIZE, 2);
		options.set(SharedDataKeys.GENERATE_HEADER_ID, false);

		// use percent encoding in URLs, e.g. translate special characters like '<' in the target path of a link like
		// [method](SomeClass.java#someMethod\(List\<String\>\))
		// to '%3C' (not '&lt;') in a path like SomeClass.java#someMethod\(List%3CString%3E\)
		// This way, we can ensure valid URLs.
		// See docs: https://github.com/vsch/flexmark-java/wiki/Extensions#renderer
		options.set(SharedDataKeys.PERCENT_ENCODE_URLS, true);

		options.set(HtmlRenderer.RENDER_HEADER_ID, true);
		options.set(HtmlRenderer.GENERATE_HEADER_ID, false);
		options.set(HtmlRenderer.FENCED_CODE_LANGUAGE_CLASS_PREFIX, "");

		// see https://github.com/vsch/flexmark-java/wiki/Tables-Extension#parsing-details
		options.set(TablesExtension.MIN_SEPARATOR_DASHES, 1);

		options.set(PlantUmlExtension.KEY_RENDER_FENCED_PLANTUML_CODE_BLOCKS, true);

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

				// see https://github.com/vsch/flexmark-java/wiki/Extensions#footnotes
				FootnoteExtension.create(),

				// Advantest's PlantUML extension
				PlantUmlExtension.create(),

				// Advantest's extension for rendering images in figure tags and with figcaption
				FiguresExtension.create(),

				// Advantest's extension for parsing and rendering math formulas
				MathExtension.create(),

			// Advantest's extension for parsing and rendering jira ticket numbers as links to the tickets
				JiraTicketExtension.create(),

				// Advantest's extension writing into every rendered element where in the Markdown
				// source code it came from, so that an editor and a preview of the same document
				// can point at each other
				SourcePositionAttributesRendererExtension.create()
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
	 * <bold>Warning!</bold> This method does not know where the source code came from,
	 * what is needed to resolve relative paths to referenced files.
	 * If the source code has file references, then use
	 * {@link #parseMarkdown(String, Resource)} or {@link #parseMarkdown(File)} instead.
	 * 
	 * @param markdownSourceCode the Markdown source code to be parsed
	 * @return the parsed abstract syntax tree's root, never <code>null</code>
	 */
	public Document parseMarkdown(String markdownSourceCode) {
		Document parsedDocument = this.markdownParser.parse(markdownSourceCode);
		setDocumentResource(parsedDocument, UnresolvedResource.UNKNOWN_DOCUMENT);
		return parsedDocument;
	}

	/**
	 * Reads the given Markdown source code and parses it, i.e. creates the source code's
	 * abstract syntax tree representation, a so called {@link Document}, and remembers where that
	 * source code came from, what is needed to resolve relative paths to referenced files.
	 * 
	 * <p>The given resource is created by the {@link com.advantest.resources.ResourceResolver}
	 * of the environment this code runs in, and it is the same resolver that later resolves the
	 * link targets found inside the parsed document.</p>
	 * 
	 * @param markdownSourceCode the Markdown source code to be parsed
	 * @param documentResource the resource the source code came from, must not be <code>null</code>,
	 *                         pass {@link UnresolvedResource#UNKNOWN_DOCUMENT} if it is unknown
	 * @return the parsed abstract syntax tree's root, never <code>null</code>
	 * @throws IllegalArgumentException if the given resource is <code>null</code>
	 */
	public Document parseMarkdown(String markdownSourceCode, Resource documentResource) {
		Document parsedDocument = this.markdownParser.parse(markdownSourceCode);
		setDocumentResource(parsedDocument, documentResource);
		return parsedDocument;
	}

	/**
	 * Remembers on the given parsed document where its source code came from.
	 * 
	 * <p>Use this only for documents that were parsed elsewhere, e.g. directly with flexmark.
	 * Documents parsed by {@link #parseMarkdown(String, Resource)} or {@link #parseMarkdown(File)}
	 * already know their resource.</p>
	 * 
	 * <p>It also prepares what the document refers to: an empty
	 * {@link #KEY_DOCUMENT_REFERENCED_RESOURCES map of targets to resources}, and the text of those
	 * targets, read from them while the document is rendered. A renderer asking for a target it
	 * found in the document is therefore answered without knowing where the document lives, and
	 * nothing is read for a document that is never rendered. A renderer that was answered with no
	 * text may ask where the target was looked for, so that it can name that place to a reader
	 * instead of only saying that something is missing.</p>
	 * 
	 * @param document the parsed document, must not be <code>null</code>
	 * @param documentResource the resource the document's source code came from, must not be
	 *                         <code>null</code>
	 * @throws IllegalArgumentException if one of the arguments is <code>null</code>
	 */
	public void setDocumentResource(Document document, Resource documentResource) {
		if (document == null || documentResource == null) {
			throw new IllegalArgumentException("Arguments must not be null.");
		}

		document.set(KEY_DOCUMENT_RESOURCE, documentResource);

		Map<String, Resource> referencedResources = new ConcurrentHashMap<>();
		document.set(KEY_DOCUMENT_REFERENCED_RESOURCES, referencedResources);

		MarkdownDocumentReferencedFiles referencedFiles = new MarkdownDocumentReferencedFiles(
				documentResource, this.relativePathResourceResolver, referencedResources,
				this.resourceContentsReader);
		document.set(PlantUmlExtension.KEY_DOCUMENT_PATH_TO_FILE_CONTENTS_MAP, referencedFiles);
		document.set(PlantUmlExtension.KEY_DOCUMENT_PLANTUML_FILE_LOCATIONS, referencedFiles);
	}

	/**
	 * Tells what turns a target written in a document into a {@link Resource}.
	 * 
	 * @return the resolver of a target relative to the document it is written in, never
	 *         <code>null</code>
	 */
	public RelativePathResourceResolver getRelativePathResourceResolver() {
		return this.relativePathResourceResolver;
	}

	/**
	 * Answers where the given parsed document's source code came from.
	 * 
	 * @param document the parsed document, must not be <code>null</code>
	 * @return the resource the document's source code came from, never <code>null</code>,
	 *         {@link UnresolvedResource#UNKNOWN_DOCUMENT} if the document does not know it
	 * @throws IllegalArgumentException if the given document is <code>null</code>
	 */
	public Resource getDocumentResource(Document document) {
		if (document == null) {
			throw new IllegalArgumentException("Argument must not be null.");
		}

		return KEY_DOCUMENT_RESOURCE.get(document);
	}

	/**
	 * Reads the given Markdown source code and parses it, i.e. creates the source code's
	 * abstract syntax tree representation, a so called {@link Document}.
	 * Since this method gets a file, not just a string, it also considers the file's path,
	 * what is needed to resolve relative paths to other files (Markdown or PlantUML file)
	 * referenced from the given source code.
	 * 
	 * @param markdownFile file to be parsed, must be readable and must be named like a
	 *                     {@link #getMarkdownFileExtensions() Markdown file}
	 * @return the parsed abstract syntax tree, never <code>null</code>
	 * @throws IOException if reading the file fails
	 * @throws IllegalArgumentException if the given file is not a readable Markdown file
	 */
	public Document parseMarkdown(File markdownFile) throws IOException {
		if (markdownFile == null || !markdownFile.canRead() || !getMarkdownFileExtensions().isMarkdownFile(markdownFile)) {
			throw new IllegalArgumentException("Argument is not a readable Markdown file.");
		}
		String textFileContents = readTextFromFile(markdownFile);

		return parseMarkdown(textFileContents, LocalFileSystemResource.of(markdownFile));
	}

	/**
	 * Tells which files this parser reads as Markdown.
	 * 
	 * @return what decides whether something is Markdown, never <code>null</code>
	 */
	public MarkdownFileExtensions getMarkdownFileExtensions() {
		return KEY_MARKDOWN_FILE_EXTENSIONS.get(this.options);
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

	/**
	 * Convenience method for parsing Markdown source code that came from the given resource
	 * and then translating it to HTML.
	 * 
	 * @param markdownSourceCode the Markdown source code to be parsed and translated to HTML
	 * @param documentResource the resource the source code came from, must not be <code>null</code>,
	 *                         pass {@link UnresolvedResource#UNKNOWN_DOCUMENT} if it is unknown
	 * @return the resulting HTML source code
	 * @throws IllegalArgumentException if the given resource is <code>null</code>
	 */
	public String parseMarkdownAndRenderHtml(String markdownSourceCode, Resource documentResource) {
		return this.renderHtml(this.parseMarkdown(markdownSourceCode, documentResource));
	}

	protected String readTextFromFile(File textFile) throws IOException {
		Path path = textFile.toPath();
		byte[] bytes = Files.readAllBytes(path);
		return new String(bytes);
	}

	/**
	 * Reads the extension out of the given file's name.
	 * 
	 * @param file the file to be read, may be <code>null</code>
	 * @return the extension without the separating dot, or <code>null</code> if the given file is
	 *         <code>null</code> or has no extension
	 * @see MarkdownFileExtensions#getFileExtension(String)
	 */
	protected String getFileExtension(File file) {
		if (file == null) {
			return null;
		}

		return MarkdownFileExtensions.getFileExtension(file.getName());
	}

	/**
	 * Builder for a customized {@link MarkdownParserAndHtmlRenderer}.
	 * 
	 * <p>All registered customizations &ndash; no matter whether they were registered as an extension,
	 * as an option value or as a {@link MarkdownCustomization} &ndash; are collected in one single ordered list
	 * and are applied in registration order. Therefore, a customization registered later always overrides
	 * a customization registered earlier as well as the default configuration.</p>
	 * 
	 * <p>Instances are created via {@link MarkdownParserAndHtmlRenderer#builder()}.</p>
	 */
	public static final class Builder {

		private final List<MarkdownCustomization> customizations = new ArrayList<>();

		private RelativePathResourceResolver relativePathResourceResolver =
				new LocalFileSystemResourceResolver();

		private ResourceContentsReader resourceContentsReader = ResourceContentsReader.FROM_THE_RESOURCE;

		private Builder() {
			// instances are created via MarkdownParserAndHtmlRenderer.builder()
		}

		/**
		 * Registers an additional flexmark {@link Extension}.
		 * 
		 * <p>The given extension is <em>appended</em> to the extensions already registered in
		 * {@link Parser#EXTENSIONS}, i.e. neither the default extensions nor extensions registered
		 * earlier via this builder are removed.</p>
		 * 
		 * @param extension the flexmark extension to be added, must not be <code>null</code>
		 * @return this builder, for method chaining
		 * @throws IllegalArgumentException if the given extension is <code>null</code>
		 */
		public Builder withExtension(Extension extension) {
			if (extension == null) {
				throw new IllegalArgumentException("The extension must not be null.");
			}

			this.customizations.add(options -> {
				List<Extension> allExtensions = new ArrayList<>(Parser.EXTENSIONS.get(options));
				allExtensions.add(extension);
				options.set(Parser.EXTENSIONS, allExtensions);
			});

			return this;
		}

		/**
		 * Says which files are read as Markdown, overriding the default that accepts
		 * <code>md</code> alone.
		 * 
		 * <pre>
		 * MarkdownParserAndHtmlRenderer renderer = MarkdownParserAndHtmlRenderer.builder()
		 *         .withMarkdownFileExtensions(MarkdownFileExtensions.accepting("md", "mdown", "mkd"))
		 *         .build();
		 * </pre>
		 * 
		 * @param markdownFileExtensions what decides whether something is Markdown, must not be
		 *                      <code>null</code>
		 * @return this builder, for method chaining
		 * @throws IllegalArgumentException if the given argument is <code>null</code>
		 */
		public Builder withMarkdownFileExtensions(MarkdownFileExtensions markdownFileExtensions) {
			if (markdownFileExtensions == null) {
				throw new IllegalArgumentException("The Markdown file extensions must not be null.");
			}

			return withOption(KEY_MARKDOWN_FILE_EXTENSIONS, markdownFileExtensions);
		}

		/**
		 * Says what turns a target written in a document into a {@link Resource}, overriding the
		 * default that reads the local file system.
		 * 
		 * <pre>
		 * MarkdownParserAndHtmlRenderer renderer = MarkdownParserAndHtmlRenderer.builder()
		 *         .withRelativePathResourceResolver(new ResolverOfMyRepository())
		 *         .build();
		 * </pre>
		 * 
		 * @param resolver what resolves a target relative to the document it is written in, must
		 *                 not be <code>null</code>
		 * @return this builder, for method chaining
		 * @throws IllegalArgumentException if the given argument is <code>null</code>
		 */
		public Builder withRelativePathResourceResolver(RelativePathResourceResolver resolver) {
			if (resolver == null) {
				throw new IllegalArgumentException("The resource resolver must not be null.");
			}

			this.relativePathResourceResolver = resolver;

			return this;
		}

		/**
		 * Says what answers with the contents of a target a document refers to while the document is
		 * rendered, overriding the default that asks the resource itself.
		 * 
		 * <p>Give one where something knows better than the resource what it contains, e.g. an editor
		 * holding a diagram its user changed and did not save yet: the rendering then shows what the
		 * user sees rather than what was saved.</p>
		 * 
		 * @param reader what reads a resolved target, must not be <code>null</code>
		 * @return this builder, for method chaining
		 * @throws IllegalArgumentException if the given argument is <code>null</code>
		 */
		public Builder withResourceContentsReader(ResourceContentsReader reader) {
			if (reader == null) {
				throw new IllegalArgumentException("The resource contents reader must not be null.");
			}

			this.resourceContentsReader = reader;

			return this;
		}

		/**
		 * Sets a single flexmark option value, thereby overriding the default value of the given key.
		 * 
		 * @param <T> the type of the option's value
		 * @param key the flexmark data key to be set, must not be <code>null</code>
		 * @param value the value to be set, must not be <code>null</code>
		 * @return this builder, for method chaining
		 * @throws IllegalArgumentException if the given key or value is <code>null</code>
		 */
		public <T> Builder withOption(DataKey<T> key, T value) {
			if (key == null) {
				throw new IllegalArgumentException("The option key must not be null.");
			}
			if (value == null) {
				throw new IllegalArgumentException("The option value must not be null.");
			}

			this.customizations.add(options -> options.set(key, value));

			return this;
		}

		/**
		 * Sets a single flexmark option value, thereby overriding the default value of the given key.
		 * 
		 * <p>{@link NullableDataKey} is flexmark's key type for settings whose value may legitimately be
		 * <code>null</code>. Therefore, <code>null</code> values are permitted here, in contrast to
		 * {@link #withOption(DataKey, Object)}.</p>
		 * 
		 * @param <T> the type of the option's value
		 * @param key the flexmark data key to be set, must not be <code>null</code>
		 * @param value the value to be set, may be <code>null</code>
		 * @return this builder, for method chaining
		 * @throws IllegalArgumentException if the given key is <code>null</code>
		 */
		public <T> Builder withOption(NullableDataKey<T> key, T value) {
			if (key == null) {
				throw new IllegalArgumentException("The option key must not be null.");
			}

			this.customizations.add(options -> options.set(key, value));

			return this;
		}

		/**
		 * Registers a generic {@link MarkdownCustomization}.
		 * 
		 * <p>This is the escape hatch for customizations that are contributed as objects, e.g. from an
		 * Eclipse plug-in extension point or as a Spring bean, and for customizations that need to add
		 * several extensions and option values at once.</p>
		 * 
		 * @param customization the customization to be applied, must not be <code>null</code>
		 * @return this builder, for method chaining
		 * @throws IllegalArgumentException if the given customization is <code>null</code>
		 */
		public Builder withCustomization(MarkdownCustomization customization) {
			if (customization == null) {
				throw new IllegalArgumentException("The customization must not be null.");
			}

			this.customizations.add(customization);

			return this;
		}

		/**
		 * Creates a new {@link MarkdownParserAndHtmlRenderer} with all customizations registered
		 * at this builder applied.
		 * 
		 * @return the newly created parser and renderer, never <code>null</code>
		 */
		public MarkdownParserAndHtmlRenderer build() {
			return new MarkdownParserAndHtmlRenderer(this.customizations, this.relativePathResourceResolver,
					this.resourceContentsReader);
		}

	}

}
