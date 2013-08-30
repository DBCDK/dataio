package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.svn.SvnConnector;
import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.engine.JavaScript;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherException;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JavaScriptProjectFetcherImpl implements JavaScriptProjectFetcher {
    /** Path delimiter for project URLs
     */
    public static final String URL_DELIMITER = "/";

    static final String TRUNK_PATH = "trunk";
    private static final String JAVASCRIPT_FILENAME_EXTENSION = ".js";
    private static final Logger log = LoggerFactory.getLogger(JavaScriptProjectFetcherImpl.class);

    private final String subversionScmEndpoint;

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
    }

    /**
     * {@inheritDoc}
     * @throws NullPointerException if given null-valued projectName
     * @throws IllegalArgumentException if given empty-valued projectName
     * @throws JavaScriptProjectFetcherException if given project name contains {@code URL_DELIMITER}
     * or on failure communicating with SCM system
     */
    @Override
    public List<RevisionInfo> fetchRevisions(String projectName)
            throws NullPointerException, IllegalArgumentException, JavaScriptProjectFetcherException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(projectName, "projectName");
        final String projectUrl = buildProjectUrl(projectName);
        List<RevisionInfo> revisions;
        try {
            revisions = SvnConnector.listAvailableRevisions(projectUrl);
        } catch (SVNException e) {
            log.error("Unable to retrieve revisions", e);
            throw new JavaScriptProjectFetcherException(e);
        } catch (URISyntaxException e) {
            log.error("Unable to retrieve revisions", e);
            throw new JavaScriptProjectFetcherException(e);
        }
        return revisions;
    }

    /**
     * {@inheritDoc}
     * @throws NullPointerException if given null-valued projectName
     * @throws IllegalArgumentException if given empty-valued projectName
     * @throws JavaScriptProjectFetcherException if given project name contains {@code URL_DELIMITER}
     * or on failure communicating with SCM system
     */
    @Override
    public List<String> fetchJavaScriptFileNames(String projectName, long revision) throws NullPointerException, IllegalArgumentException, JavaScriptProjectFetcherException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(projectName, "projectName");
        final String projectUrl = buildProjectUrl(projectName);
        final List<String> fileNames = new ArrayList<String>();
        try {
            for (String path : SvnConnector.listAvailablePaths(projectUrl, revision)) {
                if (path.endsWith(JAVASCRIPT_FILENAME_EXTENSION)) {
                    fileNames.add(path);
                }
            }
        } catch (SVNException e) {
            log.error("Unable to retrieve javaScript file names", e);
            throw new JavaScriptProjectFetcherException(e);
        } catch (URISyntaxException e) {
            log.error("Unable to retrieve javaScript file names", e);
            throw new JavaScriptProjectFetcherException(e);
        }
        return fileNames;
    }

    @Override
    public List<String> fetchJavaScriptInvocationMethods(String projectName, long revision, String javaScriptFileName) throws JavaScriptProjectFetcherException {
        return new ArrayList<String>(Arrays.asList(
            "functionA",
            "functionB",
            "functionC"
        ));
    }

    @Override
    public List<JavaScript> fetchRequiredJavaScript(String projectName, long revision, String javaScriptFileName) throws JavaScriptProjectFetcherException {
        final JavaScript main = new JavaScript("main script", "");
        final JavaScript dependency = new JavaScript("dependency script", "Dependable");
        return new ArrayList<JavaScript>(Arrays.asList(main, dependency));
    }

    private String buildProjectUrl(final String projectName) throws JavaScriptProjectFetcherException {
        // first verify legal project name
        if (projectName.contains(URL_DELIMITER)) {
            final String message = String.format("Project name contains path elements: %s", projectName);
            log.error(message);
            throw new JavaScriptProjectFetcherException(message);
        }

        URI projectUrl;
        try {
            projectUrl = new URI(join(URL_DELIMITER, subversionScmEndpoint, projectName, TRUNK_PATH));
        } catch (URISyntaxException e) {
            log.error("Unable to build project URL", e);
            throw new JavaScriptProjectFetcherException(e);
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
        return in.replaceFirst(String.format("%s$", URL_DELIMITER),"");
    }
}
