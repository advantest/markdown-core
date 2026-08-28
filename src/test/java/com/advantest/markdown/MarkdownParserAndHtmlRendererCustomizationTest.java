/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2026 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import com.advantest.flexmark.ext.jira.tickets.JiraTicketExtension;
import com.vladsch.flexmark.html.AttributeProvider;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.IndependentAttributeProviderFactory;
import com.vladsch.flexmark.html.renderer.AttributablePart;
import com.vladsch.flexmark.html.renderer.LinkResolverContext;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataKey;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.data.NullableDataKey;
import com.vladsch.flexmark.util.html.MutableAttributes;
import com.vladsch.flexmark.util.misc.Extension;

/**
 * Tests for the customization API of {@link MarkdownParserAndHtmlRenderer},
 * i.e. for {@link MarkdownParserAndHtmlRenderer.Builder} and {@link MarkdownCustomization}.
 */
class MarkdownParserAndHtmlRendererCustomizationTest {
    
    private static final String JIRA_MARKDOWN = "Please see ticket ABC-123 for details.";
    
    private static final NullableDataKey<String> NULLABLE_TEST_KEY = new NullableDataKey<>("NULLABLE_TEST_KEY", "some default");
    
    /**
     * Simple flexmark extension used to verify that additional extensions are really applied.
     * It adds an HTML attribute to every rendered node.
     */
    private static final class MarkerAttributeExtension implements HtmlRenderer.HtmlRendererExtension {
        
        private final String markerValue;
        
        MarkerAttributeExtension(String markerValue) {
            this.markerValue = markerValue;
        }
        
        @Override
        public void rendererOptions(@NotNull MutableDataHolder options) {
            // nothing to do
        }
        
        @Override
        public void extend(@NotNull HtmlRenderer.Builder htmlRendererBuilder, @NotNull String rendererType) {
            htmlRendererBuilder.attributeProviderFactory(new IndependentAttributeProviderFactory() {
                @Override
                public @NotNull AttributeProvider apply(@NotNull LinkResolverContext context) {
                    return new AttributeProvider() {
                        @Override
                        public void setAttributes(@NotNull Node node, @NotNull AttributablePart part,
                                @NotNull MutableAttributes attributes) {
                            attributes.addValue("data-marker", markerValue);
                        }
                    };
                }
            });
        }
        
    }
    
    private static Set<Class<?>> extensionClassesOf(MarkdownParserAndHtmlRenderer renderer) {
        Collection<Extension> extensions = Parser.EXTENSIONS.get(renderer.getOptions());
        return extensions.stream().map(Extension::getClass).collect(Collectors.toSet());
    }
    
    @Test
    void withExtensionKeepsAllDefaultExtensions() {
        Set<Class<?>> defaultExtensionClasses = extensionClassesOf(new MarkdownParserAndHtmlRenderer());
        
        MarkdownParserAndHtmlRenderer customizedRenderer = MarkdownParserAndHtmlRenderer.builder()
                .withExtension(new MarkerAttributeExtension("one"))
                .build();
        
        Set<Class<?>> customizedExtensionClasses = extensionClassesOf(customizedRenderer);
        
        assertFalse(defaultExtensionClasses.isEmpty(), "The default configuration should register extensions.");
        assertTrue(customizedExtensionClasses.containsAll(defaultExtensionClasses),
                "Adding an extension must not remove the default extensions.");
        assertTrue(customizedExtensionClasses.contains(MarkerAttributeExtension.class),
                "The added extension must be registered.");
        assertEquals(defaultExtensionClasses.size() + 1, customizedExtensionClasses.size());
    }
    
    @Test
    void severalAddedExtensionsAreAllKept() {
        MarkdownParserAndHtmlRenderer customizedRenderer = MarkdownParserAndHtmlRenderer.builder()
                .withExtension(new MarkerAttributeExtension("one"))
                .withExtension(new MarkerAttributeExtension("two"))
                .build();
        
        Collection<Extension> extensions = Parser.EXTENSIONS.get(customizedRenderer.getOptions());
        long markerExtensionCount = extensions.stream()
                .filter(MarkerAttributeExtension.class::isInstance)
                .count();
        
        assertEquals(2, markerExtensionCount, "Both added extensions must be registered.");
    }
    
    @Test
    void addedExtensionAffectsRenderedOutput() {
        MarkdownParserAndHtmlRenderer defaultRenderer = new MarkdownParserAndHtmlRenderer();
        MarkdownParserAndHtmlRenderer customizedRenderer = MarkdownParserAndHtmlRenderer.builder()
                .withExtension(new MarkerAttributeExtension("customized"))
                .build();
        
        String markdownSource = "Just a paragraph.";
        
        assertFalse(defaultRenderer.parseMarkdownAndRenderHtml(markdownSource).contains("data-marker"),
                "The default renderer must not be affected by the customization.");
        assertTrue(customizedRenderer.parseMarkdownAndRenderHtml(markdownSource).contains("data-marker=\"customized\""),
                "The added extension must affect the rendered HTML output.");
    }
    
    @Test
    void addedExtensionDoesNotBreakDefaultExtensions() {
        MarkdownParserAndHtmlRenderer customizedRenderer = MarkdownParserAndHtmlRenderer.builder()
                .withExtension(new MarkerAttributeExtension("customized"))
                .build();
        
        String markdownSource = """
                | Column A | Column B |
                | - | - |
                | a | b |
                """;
        
        String html = customizedRenderer.parseMarkdownAndRenderHtml(markdownSource);
        
        assertTrue(html.contains("<table"), "The default tables extension must still be active. Rendered HTML: " + html);
    }
    
    @Test
    void withOptionChangesRenderedOutput() {
        MarkdownParserAndHtmlRenderer customizedRenderer = MarkdownParserAndHtmlRenderer.builder()
                .withOption(JiraTicketExtension.JIRA_URL, "https://wetrack.example.com/browse/")
                .build();
        
        String html = customizedRenderer.parseMarkdownAndRenderHtml(JIRA_MARKDOWN);
        
        assertTrue(html.contains("href=\"https://wetrack.example.com/browse/ABC-123\""),
                "The customized Jira URL must be used. Rendered HTML: " + html);
    }
    
    @Test
    void withCustomizationAcceptsALambda() {
        MarkdownParserAndHtmlRenderer customizedRenderer = MarkdownParserAndHtmlRenderer.builder()
                .withCustomization(options -> options.set(JiraTicketExtension.JIRA_URL, "https://lambda.example.com/browse/"))
                .build();
        
        String html = customizedRenderer.parseMarkdownAndRenderHtml(JIRA_MARKDOWN);
        
        assertTrue(html.contains("href=\"https://lambda.example.com/browse/ABC-123\""),
                "The customization lambda must be applied. Rendered HTML: " + html);
    }
    
    @Test
    void customizationsAreAppliedInRegistrationOrder() {
        MarkdownParserAndHtmlRenderer customizedRenderer = MarkdownParserAndHtmlRenderer.builder()
                .withOption(JiraTicketExtension.JIRA_URL, "https://first.example.com/browse/")
                .withCustomization(options -> options.set(JiraTicketExtension.JIRA_URL, "https://second.example.com/browse/"))
                .withOption(JiraTicketExtension.JIRA_URL, "https://third.example.com/browse/")
                .build();
        
        String html = customizedRenderer.parseMarkdownAndRenderHtml(JIRA_MARKDOWN);
        
        assertTrue(html.contains("href=\"https://third.example.com/browse/ABC-123\""),
                "The customization registered last must win. Rendered HTML: " + html);
        assertFalse(html.contains("first.example.com"));
        assertFalse(html.contains("second.example.com"));
    }
    
    @Test
    void customizationOverridesValueSetByCreateOptions() {
        // createOptions() sets HtmlRenderer.FENCED_CODE_LANGUAGE_CLASS_PREFIX to the empty string
        MarkdownParserAndHtmlRenderer defaultRenderer = new MarkdownParserAndHtmlRenderer();
        MarkdownParserAndHtmlRenderer customizedRenderer = MarkdownParserAndHtmlRenderer.builder()
                .withOption(HtmlRenderer.FENCED_CODE_LANGUAGE_CLASS_PREFIX, "language-")
                .build();
        
        String markdownSource = """
                ```java
                int i = 0;
                ```
                """;
        
        assertTrue(defaultRenderer.parseMarkdownAndRenderHtml(markdownSource).contains("class=\"java\""));
        assertTrue(customizedRenderer.parseMarkdownAndRenderHtml(markdownSource).contains("class=\"language-java\""),
                "The customization must override the value set by createOptions().");
    }
    
    @Test
    void builderWithoutCustomizationsBehavesLikeTheNoArgumentConstructor() {
        MarkdownParserAndHtmlRenderer defaultRenderer = new MarkdownParserAndHtmlRenderer();
        MarkdownParserAndHtmlRenderer builtRenderer = MarkdownParserAndHtmlRenderer.builder().build();
        
        String markdownSource = """
                # Heading {#my-anchor}
                
                Some ~~struck~~ text with ticket ABC-123.
                """;
        
        assertEquals(extensionClassesOf(defaultRenderer), extensionClassesOf(builtRenderer));
        assertEquals(defaultRenderer.parseMarkdownAndRenderHtml(markdownSource),
                builtRenderer.parseMarkdownAndRenderHtml(markdownSource));
    }
    
    @Test
    void noArgumentConstructorUsesTheDefaultJiraUrl() {
        MarkdownParserAndHtmlRenderer defaultRenderer = new MarkdownParserAndHtmlRenderer();
        
        String html = defaultRenderer.parseMarkdownAndRenderHtml(JIRA_MARKDOWN);
        
        assertTrue(html.contains("href=\"" + JiraTicketExtension.JIRA_URL.getDefaultValue() + "ABC-123\""),
                "The default configuration must remain unchanged. Rendered HTML: " + html);
    }
    
    @Test
    void nullArgumentsAreRejected() {
        MarkdownParserAndHtmlRenderer.Builder builder = MarkdownParserAndHtmlRenderer.builder();
        
        assertThrows(IllegalArgumentException.class, () -> builder.withExtension(null));
        assertThrows(IllegalArgumentException.class, () -> builder.withOption((DataKey<String>) null, "some value"));
        assertThrows(IllegalArgumentException.class, () -> builder.withOption(JiraTicketExtension.JIRA_URL, null));
        assertThrows(IllegalArgumentException.class, () -> builder.withOption((NullableDataKey<String>) null, "some value"));
        assertThrows(IllegalArgumentException.class, () -> builder.withCustomization(null));
    }
    
    @Test
    void nullOptionValuesAreAcceptedForNullableDataKeys() {
        MarkdownParserAndHtmlRenderer customizedRenderer = MarkdownParserAndHtmlRenderer.builder()
                .withOption(NULLABLE_TEST_KEY, null)
                .build();
        
        assertTrue(customizedRenderer.getOptions().contains(NULLABLE_TEST_KEY),
                "The option must have been set, even though its value is null.");
        assertNull(NULLABLE_TEST_KEY.get(customizedRenderer.getOptions()),
                "The null value must be passed through to flexmark unchanged.");
    }
    
    @Test
    void nonNullOptionValuesAreAcceptedForNullableDataKeys() {
        MarkdownParserAndHtmlRenderer customizedRenderer = MarkdownParserAndHtmlRenderer.builder()
                .withOption(NULLABLE_TEST_KEY, "some value")
                .build();
        
        assertTrue(customizedRenderer.getOptions().contains(NULLABLE_TEST_KEY));
        assertEquals("some value", NULLABLE_TEST_KEY.get(customizedRenderer.getOptions()));
    }
    
}
