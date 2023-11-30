package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.types.SVNFilenames;
import dk.dbc.dataio.commons.types.SVNInvocationMethods;
import dk.dbc.dataio.commons.types.SVNRevision;
import dk.dbc.dataio.javascript.JavaScriptProjectException;
import dk.dbc.dataio.javascript.JavaScriptSubversionProject;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.ws.rs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;

import java.util.stream.Collectors;

@Stateless
@LocalBean
@Path("/")
public class SubversionFetcher extends AbstractResourceBean {
    Logger LOGGER = LoggerFactory.getLogger(SubversionFetcher.class);
    JSONBContext jsonbContext = new JSONBContext();

    JavaScriptSubversionProject javaScriptSubversionProject;
    @Inject
    @ConfigProperty(name = "SUBVERSION_URL", defaultValue = "NONE")
    private String subversionUrl;

    @PostConstruct
    public void setUpSVNFetcher() {
        LOGGER.info("Svn fetcher initialized. Url:{}", subversionUrl);
        this.javaScriptSubversionProject = new JavaScriptSubversionProject(subversionUrl);
    }



    @GET
    @Path(FlowStoreServiceConstants.SVN_PROJECT_GET_REVISIONS)
    @Produces({MediaType.APPLICATION_JSON})
    public Response getRevisions(@PathParam(FlowStoreServiceConstants.SVN_PROJECT_PATH) String path)
            throws JavaScriptProjectException, JSONBException {
        SVNRevision svnRevision = new SVNRevision()
                .withProject(path)
                .withRevisions(javaScriptSubversionProject.fetchRevisions(path).stream().map(revisionInfo -> Long.toString(revisionInfo.getRevision())).collect(Collectors.toList()));
        return Response.ok().entity(jsonbContext.marshall(svnRevision)).build();
    }

    @GET
    @Path(FlowStoreServiceConstants.SVN_PROJECT_GET_SCRIPTS)
    @Produces({MediaType.APPLICATION_JSON})
    public Response getScripts(@PathParam(FlowStoreServiceConstants.SVN_PROJECT_PATH) String path,
                               @PathParam(FlowStoreServiceConstants.SVN_PROJECT_REVISION) String revision)
            throws JavaScriptProjectException, JSONBException {
        SVNFilenames svnFilenames = new SVNFilenames()
                .withProject(path)
                .withRevision(revision)
                .withFilenames(javaScriptSubversionProject.fetchJavaScriptFileNames(path, Long.parseLong(revision)));
        return Response.ok().entity(jsonbContext.marshall(svnFilenames)).build();
    }

    @GET
    @Path(FlowStoreServiceConstants.SVN_PROJECT_GET_INVOCATION_METHODS)
    @Produces({MediaType.APPLICATION_JSON})
    public Response getScripts(@PathParam(FlowStoreServiceConstants.SVN_PROJECT_PATH) String path,
                               @PathParam(FlowStoreServiceConstants.SVN_PROJECT_REVISION) String revision,
                               @PathParam(FlowStoreServiceConstants.SVN_PROJECT_SCRIPTNAME) String scriptname)
            throws JavaScriptProjectException, JSONBException {
        SVNInvocationMethods svnInvocationMethods = new SVNInvocationMethods()
                .withProject(path)
                .withRevision(revision)
                .withFilename(path)
                .withInvocationMethods(javaScriptSubversionProject
                        .fetchJavaScriptInvocationMethods(path, Long.parseLong(revision), scriptname));
        return Response.ok().entity(jsonbContext.marshall(svnInvocationMethods)).build();
    }


}
