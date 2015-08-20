package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.sink.es.entity.EsInFlight;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

@Stateless
public class EsInFlightBean {
    @EJB
    EsSinkConfigurationBean configuration;

    @PersistenceContext
    EntityManager entityManager;

    public void addEsInFlight(EsInFlight esInFlight) {
        entityManager.persist(esInFlight);
    }

    public void removeEsInFlight(EsInFlight esInFlight) {
        entityManager.remove(esInFlight);
    }

    public List<EsInFlight> listEsInFlight() {
        final TypedQuery<EsInFlight> query = entityManager.createNamedQuery(EsInFlight.FIND_ALL, EsInFlight.class);
        query.setParameter(EsInFlight.QUERY_PARAMETER_RESOURCENAME, configuration.getEsResourceName());
        return query.getResultList();
    }
}
