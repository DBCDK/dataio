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
        final String subversionScmEndpoint = ServletUtil.getSubversionScmEndpoint();
        javaScriptProjectFetcher = new JavaScriptProjectFetcherImpl(subversionScmEndpoint);
    }

    @Override
    public List<RevisionInfo> fetchRevisions(String projectName) throws JavaScriptProjectFetcherException {
        return javaScriptProjectFetcher.fetchRevisions(projectName);
    }

    @Override
    public List<String> fetchJavaScriptFileNames(String projectName, long revision) throws JavaScriptProjectFetcherException {
        return javaScriptProjectFetcher.fetchJavaScriptFileNames(projectName, revision);
    }

    @Override
    public List<String> fetchJavaScriptInvocationMethods(String projectName, long revision, String javaScriptFileName) throws JavaScriptProjectFetcherException {
        return javaScriptProjectFetcher.fetchJavaScriptInvocationMethods(projectName, revision, javaScriptFileName);
    }

    @Override
    public List<JavaScript> fetchRequiredJavaScript(String projectName, long revision, String javaScriptFileName, String javaScriptFunction) throws JavaScriptProjectFetcherException {
        return javaScriptProjectFetcher.fetchRequiredJavaScript(projectName, revision, javaScriptFileName, javaScriptFunction);
    }
}
