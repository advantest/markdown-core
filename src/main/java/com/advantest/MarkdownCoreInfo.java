/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2022-2024 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.plantuml.dot.Graphviz;
import net.sourceforge.plantuml.dot.GraphvizRuntimeEnvironment;

public class MarkdownCoreInfo {
	
	private static String version;
	private static String plantUmlVersion;
	private static String flexmarkVersion;
	private static String flexmarkExtensionsVersion;
	
	private static final String PROPERTY_MARKDOWN_CORE_VERSION = "markdownCoreVersion";
	private static final String PROPERTY_FLEXMARK_VERSION = "flexmarkVersion";
	private static final String PROPERTY_FLEXMARK_EXTENSIONS_VERSION = "flexmarkExtensionsVersion";
	
	static {
		readProperties();
	}
	
	private static void readProperties() {
		Properties myProperties = new Properties();
		try {
			InputStream stream = MarkdownCoreInfo.class.getResourceAsStream("/settings.properties");
			if (stream == null) {
				throw new IllegalStateException("The settings of this library are missing:"
						+ " there is no /settings.properties on the class path.");
			}
			myProperties.load(stream);
		} catch (IOException e) {
			version = null;
			plantUmlVersion = null;
			flexmarkVersion = null;
			flexmarkExtensionsVersion = null;
			throw new IllegalStateException(e);
		}
		version = myProperties.getProperty(PROPERTY_MARKDOWN_CORE_VERSION);
		flexmarkVersion = myProperties.getProperty(PROPERTY_FLEXMARK_VERSION);
		flexmarkExtensionsVersion = myProperties.getProperty(PROPERTY_FLEXMARK_EXTENSIONS_VERSION);
		
		plantUmlVersion = net.sourceforge.plantuml.version.Version.versionString();
	}
	
	/**
	 * Asks PlantUML which Graphviz it would use right now. Which one that is can change while
	 * this library runs, because the executable can be named at any time, so nothing here is
	 * remembered from an earlier call.
	 */
	private static Graphviz currentGraphviz() {
		return GraphvizRuntimeEnvironment.getInstance().create(null, "foo;", "svg");
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
	
	public static String getFlexmarkExtensionsVersion() {
		return flexmarkExtensionsVersion;
	}

	public static String getPlantUmlSecurityProfile() {
		return net.sourceforge.plantuml.security.SecurityUtils.getSecurityProfile().name();
	}
	
	public static String getPlantUmlUrlAllowList() {
		Optional<String> urlAllowList = Optional.ofNullable(System.getProperty("plantuml.allowlist.url"));
		return urlAllowList.orElse("");
	}
	
	public static String getGraphvizVersion() {
		// Remove textual prefix in a string like "dot - graphviz version 12.1.2 (20240928.0832)"
		String versionText = currentGraphviz().dotVersion();
		if (versionText == null) {
			// Graphviz is not installed or does not say which version it is
			return "";
		}

		Pattern versionPattern = Pattern.compile("\\d+\\.\\d+\\.\\d+\\s\\([\\d\\.]*\\)");
		Matcher versionMatcher = versionPattern.matcher(versionText);
		if (versionMatcher.find()) {
			return versionMatcher.group();
		}

		return "";
	}
	
	public static String getGraphvizExecutable() {
		File dotExecutable = currentGraphviz().getDotExe();
		return dotExecutable != null ? dotExecutable.getAbsolutePath() : "";
	}
	
}
