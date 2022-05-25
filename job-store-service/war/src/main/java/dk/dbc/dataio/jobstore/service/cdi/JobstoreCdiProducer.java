package dk.dbc.dataio.jobstore.service.cdi;

import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;


public class JobstoreCdiProducer {

    @Produces
    @JobstoreDB
    @PersistenceContext(unitName = "jobstorePU")
    EntityManager entityManager;
}
