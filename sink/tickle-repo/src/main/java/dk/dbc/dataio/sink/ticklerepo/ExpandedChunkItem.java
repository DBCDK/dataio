package dk.dbc.dataio.sink.ticklerepo;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExpandedChunkItem extends ChunkItem {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpandedChunkItem.class);
    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    private TickleAttributes tickleAttributes = null;

    public static List<ExpandedChunkItem> from(ChunkItem chunkItem) throws IOException {
        final ArrayList<ExpandedChunkItem> items = new ArrayList<>();

        final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(chunkItem.getData()));
        while (addiReader.hasNext()) {
            final ExpandedChunkItem expandedChunkItem = new ExpandedChunkItem();
            expandedChunkItem.withId(chunkItem.getId());
            expandedChunkItem.withEncoding(chunkItem.getEncoding());
            expandedChunkItem.withStatus(chunkItem.getStatus());
            expandedChunkItem.withTrackingId(chunkItem.getTrackingId());
            expandedChunkItem.withType(chunkItem.getType().toArray(new ChunkItem.Type[chunkItem.getType().size()]));

            final AddiRecord addiRecord = addiReader.next();
            expandedChunkItem.withData(addiRecord.getContentData());
            try {
                expandedChunkItem.withTickleAttributes(
                        JSONB_CONTEXT.unmarshall(StringUtil.asString(addiRecord.getMetaData()), TickleAttributes.class));
            } catch (JSONBException je) {
                LOGGER.warn("Unable to unmarshall tickle attributes for chunk item {}", chunkItem.getTrackingId(), je);
                expandedChunkItem.withTickleAttributes(new TickleAttributes());
            }

            items.add(expandedChunkItem);
        }

        return items;
    }

    public static List<ExpandedChunkItem> safeFrom(ChunkItem chunkItem) {
        try {
            return from(chunkItem);
        } catch (Exception e) {
            return List.of();
        }
    }

    public TickleAttributes getTickleAttributes() {
        return tickleAttributes;
    }

    public ExpandedChunkItem withTickleAttributes(TickleAttributes tickleAttributes) {
        this.tickleAttributes = tickleAttributes;
        return this;
    }
}
