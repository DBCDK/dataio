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

package dk.dbc.dataio.sink.ticklerepo;

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

public class ExpandedChunkItem extends ChunkItem {
    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    private TickleAttributes tickleAttributes = null;

    public static List<ExpandedChunkItem> from(ChunkItem chunkItem) throws IOException, JSONBException {
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
            expandedChunkItem.withTickleAttributes(
                    JSONB_CONTEXT.unmarshall(StringUtil.asString(addiRecord.getMetaData()), TickleAttributes.class));

            items.add(expandedChunkItem);
        }

        return items;
    }

    public static List<ExpandedChunkItem> safeFrom(ChunkItem chunkItem) {
        try {
            return from(chunkItem);
        } catch (Exception e) {
            return Collections.emptyList();
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
