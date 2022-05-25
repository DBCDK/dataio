package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.commons.javascript.JavaScriptProject;
import dk.dbc.dataio.commons.javascript.JavaScriptProjectError;
import dk.dbc.dataio.commons.javascript.JavaScriptProjectException;
import dk.dbc.dataio.commons.javascript.JavaScriptSubversionProject;
import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherError;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherException;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcher;

import javax.servlet.ServletException;
import java.util.List;

public class JavaScriptProjectFetcherServlet extends RemoteServiceServlet implements JavaScriptProjectFetcher {
    private static final long serialVersionUID = 6080138003266070829L;
    private transient JavaScriptSubversionProject javaScriptSubversionProject;

    @Override
    public void init() throws ServletException {
        super.init();
        final String subversionScmEndpoint =
                ServiceUtil.getStringValueFromSystemEnvironmentOrProperty("SUBVERSION_URL");
        javaScriptSubversionProject = new JavaScriptSubversionProject(subversionScmEndpoint);
    }

    @Override
    public List<RevisionInfo> fetchRevisions(String projectName) throws JavaScriptProjectFetcherException {
        try {
            return javaScriptSubversionProject.fetchRevisions(projectName);
        } catch (JavaScriptProjectException e) {
            throw asJavaScriptProjectFetcherException(e);
        }
    }

    @Override
    public List<String> fetchJavaScriptFileNames(String projectName, long revision) throws JavaScriptProjectFetcherException {
        try {
            return javaScriptSubversionProject.fetchJavaScriptFileNames(projectName, revision);
        } catch (JavaScriptProjectException e) {
            throw asJavaScriptProjectFetcherException(e);
        }
    }

    @Override
    public List<String> fetchJavaScriptInvocationMethods(String projectName, long revision, String javaScriptFileName) throws JavaScriptProjectFetcherException {
        try {
            return javaScriptSubversionProject.fetchJavaScriptInvocationMethods(projectName, revision, javaScriptFileName);
        } catch (JavaScriptProjectException e) {
            throw asJavaScriptProjectFetcherException(e);
        }
    }

    @Override
    public fetchRequiredJavaScriptResult fetchRequiredJavaScript(String projectName, long revision, String javaScriptFileName, String javaScriptFunction) throws JavaScriptProjectFetcherException {
        try {
            final JavaScriptProject javaScriptProject = javaScriptSubversionProject.fetchRequiredJavaScript(projectName, revision, javaScriptFileName, javaScriptFunction);
            return new fetchRequiredJavaScriptResult(javaScriptProject.getJavaScripts(), javaScriptProject.getRequireCache());
        } catch (JavaScriptProjectException e) {
            throw asJavaScriptProjectFetcherException(e);
        }
    }

    static JavaScriptProjectFetcherException asJavaScriptProjectFetcherException(JavaScriptProjectException e) {
        if (e.getMessage() != null) {
            return new JavaScriptProjectFetcherException(
                    toJavaScriptProjectFetcherError(e.getErrorCode()), e.getMessage());
        } else {
            return new JavaScriptProjectFetcherException(
                    toJavaScriptProjectFetcherError(e.getErrorCode()), e.getCause());
        }
    }

    private static JavaScriptProjectFetcherError toJavaScriptProjectFetcherError(JavaScriptProjectError error) {
        switch (error) {
            case SCM_SERVER_ERROR:
                return JavaScriptProjectFetcherError.SCM_SERVER_ERROR;
            case SCM_INVALID_URL:
                return JavaScriptProjectFetcherError.SCM_INVALID_URL;
            case SCM_ILLEGAL_PROJECT_NAME:
                return JavaScriptProjectFetcherError.SCM_ILLEGAL_PROJECT_NAME;
            case SCM_RESOURCE_NOT_FOUND:
                return JavaScriptProjectFetcherError.SCM_RESOURCE_NOT_FOUND;
            case JAVASCRIPT_EVAL_ERROR:
                return JavaScriptProjectFetcherError.JAVASCRIPT_EVAL_ERROR;
            case JAVASCRIPT_REFERENCE_ERROR:
                return JavaScriptProjectFetcherError.JAVASCRIPT_REFERENCE_ERROR;
            case JAVASCRIPT_READ_ERROR:
                return JavaScriptProjectFetcherError.JAVASCRIPT_READ_ERROR;
            default:
                return JavaScriptProjectFetcherError.UNKNOWN;
        }
    }
}
