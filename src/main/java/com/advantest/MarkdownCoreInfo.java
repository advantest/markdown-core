/*
 * Copyright © 2022-2026 Advantest Europe GmbH
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.advantest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.advantest.flexmark.FlexmarkExtensionsInfo;
import com.vladsch.flexmark.FlexmarkInfo;

import net.sourceforge.plantuml.dot.Graphviz;
import net.sourceforge.plantuml.dot.GraphvizRuntimeEnvironment;

/**
 * Tells which version of this library runs, and which versions and settings of PlantUML and
 * Graphviz it renders the diagrams with.
 */
public class MarkdownCoreInfo {
	
	/**
	 * The resource the build writes the version of this library into. It is named after the
	 * coordinates of this library, so that no other library on the class path can hide it.
	 */
	static final String VERSION_RESOURCE = "META-INF/com.advantest.markdown/markdown-core/version.properties";
	
	private static final String VERSION_KEY = "version";
	
	private static final String VERSION = readVersion(VERSION_RESOURCE);
	
	/**
	 * Reads the version a resource of this library names.
	 * 
	 * @return the version, or {@code null} if the resource cannot be read, names no version or was
	 *         not filled in by the build
	 */
	static String readVersion(String resource) {
		try (InputStream stream = MarkdownCoreInfo.class.getClassLoader().getResourceAsStream(resource)) {
			if (stream == null) {
				return null;
			}
			Properties properties = new Properties();
			properties.load(stream);
			String version = properties.getProperty(VERSION_KEY);
			if (version == null || version.isBlank() || version.contains("${")) {
				return null;
			}
			return version.strip();
		} catch (IOException | IllegalArgumentException e) {
			return null;
		}
	}
		/**
	 * Asks PlantUML which Graphviz it would use right now. Which one that is can change while
	 * this library runs, because the executable can be named at any time, so nothing here is
	 * remembered from an earlier call.
	 */
	private static Graphviz currentGraphviz() {
		return GraphvizRuntimeEnvironment.getInstance().create(null, "foo;", "svg");
	}
	
	/**
	 * @return the version of this library, or {@code null} if it cannot be read
	 */
	public static String getVersion() {
		return VERSION;
	}
	
	public static String getPlantUmlVersion() {
		return net.sourceforge.plantuml.version.Version.versionString();
	}
	
	/**
	 * @return the version of the flexmark library that runs
	 * @deprecated flexmark tells its version itself: use {@link FlexmarkInfo#getVersion()} instead.
	 */
	@Deprecated
	public static String getFlexmarkVersion() {
		return FlexmarkInfo.getVersion();
	}
	
	/**
	 * @return the version of the flexmark extensions that run
	 * @deprecated the flexmark extensions tell their version themselves: use
	 *             {@link FlexmarkExtensionsInfo#getVersion()} instead.
	 */
	@Deprecated
	public static String getFlexmarkExtensionsVersion() {
		return FlexmarkExtensionsInfo.getVersion();
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
