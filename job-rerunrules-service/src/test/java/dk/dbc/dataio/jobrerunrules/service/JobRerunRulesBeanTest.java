/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.dataio.jobrerunrules.service;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.jobrerunrules.service.ejb.JobRerunSchemeParser;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.jsonb.JSONBContext;
import dk.dbc.jsonb.JSONBException;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobRerunRulesBeanTest {
    private final JobStoreServiceConnectorBean jobStoreServiceConnectorBean =
        mock(JobStoreServiceConnectorBean.class);
    private final JobRerunSchemeParser jobRerunSchemeParser = mock(
        JobRerunSchemeParser.class);
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void getRerunRulesFromJobInfo() throws FlowStoreServiceConnectorException,
            JobStoreServiceConnectorException, JSONBException {
        final JobRerunRulesBean jobRerunRulesBean = getJobRerunRulesBean();
        final String jobInfoSnapshotJson = jsonbContext.marshall(
            getJobInfoSnapshot());
        final Response response = jobRerunRulesBean.getRerunRulesFromJobInfo(
            jobInfoSnapshotJson);

        assertThat("status", response.getStatus(),
            is(Response.Status.OK.getStatusCode()));
        assertThat("has entity", response.hasEntity(), is(true));

        final JobRerunScheme jobRerunScheme = jsonbContext.unmarshall(
            (String) response.getEntity(), JobRerunScheme.class);
        assertThat("entity is not null", jobRerunScheme, is(notNullValue()));
        assertThat("action", jobRerunScheme.getActions().iterator().next(),
            is(JobRerunScheme.Action.RERUN_ALL));
        assertThat("type", jobRerunScheme.getType(), is(JobRerunScheme.Type.RR));
    }

    @Test
    public void getRerunRulesFromJobId() throws FlowStoreServiceConnectorException,
            JobStoreServiceConnectorException, JSONBException {
        final JobRerunRulesBean jobRerunRulesBean = getJobRerunRulesBean();
        final long jobId = 1;
        final Response response = jobRerunRulesBean.getRerunRulesFromJobId(
            jobId);

        assertThat("status", response.getStatus(),
            is(Response.Status.OK.getStatusCode()));
        assertThat("has entity", response.hasEntity(), is(true));

        final JobRerunScheme jobRerunScheme = jsonbContext.unmarshall(
            (String) response.getEntity(), JobRerunScheme.class);
        assertThat("entity is not null", jobRerunScheme, is(notNullValue()));
        assertThat("action", jobRerunScheme.getActions().iterator().next(),
            is(JobRerunScheme.Action.RERUN_ALL));
        assertThat("type", jobRerunScheme.getType(), is(JobRerunScheme.Type.RR));
    }

    private JobRerunRulesBean getJobRerunRulesBean()
            throws FlowStoreServiceConnectorException,
            JobStoreServiceConnectorException, JSONBException {
        final JobRerunScheme jobRerunScheme = new JobRerunScheme()
            .withActions(Stream.of(JobRerunScheme.Action.RERUN_ALL).collect(
                Collectors.toSet()))
            .withType(JobRerunScheme.Type.RR);
        when(jobRerunSchemeParser.parse(any(JobInfoSnapshot.class)))
            .thenReturn(jobRerunScheme);
        final JobRerunRulesBean jobRerunRulesBean = new JobRerunRulesBean();
        jobRerunRulesBean.jobStoreServiceConnectorBean =
            jobStoreServiceConnectorBean;
        final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(jobStoreServiceConnector);
        when(jobStoreServiceConnector.listJobs(any(
            JobListCriteria.class))).thenReturn(
            Collections.singletonList(getJobInfoSnapshot()));
        jobRerunRulesBean.jobRerunSchemeParser = jobRerunSchemeParser;
        return jobRerunRulesBean;
    }

    private JobInfoSnapshot getJobInfoSnapshot() throws JSONBException {
        final FlowStoreReferences flowStoreReferences = new FlowStoreReferences()
            .withReference(FlowStoreReferences.Elements.SINK,
            new FlowStoreReference(1, 1, "1"));
        final JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshot()
            .withJobId(1)
            .withFlowStoreReferences(flowStoreReferences);
        return jobInfoSnapshot;
    }
}
