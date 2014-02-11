package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.rest.JobStoreServiceEntryPoint;

import javax.ejb.Stateless;
import javax.ws.rs.Path;

@Stateless
@Path(JobStoreServiceEntryPoint.JOBS)
public class MockedJobsBean extends JobsBean { }
