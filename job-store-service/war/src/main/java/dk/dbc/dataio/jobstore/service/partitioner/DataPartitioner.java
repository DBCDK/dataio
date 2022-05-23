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

package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.jobstore.types.PrematureEndOfDataException;

import java.nio.charset.Charset;
import java.util.Iterator;

public interface DataPartitioner extends Iterable<DataPartitionerResult> {
    long NO_BYTE_COUNT_AVAILABLE = -128;

    Charset getEncoding() throws InvalidEncodingException;

    long getBytesRead();

    @SuppressWarnings("PMD.EmptyCatchBlock")
    default void drainItems(int itemsToRemove) {
        if (itemsToRemove < 0) throw new IllegalArgumentException("Unable to drain a negative number of items");
        final Iterator<DataPartitionerResult> iterator = this.iterator();
        while (--itemsToRemove >=0) {
            try {
                iterator.next();
            } catch (PrematureEndOfDataException e) {
                throw e;    // to potentially trigger a retry
            } catch (Exception e) {
                // we simply swallow these as they have already been handled in chunk items
            }
        }
    }

    default int getAndResetSkippedCount() {
        return 0;
    }
}
