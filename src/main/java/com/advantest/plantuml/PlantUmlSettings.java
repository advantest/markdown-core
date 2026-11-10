/*
 * Copyright © 2022-2026 Advantest Europe GmbH
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.advantest.plantuml;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.sourceforge.plantuml.dot.GraphvizRuntimeEnvironment;
import net.sourceforge.plantuml.security.SecurityProfile;
import net.sourceforge.plantuml.security.SecurityUtils;

/**
 * The settings PlantUML reads while it renders a diagram: where the Graphviz <code>dot</code>
 * executable lies, which security profile applies and which addresses a diagram may read.
 * 
 * <p>None of them belongs to a single diagram, to a single document or to a single renderer.
 * PlantUML keeps each of them once for the whole process, so setting one of them here changes
 * every diagram rendered afterwards, no matter who renders it. That is why every method of this
 * class is static: an instance would promise a scope that does not exist.</p>
 */
public final class PlantUmlSettings {
	
	private static final String PROPERTY_SECURITY_PROFILE = "PLANTUML_SECURITY_PROFILE";
	
	private static final String ALLOW_LIST_SEPARATOR = ";";
	
	private PlantUmlSettings() {
		// nothing to create, every setting belongs to the process
	}
	
	/**
	 * Names the Graphviz <code>dot</code> executable PlantUML shall run to lay a graph out.
	 * 
	 * <p>PlantUML does not look for the executable along the search path. It takes the one named
	 * here, then the one named in the <code>GRAPHVIZ_DOT</code> system property or environment
	 * variable, and where nothing names one it falls back to the Graphviz it carries inside
	 * itself, which lays a graph out differently than an installed Graphviz does. What is named
	 * here therefore wins over the environment, which is what an application offering its user a
	 * setting needs: what the user chose wins over what the machine happens to say.</p>
	 * 
	 * @param dotExecutablePath the path of the executable, which must name an existing file that
	 *                          can be run; <code>null</code> or blank takes the name back, so that
	 *                          PlantUML decides again
	 * @throws IllegalArgumentException if the given path names nothing that can be run, because a
	 *                                  caller shall learn that here and not from a diagram that
	 *                                  comes out laid out by somebody else
	 */
	public static void setDotExecutable(String dotExecutablePath) {
		if (dotExecutablePath == null || dotExecutablePath.isBlank()) {
			GraphvizRuntimeEnvironment.getInstance().setDotExecutable(null);
			return;
		}
		
		File executable = new File(dotExecutablePath.trim());
		if (!executable.isFile() || !executable.canExecute()) {
			throw new IllegalArgumentException(String.format(
					"There is no Graphviz executable that could be run at \"%s\".", dotExecutablePath));
		}
		
		GraphvizRuntimeEnvironment.getInstance().setDotExecutable(executable.getAbsolutePath());
	}
	
	/**
	 * Answers the Graphviz <code>dot</code> executable PlantUML would run, as far as anybody named
	 * one: the one {@link #setDotExecutable(String)} was given, or else the one the
	 * <code>GRAPHVIZ_DOT</code> system property or environment variable names.
	 * 
	 * @return the path of the executable, or <code>null</code> where nobody named one, in which
	 *         case PlantUML falls back to the Graphviz it carries inside itself
	 */
	public static String getDotExecutable() {
		return GraphvizRuntimeEnvironment.getInstance().getenvGraphvizDot();
	}
	
	/**
	 * Sets the security profile PlantUML applies, which decides whether a diagram may read a file
	 * of the machine it runs on and whether it may read an address.
	 * 
	 * <p>PlantUML reads its profile once and then keeps it for the rest of the process, so this
	 * has to be said before the first diagram is rendered. Where the profile was already read and
	 * is a different one, this method says so rather than leaving a caller believing in a profile
	 * that does not apply.</p>
	 * 
	 * <p>Where nothing is said here, the profile PlantUML chooses itself applies, which is the one
	 * its <code>PLANTUML_SECURITY_PROFILE</code> system property or environment variable names and
	 * otherwise {@link SecurityProfile#LEGACY}.</p>
	 * 
	 * @param profile the profile to apply, must not be <code>null</code>
	 * @throws IllegalStateException if PlantUML already read a different profile, which cannot be
	 *                               changed any more
	 */
	public static void setSecurityProfile(SecurityProfile profile) {
		if (profile == null) {
			throw new IllegalArgumentException("Argument must not be null.");
		}
		
		String profileBefore = System.getProperty(PROPERTY_SECURITY_PROFILE);
		System.setProperty(PROPERTY_SECURITY_PROFILE, profile.name());
		
		SecurityProfile appliedProfile = SecurityUtils.getSecurityProfile();
		if (appliedProfile != profile) {
			if (profileBefore == null) {
				System.clearProperty(PROPERTY_SECURITY_PROFILE);
			} else {
				System.setProperty(PROPERTY_SECURITY_PROFILE, profileBefore);
			}
			throw new IllegalStateException(String.format(
					"PlantUML already reads its security profile as %s, so %s cannot be applied any more."
					+ " The profile has to be set before the first diagram is rendered.",
					appliedProfile.name(), profile.name()));
		}
	}
	
	/**
	 * Answers the security profile PlantUML applies, reading it where it was not read yet, so that
	 * it cannot be changed afterwards.
	 * 
	 * @return the profile that applies, never <code>null</code>
	 */
	public static SecurityProfile getSecurityProfile() {
		return SecurityUtils.getSecurityProfile();
	}
	
	/**
	 * Sets the addresses a diagram may read, replacing whatever was allowed before.
	 * 
	 * <p>The list only has an effect under the security profile {@link SecurityProfile#ALLOWLIST},
	 * where a diagram may read the addresses named here and no other. PlantUML reads the list
	 * whenever a diagram asks for an address, so it may be changed while the process runs.</p>
	 * 
	 * @param urls the addresses a diagram may read, must not be <code>null</code>; an empty list
	 *             allows none
	 */
	public static void setUrlAllowList(List<String> urls) {
		if (urls == null) {
			throw new IllegalArgumentException("Argument must not be null.");
		}
		
		System.setProperty(SecurityUtils.ALLOWLIST_URL,
				urls.stream().map(String::trim).filter(url -> !url.isEmpty())
						.collect(Collectors.joining(ALLOW_LIST_SEPARATOR)));
	}
	
	/**
	 * Answers the addresses a diagram may read, as far as anybody allowed one.
	 * 
	 * @return the allowed addresses in the order they were allowed in, never <code>null</code>
	 */
	public static List<String> getUrlAllowList() {
		String allowedUrls = System.getProperty(SecurityUtils.ALLOWLIST_URL);
		if (allowedUrls == null || allowedUrls.isBlank()) {
			return List.of();
		}
		
		return Arrays.stream(allowedUrls.split(ALLOW_LIST_SEPARATOR)).map(String::trim)
				.filter(url -> !url.isEmpty()).collect(Collectors.toList());
	}
	
	/**
	 * Allows a diagram to read the given address, leaving the addresses allowed before allowed.
	 * 
	 * @param url the address to allow, must not be <code>null</code> or blank
	 * @see #setUrlAllowList(List)
	 */
	public static void allowUrl(String url) {
		if (url == null || url.isBlank()) {
			throw new IllegalArgumentException("Argument must not be null or blank.");
		}
		
		List<String> allowedUrls = new ArrayList<>(getUrlAllowList());
		if (!allowedUrls.contains(url.trim())) {
			allowedUrls.add(url.trim());
		}
		setUrlAllowList(allowedUrls);
	}
	
}
