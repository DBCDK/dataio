/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

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
