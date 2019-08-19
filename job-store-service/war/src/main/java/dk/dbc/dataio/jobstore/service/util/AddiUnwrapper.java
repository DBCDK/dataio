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

package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Addi format unwrapper
 */
public class AddiUnwrapper implements ChunkItemUnwrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddiUnwrapper.class);

    /**
     * Unwraps content from Addi records contained in given chunk item.
     * If chunk type is {@link dk.dbc.dataio.commons.types.ChunkItem.Type#UNKNOWN}
     * and content NOT contains an Addi record, the chunk item is
     * added to the result list unchanged.
     * @param wrappedChunkItem chunk item containing Addi records
     * @return list of unwrapped chunk items
     * @throws NullPointerException if given null valued chunk item
     * @throws JobStoreException if wrapping type of given chunk item is not
     * {@link dk.dbc.dataio.commons.types.ChunkItem.Type#ADDI} or
     * {@link dk.dbc.dataio.commons.types.ChunkItem.Type#UNKNOWN}
     */
    @Override
    public List<ChunkItem> unwrap(ChunkItem wrappedChunkItem) throws NullPointerException, JobStoreException {
        return addiUnwrap(InvariantUtil.checkNotNullOrThrow(wrappedChunkItem, "wrappedChunkItem"));
    }

    private List<ChunkItem.Type> getUnwrappedType(ChunkItem wrappedChunkItem) throws JobStoreException {
        final List<ChunkItem.Type> type = wrappedChunkItem.getType();
        final List<ChunkItem.Type> unwrappedType = new ArrayList<>(Math.max(type.size() - 1, 1));
        if (type.size() == 1) {
            if (type.get(0) == ChunkItem.Type.UNKNOWN) {
                // Special case handling of chunk items since
                // the type system is not fully implemented.
                unwrappedType.add(ChunkItem.Type.UNKNOWN);
            } else {
                throw new JobStoreException("Type is not wrapped in Addi: " + type.toString());
            }
        } else {
            if (type.get(0) != ChunkItem.Type.ADDI) {
                throw new JobStoreException("Type is not wrapped in Addi: " + type.toString());
            }
            unwrappedType.addAll(type.subList(1, type.size()));
        }
        return unwrappedType;
    }

    private List<ChunkItem> addiUnwrap(ChunkItem wrappedChunkItem) throws JobStoreException {
        final List<ChunkItem.Type> unwrappedType = getUnwrappedType(wrappedChunkItem);
        final ArrayList<ChunkItem> unwrappedChunkItems = new ArrayList<>();
        try {
            final ArrayList<Diagnostic> diagnostics = wrappedChunkItem.getDiagnostics();
            final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(wrappedChunkItem.getData()));
            AddiRecord record = addiReader.getNextRecord();
            while (record != null) {
                final ChunkItem unwrappedChunkItem = new ChunkItem(
                        wrappedChunkItem.getId(),
                        record.getContentData(),
                        wrappedChunkItem.getStatus(),
                        unwrappedType,
                        wrappedChunkItem.getEncoding()
                );

                unwrappedChunkItem.appendDiagnostics(diagnostics);
                unwrappedChunkItems.add(unwrappedChunkItem);

                record = addiReader.getNextRecord();
            }
        } catch(Exception e) {
            LOGGER.warn("Failed to parse chunk item as Addi", e);
            unwrappedChunkItems.add(wrappedChunkItem);
        }
        return unwrappedChunkItems;
    }
}
