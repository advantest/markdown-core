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
import net.sourceforge.plantuml.dot.GraphvizUtils;
import net.sourceforge.plantuml.preproc.Defines;

public class PlantUmlToSvgRenderer {
	
	public void setDotExecutable(String dotExecutablePath) {
		if (dotExecutablePath != null && !dotExecutablePath.isBlank()) {
			GraphvizUtils.setDotExecutable(dotExecutablePath);
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
			reader = new SourceFileReader(Defines.createWithFileName(plantUmlSourceFile),
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
