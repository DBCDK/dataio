package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.engine.JavaScript;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherException;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcher;

import javax.servlet.ServletException;
import java.util.List;

public class JavaScriptProjectFetcherServlet extends RemoteServiceServlet implements JavaScriptProjectFetcher {
    private static final long serialVersionUID = 6080138003266070829L;
    private transient JavaScriptProjectFetcher javaScriptProjectFetcher;

    @Override
    public void init() throws ServletException {
        super.init();
        javaScriptProjectFetcher = new JavaScriptProjectFetcherImpl();
    }

    @Override
    public List<RevisionInfo> fetchRevisions(String projectUrl) throws JavaScriptProjectFetcherException {
        return javaScriptProjectFetcher.fetchRevisions(projectUrl);
    }

    @Override
    public List<String> fetchJavaScriptFileNames(String projectUrl, long revision) throws JavaScriptProjectFetcherException {
        return javaScriptProjectFetcher.fetchJavaScriptFileNames(projectUrl, revision);
    }

    @Override
    public List<String> fetchJavaScriptInvocationMethods(String projectUrl, long revision, String javaScriptFileName) throws JavaScriptProjectFetcherException {
        return javaScriptProjectFetcher.fetchJavaScriptInvocationMethods(projectUrl, revision, javaScriptFileName);
    }

    @Override
    public List<JavaScript> fetchRequiredJavaScript(String projectUrl, long revision, String javaScriptFileName) throws JavaScriptProjectFetcherException {
        return javaScriptProjectFetcher.fetchRequiredJavaScript(projectUrl, revision, javaScriptFileName);
    }
}
