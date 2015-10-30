package dk.dbc.dataio.jobstore.service.ejb;

import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.net.URISyntaxException;

import static org.mockito.Mockito.mock;

/**
 * Created by ThomasBerg on 30/10/15.
 */
public class NotificationBeanTest {

    private NotificationBean notificationBean;

    @Before
    public void setup() throws URISyntaxException {
        initializeNotificationBean();
    }

    private void initializeNotificationBean() {
        notificationBean = new NotificationBean();
        notificationBean.jobNotificationRepository = mock(JobNotificationRepository.class);
    }
//
//    @Test
//    public void addNotification_marshallingFailure_returnsResponseWithHttpStatusBadRequest() throws Exception {
//        final Response response = jobsBean.addJob(mockedUriInfo, "invalid JSON");
//        assertThat(response.hasEntity(), is(true));
//        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
//
//        final JobError jobErrorReturned = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
//        assertThat(jobErrorReturned, is(notNullValue()));
//        assertThat(jobErrorReturned.getCode(), is(JobError.Code.INVALID_JSON));
//    }


    @Test
    public void addNotification_returnsResponseWithStatusOk() throws Exception {

//        final JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshotBuilder().setJobId(JOB_ID).build();
//        final JobInputStream jobInputStream = new JobInputStream(jobInfoSnapshot.getSpecification(), false, PART_NUMBER);
//        final String jobInputStreamJson = asJson(jobInputStream);

//        when(jobsBean.jobStore.addAndScheduleJob(any(JobInputStream.class))).thenReturn(jobInfoSnapshot);

//        final Response response = notificationBean.addNotification();
//        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
//        assertThat(response.getLocation().toString(), is(LOCATION));
//        assertThat(response.hasEntity(), is(true));
//
//        final JobInfoSnapshot returnedJobInfoSnapshot = jsonbContext.unmarshall((String) response.getEntity(), JobInfoSnapshot.class);
//        assertThat(returnedJobInfoSnapshot, is(notNullValue()));
//        assertThat(returnedJobInfoSnapshot.hasFatalError(), is(false));
//        assertThat(returnedJobInfoSnapshot.getJobId(), is(jobInfoSnapshot.getJobId()));
//        assertThat(returnedJobInfoSnapshot.getSpecification(), is(jobInfoSnapshot.getSpecification()));
//        assertThat(returnedJobInfoSnapshot.getState(), is(jobInfoSnapshot.getState()));
//        assertThat(returnedJobInfoSnapshot.getFlowStoreReferences(), is(jobInfoSnapshot.getFlowStoreReferences()));
    }

}