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
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChunkItemWithWorldCatAttributes extends ChunkItem {
    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    private WorldCatAttributes worldCatAttributes = null;
    private int checksum;

    public static List<ChunkItemWithWorldCatAttributes> of(ChunkItem chunkItem) throws IOException, JSONBException {
        final ArrayList<ChunkItemWithWorldCatAttributes> items = new ArrayList<>();

        final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(chunkItem.getData()));
        while (addiReader.hasNext()) {
            final ChunkItemWithWorldCatAttributes extendedChunkItem = new ChunkItemWithWorldCatAttributes();
            extendedChunkItem.withId(chunkItem.getId());
            extendedChunkItem.withEncoding(chunkItem.getEncoding());
            extendedChunkItem.withStatus(chunkItem.getStatus());
            extendedChunkItem.withTrackingId(chunkItem.getTrackingId());
            extendedChunkItem.withType(chunkItem.getType().toArray(new ChunkItem.Type[chunkItem.getType().size()]));

            final AddiRecord addiRecord = addiReader.next();
            extendedChunkItem.withData(addiRecord.getContentData());
            extendedChunkItem.withWorldCatAttributes(
                    JSONB_CONTEXT.unmarshall(StringUtil.asString(addiRecord.getMetaData()), WorldCatAttributes.class));
            extendedChunkItem.withChecksum(Checksum.of(addiRecord.getMetaData()));

            items.add(extendedChunkItem);
        }

        return items;
    }

    public WorldCatAttributes getWorldCatAttributes() {
        return worldCatAttributes;
    }

    public ChunkItemWithWorldCatAttributes withWorldCatAttributes(WorldCatAttributes worldCatAttributes) {
        this.worldCatAttributes = worldCatAttributes;
        return this;
    }

    public int getChecksum() {
        return checksum;
    }

    public ChunkItemWithWorldCatAttributes withChecksum(int checksum) {
        this.checksum = checksum;
        return this;
    }
}
