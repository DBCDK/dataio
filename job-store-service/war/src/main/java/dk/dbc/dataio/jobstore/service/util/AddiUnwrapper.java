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
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.types.JobStoreException;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Addi format unwrapper
 */
public class AddiUnwrapper implements ChunkItemUnwrapper {
    /**
     * Unwraps content from Addi records contained in given chunk item
     * @param wrappedChunkItem chunk item containing Addi records
     * @return list of unwrapped chunk items
     * @throws NullPointerException if given null valued chunk item
     * @throws JobStoreException if wrapping type of given chunk item is not
     * {@link dk.dbc.dataio.commons.types.ChunkItem.Type#ADDI} or if chunk item contains invalid Addi data
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
                // Special case handling of chunk items from
                // before the type system was implemented.
                // MarcXchange wrapped in Addi is assumed.
                unwrappedType.add(ChunkItem.Type.MARCXCHANGE);
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
        try {
            final ArrayList<Diagnostic> diagnostics = wrappedChunkItem.getDiagnostics();
            final ArrayList<ChunkItem> unwrappedChunkItems = new ArrayList<>();
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
            return unwrappedChunkItems;
        } catch(Exception e) {
            throw new JobStoreException("Exception caught while reading Addi content", e);
        }
    }
}
