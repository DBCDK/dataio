package dk.dbc.dataio.jobstore.ejb;

import javax.ejb.Stateless;
import javax.ws.rs.Path;

@Stateless
@Path("/")
public class MockedJobsBean extends JobsBean { }
