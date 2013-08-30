package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.RemoteService;
import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.engine.JavaScript;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherException;

import java.util.List;

public interface JavaScriptProjectFetcher extends RemoteService {
    /**
     * Fetches information from source control management system for
     * all commited revisions for project pointed to by given URL
     *
     * @param projectUrl project URL
     *
     * @return list of revision information in descending revision order
     *
     * @throws JavaScriptProjectFetcherException if unable to fetch revision information
     */
    List<RevisionInfo> fetchRevisions(String projectUrl) throws JavaScriptProjectFetcherException;

    /**
     * Fetches paths of all files with a .js extension contained in specified
     * revision of project pointed to by given URL
     *
     * @param projectUrl project URL
     * @param revision project revision
     *
     * @return list of file names
     *
     * @throws JavaScriptProjectFetcherException if unable to fetch files
     */
    List<String> fetchJavaScriptFileNames(String projectUrl, long revision) throws JavaScriptProjectFetcherException;

    /**
     * Fetches names of all potential invocation methods contained in specified
     * javaScript file in given revision of project pointed to by given URL
     *
     * @param projectUrl project URL
     * @param revision project revision
     * @param javaScriptFileName name of script file
     *
     * @return list of method names in alphabetical order
     *
     * @throws JavaScriptProjectFetcherException if unable to fetch method names
     */
    List<String> fetchJavaScriptInvocationMethods(String projectUrl, long revision, String javaScriptFileName) throws JavaScriptProjectFetcherException;

    /**
     * Feches script content of specified javaScript file (and any of its dependencies)
     * in given revision of project pointed to by given URL
     *
     * @param projectUrl project URL
     * @param revision project revision
     * @param javaScriptFileName name of script file
     *
     * @return list of javaScripts
     *
     * @throws JavaScriptProjectFetcherException if unable to fetch javaScript content
     */
    List<JavaScript> fetchRequiredJavaScript(String projectUrl, long revision, String javaScriptFileName) throws JavaScriptProjectFetcherException;
}
