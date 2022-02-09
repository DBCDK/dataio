package dk.dbc.dataio.harvester.dmat;

import dk.dbc.dmat.service.persistence.DMatRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class TickleFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(TickleFetcher.class);

    public static byte[] getOnixProductFor(DMatRecord dMatRecord) {
        String id = dMatRecord.getIsbn();
        LOGGER.info("Looking up original Publizon record with id {} in tickle-repo", id);

        // Fetch onix record from tickle-repo
        // Todo: Using tickle-repo-api, fetch the original incomming <Product/> block for
        //       this record, identified by the isbn number.
        //       Ref: https://dbcjira.atlassian.net/browse/DM2-217
        return "<Product></Product>".getBytes(StandardCharsets.UTF_8);
    }
}
