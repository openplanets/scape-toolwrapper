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
package eu.scape_project.tool.toolwrapper.toolwrapper_bash_debian_generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import eu.scape_project.tool.toolwrapper.core.ToolWrapperCommandline;
import eu.scape_project.tool.toolwrapper.core.ToolWrapperGenerator;
import eu.scape_project.tool.toolwrapper.core.configuration.Constants;
import eu.scape_project.tool.toolwrapper.core.exceptions.ErrorParsingCmdArgsException;
import eu.scape_project.tool.toolwrapper.core.exceptions.SpecParsingException;
import eu.scape_project.tool.toolwrapper.core.utils.Utils;
import eu.scape_project.tool.toolwrapper.data.tool_spec.Input;
import eu.scape_project.tool.toolwrapper.data.tool_spec.Installation;
import eu.scape_project.tool.toolwrapper.data.tool_spec.OperatingSystemDependency;
import eu.scape_project.tool.toolwrapper.data.tool_spec.Operation;
import eu.scape_project.tool.toolwrapper.data.tool_spec.Output;
import eu.scape_project.tool.toolwrapper.data.tool_spec.PackageManager;
import eu.scape_project.tool.toolwrapper.data.tool_spec.Parameter;
import eu.scape_project.tool.toolwrapper.data.tool_spec.ParameterPossibleValue;
import eu.scape_project.tool.toolwrapper.data.tool_spec.Tool;
import eu.scape_project.tool.toolwrapper.toolwrapper_bash_debian_generator.utils.DebianFileFilter;

/** Class that generates, from a toolspec, a Debian package */
public class DebianBashWrapperGenerator extends ToolWrapperCommandline
		implements ToolWrapperGenerator {

	private static Logger log = Logger
			.getLogger(DebianBashWrapperGenerator.class);
	private static final List<String> DEBIAN_FILES = Arrays.asList(
			"source/format", "compat", "control", "copyright", "dirs", "raw2nexus.lintian-overrides", 
			"install", "MAN.manpages", "MAN.pod", "README", "rules");
	private static final String RFC_822_DATE_PATTERN = "EEE, dd MMM yyyy HH:mm:ss Z";
	private static final String DEBIAN_TEMPLATE_DIRECTORY_NAME = "/debian_package_template/";
	private static final String CMD_OPTIONS_DESCRIPTION_SHORT = "desc";

	private Tool tool;
	private Operation operation;
	private String wrapperName;
	private String maintainerEmail;
	private String inputDir;
	private String changelog;
	private String description;
	private File outputDirectory;
	private boolean aggregate;
	// /tmp/TEMP_DIR
	private File tempDebianBaseDir;
	// /tmp/TEMP_DIR/DEB_NAME
	private File tempDebianDir;
	// /tmp/TEMP_DIR/DEB_NAME/debian
	private File tempDebianInnerDir;
	private Map<String, Template> debianTemplates;

	/**
	 * Public empty constructor (adds a bunch of command-line options and sets
	 * instance variables values to null)
	 */
	public DebianBashWrapperGenerator() {
		super();
		Options options = super.getOptions();
		Option opt = new Option("e", "email", true,
				"maintainer e-mail for Debian package generation");
		opt.setRequired(true);
		options.addOption(opt);
		opt = new Option("a", "aggregate", false,
				"aggregates all artifacts in a single Debian package (use it with -d)");
		opt.setRequired(false);
		options.addOption(opt);
		opt = new Option("d", "debName", true,
				"name of the Debian package (use it with -a)");
		opt.setRequired(false);
		options.addOption(opt);
		opt = new Option("i", "inDir", true,
				"directory where to input artifacts are");
		opt.setRequired(true);
		options.addOption(opt);

		opt = new Option("ch", "changelog", true,
				"location of the changelog to be included");
		opt.setRequired(true);
		options.addOption(opt);
		opt = new Option(CMD_OPTIONS_DESCRIPTION_SHORT, "description", true,
				"description to be added to the Debian package (use it with -a and -d)");
		opt.setRequired(false);
		options.addOption(opt);

		tool = null;
		operation = null;
		wrapperName = null;
		maintainerEmail = null;
		inputDir = null;
		debianTemplates = null;
		changelog = null;
		description = "";
		outputDirectory = null;
		aggregate = true;
		tempDebianBaseDir = null;
		tempDebianDir = null;
		tempDebianInnerDir = null;
		debianTemplates = null;
	}

	public String getWrapperName() {
		return wrapperName;
	}

	public void setOperation(Operation operation) {
		this.operation = operation;
	}

	public void setWrapperName(String wrapperName) {
		this.wrapperName = wrapperName;
	}

	public void setMaintainerEmail(String maintainerEmail) {
		this.maintainerEmail = maintainerEmail;
	}

	public void setInputDir(String inputDir) {
		this.inputDir = inputDir;
	}

	public void setChangelog(String changelog) {
		this.changelog = changelog;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Method that implements all the logic of creating a Debian package (using
	 * Velocity templates)
	 * 
	 * @param tool
	 *            tool from which the artifact(s) is being created
	 * @param operation
	 *            if the artifact(s) is related to a particular
	 *            {@link Operation}
	 * @param outputDirectory
	 *            directory where to place the created artifact(s)
	 * 
	 * */
	@Override
	public boolean generateWrapper(Tool tool, Operation operation,
			File outputDirectory) {
		this.tool = tool;
		this.operation = operation;
		this.aggregate = (operation == null);
		this.outputDirectory = outputDirectory;
		boolean res = true;

		// init velocity, i.e., set property to load templates from the
		// classpath
		initVelocity();

		// 1) verify if the needed directories exist or can be created
		// 2) verify if toolspec semantic aspects are correct
		if (verifyNeededDirectories(outputDirectory)
				&& verifyToolspecSemanticAspects()) {
			VelocityContext wrapperContext;

			if (operation != null) {
				// generate a Debian package for each operation

				wrapperContext = new VelocityContext();

				// add bash related information to the template context
				addGeneralInformationToContext(wrapperContext);
				addUsageInformationToContext(wrapperContext);

				res = generateDebianPackage(wrapperContext);

			} else {
				// generate a single Debian

				wrapperContext = new VelocityContext();

				// add bash related information to the template context
				addGeneralInformationToContext(wrapperContext);

				res = generateDebianPackage(wrapperContext);
			}

			cleanEnvironment();
		} else {
			res = false;
		}
		return res;
	}

	private void initVelocity() {
		Properties p = new Properties();
		p.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		p.setProperty("classpath.resource.loader.class",
				ClasspathResourceLoader.class.getName());
		p.setProperty("runtime.log.logsystem.log4j.logger",
				"org.apache.velocity");
		Velocity.init(p);
	}

	private boolean verifyNeededDirectories(File outputDirectory) {
		boolean res = true;
		if (!outputDirectory.exists() && !outputDirectory.mkdir()) {
			log.error("The directory \"" + outputDirectory.getAbsolutePath()
					+ "\" cannot be created...");
			res = false;
		} else {
			// create a directory where the Debian package will end up
			File childDirectory = new File(outputDirectory,
					Constants.DEBIANBASHGENERATOR_DEBS_OUTDIRNAME);
			boolean operationResult = (childDirectory.exists() || childDirectory
					.mkdir());
			res = res && operationResult;
			if (!operationResult) {
				log.error("The directory \"" + childDirectory
						+ "\" either not exists or cannot be created...");
			}
		}
		return res;
	}

	private boolean verifyToolspecSemanticAspects() {
		boolean res = true;

		// verify if the operations description has the right size to be added
		// to the Debian files (control)
		for (Operation op : tool.getOperations().getOperation()) {
			if (op.getDescription().length() > 60) {
				log.error("Description size for operation \""
						+ op.getName()
						+ "\" is greater than 60 and it shouldn't.\nFix it before generating the Debian package!");
				res = false;
			}
		}
		// also verify it if it was passed through the command-line
		if (!"".equals(description) && description.length() > 60) {
			log.error("Description size is greater than 60 and it shouldn't.\nFix it before generating the Debian package!");
			res = false;
		}
		return res;
	}

	private void addGeneralInformationToContext(VelocityContext context) {
		// general information to be added to the different Debian package
		// templates
		context.put("toolName", tool.getName());
		context.put("toolHomepage", tool.getHomepage());
		context.put("toolVersion", tool.getVersion());
		context.put("wrapperName", wrapperName);
		context.put("aggregate", aggregate);
		context.put("operations", tool.getOperations().getOperation());
	}

	private void addUsageInformationToContext(VelocityContext wrapperContext) {
		wrapperContext.put("usageDescription", operation.getDescription());
		addInputUsageInformationToContext(wrapperContext);
		addParamUsageInformationToContext(wrapperContext);
		addOutputUsageInformationToContext(wrapperContext);
	}

	private void addInputUsageInformationToContext(
			VelocityContext wrapperContext) {
		if (operation.getInputs().getStdin() != null) {
			wrapperContext.put("usageInputParameter", "-i STDIN");
			wrapperContext.put("usageInputParameterDescription",
					"-i STDIN > Read input from the STDIN");
		} else {
			// user input parameters
			StringBuilder uip = new StringBuilder("");
			// user input parameters description to print on the bash usage
			// function
			StringBuilder uipd = new StringBuilder("");
			// user input parameters description for man page
			StringBuilder uipdman = new StringBuilder("");
			int i = 0;
			for (Input input : operation.getInputs().getInput()) {
				String value = "-i " + input.getName()
						+ (i == 0 ? "|STDIN" : "");
				uip.append((uip.length() == 0 ? "" : " "));
				if (input.isRequired()) {
					uip.append(value);
				} else {
					uip.append("[" + value + "]");
				}
				String inputDescription = value + " > "
						+ input.getDescription()
						+ (i == 0 ? " OR Read input from the STDIN" : "");
				uipd.append((uipd.length() != 0 ? "\n\t" : "")
						+ inputDescription);
				uipdman.append(inputDescription + "\n\n");
				i++;
			}
			wrapperContext.put("usageInputParameter", uip.toString());
			wrapperContext.put("usageInputParameterDescription",
					uipd.toString());
			wrapperContext.put("usageInputParameterDescriptionForMan",
					uipdman.toString());
		}
	}

	private void addParamUsageInformationToContext(
			VelocityContext wrapperContext) {
		// user input parameters
		StringBuilder uip = new StringBuilder("");
		// user input parameters description to print on the bash usage function
		StringBuilder uipd = new StringBuilder("");
		// user input parameters description for man page
		StringBuilder uipdman = new StringBuilder("");
		for (Parameter param : operation.getInputs().getParameter()) {
			String value = "-p " + param.getName();
			uip.append((uip.length() == 0 ? "" : " "));
			if (param.isRequired()) {
				uip.append(value);
			} else {
				uip.append("[" + value + "]");
			}
			String paramDescription = value + " > " + param.getDescription();
			uipd.append((uipd.length() != 0 ? "\n\t" : "") + paramDescription);
			uipdman.append(paramDescription + "\n\n");
			
			for (ParameterPossibleValue possibleValue : param
					.getPossibleValue()) {
				uipd.append((uipd.length() != 0 ? "\n\t\t" : "") + "\\\""
						+ possibleValue.getValue() + "\\\" > "
						+ possibleValue.getDescription());
				uipdman.append("\t\""
						+ possibleValue.getValue() + "\" > "
						+ possibleValue.getDescription() + "\n\n");
			}
			
		}
		wrapperContext.put("usageParamParameter", uip.toString());
		wrapperContext.put("usageParamParameterDescription", uipd.toString());
		wrapperContext.put("usageParamParameterDescriptionForMan",
				uipdman.toString());
	}

	private void addOutputUsageInformationToContext(
			VelocityContext wrapperContext) {
		if (operation.getOutputs().getStdout() != null) {
			wrapperContext.put("usageOutputParameter", "-o STDOUT");
			wrapperContext.put("usageOutputParameterDescription",
					"-o STDOUT > Write output to the STDOUT");
		} else {
			// user input parameters
			StringBuilder uip = new StringBuilder("");
			// user input parameters description to print on the bash usage
			// function
			StringBuilder uipd = new StringBuilder("");
			// user input parameters description for man page
			StringBuilder uipdman = new StringBuilder("");
			int i = 0;
			for (Output output : operation.getOutputs().getOutput()) {
				String value = "-o " + output.getName()
						+ (i == 0 ? "|STDOUT" : "");
				uip.append((uip.length() == 0 ? "" : " "));
				if (output.isRequired()) {
					uip.append(value);
				} else {
					uip.append("[" + value + "]");
				}
				String outputDescription = value + " > "
						+ output.getDescription()
						+ (i == 0 ? " OR Write output to the STDOUT" : "");
				uipd.append((uipd.length() != 0 ? "\n\t" : "")
						+ outputDescription);
				uipdman.append(outputDescription + "\n\n");
				i++;
			}
			wrapperContext.put("usageOutputParameter", uip.toString());
			wrapperContext.put("usageOutputParameterDescription",
					uipd.toString());
			wrapperContext.put("usageOutputParameterDescriptionForMan",
					uipdman.toString());
		}
	}

	private boolean generateDebianPackage(VelocityContext context) {
boolean result = setupEnvironment(context); 


boolean copye = copyExternalFiles();
boolean build = buildPackage();
boolean copy = copyPackage();
System.out.println("generateDebianPackage: env = " + result + " copyext: " + copye + " build: " + build + " copypackage: " + copy);
return result && build && copy && copye;	
}


	private boolean setupEnvironment(VelocityContext context) {
boolean create =createDebianPackageDirectorySkeleton();
boolean install =installExtraFilesIfAny(context);
boolean load =loadDebianTemplates();
boolean add =  addInformationToDebianTemplates(context);
System.out.println ("setupEnvironment: create: " + create + " install: " + install + " load: " + load + " add: " + add );

				return create && install && load
				&& add;
	}

	private boolean createDebianPackageDirectorySkeleton() {
		boolean success = true;
		tempDebianBaseDir = Utils.createTemporaryDirectory(wrapperName);
		tempDebianDir = new File(tempDebianBaseDir, wrapperName);
		success = success && tempDebianDir.mkdir();
		tempDebianInnerDir = new File(tempDebianDir, "debian");
		success = success && tempDebianInnerDir.mkdir();
		if (tempDebianInnerDir != null) {
			File sourceDir = new File(tempDebianInnerDir, "source");
			success = success && sourceDir.mkdir();
		}
		return success;
	}

	private boolean installExtraFilesIfAny(VelocityContext context) {
		boolean res = true;
		// directory with files to be installed (if any)
		File directoryWithFilesToBeInstalled = new File(inputDir,
				Constants.BASHGENERATOR_INSTALL_OUTDIRNAME);
		List<String> installList = new ArrayList<String>();

		if (directoryWithFilesToBeInstalled.exists()
				&& directoryWithFilesToBeInstalled.isDirectory()) {

			// all files and directories to be installed
			File[] listFiles = directoryWithFilesToBeInstalled.listFiles();

			try {

				// copy everything to the directory where the Debian package is
				// being built
				FileUtils.copyDirectory(directoryWithFilesToBeInstalled,
						tempDebianDir);

				// add each file/directory to the install script template
				for (File file : listFiles) {
					installList.add(file.getName());
				}

				// as the Apache Commons IO copy methods does not preserve
				// execution permission, it needs to be set manually
				for (File file : listFiles) {
					verifyExecutionPermissionsOfFilesBeingInstalled(file);
				}
			} catch (IOException e) {
				log.error("Error copying \"" + directoryWithFilesToBeInstalled
						+ "\" to \"" + tempDebianDir + "\"");
			}
			context.put("installList", installList);
		}
		return res;
	}

	private void verifyExecutionPermissionsOfFilesBeingInstalled(File file) {
		String absolutePath = file.getAbsolutePath();
		String relativePath = absolutePath.replaceFirst(new File(inputDir,
				Constants.BASHGENERATOR_INSTALL_OUTDIRNAME).getAbsolutePath(),
				"");
		if (file.isFile()) {

			// if the original has execution permission, then add the same
			// permission to the copied version
			if (file.canExecute()) {
				new File(tempDebianDir, relativePath).setExecutable(true);
			}
		} else if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				verifyExecutionPermissionsOfFilesBeingInstalled(f);
			}
		}
	}

	private boolean loadDebianTemplates() {
		boolean success = true;
		if (debianTemplates == null) {
			debianTemplates = new HashMap<String, Template>();
			for (String file : DEBIAN_FILES) {
				debianTemplates
						.put(file,
								loadVelocityTemplateFromResources(DEBIAN_TEMPLATE_DIRECTORY_NAME
										+ file));
			}
		}
		return success;
	}

	private Template loadVelocityTemplateFromResources(String templatePath) {
		Template template = null;
		try {
			template = Velocity.getTemplate(templatePath, "UTF-8");
		} catch (ResourceNotFoundException e) {
			log.error(e);
		} catch (ParseErrorException e) {
			log.error(e);
		}
		return template;
	}

	private String processInstallationInformation(Installation installation) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		if (installation != null) {
			List<OperatingSystemDependency> operatingSystemList = installation
					.getOperatingSystem();

			for (OperatingSystemDependency osd : operatingSystemList) {
				if ("Ubuntu".equalsIgnoreCase(osd.getOperatingSystemName()
						.toString())
						|| "Debian".equalsIgnoreCase(osd
								.getOperatingSystemName().toString())) {
					List<PackageManager> packageManagerList = osd
							.getPackageManager();
					for (PackageManager packageManager : packageManagerList) {
						if ("Dpkg".equalsIgnoreCase(packageManager.getType()
								.name())) {
							sb.append((i == 0 ? "" : ",")
									+ packageManager.getConfig().toString());
							i++;
						}
					}
					break;
				}
			}
		}
		return sb.toString();
	}

	private boolean addInformationToDebianTemplates(VelocityContext context) {
		SimpleDateFormat sdf = new SimpleDateFormat(RFC_822_DATE_PATTERN,
				Locale.ENGLISH);
		context.put("dateOfGeneration", sdf.format(new Date()));
		context.put("maintainerEmail", maintainerEmail);
		context.put("license", tool.getLicense().getName().value());

		Installation installation = tool.getInstallation();
		context.put("toolDependencies",
				processInstallationInformation(installation));

		if (aggregate) {
			context.put("toolDescription", description);
		} else {
			context.put("toolDescription", operation.getDescription());
		}
		context.put("toolHomepage", tool.getHomepage());
		context.put("toolVersion", tool.getVersion());

		context.put("folder", tempDebianDir.getAbsolutePath());

		for (Entry<String, Template> debianTemplateEntry : debianTemplates
				.entrySet()) {
			StringWriter sw = new StringWriter();

			if (aggregate && debianTemplateEntry.getKey().startsWith("MAN.pod")) {
				for (Operation op : tool.getOperations().getOperation()) {
					sw = new StringWriter();
					setOperation(op);
					addUsageInformationToContext(context);
					context.put("wrapperName", op.getName());
					context.put("toolDescription", op.getDescription());
					debianTemplateEntry.getValue().merge(context, sw);
					Utils.writeTemplateContent(
							tempDebianInnerDir,
							null,
							debianTemplateEntry
									.getKey()
									.replaceFirst("MAN.pod",
											op.getName() + ".pod")
									.replaceFirst("MAN", wrapperName), sw,
							false);
				}
				setOperation(null);
			} else {
				debianTemplateEntry.getValue().merge(context, sw);
				Utils.writeTemplateContent(
						tempDebianInnerDir,
						null,
						debianTemplateEntry.getKey().replaceFirst("MAN",
								wrapperName), sw, false);
			}
		}

		return true;
	}

	private boolean copyExternalFiles() {
		boolean success = true;

		// copy command-line provided changelog to the temporary directory
		success = success
				&& Utils.copyFile(new File(changelog), new File(
						tempDebianInnerDir, "changelog"), false);

		if (aggregate) {
			for (Operation op : tool.getOperations().getOperation()) {
				String filename = op.getName();
				success = success
						&& Utils.copyFile(new File(inputDir,
								Constants.BASHGENERATOR_WRAPPER_OUTDIRNAME
										+ "/" + filename), new File(
								tempDebianDir, filename), true);

				// copy the workflow to the temporary directory
				filename = op.getName()
						+ Constants.BASHGENERATOR_WORKFLOW_EXTENSION;
				success = success
						&& Utils.copyFile(new File(inputDir,
								Constants.BASHGENERATOR_WORKFLOW_OUTDIRNAME
										+ "/" + filename), new File(
								tempDebianDir, filename), false);
			}
		} else {
			// copy the bash wrapper to the temporary directory
			String filename = operation.getName();
			success = success
					&& Utils.copyFile(new File(inputDir,
							Constants.BASHGENERATOR_WRAPPER_OUTDIRNAME + "/"
									+ filename), new File(tempDebianDir,
							filename), true);

			// copy the workflow to the temporary directory
			filename = operation.getName()
					+ Constants.BASHGENERATOR_WORKFLOW_EXTENSION;
			success = success
					&& Utils.copyFile(new File(inputDir,
							Constants.BASHGENERATOR_WORKFLOW_OUTDIRNAME + "/"
									+ filename), new File(tempDebianDir,
							filename), false);
		}

		return success;
	}

	private boolean buildPackage() {
		boolean success = true;
		log.info("generatePackage starting now...");
		Runtime rt = Runtime.getRuntime();
		BufferedReader br = null;
	BufferedReader br2 = null;
		try {
			// / usr/bin/dpkg-buildpackage
			Process exec = rt.exec("/usr/bin/debuild -us -uc -b", null,
					tempDebianDir);
System.out.println("buildPackage: tempDebianDir " + tempDebianDir);
			br = new BufferedReader(new InputStreamReader(	exec.getInputStream(), Charset.defaultCharset()));
 br2 = new BufferedReader(new InputStreamReader(  exec.getErrorStream(), Charset.defaultCharset()));			
success = (exec.waitFor() == 0);
 			String line = br.readLine();
                        while (line != null) {
                                log.info(line);
				System.out.println("buildPackage: " + line);
				line = br.readLine();
                        }


                        line = br2.readLine();
                        while (line != null) {
                                log.info(line);
                                System.out.println("buildPackage: " + line);
                                line = br2.readLine();
                        }



		} catch (InterruptedException e) {
			success = false;
e.printStackTrace();
		} catch (IOException e) {
			success = false;
e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					log.error(e);
				}
			}
		}
		return success;
	}

	private boolean copyPackage() {
		boolean res = true;

		// copy the Debian package
		for (File file : tempDebianBaseDir.listFiles(new DebianFileFilter())) {
			try {
				FileUtils.copyFileToDirectory(file, new File(outputDirectory,
						Constants.DEBIANBASHGENERATOR_DEBS_OUTDIRNAME));
				break;
			} catch (IOException e) {
				log.error("Error while copying file \"" + file
						+ "\" to directory \"" + outputDirectory + "\"");
				res = false;
			}
		}
		return res;
	}

	private void cleanEnvironment() {
		// delete the temporary directory as it isn't needed anymore
		//boolean deleteRes = FileUtils.deleteQuietly(tempDebianBaseDir);
		//if (!deleteRes) {
		//	log.error("Error while deleting temporary directory \""
		//			+ tempDebianBaseDir + "\"");
		//}
	}

	private static boolean areNonRequiredArgumentsThatCanBeCombinedInvalid(
			CommandLine cmd) {
		boolean res = false;

		// validate command-line parameters that are not required but need to be
		// used

		if ((!cmd.hasOption("d") || !cmd
				.hasOption(CMD_OPTIONS_DESCRIPTION_SHORT))
				&& cmd.hasOption("a")) {
			res = true;
		} else if ((!cmd.hasOption("a") || !cmd
				.hasOption(CMD_OPTIONS_DESCRIPTION_SHORT))
				&& cmd.hasOption("d")) {
			res = true;
		} else if ((!cmd.hasOption("a") || !cmd.hasOption("d"))
				&& cmd.hasOption(CMD_OPTIONS_DESCRIPTION_SHORT)) {
			res = true;
		}
		return res;
	}

	private static int createOneDebianPackage(DebianBashWrapperGenerator dbwg,
			CommandLine cmd, Tool tool, File outputdir) {
		int exitCode = 0;
		dbwg.setWrapperName(cmd.getOptionValue("d"));
		dbwg.setDescription(cmd.getOptionValue(CMD_OPTIONS_DESCRIPTION_SHORT));

		boolean generationOK = dbwg.generateWrapper(tool, null, outputdir);
		if (!generationOK) {
			log.error("Error generating Debian package");
			exitCode = 3;
		}
		return exitCode;
	}

	private static int createSeveralDebianPackages(
			DebianBashWrapperGenerator dbwg, Tool tool, File outputdir) {
		int exitCode = 0;
		for (Operation operation : tool.getOperations().getOperation()) {
			dbwg.setWrapperName(operation.getName());

			boolean generationOK = dbwg.generateWrapper(tool, operation,
					outputdir);
			if (!generationOK) {
				log.error("Error generating Debian package for operation \""
						+ operation.getName() + "\"");
				exitCode = 4;
			}
		}
		return exitCode;
	}

	/**
	 * Main method that parses the parameters, and depending on the parameters
	 * provided, generates a Debian package for each operation artifacts or a
	 * Debian package containing all operation artifacts
	 * 
	 * @param args
	 *            command-line provided arguments
	 */
	public static void main(String[] args) {
		DebianBashWrapperGenerator dbwg = new DebianBashWrapperGenerator();
		int exitCode = 0;
		try {
			ImmutablePair<CommandLine, Tool> pair = dbwg
					.processToolWrapperGenerationRequest(args);
			CommandLine cmd = null;
			Tool tool = null;

			if (pair != null) {
				cmd = pair.getLeft();
				tool = pair.getRight();

				File outputdirFile = cmd.hasOption("o") ? new File(
						cmd.getOptionValue("o")) : null;

				dbwg.setInputDir(cmd.getOptionValue("i"));
				dbwg.setMaintainerEmail(cmd.getOptionValue("e"));
				dbwg.setChangelog(cmd.getOptionValue("ch"));

				if (areNonRequiredArgumentsThatCanBeCombinedInvalid(cmd)) {
					log.error("Missing required option: "
							+ (cmd.hasOption("a") ? (cmd.hasOption("d") ? CMD_OPTIONS_DESCRIPTION_SHORT
									: "d")
									: "a") + "\n");
					dbwg.printUsage();
					exitCode = 3;
				} else {
					if (cmd.hasOption("a") && cmd.hasOption("d")
							&& cmd.hasOption(CMD_OPTIONS_DESCRIPTION_SHORT)) {

						// only one Debian package will be generated
						exitCode = createOneDebianPackage(dbwg, cmd, tool, outputdirFile);
					} else {

						// generate a Debian package for each operation
						exitCode = createSeveralDebianPackages(dbwg, tool, outputdirFile);
					}
				}
			}
		} catch (ErrorParsingCmdArgsException e) {
			log.error("[ERROR] " + e.getMessage());
			dbwg.printUsage();
			exitCode = 2;
		} catch (SpecParsingException e) {
			log.error("[ERROR] " + e.getMessage(), e);
			dbwg.printUsage();
			exitCode = 1;
		}
		System.exit(exitCode);
	}
}
