package dk.dbc.dataio.jobstore.ejb;

import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
@Path(JobsBean.JOBS_ENTRY_POINT)
public class JobsBean {

    public static final String JOBS_ENTRY_POINT = "jobs";
    private static final Logger log = LoggerFactory.getLogger(JobsBean.class);

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    public Response createJob(@Context UriInfo uriInfo, String jobContent) {
        log.info("JobContent: {}", jobContent);
        return Response.created(uriInfo.getAbsolutePath()).build();
    }
}
