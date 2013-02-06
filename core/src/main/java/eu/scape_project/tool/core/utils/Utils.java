/**
 ################################################################################
 #                  Copyright 2012 The SCAPE Project Consortium
 #
 #   This software is copyrighted by the SCAPE Project Consortium. 
 #   The SCAPE project is co-funded by the European Union under
 #   FP7 ICT-2009.4.1 (Grant Agreement number 270137).
 #
 #   Licensed under the Apache License, Version 2.0 (the "License");
 #   you may not use this file except in compliance with the License.
 #   You may obtain a copy of the License at
 #
 #                   http://www.apache.org/licenses/LICENSE-2.0              
 #
 #   Unless required by applicable law or agreed to in writing, software
 #   distributed under the License is distributed on an "AS IS" BASIS,
 #   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   
 #   See the License for the specific language governing permissions and
 #   limitations under the License.
 ################################################################################
 */
package eu.scape_project.tool.core.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/** Utility class with common functionalities to the core package */

public final class Utils {
	private static Logger log = Logger.getLogger(Utils.class);

	/** Private empty constructor as this class shouldn't be instantiated */
	private Utils() {

	}

	/**
	 * Method useful to create a temporary directory identified by a given name
	 * 
	 * @param name
	 *            name of the temporary directory to be created
	 * @return the {@link File} object that identifies the created temporary
	 *         directory or null if any exception occurs
	 * */
	public static File createTemporaryDirectory(String name) {
		File tempDir = null;
		try {
			tempDir = File.createTempFile(name, "");
			if (!(tempDir.delete() && tempDir.mkdir())) {
				tempDir = null;
				log.error("Error creating temp folder");
			}
		} catch (IOException e) {
			log.error("Error while creating temporary folder (\"" + name
					+ "\")");
		}
		return tempDir;
	}

	/**
	 * Method useful to copy a particular resource (file under
	 * src/main/resources) to a temporary directory
	 * 
	 * @param tempDir
	 *            directory where the resource is being put on
	 * @param resourcePath
	 *            the path to the resource (beneath src/main/resources)
	 * @param resourceName
	 *            the name of the resource being copied
	 * @param newName
	 *            new name to be given to the resouce being copied
	 * @param createTempDir
	 *            flag on either the temporary directory needs to be created
	 * @param invokingClass
	 *            class to which the resource is related to
	 * @return true if the resource was successfully copied, false otherwise
	 * */
	public static boolean copyResourceToTemporaryDirectory(File tempDir,
			String resourcePath, String resourceName, String newName,
			boolean createTempDir, Class<?> invokingClass) {
		boolean success = false;
		FileOutputStream fileOutputStream = null;
		try {
			if (createTempDir && !tempDir.mkdir()) {
				log.error("Error creating directory \"" + tempDir + "\"...");
			}
			File f = new File(tempDir, newName != null ? newName : resourceName);
			fileOutputStream = new FileOutputStream(f);
			IOUtils.copy(
					invokingClass.getResourceAsStream(resourcePath
							+ resourceName), fileOutputStream);
			success = true;
		} catch (IOException e) {
			log.error(e);
		} finally {
			if (fileOutputStream != null) {
				try {
					fileOutputStream.close();
				} catch (IOException e) {
					log.error(e);
				}
			}
		}
		return success;
	}

	/**
	 * Method useful to write content that was previously merged to a certain
	 * Velocity template into a file
	 * 
	 * @param outputDirectory
	 *            directory where to write to
	 * @param childDirectory
	 *            child directory if the information being written should be
	 *            placed in here
	 * @param filename
	 *            name of the file being created
	 * @param w
	 *            {@link StringWriter} that holds the information that is going
	 *            to be written
	 * @param makeItExecutable
	 *            if the file being created should be executable
	 * @return true if the file was successfully written, false otherwise
	 * */
	public static boolean writeTemplateContent(String outputDirectory,
			String childDirectory, String filename, StringWriter w,
			boolean makeItExecutable) {
		boolean res = true;
		File directory;
		if (childDirectory != null) {
			directory = new File(outputDirectory, childDirectory);
		} else {
			directory = new File(outputDirectory);
		}
		File file = new File(directory, filename);
		FileOutputStream fos = null;
		if (file != null) {
			try {
				fos = new FileOutputStream(file);
				fos.write(w.toString().getBytes(Charset.defaultCharset()));
			} catch (FileNotFoundException e) {
				log.error(e);
				res = false;
			} catch (IOException e) {
				log.error(e);
				res = false;
			} finally {
				if (fos != null) {
					try {
						fos.close();
						file.setExecutable(makeItExecutable);
					} catch (IOException e) {
						log.error(e);
						res = false;
					}
				}
			}
		}
		return res;
	}

	/**
	 * Method useful to copy a file from one place to another
	 * 
	 * @param source
	 *            file path being copied
	 * @param destination
	 *            file path where to the file is being copied
	 * @return true if the file was successfully copied, false otherwise
	 * */
	public static boolean copyFile(File source, File destination,
			boolean setExecutable) {
		boolean success = true;
		if (source.exists()) {
			try {
				FileUtils.copyFile(source, destination);
				if (!destination.setExecutable(setExecutable)) {
					log.error("Error while setting execution permissions on \""
							+ destination + "\"...");
				}
			} catch (IOException e) {
				log.error("Error while copying file \"" + source + "\" to \""
						+ destination + "\"...");
				success = false;
			}
		} else {
			log.error("File \"" + source + "\" does not exists...");
			success = false;
		}
		return success;
	}

	/**
	 * Method that wraps a text in double quotes
	 * 
	 * @param in
	 *            string being double quotes wrapped
	 * @return wrapped string
	 * */
	public static String wrapWithDoubleQuotes(String in) {
		return "\"" + in + "\"";
	}

}
