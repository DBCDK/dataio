package dk.dbc.dataio.jobstore.service.cdi;

import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;


public class JobstoreCdiProducer {

    @Produces
    @JobstoreDB
    @PersistenceContext(unitName = "jobstorePU")
    EntityManager entityManager;
}
