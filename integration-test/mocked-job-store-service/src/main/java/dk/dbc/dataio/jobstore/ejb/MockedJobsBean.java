package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;

import javax.ejb.Stateless;
import javax.ws.rs.Path;

@Stateless
@Path(JobStoreServiceConstants.JOBS)
public class MockedJobsBean extends JobsBean { }
