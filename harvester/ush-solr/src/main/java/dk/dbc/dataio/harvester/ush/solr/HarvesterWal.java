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

package dk.dbc.dataio.harvester.ush.solr;

import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Simple write-ahead-log for harvester redo functionality.
 * <pre>
 * {@code
 *
 *  Optional<String> walEntry = harvesterWal.read();
 *  if (walEntry.isPresent()) {
 *      // redo whatever
 *      harvesterWal.commit();
 *  }
 *  harvesterWal.write(entry);
 *  harvesterWal.commit();
 * }
 * </pre>
 */
public class HarvesterWal {
    private final UshSolrHarvesterConfig config;
    private final BinaryFileStore binaryFileStore;
    private final BinaryFile walFile;

    public HarvesterWal(UshSolrHarvesterConfig config,
                        BinaryFileStore binaryFileStore) throws NullPointerException {
        this.config = InvariantUtil.checkNotNullOrThrow(config, "config");
        this.binaryFileStore = InvariantUtil.checkNotNullOrThrow(binaryFileStore, "binaryFileStore");
        this.walFile = getWalFile();
    }

    /**
     * Reads non-committed entry from this WAL if it exists
     * @return WAL entry as string, otherwise empty
     * @throws HarvesterException on error while reading WAL entry
     */
    public Optional<String> read() throws HarvesterException {
        if (walFile.exists()) {
            try {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                walFile.read(baos);
                return Optional.of(new String(baos.toByteArray(), StandardCharsets.UTF_8));
            } catch (IllegalStateException e) {
                throw new HarvesterException("Unexpected exception caught while reading wal file: " + walFile.getPath().toString(), e);
            }
        }
        return Optional.empty();
    }

    /**
     * Writes WAL entry
     * @param logEntry WAL entry to be written
     * @throws HarvesterException on attempting to write to WAL already containing non-committed entry, or on general error writing WAL
     */
    public void write(String logEntry) throws HarvesterException {
        if (walFile.exists()) {
            throw new HarvesterException("Attempting to write already existing wal file: " + walFile.getPath().toString());
        }
        try {
            walFile.write(new ByteArrayInputStream(logEntry.getBytes(StandardCharsets.UTF_8)));
        } catch (NullPointerException | IllegalStateException e) {
            throw new HarvesterException("Unexpected exception caught while writing wal file: " + walFile.getPath().toString(), e);
        }
    }

    /**
     * Commits this WAL
     */
    public void commit() {
        walFile.delete();
    }

    private BinaryFile getWalFile() {
        return binaryFileStore.getBinaryFile(Paths.get(config.getContent().getUshHarvesterJobId() + ".wal"));
    }
}
