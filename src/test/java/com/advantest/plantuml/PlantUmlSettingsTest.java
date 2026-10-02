/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2022-2024 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.plantuml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.advantest.MarkdownCoreInfo;

import net.sourceforge.plantuml.security.SecurityProfile;
import net.sourceforge.plantuml.security.SecurityUtils;

class PlantUmlSettingsTest {
	
	private static final String PROPERTY_SECURITY_PROFILE = "PLANTUML_SECURITY_PROFILE";
	
	@AfterEach
	void leaveTheSettingsAsTheyWere() {
		PlantUmlSettings.setDotExecutable(null);
		System.clearProperty(SecurityUtils.ALLOWLIST_URL);
	}
	
	private File anExecutableFile() throws IOException {
		File executable = File.createTempFile("dot-of-a-test", ".exe");
		executable.deleteOnExit();
		executable.setExecutable(true);
		return executable;
	}
	
	@Test
	void theExecutableThatWasNamedIsTheOneThatIsUsed() throws IOException {
		File executable = anExecutableFile();
		
		PlantUmlSettings.setDotExecutable(executable.getAbsolutePath());
		
		assertEquals(executable.getAbsolutePath(), PlantUmlSettings.getDotExecutable());
		assertEquals(executable.getAbsolutePath(), MarkdownCoreInfo.getGraphvizExecutable());
	}
	
	@Test
	void namingNothingLetsPlantUmlDecideAgain() throws IOException {
		PlantUmlSettings.setDotExecutable(anExecutableFile().getAbsolutePath());
		
		PlantUmlSettings.setDotExecutable(null);
		
		assertNull(PlantUmlSettings.getDotExecutable());
	}
	
	@Test
	void namingAPathThatHoldsNoExecutableSaysSo() {
		File missingExecutable = new File("this-directory-does-not-exist", "dot");
		
		IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> PlantUmlSettings.setDotExecutable(missingExecutable.getPath()));
		
		assertTrue(failure.getMessage().contains(missingExecutable.getPath()));
	}
	
	@Test
	void theProfilePlantUmlAlreadyAppliesCanBeSetAgain() {
		SecurityProfile appliedProfile = PlantUmlSettings.getSecurityProfile();
		
		PlantUmlSettings.setSecurityProfile(appliedProfile);
		
		assertEquals(appliedProfile, PlantUmlSettings.getSecurityProfile());
	}
	
	@Test
	void aProfileThatCannotBeAppliedAnyMoreIsSaidToBeOne() {
		SecurityProfile appliedProfile = PlantUmlSettings.getSecurityProfile();
		SecurityProfile anotherProfile = appliedProfile == SecurityProfile.SANDBOX
				? SecurityProfile.INSECURE
				: SecurityProfile.SANDBOX;
		
		IllegalStateException failure = assertThrows(IllegalStateException.class,
				() -> PlantUmlSettings.setSecurityProfile(anotherProfile));
		
		assertTrue(failure.getMessage().contains(appliedProfile.name()));
		assertEquals(appliedProfile, PlantUmlSettings.getSecurityProfile());
	}
	
	@Test
	void nothingIsAllowedUntilSomebodyAllowsIt() {
		assertEquals(List.of(), PlantUmlSettings.getUrlAllowList());
	}
	
	@Test
	void theAddressesThatWereAllowedAreTheOnesPlantUmlReads() {
		PlantUmlSettings.setUrlAllowList(List.of("https://one.example.com/", "https://two.example.com/"));
		
		assertEquals(List.of("https://one.example.com/", "https://two.example.com/"),
				PlantUmlSettings.getUrlAllowList());
		assertEquals("https://one.example.com/;https://two.example.com/",
				MarkdownCoreInfo.getPlantUmlUrlAllowList());
	}
	
	@Test
	void allowingAnAddressLeavesTheOnesAllowedBeforeAllowed() {
		PlantUmlSettings.setUrlAllowList(List.of("https://one.example.com/"));
		
		PlantUmlSettings.allowUrl("https://two.example.com/");
		PlantUmlSettings.allowUrl("https://one.example.com/");
		
		assertEquals(List.of("https://one.example.com/", "https://two.example.com/"),
				PlantUmlSettings.getUrlAllowList());
	}

	@Test
	void namingABlankPathTakesTheNameBackLikeNamingNothing() throws IOException {
		PlantUmlSettings.setDotExecutable(anExecutableFile().getAbsolutePath());
		
		PlantUmlSettings.setDotExecutable("   ");
		
		assertNull(PlantUmlSettings.getDotExecutable());
	}
	
	@Test
	void aPathWrittenWithSurroundingBlanksNamesTheSameExecutable() throws IOException {
		File executable = anExecutableFile();
		
		PlantUmlSettings.setDotExecutable("  " + executable.getAbsolutePath() + " ");
		
		assertEquals(executable.getAbsolutePath(), PlantUmlSettings.getDotExecutable());
	}
	
	@Test
	void namingADirectoryInsteadOfAnExecutableSaysSo() throws IOException {
		File directory = Files.createTempDirectory("dot-directory-of-a-test").toFile();
		directory.deleteOnExit();
		
		assertThrows(IllegalArgumentException.class, () -> PlantUmlSettings.setDotExecutable(directory.getPath()));
		assertNull(PlantUmlSettings.getDotExecutable());
	}
	
	@Test
	void aMissingProfileIsRefused() {
		assertThrows(IllegalArgumentException.class, () -> PlantUmlSettings.setSecurityProfile(null));
	}
	
	@Test
	void aProfileThatCannotBeAppliedLeavesNoSystemPropertyBehind() {
		System.clearProperty(PROPERTY_SECURITY_PROFILE);
		SecurityProfile anotherProfile = anotherProfileThanTheAppliedOne();
		
		assertThrows(IllegalStateException.class, () -> PlantUmlSettings.setSecurityProfile(anotherProfile));
		
		assertNull(System.getProperty(PROPERTY_SECURITY_PROFILE));
	}
	
	@Test
	void aProfileThatCannotBeAppliedLeavesTheSystemPropertyAsItWas() {
		SecurityProfile appliedProfile = PlantUmlSettings.getSecurityProfile();
		System.setProperty(PROPERTY_SECURITY_PROFILE, appliedProfile.name());
		
		try {
			assertThrows(IllegalStateException.class,
					() -> PlantUmlSettings.setSecurityProfile(anotherProfileThanTheAppliedOne()));
			
			assertEquals(appliedProfile.name(), System.getProperty(PROPERTY_SECURITY_PROFILE));
		} finally {
			System.clearProperty(PROPERTY_SECURITY_PROFILE);
		}
	}
	
	@Test
	void aMissingListOfAddressesIsRefused() {
		assertThrows(IllegalArgumentException.class, () -> PlantUmlSettings.setUrlAllowList(null));
	}
	
	@Test
	void anEmptyListOfAddressesAllowsNoneAndReplacesWhatWasAllowed() {
		PlantUmlSettings.setUrlAllowList(List.of("https://one.example.com/"));
		
		PlantUmlSettings.setUrlAllowList(List.of());
		
		assertEquals(List.of(), PlantUmlSettings.getUrlAllowList());
		assertEquals("", MarkdownCoreInfo.getPlantUmlUrlAllowList());
	}
	
	@Test
	void blanksAroundAnAllowedAddressAndBlankAddressesAreLeftOut() {
		PlantUmlSettings.setUrlAllowList(List.of("  https://one.example.com/ ", "   ", "", "https://two.example.com/"));
		
		assertEquals(List.of("https://one.example.com/", "https://two.example.com/"),
				PlantUmlSettings.getUrlAllowList());
	}
	
	@Test
	void anAllowListThatWasSetToNothingButBlanksAllowsNoAddress() {
		System.setProperty(SecurityUtils.ALLOWLIST_URL, "  ");
		assertEquals(List.of(), PlantUmlSettings.getUrlAllowList());
		
		System.setProperty(SecurityUtils.ALLOWLIST_URL, " ; ;");
		assertEquals(List.of(), PlantUmlSettings.getUrlAllowList());
	}
	
	@Test
	void allowingAnAddressWrittenWithBlanksAllowsItWithoutThem() {
		PlantUmlSettings.allowUrl("  https://one.example.com/ ");
		
		assertEquals(List.of("https://one.example.com/"), PlantUmlSettings.getUrlAllowList());
	}
	
	@Test
	void allowingNothingIsRefused() {
		assertThrows(IllegalArgumentException.class, () -> PlantUmlSettings.allowUrl(null));
		assertThrows(IllegalArgumentException.class, () -> PlantUmlSettings.allowUrl("  "));
		assertEquals(List.of(), PlantUmlSettings.getUrlAllowList());
	}
	
	private SecurityProfile anotherProfileThanTheAppliedOne() {
		return PlantUmlSettings.getSecurityProfile() == SecurityProfile.SANDBOX
				? SecurityProfile.INSECURE
				: SecurityProfile.SANDBOX;
	}
	
}
