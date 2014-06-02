package dk.dbc.dataio.sink.fbs.ejb;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import java.util.ArrayList;

@Stateless
public class FbsPusherBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(FbsPusherBean.class);

    /*
    @EJB
    UpdateMarcExchangeConnectorBean updateMarcExchangeConnector;
    */

    public SinkChunkResult push(ChunkResult chunkResult) {
        final SinkChunkResult sinkChunkResult = new SinkChunkResult(chunkResult.getJobId(),
                chunkResult.getChunkId(), chunkResult.getEncoding(), new ArrayList<ChunkItem>());
        for (ChunkItem chunkItem : chunkResult.getItems()) {
            LOGGER.error("Replace me with real functionality {}", chunkItem.getId());
            // updateMarcExchangeConnector.update(...);
            // sinkChunkResult.addItem(...);
        }
        return sinkChunkResult;
    }
}
