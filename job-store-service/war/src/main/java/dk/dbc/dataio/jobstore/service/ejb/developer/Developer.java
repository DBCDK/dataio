package dk.dbc.dataio.jobstore.service.ejb.developer;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.jobstore.service.ejb.PgJobStore;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

@Stateless
@Path("/")
public class Developer {
    private static Logger LOGGER = LoggerFactory.getLogger(Developer.class);
    JSONBContext jsonbContext = new JSONBContext();

    @EJB
    PgJobStore jobStore;

    @Inject
    FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;


    @POST
    @Path(JobStoreServiceConstants.JOB_COLLECTION + "/developer/{recordsplitter}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Stopwatch
    public Response addJobDeveloper(@Context UriInfo uriInfo,
                                    @PathParam("recordsplitter") RecordSplitterConstants.RecordSplitter recordSplitter,
                                    String jobInputStreamData) throws JSONBException, JobStoreException {
        LOGGER.info("JobInputStream: {}", jobInputStreamData);
        final JobInputStream jobInputStream;
        JobInfoSnapshot jobInfoSnapshot;

        try {
            jobInputStream = jsonbContext.unmarshall(jobInputStreamData, JobInputStream.class);
            FlowComponentContent flowComponentContent = new FlowComponentContent("FC 1",
                    "javascript", 1L, "invocationscript",
                    List.of(), "invocationmethod", "describe 1");
            FlowComponent flowComponent = new FlowComponent(1, 1, flowComponentContent, flowComponentContent);
            Flow flow = new Flow(1, 1, new FlowContent("Flow 1", "describe 1",
                    List.of(flowComponent)));
            Sink sink = new Sink(1, 1, new SinkContent("sink 1", "sinkqueue1",
                    "descibe 1", SinkContent.SinkType.HIVE, null,
                    SinkContent.SequenceAnalysisOption.ID_ONLY));

            Submitter submitter = new Submitter(1, 1,
                    new SubmitterContent(1, "submitter 1", "describe 1",
                            Priority.NORMAL, true));
            AddJobParamDeveloper addJobParamDeveloper = new AddJobParamDeveloper(jobInputStream)
                    .withTypeOfDataPartitioner(recordSplitter)
                    .withDiagnostics(new ArrayList<>())
                    .withFlowStoreServiceConnector(null)
                    .withFlow(flow)
                    .withSink(sink)
                    .withSubmitter(submitter)
                    .withFlowStoreReferences(newFlowStoreReferences(null, flow, sink, submitter));

            jobInfoSnapshot = jobStore.addJob(addJobParamDeveloper);
            return Response.created(getUri(uriInfo, Integer.toString(jobInfoSnapshot.getJobId())))
                    .entity(jsonbContext.marshall(jobInfoSnapshot))
                    .build();
        } catch (JSONBException e) {
            LOGGER.error("addJobDeveloper:", e);
            return Response.status(BAD_REQUEST)
                    .entity(jsonbContext.marshall(new JobError(JobError.Code.INVALID_JSON, e.getMessage(), ServiceUtil.stackTraceToString(e))))
                    .build();
        }
    }

    private URI getUri(UriInfo uriInfo, String jobId) {
        final UriBuilder absolutePathBuilder = uriInfo.getAbsolutePathBuilder();
        return absolutePathBuilder.path(jobId).build();
    }

    private FlowStoreReferences newFlowStoreReferences(FlowBinder flowBinder, Flow flow, Sink sink, Submitter submitter) {
        final FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        if (flowBinder != null) {
            flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW_BINDER,
                    new FlowStoreReference(flowBinder.getId(), flowBinder.getVersion(), flowBinder.getContent().getName()));
        }
        if (flow != null) {
            flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW,
                    new FlowStoreReference(flow.getId(), flow.getVersion(), flow.getContent().getName()));
        }
        if (sink != null) {
            flowStoreReferences.setReference(FlowStoreReferences.Elements.SINK,
                    new FlowStoreReference(sink.getId(), sink.getVersion(), sink.getContent().getName()));
        }
        if (submitter != null) {
            flowStoreReferences.setReference(FlowStoreReferences.Elements.SUBMITTER,
                    new FlowStoreReference(submitter.getId(), submitter.getVersion(), submitter.getContent().getName()));
        }
        return flowStoreReferences;
    }
}
