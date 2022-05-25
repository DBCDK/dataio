package dk.dbc.dataio.sink.worldcat;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.lang.StringUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ChunkItemWithWorldCatAttributes extends ChunkItem {
    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    private WorldCatAttributes worldCatAttributes = null;

    public static ChunkItemWithWorldCatAttributes of(ChunkItem chunkItem) throws IllegalArgumentException {
        final ChunkItemWithWorldCatAttributes extendedChunkItem = new ChunkItemWithWorldCatAttributes();

        try {
            final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(chunkItem.getData()));
            if (addiReader.hasNext()) {
                final AddiRecord addiRecord = addiReader.next();
                extendedChunkItem
                        .withId(chunkItem.getId())
                        .withEncoding(chunkItem.getEncoding())
                        .withStatus(chunkItem.getStatus())
                        .withTrackingId(chunkItem.getTrackingId())
                        .withType(chunkItem.getType().toArray(new ChunkItem.Type[chunkItem.getType().size()]))
                        .withData(addiRecord.getContentData());

                extendedChunkItem.withWorldCatAttributes(
                        JSONB_CONTEXT.unmarshall(StringUtil.asString(addiRecord.getMetaData()), WorldCatAttributes.class));
            }

            if (addiReader.hasNext()) {
                throw new IllegalArgumentException("Chunk item contains multiple ADDI records");
            }
        } catch (IOException | JSONBException | NullPointerException e) {
            throw new IllegalArgumentException(e);
        }

        return extendedChunkItem;
    }

    public WorldCatAttributes getWorldCatAttributes() {
        return worldCatAttributes;
    }

    public ChunkItemWithWorldCatAttributes withWorldCatAttributes(WorldCatAttributes worldCatAttributes) {
        this.worldCatAttributes = worldCatAttributes;
        return this;
    }

    /**
     * @return List of symbols for holdings with action INSERT
     */
    public List<String> getActiveHoldingSymbols() {
        if (worldCatAttributes.getHoldings() != null) {
            return worldCatAttributes.getHoldings().stream()
                    .filter(holding -> holding.getAction() == Holding.Action.INSERT)
                    .map(Holding::getSymbol)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Adds holdings marked as DELETE for each symbol in given list not present in current attributes
     *
     * @param formerActiveHoldingSymbols list of symbols to compare with current attributes
     */
    public void addDiscontinuedHoldings(List<String> formerActiveHoldingSymbols) {
        if (formerActiveHoldingSymbols == null) {
            return;
        }

        final List<String> difference = new ArrayList<>(formerActiveHoldingSymbols);
        if (worldCatAttributes.getHoldings() != null) {
            final List<String> currentHoldingSymbols = worldCatAttributes.getHoldings().stream()
                    .filter(holding -> holding.getAction() == Holding.Action.INSERT)
                    .map(Holding::getSymbol)
                    .collect(Collectors.toList());

            difference.removeAll(currentHoldingSymbols);
        }

        if (!difference.isEmpty() && worldCatAttributes.getHoldings() == null) {
            worldCatAttributes.withHoldings(new ArrayList<>(difference.size()));
        }

        for (String symbol : difference) {
            worldCatAttributes.getHoldings().add(new Holding().withSymbol(symbol).withAction(Holding.Action.DELETE));
        }
    }
}
