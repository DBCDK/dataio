package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.sink.es.entity.EsInFlight;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.naming.NamingException;
import java.io.IOException;
import java.sql.SQLException;

@Stateless
public class EsChunkResultProcessorBean {
    @EJB
    EsSinkConfigurationBean configuration;

    @EJB
    EsInFlightBean esInFlightAdmin;

    @EJB
    EsConnectorBean esConnector;

    @Resource
    private SessionContext context;

    public int process(ChunkResult chunkResult) throws SQLException, NamingException, IOException {
        final int targetReference = esConnector.insertEsTaskPackage(chunkResult);

        final EsInFlight esInFlight = new EsInFlight();
        esInFlight.setResourceName(configuration.getEsResourceName());
        esInFlight.setTargetReference(targetReference);
        esInFlight.setJobId(chunkResult.getJobId());
        esInFlight.setChunkId(chunkResult.getChunkId());
        esInFlight.setRecordSlots(chunkResult.getResults().size());
        esInFlightAdmin.addEsInFlight(esInFlight);

        return targetReference;
    }

}
