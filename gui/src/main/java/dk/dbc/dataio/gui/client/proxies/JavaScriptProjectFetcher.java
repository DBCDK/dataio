package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@RemoteServiceRelativePath("JavaScriptProjectFetcher")
public interface JavaScriptProjectFetcher extends RemoteService {
    /**
     * Fetches information from source control management system for
     * all commited revisions for project pointed to by given URL
     *
     * @param projectUrl project URL
     * @return list of revision information in descending revision order
     * @throws JavaScriptProjectFetcherException if unable to fetch revision information
     */
    List<RevisionInfo> fetchRevisions(String projectUrl) throws JavaScriptProjectFetcherException;

    /**
     * Fetches paths of all files with a .js extension contained in specified
     * revision of project pointed to by given URL
     *
     * @param projectUrl project URL
     * @param revision   project revision
     * @return list of file names
     * @throws JavaScriptProjectFetcherException if unable to fetch files
     */
    List<String> fetchJavaScriptFileNames(String projectUrl, long revision) throws JavaScriptProjectFetcherException;

    /**
     * Fetches names of all potential invocation methods contained in specified
     * javaScript file in given revision of project pointed to by given URL
     *
     * @param projectUrl         project URL
     * @param revision           project revision
     * @param javaScriptFileName name of script file
     * @return list of method names in alphabetical order
     * @throws JavaScriptProjectFetcherException if unable to fetch method names
     */
    List<String> fetchJavaScriptInvocationMethods(String projectUrl, long revision, String javaScriptFileName) throws JavaScriptProjectFetcherException;

    /**
     * Feches script content of specified javaScript file (and any of its dependencies)
     * in given revision of project pointed to by given URL
     *
     * @param projectUrl         project URL
     * @param revision           project revision
     * @param javaScriptFileName name of script file
     * @param javaScriptFunction name of invocation function in script file
     * @return list of javaScripts
     * @throws JavaScriptProjectFetcherException if unable to fetch javaScript content
     */
    fetchRequiredJavaScriptResult fetchRequiredJavaScript(String projectUrl, long revision, String javaScriptFileName, String javaScriptFunction) throws JavaScriptProjectFetcherException;

    /**
     * The factory class for JavaScriptProjectFetcher
     */
    class Factory {
        private static JavaScriptProjectFetcherAsync asyncInstance = null;

        public static JavaScriptProjectFetcherAsync getAsyncInstance() {
            if (asyncInstance == null) {
                asyncInstance = GWT.create(JavaScriptProjectFetcher.class);
            }
            return asyncInstance;
        }
    }

    class fetchRequiredJavaScriptResult implements Serializable {
        public List<JavaScript> javaScripts;
        public String requireCache = null;

        public fetchRequiredJavaScriptResult(List<JavaScript> javaScripts, String requireCache) {
            this.javaScripts = javaScripts;
            this.requireCache = requireCache;
        }

        public fetchRequiredJavaScriptResult() {
            javaScripts = new ArrayList<JavaScript>();
        }
    }


}
