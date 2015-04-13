package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherException;

import java.util.List;

public interface JavaScriptProjectFetcherAsync {
    void fetchRevisions(String projectUrl, AsyncCallback<List<RevisionInfo>> async);
    void fetchJavaScriptFileNames(String projectUrl, long revision, AsyncCallback<List<String>> async);
    void fetchJavaScriptInvocationMethods(String projectUrl, long revision, String javaScriptFileName, AsyncCallback<List<String>> async);

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
    void fetchRequiredJavaScript(String projectUrl, long revision, String javaScriptFileName, String javaScriptFunction, AsyncCallback<JavaScriptProjectFetcher.fetchRequiredJavaScriptResult> async);
}
