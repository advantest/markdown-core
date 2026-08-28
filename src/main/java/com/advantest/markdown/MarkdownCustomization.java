/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2026 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.markdown;

import com.vladsch.flexmark.util.data.MutableDataSet;

/**
 * A single, self-contained customization of the flexmark configuration used by
 * {@link MarkdownParserAndHtmlRenderer}.
 * 
 * <p>Implementations receive the fully pre-populated default options, i.e. all default option values
 * and all default flexmark extensions are already set when {@link #customize(MutableDataSet)} is called.
 * An implementation may</p>
 * <ul>
 *   <li>register additional flexmark extensions (by reading the current value of
 *       {@link com.vladsch.flexmark.parser.Parser#EXTENSIONS}, copying it, appending to the copy and
 *       setting the copy back &ndash; simply replacing the value would silently drop all default extensions), and/or</li>
 *   <li>set arbitrary flexmark data keys, thereby overriding default option values.</li>
 * </ul>
 * 
 * <p>Customizations are applied in registration order, i.e. in the order in which they were added via
 * {@link MarkdownParserAndHtmlRenderer.Builder}. Consequently, a customization registered later
 * overrides values set by a customization registered earlier as well as the defaults.</p>
 * 
 * <p>Most customizations only need to add one extension or set one option. For those cases, prefer the
 * convenience methods {@link MarkdownParserAndHtmlRenderer.Builder#withExtension(com.vladsch.flexmark.util.misc.Extension)}
 * and {@link MarkdownParserAndHtmlRenderer.Builder#withOption(com.vladsch.flexmark.util.data.DataKey, Object)}.
 * This interface is the generic escape hatch for cases in which a customization has to be contributed as an
 * <em>object</em>, e.g. from an Eclipse plug-in extension point or as a Spring bean.</p>
 * 
 * <p>Example:</p>
 * <pre>
 * MarkdownParserAndHtmlRenderer renderer = MarkdownParserAndHtmlRenderer.builder()
 *         .withCustomization(options -&gt; options.set(JiraTicketExtension.JIRA_URL, "https://jira.example.com/browse/"))
 *         .build();
 * </pre>
 * 
 * @see MarkdownParserAndHtmlRenderer#builder()
 */
@FunctionalInterface
public interface MarkdownCustomization {

	/**
	 * Customizes the given flexmark options.
	 * 
	 * <p>The given options are already populated with all defaults of
	 * {@link MarkdownParserAndHtmlRenderer} and with the effects of all customizations
	 * registered before this one. Implementations modify the given options in place.</p>
	 * 
	 * @param options the flexmark options to be customized, never <code>null</code>
	 */
	void customize(MutableDataSet options);

}
