/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.dataio.jobrerunrules.service;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.jobrerunrules.service.ejb.JobRerunSchemeParser;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.jsonb.JSONBContext;
import dk.dbc.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Stateless
@Path("/")
public class JobRerunRulesBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(
        JobRerunRulesBean.class);
    private static final String rerunRulesFromJobInfo = "jobs/info";
    private static final String rerunRulesFromJobId = "jobs/{id}";
    private static final String rerunRulesFromJobIdPathParam = "id";

    @EJB JobRerunSchemeParser jobRerunSchemeParser;
    @EJB JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    /**
     * @param jobInfoSnapshot json containing job info snapshot
     * @return http 200 ok with jobRerunScheme json containing legal
     * actions for given job as well as type of rerun.
     * @throws FlowStoreServiceConnectorException on flow store connector exception
     * @throws JSONBException on json marshalling exception
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path(rerunRulesFromJobInfo)
    public Response getRerunRulesFromJobInfo(String jobInfoSnapshot) throws
            FlowStoreServiceConnectorException, JSONBException {
        JobInfoSnapshot jobInfo = new JSONBContext().unmarshall(
            jobInfoSnapshot, JobInfoSnapshot.class);
        final JobRerunScheme jobRerunScheme = jobRerunSchemeParser
            .parse(jobInfo);
        final String json = new JSONBContext().marshall(jobRerunScheme);
        return Response.ok(json).build();
    }

    /**
     * @param id job id
     * @return http 200 ok with jobRerunScheme json containing legal
     * actions for given job as well as type of rerun.
     * @throws JobStoreServiceConnectorException on job store connector exception
     * @throws FlowStoreServiceConnectorException on flow store connector exception
     * @throws JSONBException on json marshalling exception
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(rerunRulesFromJobId)
    public Response getRerunRulesFromJobId(@PathParam(rerunRulesFromJobIdPathParam) long id)
            throws JobStoreServiceConnectorException, FlowStoreServiceConnectorException, JSONBException {
        JobListCriteria criteria = new JobListCriteria().where(
            new ListFilter<>(JobListCriteria.Field.JOB_ID,
            ListFilter.Op.EQUAL, id));
        List<JobInfoSnapshot> jobInfos = jobStoreServiceConnectorBean
            .getConnector().listJobs(criteria);
        if(jobInfos.size() != 1) {
            final String errorMsg = String.format(
                "connector returned too many or too few jobinfo snapshots " +
                "for id %s: %s", id, jobInfos.size());
            LOGGER.error(errorMsg);
            return Response.serverError().entity(errorMsg).build();
        }
        final JobRerunScheme jobRerunScheme = jobRerunSchemeParser
            .parse(jobInfos.get(0));
        final String json = new JSONBContext().marshall(jobRerunScheme);
        return Response.ok(json).build();
    }
}
