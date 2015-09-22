package dk.dbc.dataio.jobstore.service.util;

import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;


public class JobstoreCdiProducer {

	@Produces @JobstoreDB @PersistenceContext(unitName="jobstorePU")
	EntityManager entityManager;
}
