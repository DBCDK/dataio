package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.commons.types.RevisionInfo;

import java.util.List;

public interface JavaScriptProjectFetcherAsync {
    void fetchRevisions(String projectUrl, AsyncCallback<List<RevisionInfo>> async);

    void fetchJavaScriptFileNames(String projectUrl, long revision, AsyncCallback<List<String>> async);

    void fetchJavaScriptInvocationMethods(String projectUrl, long revision, String javaScriptFileName, AsyncCallback<List<String>> async);

    void fetchRequiredJavaScript(String projectUrl, long revision, String javaScriptFileName, String javaScriptFunction, AsyncCallback<JavaScriptProjectFetcher.fetchRequiredJavaScriptResult> async);
}
