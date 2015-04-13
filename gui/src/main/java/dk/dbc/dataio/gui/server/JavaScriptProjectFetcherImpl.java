package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.javascript.JavascriptUtil;
import dk.dbc.dataio.commons.javascript.SpecializedFileSchemeHandler;
import dk.dbc.dataio.commons.svn.SvnConnector;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherError;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherException;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcher;
import org.apache.commons.codec.binary.Base64;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNException;

import java.io.File;
import java.io.IOException;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JavaScriptProjectFetcherImpl implements JavaScriptProjectFetcher {
    /** Path delimiter for project URLs
     */
    public static final String URL_DELIMITER = "/";

    static final String TRUNK_PATH = "trunk";
    private static final String JAVASCRIPT_FILENAME_EXTENSION = ".js";
    private static final String JAVASCRIPT_USE_MODULE_EXTENSION = ".use.js";
    private static final Logger log = LoggerFactory.getLogger(JavaScriptProjectFetcherImpl.class);
    private static final Charset CHARSET = StandardCharsets.UTF_8;

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
    public JavaScriptProjectFetcherImpl(final String subversionScmEndpoint)
            throws NullPointerException, IllegalArgumentException {
        this.subversionScmEndpoint = removeTrailingDelimiter(InvariantUtil.checkNotNullNotEmptyOrThrow(subversionScmEndpoint, "subversionScmEndpoint"));
        this.dependencies = new ArrayList<String>(Arrays.asList("jscommon"));
        log.info("Using SCM endpoint {}", subversionScmEndpoint);
    }

    /**
     * {@inheritDoc}
     * @throws NullPointerException if given null-valued projectName
     * @throws IllegalArgumentException if given empty-valued projectName
     * @throws JavaScriptProjectFetcherException with error code:
     *          SCM_RESOURCE_NOT_FOUND - if given project name can not be found in the SCM system.
     *          SCM_ILLEGAL_PROJECT_NAME - if given project name contains {@code URL_DELIMITER}.
     *          SCM_INVALID_URL - if requested project URL is invalid.
     *          SCM_SERVER_ERROR - on general failure to communicate with the SCM system.
     */
    @Override
    public List<RevisionInfo> fetchRevisions(String projectName)
            throws NullPointerException, IllegalArgumentException, JavaScriptProjectFetcherException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(projectName, "projectName");
        final String projectUrl = buildProjectUrl(projectName);
        final String errorMessage = "Unable to retrieve revisions from project '{}'";
        List<RevisionInfo> revisions;
        try {
            revisions = SvnConnector.listAvailableRevisions(projectUrl);
        } catch (SVNException e) {
            log.error(errorMessage, projectName, e);
            throw new JavaScriptProjectFetcherException(interpretSvnException(e), e);
        } catch (URISyntaxException e) {
            log.error(errorMessage, projectName, e);
            throw new JavaScriptProjectFetcherException(JavaScriptProjectFetcherError.SCM_INVALID_URL, e);
        }
        return revisions;
    }

    /**
     * {@inheritDoc}
     * @throws NullPointerException if given null-valued projectName
     * @throws IllegalArgumentException if given empty-valued projectName
     * @throws JavaScriptProjectFetcherException with error code:
     *          SCM_RESOURCE_NOT_FOUND - if given project name can not be found in the SCM system.
     *          SCM_ILLEGAL_PROJECT_NAME - if given project name contains {@code URL_DELIMITER}.
     *          SCM_INVALID_URL - if requested project URL is invalid.
     *          SCM_SERVER_ERROR - on general failure to communicate with the SCM system.
     */
    @Override
    public List<String> fetchJavaScriptFileNames(String projectName, long revision) throws NullPointerException, IllegalArgumentException, JavaScriptProjectFetcherException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(projectName, "projectName");
        final String projectUrl = buildProjectUrl(projectName);
        final String errorMessage = "Unable to retrieve javaScript file names from revision {} of project '{}'";
        final List<String> fileNames = new ArrayList<String>();
        try {
            for (String path : SvnConnector.listAvailablePaths(projectUrl, revision)) {
                if (path.endsWith(JAVASCRIPT_FILENAME_EXTENSION)
                        && !path.endsWith(JAVASCRIPT_USE_MODULE_EXTENSION)) {
                    fileNames.add(path);
                }
            }
            Collections.sort(fileNames);
        } catch (SVNException e) {
            log.error(errorMessage, revision, projectName, e);
            throw new JavaScriptProjectFetcherException(interpretSvnException(e), e);
        } catch (URISyntaxException e) {
            log.error(errorMessage, revision, projectName, e);
            throw new JavaScriptProjectFetcherException(JavaScriptProjectFetcherError.SCM_INVALID_URL, e);
        }
        return fileNames;
    }

    /**
     * {@inheritDoc}
     * @throws NullPointerException if given null-valued projectName or javaScriptFileName
     * @throws IllegalArgumentException if given empty-valued projectName or javaScriptFileName
     * @throws JavaScriptProjectFetcherException with error code:
     *          SCM_RESOURCE_NOT_FOUND - if project resource can not be found in the SCM system.
     *          SCM_ILLEGAL_PROJECT_NAME - if given project name contains {@code URL_DELIMITER}.
     *          SCM_INVALID_URL - if requested project URL is invalid.
     *          SCM_SERVER_ERROR - on general failure to communicate with the SCM system.
     *          JAVASCRIPT_REFERENCE_ERROR - on failure to evaluate JavaScript with fake use functionality.
     *          JAVASCRIPT_EVAL_ERROR - on failure to evaluate JavaScript.
     */
    @Override
    public List<String> fetchJavaScriptInvocationMethods(String projectName, long revision, String javaScriptFileName)
            throws NullPointerException, IllegalArgumentException, JavaScriptProjectFetcherException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(projectName, "projectName");
        InvariantUtil.checkNotNullNotEmptyOrThrow(javaScriptFileName, "javaScriptFileName");
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
        } catch (EcmaError e) {
            log.error(errorMessage, javaScriptFileName, revision, projectName, e);
            throw new JavaScriptProjectFetcherException(JavaScriptProjectFetcherError.JAVASCRIPT_REFERENCE_ERROR, e);
        } catch (EvaluatorException e) {
            log.error(errorMessage, javaScriptFileName, revision, projectName, e);
            throw new JavaScriptProjectFetcherException(JavaScriptProjectFetcherError.JAVASCRIPT_EVAL_ERROR, e);
        } catch (SVNException e) {
            log.error(errorMessage, javaScriptFileName, revision, projectName, e);
            throw new JavaScriptProjectFetcherException(interpretSvnException(e), e);
        } catch (URISyntaxException e) {
            log.error(errorMessage, javaScriptFileName, revision, projectName, e);
            throw new JavaScriptProjectFetcherException(JavaScriptProjectFetcherError.SCM_INVALID_URL, e);
        }  finally {
           deleteFolder(exportFolder);
        }
        return methodNames;
    }

    /**
     * {@inheritDoc}
     * @throws NullPointerException if given null-valued projectName, javaScriptFileName or javaScriptFunction
     * @throws IllegalArgumentException if given empty-valued projectName, javaScriptFileName or javaScriptFunction
     * @throws JavaScriptProjectFetcherException with error code:
     *          SCM_RESOURCE_NOT_FOUND - if project resource can not be found in the SCM system.
     *          SCM_ILLEGAL_PROJECT_NAME - if given project name contains {@code URL_DELIMITER}.
     *          SCM_INVALID_URL - if requested project URL is invalid.
     *          SCM_SERVER_ERROR - on general failure to communicate with the SCM system.
     *          JAVASCRIPT_READ_ERROR - on failure to read JavaScript exported from the SCM system.
     */
    @Override
    public fetchRequiredJavaScriptResult fetchRequiredJavaScript(String projectName, long revision, String javaScriptFileName, String javaScriptFunction)
            throws NullPointerException, IllegalArgumentException, JavaScriptProjectFetcherException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(projectName, "projectName");
        InvariantUtil.checkNotNullNotEmptyOrThrow(javaScriptFileName, "javaScriptFileName");
        InvariantUtil.checkNotNullNotEmptyOrThrow(javaScriptFunction, "javaScriptFunction");
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
            final JavaScript mainJs = new JavaScript(Base64.encodeBase64String(mainJsContent.getBytes(CHARSET)), "");
            javaScripts.add(mainJs);

            for (String dependency : dependencies) {
                SvnConnector.export(buildProjectUrl(dependency), revision, Paths.get(exportFolder.toString(), dependency));
            }

            JavascriptUtil.getAllDependatJavascriptsResult result=JavascriptUtil.getAllDependentJavascripts(exportFolder,mainJsPath);
            for (SpecializedFileSchemeHandler.JS js : result.javaScripts) {
                javaScripts.add(new JavaScript(Base64.encodeBase64String(js.javascript.getBytes(CHARSET)), js.modulename));
            }

            if( result.requireCache != null ) {
                requireCache = Base64.encodeBase64String( result.requireCache.getBytes(CHARSET) );
            }
        } catch (SVNException e) {
            log.error(errorMessage, javaScriptFunction, javaScriptFileName, revision, projectName, e);
            throw new JavaScriptProjectFetcherException(interpretSvnException(e), e);
        } catch (URISyntaxException e) {
            log.error(errorMessage, javaScriptFunction, javaScriptFileName, revision, projectName, e);
            throw new JavaScriptProjectFetcherException(JavaScriptProjectFetcherError.SCM_INVALID_URL, e);
        } catch (IOException e) {
            log.error(errorMessage, javaScriptFunction, javaScriptFileName, revision, projectName, e);
            throw new JavaScriptProjectFetcherException(JavaScriptProjectFetcherError.JAVASCRIPT_READ_ERROR, e);
        } finally {
            deleteFolder(exportFolder);
        }
        return new fetchRequiredJavaScriptResult(javaScripts, requireCache );
    }

    private String buildProjectUrl(final String projectName) throws JavaScriptProjectFetcherException {
        // first verify legal project name
        if (projectName.contains(URL_DELIMITER)) {
            final String message = String.format("Project name contains path elements: %s", projectName);
            log.error(message);
            throw new JavaScriptProjectFetcherException(JavaScriptProjectFetcherError.SCM_ILLEGAL_PROJECT_NAME, message);
        }

        URI projectUrl;
        try {
            projectUrl = new URI(join(URL_DELIMITER, subversionScmEndpoint, projectName, TRUNK_PATH));
        } catch (URISyntaxException e) {
            log.error("Unable to build project URL", e);
            throw new JavaScriptProjectFetcherException(JavaScriptProjectFetcherError.SCM_INVALID_URL, e);
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
            log.trace("Created temporary folder {}", folder);
        } catch (IOException e) {
            log.error("Error creating temporary folder", e);
        }
        return folder;
    }

    private static void deleteFolder(Path folder) {
        if (folder != null) {
            try {
                Files.walkFileTree(folder, new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        log.trace("Deleting {}", file);
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
                        log.trace("Deletion of file {} failed", file, e);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                        log.trace("Deleting {}", dir);
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                log.error("Error deleting folder {}", folder);
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
                            FileUtil.getReaderForFile(exportedFile), javaScriptFileName));
            Collections.sort(functionNames);
            return functionNames;
        } catch (IOException e) {
            log.error("Caught unexpected exception trying to read javaScript file '{}'", exportedFile, e);
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("PMD.CollapsibleIfStatements") // The code is more readable without collapsed if-statements
    private static JavaScriptProjectFetcherError interpretSvnException(SVNException e) {
        final String message = e.getMessage();
        if (message != null) {
            if (message.contains("E160013") || message.contains("E180001") || message.contains("404 Not Found")) {
                return JavaScriptProjectFetcherError.SCM_RESOURCE_NOT_FOUND;
            }
        }
        return JavaScriptProjectFetcherError.SCM_SERVER_ERROR;
    }
}
