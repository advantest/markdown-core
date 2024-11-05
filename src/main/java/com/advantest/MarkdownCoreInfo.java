/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2022-2024 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.plantuml.dot.Graphviz;
import net.sourceforge.plantuml.dot.GraphvizUtils;

public class MarkdownCoreInfo {
	
	private static String version;
	private static String plantUmlVersion;
	private static String flexmarkVersion;
	private static String graphvizVersion;
	private static String graphvizExecutable;
	
	private static final String PROPERTY_MARKDOWN_CORE_VERSION = "markdownCoreVersion";
	private static final String PROPERTY_FLEXMARK_VERSION = "flexmarkVersion";
	private static final String PROPERTY_PLANTUML_VERSION = "plantUmlVersion";
	
	static {
		readProperties();
		readGraphvizVersion();
	}
	
	private static void readProperties() {
		Properties myProperties = new Properties();
		try {
			InputStream stream = MarkdownCoreInfo.class.getResourceAsStream("/settings.properties");
			myProperties.load(stream);
		} catch (IOException e) {
			version = null;
			plantUmlVersion = null;
			flexmarkVersion = null;
			throw new IllegalStateException(e);
		}
		version = myProperties.getProperty(PROPERTY_MARKDOWN_CORE_VERSION);
		plantUmlVersion = myProperties.getProperty(PROPERTY_PLANTUML_VERSION);
		flexmarkVersion = myProperties.getProperty(PROPERTY_FLEXMARK_VERSION);
		
		if (plantUmlVersion == null || plantUmlVersion.isBlank()) {
			plantUmlVersion = net.sourceforge.plantuml.version.Version.versionString();
		}
	}
	
	private static void readGraphvizVersion() {
		Graphviz graphviz = GraphvizUtils.create(null, "foo;", "svg");
		
		graphvizExecutable = graphviz.getDotExe() != null ? graphviz.getDotExe().getAbsolutePath() : "";
		
		// Remove textual prefix in a string like "dot - graphviz version 12.1.2 (20240928.0832)"
		String versionText = graphviz.dotVersion();
		Pattern versionPattern = Pattern.compile("\\d+\\.\\d+\\.\\d+\\s\\([\\d\\.]*\\)");
		Matcher versionMatcher = versionPattern.matcher(versionText);
		if (versionMatcher.find()) {
			graphvizVersion = versionMatcher.group();
			return;
		}

		graphvizVersion = "";
	}
	
	public static String getVersion() {
		return version;
	}
	
	public static String getPlantUmlVersion() {
		return plantUmlVersion;
	}
	
	public static String getFlexmarkVersion() {
		return flexmarkVersion;
	}

	public static String getPlantUmlSecurityProfile() {
		return net.sourceforge.plantuml.security.SecurityUtils.getSecurityProfile().name();
	}
	
	public static String getPlantUmlUrlAllowList() {
		Optional<String> urlAllowList = Optional.ofNullable(System.getProperty("plantuml.allowlist.url"));
		return urlAllowList.orElse("");
	}
	
	public static String getGraphvizVersion() {
		return graphvizVersion;
	}
	
	public static String getGraphvizExecutable() {
		return graphvizExecutable;
	}
	
}
