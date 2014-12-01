package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.test.json.JobSpecificationJsonBuilder;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

public class JobStoreBeanTest {

    public JobStoreBeanTest() {
    }

    @Test
    public void unmarshallJobSpecDataOrThrow_nullArgument_throws() throws JobStoreException {
        JobStoreBean jobStoreBean = new JobStoreBean();
        jobStoreBean.jsonbb = new JSONBBean();
        jobStoreBean.jsonbb.initialiseContext();
        try {
            jobStoreBean.unmarshallJobSpecDataOrThrow(null);
            fail("No NullPointerException Thrown");
        } catch (NullPointerException ex) {
            // all is good
        }
    }

    @Test
    public void unmarshallJobSpecDataOrThrow_emptyArgument_throws() {
        JobStoreBean jobStoreBean = new JobStoreBean();
        jobStoreBean.jsonbb = new JSONBBean();
        jobStoreBean.jsonbb.initialiseContext();
        try {
            jobStoreBean.unmarshallJobSpecDataOrThrow("");
            fail("No JobStoreException Thrown");
        } catch (JobStoreException ex) {
            // all is good
        }
    }

    @Test
    public void unmarshallJobSpecDataOrThrow_illegalStringArgument_throws() {
        JobStoreBean jobStoreBean = new JobStoreBean();
        jobStoreBean.jsonbb = new JSONBBean();
        jobStoreBean.jsonbb.initialiseContext();
        try {
            jobStoreBean.unmarshallJobSpecDataOrThrow("This is not Json");
            fail("No JobStoreException Thrown");
        } catch (JobStoreException ex) {
            // all is good
        }
    }

    @Test
    public void unmarshallJobSpecDataOrThrow_wrongJsonStringArgument_throws() {
        JobStoreBean jobStoreBean = new JobStoreBean();
        jobStoreBean.jsonbb = new JSONBBean();
        jobStoreBean.jsonbb.initialiseContext();
        try {
            jobStoreBean.unmarshallJobSpecDataOrThrow("{ \"something\":\"other\" }");
            fail("No JobStoreException Thrown");
        } catch (JobStoreException ex) {
            // all is good
        }
    }

    @Test
    public void unmarshallJobSpecDataOrThrow_incompleteJsonStringArgument_throws() {
        JobStoreBean jobStoreBean = new JobStoreBean();
        jobStoreBean.jsonbb = new JSONBBean();
        jobStoreBean.jsonbb.initialiseContext();
        try {
            JobSpecification jobSpec = jobStoreBean.unmarshallJobSpecDataOrThrow("{ \"packaging\":\"a\" }");
            fail("No JobStoreException Thrown");
        } catch (JobStoreException ex) {
            // all is good
        }
    }

    @Test
    public void unmarshallJobSpecDataOrThrow_validJsonStringArgument_success() throws JobStoreException {
        JobStoreBean jobStoreBean = new JobStoreBean();
        jobStoreBean.jsonbb = new JSONBBean();
        jobStoreBean.jsonbb.initialiseContext();
        jobStoreBean.unmarshallJobSpecDataOrThrow(new JobSpecificationJsonBuilder().build());
    }

}
