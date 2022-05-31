package dk.dbc.dataio.commons.javascript;

import dk.dbc.dataio.commons.svn.SvnConnector;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaScriptSubversionProject {
    /* Path delimiter for project URLs */
    static final String URL_DELIMITER = "/";
    static final String TRUNK_PATH = "trunk";

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaScriptSubversionProject.class);
    private static final String JAVASCRIPT_FILENAME_EXTENSION = ".js";
    private static final String JAVASCRIPT_USE_MODULE_EXTENSION = ".use.js";

    private final String subversionScmEndpoint;
    private final List<String> dependencies;

    /**
     * Class constructor
     *
     * @param subversionScmEndpoint base URL of subversion repository
     * @throws NullPointerException     if given null-valued subversionScmEndpoint argument
     * @throws IllegalArgumentException if given empty-valued subversionScmEndpoint argument
     */
    public JavaScriptSubversionProject(final String subversionScmEndpoint)
            throws NullPointerException, IllegalArgumentException {
        this.subversionScmEndpoint = removeTrailingDelimiter(InvariantUtil.checkNotNullNotEmptyOrThrow(subversionScmEndpoint, "subversionScmEndpoint"));
        this.dependencies = new ArrayList<>(Arrays.asList("jscommon", "datawell-convert"));
        LOGGER.info("JavaScriptSubversionProject(): Using SCM endpoint {}", subversionScmEndpoint);
    }

    /**
     * Fetches information from source control management system for
     * all committed revisions for given project path
     *
     * @param projectPath project path
     * @return list of revision information in descending revision order
     * @throws NullPointerException       if given null-valued projectName
     * @throws IllegalArgumentException   if given empty-valued projectName
     * @throws JavaScriptProjectException with error code:
     *                                    SCM_RESOURCE_NOT_FOUND - if given project path can not be found in the SCM system.
     *                                    SCM_INVALID_URL - if requested project URL is invalid.
     *                                    SCM_SERVER_ERROR - on general failure to communicate with the SCM system.
     */
    public List<RevisionInfo> fetchRevisions(String projectPath)
            throws NullPointerException, IllegalArgumentException, JavaScriptProjectException {
        LOGGER.trace("fetchRevisions(\"{}\");", projectPath);
        InvariantUtil.checkNotNullNotEmptyOrThrow(projectPath, "projectPath");
        final StopWatch stopWatch = new StopWatch();
        final String projectUrl = buildProjectUrl(projectPath);
        final String errorMessage = "Unable to retrieve revisions from project '{}'";
        try {
            return SvnConnector.listAvailableRevisions(projectUrl);
        } catch (SVNException e) {
            LOGGER.error(errorMessage, projectPath, e);
            throw new JavaScriptProjectException(interpretSvnException(e), e);
        } catch (URISyntaxException e) {
            LOGGER.error(errorMessage, projectPath, e);
            throw new JavaScriptProjectException(JavaScriptProjectError.SCM_INVALID_URL, e);
        } finally {
            LOGGER.debug("fetchRevisions() took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Fetches paths of all files with a .js extension contained in specified
     * revision of given project path
     *
     * @param projectPath project path
     * @param revision    project revision
     * @return list of file names
     * @throws NullPointerException       if given null-valued projectPath
     * @throws IllegalArgumentException   if given empty-valued projectPath
     * @throws JavaScriptProjectException with error code:
     *                                    SCM_RESOURCE_NOT_FOUND - if given project name can not be found in the SCM system.
     *                                    SCM_INVALID_URL - if requested project URL is invalid.
     *                                    SCM_SERVER_ERROR - on general failure to communicate with the SCM system.
     */
    public List<String> fetchJavaScriptFileNames(String projectPath, long revision)
            throws NullPointerException, IllegalArgumentException, JavaScriptProjectException {
        LOGGER.trace("fetchJavaScriptFileNames(\"{}\", {});", projectPath, revision);
        InvariantUtil.checkNotNullNotEmptyOrThrow(projectPath, "projectPath");
        final StopWatch stopWatch = new StopWatch();
        final String projectUrl = buildProjectUrl(projectPath);
        final String errorMessage = "Unable to retrieve javaScript file names from revision {} of project '{}'";
        try {
            return SvnConnector.listAvailablePaths(projectUrl, revision).stream()
                    .filter(path -> path.endsWith(JAVASCRIPT_FILENAME_EXTENSION) && !path.endsWith(JAVASCRIPT_USE_MODULE_EXTENSION))
                    .sorted(String::compareTo)
                    .collect(Collectors.toList());
        } catch (SVNException e) {
            LOGGER.error(errorMessage, revision, projectPath, e);
            throw new JavaScriptProjectException(interpretSvnException(e), e);
        } catch (URISyntaxException e) {
            LOGGER.error(errorMessage, revision, projectPath, e);
            throw new JavaScriptProjectException(JavaScriptProjectError.SCM_INVALID_URL, e);
        } finally {
            LOGGER.debug("fetchJavaScriptFileNames() took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Fetches names of all potential invocation methods contained in specified
     * javaScript file in given revision of given project path
     *
     * @param projectPath        project path
     * @param revision           project revision
     * @param javaScriptFileName name of script file
     * @return list of method names in alphabetical order
     * @throws NullPointerException       if given null-valued projectPath or javaScriptFileName
     * @throws IllegalArgumentException   if given empty-valued projectPath or javaScriptFileName
     * @throws JavaScriptProjectException with error code:
     *                                    SCM_RESOURCE_NOT_FOUND - if project resource can not be found in the SCM system.
     *                                    SCM_INVALID_URL - if requested project URL is invalid.
     *                                    SCM_SERVER_ERROR - on general failure to communicate with the SCM system.
     *                                    JAVASCRIPT_REFERENCE_ERROR - on failure to evaluate JavaScript with fake use functionality.
     *                                    JAVASCRIPT_EVAL_ERROR - on failure to evaluate JavaScript.
     */
    public List<String> fetchJavaScriptInvocationMethods(String projectPath, long revision, String javaScriptFileName)
            throws NullPointerException, IllegalArgumentException, JavaScriptProjectException {
        LOGGER.trace("fetchJavaScriptInvocationMethods(\"{}\", {}, \"{}\");", projectPath, revision, javaScriptFileName);
        InvariantUtil.checkNotNullNotEmptyOrThrow(projectPath, "projectPath");
        InvariantUtil.checkNotNullNotEmptyOrThrow(javaScriptFileName, "javaScriptFileName");
        final StopWatch stopWatch = new StopWatch();
        final String errorMessage = "Unable to retrieve method names from file '{}' in revision {} of project '{}'";
        final String projectUrl = buildProjectUrl(projectPath);
        Path exportFolder = null;
        try {
            exportFolder = createTmpDirectory(getClass().getName());
            final String trimmedJavaScriptFileName = leftTrimFileNameByRemovingDelimiterAndTrunkPath(javaScriptFileName);
            final String fileUrl = removeTrailingDelimiter(urlJoin(projectUrl, trimmedJavaScriptFileName));
            SvnConnector.export(fileUrl, revision, exportFolder);
            return getJavaScriptFunctionsSortedByPathNameFromFile(exportFolder, trimmedJavaScriptFileName);
        } catch (SVNException e) {
            LOGGER.error(errorMessage, javaScriptFileName, revision, projectPath, e);
            throw new JavaScriptProjectException(interpretSvnException(e), e);
        } catch (URISyntaxException e) {
            LOGGER.error(errorMessage, javaScriptFileName, revision, projectPath, e);
            throw new JavaScriptProjectException(JavaScriptProjectError.SCM_INVALID_URL, e);
        } catch (Exception e) {
            LOGGER.error(errorMessage, javaScriptFileName, revision, projectPath, e);
            throw new JavaScriptProjectException(JavaScriptProjectError.JAVASCRIPT_EVAL_ERROR, e);
        } finally {
            deleteFolder(exportFolder);
            LOGGER.debug("fetchJavaScriptInvocationMethods() took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Fetches script content of specified javaScript file (and any of its dependencies)
     * in given revision of given project path
     *
     * @param projectPath        project path
     * @param revision           project revision
     * @param javaScriptFileName name of script file
     * @param javaScriptFunction name of invocation function in script file
     * @return {@link JavaScriptProject} instance
     * @throws NullPointerException       if given null-valued projectPath, javaScriptFileName or javaScriptFunction
     * @throws IllegalArgumentException   if given empty-valued projectPath, javaScriptFileName or javaScriptFunction
     * @throws JavaScriptProjectException with error code:
     *                                    SCM_RESOURCE_NOT_FOUND - if project resource can not be found in the SCM system.
     *                                    SCM_INVALID_URL - if requested project URL is invalid.
     *                                    SCM_SERVER_ERROR - on general failure to communicate with the SCM system.
     *                                    JAVASCRIPT_READ_ERROR - on failure to read JavaScript exported from the SCM system.
     */
    public JavaScriptProject fetchRequiredJavaScript(String projectPath, long revision, String javaScriptFileName, String javaScriptFunction)
            throws NullPointerException, IllegalArgumentException, JavaScriptProjectException {
        LOGGER.trace("fetchRequiredJavaScript(\"{}\", {}, \"{}\", \"{}\");", projectPath, revision, javaScriptFileName, javaScriptFunction);
        InvariantUtil.checkNotNullNotEmptyOrThrow(projectPath, "projectPath");
        InvariantUtil.checkNotNullNotEmptyOrThrow(javaScriptFileName, "javaScriptFileName");
        InvariantUtil.checkNotNullNotEmptyOrThrow(javaScriptFunction, "javaScriptFunction");
        final StopWatch stopWatch = new StopWatch();
        final String errorMessage = "Unable to retrieve required javaScript for function '{}' in script in file '{}' in revision {} of project '{}'";
        final String projectUrl = buildProjectUrl(projectPath);
        Path tmpDir = null;
        try {
            tmpDir = createTmpDirectory(getClass().getName());

            // Since the javascript directories are sorted (and therefore also added to search path) alphabetically
            // we add a sort prefix a_, b_, c_ etc.
            int sort_prefix = 96;
            final Path exportDir = Paths.get(tmpDir.toString(), (char) ++sort_prefix + "_" + projectPath);
            SvnConnector.export(projectUrl, revision, exportDir);

            for (String dependency : dependencies) {
                final Path dependencyPath = Paths.get(tmpDir.toString(), (char) ++sort_prefix + "_" + dependency);
                if (!Files.exists(dependencyPath)) {
                    SvnConnector.export(buildProjectUrl(dependency), revision, dependencyPath);
                }
            }

            final Path scriptPath = Paths.get(exportDir.toString(), leftTrimFileNameByRemovingDelimiterAndTrunkPath(javaScriptFileName));
            return JavaScriptProject.of(scriptPath, tmpDir);
        } catch (SVNException e) {
            LOGGER.error(errorMessage, javaScriptFunction, javaScriptFileName, revision, projectPath, e);
            throw new JavaScriptProjectException(interpretSvnException(e), e);
        } catch (URISyntaxException e) {
            LOGGER.error(errorMessage, javaScriptFunction, javaScriptFileName, revision, projectPath, e);
            throw new JavaScriptProjectException(JavaScriptProjectError.SCM_INVALID_URL, e);
        } catch (Exception e) {
            LOGGER.error(errorMessage, javaScriptFunction, javaScriptFileName, revision, projectPath, e);
            throw new JavaScriptProjectException(JavaScriptProjectError.JAVASCRIPT_READ_ERROR, e);
        } finally {
            deleteFolder(tmpDir);
            LOGGER.debug("fetchRequiredJavaScript() took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    private static Reader getReaderForFile(Path file) throws FileNotFoundException, UnsupportedEncodingException {
        return new InputStreamReader(new FileInputStream(file.toFile()), StandardCharsets.UTF_8);
    }

    private String buildProjectUrl(final String projectPath) throws JavaScriptProjectException {
        URI projectUrl;
        try {
            projectUrl = new URI(urlJoin(subversionScmEndpoint, projectPath));
            final SVNRepository repository = SvnConnector.getRepository(projectUrl.toString());
            if (SvnConnector.dirExists(repository, TRUNK_PATH)) {
                projectUrl = new URI(urlJoin(subversionScmEndpoint, projectPath, TRUNK_PATH));
            }
        } catch (URISyntaxException | SVNException e) {
            LOGGER.error("Unable to build project URL", e);
            throw new JavaScriptProjectException(JavaScriptProjectError.SCM_INVALID_URL, e);
        }
        return removeTrailingDelimiter(projectUrl.toString());
    }

    private static String urlJoin(String... elements) {
        return Stream.of(elements)
                .collect(Collectors.joining(URL_DELIMITER));
    }

    private static String removeTrailingDelimiter(final String in) {
        return in.replaceFirst(String.format("%s$", URL_DELIMITER), "");
    }

    private static Path createTmpDirectory(final String prefix) {
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
        String trimmedJavaScriptFileName = javaScriptFileName.replaceFirst(String.format("^%s", URL_DELIMITER), "");
        trimmedJavaScriptFileName = trimmedJavaScriptFileName.replaceFirst(String.format("^%s%s", TRUNK_PATH, URL_DELIMITER), "");
        return trimmedJavaScriptFileName;
    }

    private static List<String> getJavaScriptFunctionsSortedByPathNameFromFile(Path exportFolder, String javaScriptFileNameWithPath) {
        final String javaScriptFileName = new File(javaScriptFileNameWithPath).getName();
        final Path exportedFile = Paths.get(exportFolder.toString(), javaScriptFileName);
        try {
            final Reader exportedFileReader = getReaderForFile(exportedFile);
            return JavascriptUtil.getAllToplevelFunctionsInJavascriptWithFakeUseFunction(exportedFileReader, javaScriptFileName).stream()
                    .sorted(String::compareTo)
                    .collect(Collectors.toList());
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
