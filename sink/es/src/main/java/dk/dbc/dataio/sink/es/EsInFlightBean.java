package dk.dbc.dataio.sink.es;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.sink.es.entity.inflight.EsInFlight;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

@Stateless
public class EsInFlightBean {

    @PersistenceContext(unitName = "esInFlightPU")
    EntityManager entityManager;
    JSONBContext jsonbContext = new JSONBContext();

    public void addEsInFlight(EsInFlight esInFlight) {
        entityManager.persist(esInFlight);
    }

    public void removeEsInFlight(EsInFlight esInFlight) {
        entityManager.remove(esInFlight);
    }

    public List<EsInFlight> listEsInFlight(long sinkId) {
        final TypedQuery<EsInFlight> query = entityManager.createNamedQuery(EsInFlight.FIND_ALL, EsInFlight.class);
        query.setParameter(EsInFlight.QUERY_PARAMETER_SINKID, sinkId);
        return query.getResultList();
    }

    public EsInFlight buildEsInFlight(Chunk chunk, int targetReference, String databaseName, int redelivered, long sinkId) throws JSONBException {
        final EsInFlight esInFlight = new EsInFlight();
        esInFlight.setSinkId(sinkId);
        esInFlight.setDatabaseName(databaseName);
        esInFlight.setJobId(chunk.getJobId());
        esInFlight.setChunkId(chunk.getChunkId());
        esInFlight.setTargetReference(targetReference);
        esInFlight.setIncompleteDeliveredChunk(jsonbContext.marshall(chunk));
        esInFlight.setRedelivered(redelivered);
        return esInFlight;
    }
}
