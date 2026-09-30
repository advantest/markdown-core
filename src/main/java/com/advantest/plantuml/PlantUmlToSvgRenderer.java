/*
 * This work is made available under the terms of the BSD 2-Clause "Simplified" License.
 * The BSD accompanies this distribution (LICENSE.txt).
 * 
 * Copyright © 2022-2024 Advantest Europe GmbH. All rights reserved.
 */
package com.advantest.plantuml;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.GeneratedImage;
import net.sourceforge.plantuml.SourceFileReader;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.dot.GraphvizRuntimeEnvironment;
import net.sourceforge.plantuml.preproc.Defines;

public class PlantUmlToSvgRenderer {
	
	/**
	 * Names the Graphviz <code>dot</code> executable PlantUML shall run, without checking whether
	 * there is one at the given path.
	 * 
	 * @param dotExecutablePath the path of the executable, may be <code>null</code> or blank, in
	 *                          which case nothing is named and PlantUML decides
	 * @deprecated where the executable lies is a setting of the machine rather than of a renderer,
	 *             so it belongs to {@link PlantUmlSettings#setDotExecutable(String)}, which also
	 *             says so where nothing at the given path can be run.
	 */
	@Deprecated
	public void setDotExecutable(String dotExecutablePath) {
		if (dotExecutablePath != null && !dotExecutablePath.isBlank()) {
			GraphvizRuntimeEnvironment.getInstance().setDotExecutable(dotExecutablePath);
		}
	}
	
	public String plantUmlToSvg(String plantUmlCode) {
		String value;
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			SourceStringReader reader = new SourceStringReader(plantUmlCode);
			reader.outputImage(os, new FileFormatOption(FileFormat.SVG));
			value = new String(os.toByteArray(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new RuntimeException("PlantUML exception on given PlantUML source code:\n" + plantUmlCode, e);
		}

		return value;
	}
	
	public File plantUmlToSvg(File plantUmlSourceFile, File targetDirectory) {
		if (plantUmlSourceFile == null || !plantUmlSourceFile.canRead()) {
			throw new IllegalArgumentException(String.format("Cannot read PlantUML source file %s", plantUmlSourceFile));
		}
		
		File targetDir = targetDirectory == null ? plantUmlSourceFile.getParentFile() : targetDirectory;
		File targetFile = null;
		SourceFileReader reader;
		try {
			reader = new SourceFileReader(true, Defines.createWithFileName(plantUmlSourceFile),
					plantUmlSourceFile, targetDir, Collections.<String>emptyList(), StandardCharsets.UTF_8.name(), new FileFormatOption(FileFormat.SVG));
			reader.setCheckMetadata(true);
			List<GeneratedImage> list = reader.getGeneratedImages();
			
			if (!list.isEmpty()) {
				GeneratedImage img = list.get(0);
				targetFile = img.getPngFile();
			}
		} catch (Exception e) {
			throw new RuntimeException(
					String.format("PlantUML exception on PlantUML source code file %s", plantUmlSourceFile.getAbsolutePath()), e);
		}
		
		return targetFile;
	}

	public File plantUmlToSvg(File pumlSourceFile) {
		return plantUmlToSvg(pumlSourceFile, null);
	}
	
}
