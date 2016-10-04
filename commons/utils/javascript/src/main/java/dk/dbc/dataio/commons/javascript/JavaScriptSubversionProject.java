/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.commons.javascript;

import dk.dbc.dataio.commons.svn.SvnConnector;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JavaScriptSubversionProject {
    /** Path delimiter for project URLs
     */
    public static final String URL_DELIMITER = "/";

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaScriptSubversionProject.class);
    private static final String JAVASCRIPT_FILENAME_EXTENSION = ".js";
    private static final String JAVASCRIPT_USE_MODULE_EXTENSION = ".use.js";
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    static final String TRUNK_PATH = "trunk";

    private final String subversionScmEndpoint;
    private final List<String> dependencies;

    /**
     * Class constructor
     *
     * @param subversionScmEndpoint base URL of subversion repository
     *
     * @throws NullPointerException if given null-valued subversionScmEndpoint argument
     * @throws IllegalArgumentException if given empty-valued subversionScmEndpoint argument
     */
    public JavaScriptSubversionProject(final String subversionScmEndpoint)
            throws NullPointerException, IllegalArgumentException {
        this.subversionScmEndpoint = removeTrailingDelimiter(InvariantUtil.checkNotNullNotEmptyOrThrow(subversionScmEndpoint, "subversionScmEndpoint"));
        this.dependencies = new ArrayList<>(Collections.singletonList("jscommon"));
        LOGGER.info("JavaScriptSubversionProject(): Using SCM endpoint {}", subversionScmEndpoint);
    }

    /**
     * Fetches information from source control management system for
     * all committed revisions for project identified by given name
     * @param projectName project name
     * @return list of revision information in descending revision order
     * @throws NullPointerException if given null-valued projectName
     * @throws IllegalArgumentException if given empty-valued projectName
     * @throws JavaScriptProjectException with error code:
     *          SCM_RESOURCE_NOT_FOUND - if given project name can not be found in the SCM system.
     *          SCM_ILLEGAL_PROJECT_NAME - if given project name contains {@code URL_DELIMITER}.
     *          SCM_INVALID_URL - if requested project URL is invalid.
     *          SCM_SERVER_ERROR - on general failure to communicate with the SCM system.
     */
    public List<RevisionInfo> fetchRevisions(String projectName)
            throws NullPointerException, IllegalArgumentException, JavaScriptProjectException {
        LOGGER.trace("fetchRevisions(\"{}\");", projectName);
        InvariantUtil.checkNotNullNotEmptyOrThrow(projectName, "projectName");
        final StopWatch stopWatch = new StopWatch();
        final String projectUrl = buildProjectUrl(projectName);
        final String errorMessage = "Unable to retrieve revisions from project '{}'";
        List<RevisionInfo> revisions;
        try {
            revisions = SvnConnector.listAvailableRevisions(projectUrl);
        } catch (SVNException e) {
            LOGGER.error(errorMessage, projectName, e);
            throw new JavaScriptProjectException(interpretSvnException(e), e);
        } catch (URISyntaxException e) {
            LOGGER.error(errorMessage, projectName, e);
            throw new JavaScriptProjectException(JavaScriptProjectError.SCM_INVALID_URL, e);
        } finally {
            LOGGER.debug("fetchRevisions() took {} milliseconds", stopWatch.getElapsedTime());
        }
        return revisions;
    }

    /**
     * Fetches paths of all files with a .js extension contained in specified
     * revision of project identified by given name
     * @param projectName project name
     * @param revision project revision
     * @return list of file names
     * @throws NullPointerException if given null-valued projectName
     * @throws IllegalArgumentException if given empty-valued projectName
     * @throws JavaScriptProjectException with error code:
     *          SCM_RESOURCE_NOT_FOUND - if given project name can not be found in the SCM system.
     *          SCM_ILLEGAL_PROJECT_NAME - if given project name contains {@code URL_DELIMITER}.
     *          SCM_INVALID_URL - if requested project URL is invalid.
     *          SCM_SERVER_ERROR - on general failure to communicate with the SCM system.
     */
    public List<String> fetchJavaScriptFileNames(String projectName, long revision)
            throws NullPointerException, IllegalArgumentException, JavaScriptProjectException {
        LOGGER.trace("fetchJavaScriptFileNames(\"{}\", {});", projectName, revision);
        InvariantUtil.checkNotNullNotEmptyOrThrow(projectName, "projectName");
        final StopWatch stopWatch = new StopWatch();
        final String projectUrl = buildProjectUrl(projectName);
        final String errorMessage = "Unable to retrieve javaScript file names from revision {} of project '{}'";
        final List<String> fileNames = new ArrayList<>();
        try {
            for (String path : SvnConnector.listAvailablePaths(projectUrl, revision)) {
                if (path.endsWith(JAVASCRIPT_FILENAME_EXTENSION)
                        && !path.endsWith(JAVASCRIPT_USE_MODULE_EXTENSION)) {
                    fileNames.add(path);
                }
            }
            Collections.sort(fileNames);
        } catch (SVNException e) {
            LOGGER.error(errorMessage, revision, projectName, e);
            throw new JavaScriptProjectException(interpretSvnException(e), e);
        } catch (URISyntaxException e) {
            LOGGER.error(errorMessage, revision, projectName, e);
            throw new JavaScriptProjectException(JavaScriptProjectError.SCM_INVALID_URL, e);
        } finally {
            LOGGER.debug("fetchJavaScriptFileNames() took {} milliseconds", stopWatch.getElapsedTime());
        }
        return fileNames;
    }

    /**
     * Fetches names of all potential invocation methods contained in specified
     * javaScript file in given revision of project identified by given name
     * @param projectName project name
     * @param revision project revision
     * @param javaScriptFileName name of script file
     * @return list of method names in alphabetical order
     * @throws NullPointerException if given null-valued projectName or javaScriptFileName
     * @throws IllegalArgumentException if given empty-valued projectName or javaScriptFileName
     * @throws JavaScriptProjectException with error code:
     *          SCM_RESOURCE_NOT_FOUND - if project resource can not be found in the SCM system.
     *          SCM_ILLEGAL_PROJECT_NAME - if given project name contains {@code URL_DELIMITER}.
     *          SCM_INVALID_URL - if requested project URL is invalid.
     *          SCM_SERVER_ERROR - on general failure to communicate with the SCM system.
     *          JAVASCRIPT_REFERENCE_ERROR - on failure to evaluate JavaScript with fake use functionality.
     *          JAVASCRIPT_EVAL_ERROR - on failure to evaluate JavaScript.
     */
    public List<String> fetchJavaScriptInvocationMethods(String projectName, long revision, String javaScriptFileName)
            throws NullPointerException, IllegalArgumentException, JavaScriptProjectException {
        LOGGER.trace("fetchJavaScriptInvocationMethods(\"{}\", {}, \"{}\");", projectName, revision, javaScriptFileName);
        InvariantUtil.checkNotNullNotEmptyOrThrow(projectName, "projectName");
        InvariantUtil.checkNotNullNotEmptyOrThrow(javaScriptFileName, "javaScriptFileName");
        final StopWatch stopWatch = new StopWatch();
        final String errorMessage = "Unable to retrieve method names from file '{}' in revision {} of project '{}'";
        final String projectUrl = buildProjectUrl(projectName);
        final List<String> methodNames;
        Path exportFolder = null;
        try {
            exportFolder = createTmpFolder(getClass().getName());
            final String trimmedJavaScriptFileName = leftTrimFileNameByRemovingDelimiterAndTrunkPath(javaScriptFileName);
            final String fileUrl = removeTrailingDelimiter(join(URL_DELIMITER, projectUrl, trimmedJavaScriptFileName));
            SvnConnector.export(fileUrl, revision, exportFolder);
            methodNames = getJavaScriptFunctionsSortedByPathNameFromFile(exportFolder, trimmedJavaScriptFileName);
        } catch (SVNException e) {
            LOGGER.error(errorMessage, javaScriptFileName, revision, projectName, e);
            throw new JavaScriptProjectException(interpretSvnException(e), e);
        } catch (URISyntaxException e) {
            LOGGER.error(errorMessage, javaScriptFileName, revision, projectName, e);
            throw new JavaScriptProjectException(JavaScriptProjectError.SCM_INVALID_URL, e);
        } catch (Exception e) {
            LOGGER.error(errorMessage, javaScriptFileName, revision, projectName, e);
            throw new JavaScriptProjectException(JavaScriptProjectError.JAVASCRIPT_EVAL_ERROR, e);
        }  finally {
            deleteFolder(exportFolder);
            LOGGER.debug("fetchJavaScriptInvocationMethods() took {} milliseconds", stopWatch.getElapsedTime());
        }
        return methodNames;
    }

    /**
     * Fetches script content of specified javaScript file (and any of its dependencies)
     * in given revision of project identified by name
     * @param projectName project name
     * @param revision project revision
     * @param javaScriptFileName name of script file
     * @param javaScriptFunction name of invocation function in script file
     * @return {@link JavaScriptProject} instance
     * @throws NullPointerException if given null-valued projectName, javaScriptFileName or javaScriptFunction
     * @throws IllegalArgumentException if given empty-valued projectName, javaScriptFileName or javaScriptFunction
     * @throws JavaScriptProjectException with error code:
     *          SCM_RESOURCE_NOT_FOUND - if project resource can not be found in the SCM system.
     *          SCM_ILLEGAL_PROJECT_NAME - if given project name contains {@code URL_DELIMITER}.
     *          SCM_INVALID_URL - if requested project URL is invalid.
     *          SCM_SERVER_ERROR - on general failure to communicate with the SCM system.
     *          JAVASCRIPT_READ_ERROR - on failure to read JavaScript exported from the SCM system.
     */
    public JavaScriptProject fetchRequiredJavaScript(String projectName, long revision, String javaScriptFileName, String javaScriptFunction)
            throws NullPointerException, IllegalArgumentException, JavaScriptProjectException {
        LOGGER.trace("fetchRequiredJavaScript(\"{}\", {}, \"{}\", \"{}\");", projectName, revision, javaScriptFileName, javaScriptFunction);
        InvariantUtil.checkNotNullNotEmptyOrThrow(projectName, "projectName");
        InvariantUtil.checkNotNullNotEmptyOrThrow(javaScriptFileName, "javaScriptFileName");
        InvariantUtil.checkNotNullNotEmptyOrThrow(javaScriptFunction, "javaScriptFunction");
        final StopWatch stopWatch = new StopWatch();
        final String errorMessage = "Unable to retrieve required javaScript for function '{}' in script in file '{}' in revision {} of project '{}'";
        final String projectUrl = buildProjectUrl(projectName);
        final List<JavaScript> javaScripts = new ArrayList<JavaScript>();
        String requireCache=null;
        Path exportFolder = null;
        try {
            exportFolder = createTmpFolder(getClass().getName());
            final Path projectPath = Paths.get(exportFolder.toString(), projectName);
            SvnConnector.export(projectUrl, revision, projectPath);

            final Path mainJsPath = Paths.get(projectPath.toString(), leftTrimFileNameByRemovingDelimiterAndTrunkPath(javaScriptFileName));
            final String mainJsContent = new String(Files.readAllBytes(mainJsPath), CHARSET);
            final JavaScript mainJs = new JavaScript(StringUtil.base64encode(mainJsContent, CHARSET), "");
            javaScripts.add(mainJs);

            for (String dependency : dependencies) {
                SvnConnector.export(buildProjectUrl(dependency), revision, Paths.get(exportFolder.toString(), dependency));
            }

            JavascriptUtil.getAllDependentJavascriptsResult result=JavascriptUtil.getAllDependentJavascripts(exportFolder,mainJsPath);
            for (SpecializedFileSchemeHandler.JS js : result.javaScripts) {
                javaScripts.add(new JavaScript(StringUtil.base64encode(js.javascript, CHARSET), js.modulename));
            }

            if( result.requireCache != null ) {
                requireCache = StringUtil.base64encode(result.requireCache, CHARSET);
            }
        } catch (SVNException e) {
            LOGGER.error(errorMessage, javaScriptFunction, javaScriptFileName, revision, projectName, e);
            throw new JavaScriptProjectException(interpretSvnException(e), e);
        } catch (URISyntaxException e) {
            LOGGER.error(errorMessage, javaScriptFunction, javaScriptFileName, revision, projectName, e);
            throw new JavaScriptProjectException(JavaScriptProjectError.SCM_INVALID_URL, e);
        } catch (Exception e) {
            LOGGER.error(errorMessage, javaScriptFunction, javaScriptFileName, revision, projectName, e);
            throw new JavaScriptProjectException(JavaScriptProjectError.JAVASCRIPT_READ_ERROR, e);
        } finally {
            deleteFolder(exportFolder);
            LOGGER.debug("fetchRequiredJavaScript() took {} milliseconds", stopWatch.getElapsedTime());
        }
        return new JavaScriptProject(javaScripts, requireCache);
    }

    static Reader getReaderForFile(Path file) throws FileNotFoundException, UnsupportedEncodingException {
        return new InputStreamReader(new FileInputStream(file.toFile()), StandardCharsets.UTF_8);
    }

    private String buildProjectUrl(final String projectName) throws JavaScriptProjectException {
        // first verify legal project name
        if (projectName.contains(URL_DELIMITER)) {
            final String message = String.format("Project name contains path elements: %s", projectName);
            LOGGER.error(message);
            throw new JavaScriptProjectException(JavaScriptProjectError.SCM_ILLEGAL_PROJECT_NAME, message);
        }

        URI projectUrl;
        try {
            projectUrl = new URI(join(URL_DELIMITER, subversionScmEndpoint, projectName, TRUNK_PATH));
        } catch (URISyntaxException e) {
            LOGGER.error("Unable to build project URL", e);
            throw new JavaScriptProjectException(JavaScriptProjectError.SCM_INVALID_URL, e);
        }
        return removeTrailingDelimiter(projectUrl.toString());
    }

    private static String join(final String delimiter, String... elements) {
        final StringBuilder stringbuilder = new StringBuilder();
        for (String s : elements) {
            stringbuilder.append(s).append(delimiter);
        }
        return stringbuilder.toString();
    }

    private static String removeTrailingDelimiter(final String in) {
        return in.replaceFirst(String.format("%s$", URL_DELIMITER), "");
    }

    private static Path createTmpFolder(final String prefix) {
        Path folder = null;
        try {
            folder = Files.createTempDirectory(Paths.get(System.getProperty("java.io.tmpdir")), prefix);
            LOGGER.trace("Created temporary folder {}", folder);
        } catch (IOException e) {
            LOGGER.error("Error creating temporary folder", e);
        }
        return folder;
    }

    private static void deleteFolder(Path folder) {
        if (folder != null) {
            try {
                Files.walkFileTree(folder, new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        LOGGER.trace("Deleting {}", file);
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
                        LOGGER.trace("Deletion of file {} failed", file, e);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                        LOGGER.trace("Deleting {}", dir);
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                LOGGER.error("Error deleting folder {}", folder);
            }
        }
    }

    private static String leftTrimFileNameByRemovingDelimiterAndTrunkPath(String javaScriptFileName) {
        String trimmedJavaScriptFileName = javaScriptFileName.replaceFirst(String.format("^%s", URL_DELIMITER),"");
        trimmedJavaScriptFileName = trimmedJavaScriptFileName.replaceFirst(String.format("^%s%s", TRUNK_PATH, URL_DELIMITER),"");
        return trimmedJavaScriptFileName;
    }

    private static List<String> getJavaScriptFunctionsSortedByPathNameFromFile(Path exportFolder, String javaScriptFileNameWithPath) {
        final String javaScriptFileName = new File(javaScriptFileNameWithPath).getName();
        final Path exportedFile = Paths.get(exportFolder.toString(), javaScriptFileName);
        try {
            final List<String> functionNames = new ArrayList<String>(
                    JavascriptUtil.getAllToplevelFunctionsInJavascriptWithFakeUseFunction(
                            getReaderForFile(exportedFile), javaScriptFileName));
            Collections.sort(functionNames);
            return functionNames;
        } catch (Throwable e) {
            LOGGER.error("Caught unexpected exception trying to read javaScript file '{}'", exportedFile, e);
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("PMD.CollapsibleIfStatements") // The code is more readable without collapsed if-statements
    private static JavaScriptProjectError interpretSvnException(SVNException e) {
        final String message = e.getMessage();
        if (message != null) {
            if (message.contains("E160013") || message.contains("E180001") || message.contains("404 Not Found")) {
                return JavaScriptProjectError.SCM_RESOURCE_NOT_FOUND;
            }
        }
        return JavaScriptProjectError.SCM_SERVER_ERROR;
    }
}
