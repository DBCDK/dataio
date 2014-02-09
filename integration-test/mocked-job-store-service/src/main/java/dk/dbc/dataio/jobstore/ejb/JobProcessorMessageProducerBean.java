package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.NewJob;
import dk.dbc.dataio.jobstore.types.JobStoreException;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;

@LocalBean
@Stateless
public class JobProcessorMessageProducerBean {
    public void send(NewJob newJob) throws NullPointerException, JobStoreException {
    }
}
