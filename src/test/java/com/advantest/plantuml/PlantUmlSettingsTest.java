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
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.advantest.MarkdownCoreInfo;

import net.sourceforge.plantuml.security.SecurityProfile;
import net.sourceforge.plantuml.security.SecurityUtils;

class PlantUmlSettingsTest {
	
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
	
}
