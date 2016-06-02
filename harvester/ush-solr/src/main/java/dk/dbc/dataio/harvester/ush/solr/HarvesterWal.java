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
import java.util.Date;
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
    public static String HARVESTER_ID = "ush-solr";

    private final UshSolrHarvesterConfig config;
    private final BinaryFileStore binaryFileStore;
    private final BinaryFile walFile;

    public HarvesterWal(UshSolrHarvesterConfig config,
                        BinaryFileStore binaryFileStore) throws NullPointerException, HarvesterException {
        this.config = InvariantUtil.checkNotNullOrThrow(config, "config");
        this.binaryFileStore = InvariantUtil.checkNotNullOrThrow(binaryFileStore, "binaryFileStore");
        this.walFile = getWalFile();
    }

    /**
     * Reads non-committed entry from this WAL if it exists
     * @return WalEntry, otherwise empty
     * @throws HarvesterException on error while reading WAL entry
     */
    public Optional<WalEntry> read() throws HarvesterException {
        if (walFile.exists()) {
            try {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                walFile.read(baos);
                return Optional.of(new WalEntry(new String(baos.toByteArray(), StandardCharsets.UTF_8)));
            } catch (RuntimeException e) {
                throw new HarvesterException("Unexpected exception caught while reading wal file: " + walFile.getPath().toString(), e);
            }
        }
        return Optional.empty();
    }

    /**
     * Writes WAL entry
     * @param walEntry WalEntry to be written
     * @throws HarvesterException on attempting to write to WAL already containing non-committed entry, or on general error writing WAL
     */
    public void write(WalEntry walEntry) throws HarvesterException {
        if (walFile.exists()) {
            throw new HarvesterException("Attempting to write already existing wal file: " + walFile.getPath().toString());
        }
        try {
            walFile.write(new ByteArrayInputStream(walEntry.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (RuntimeException e) {
            throw new HarvesterException("Unexpected exception caught while writing wal file: " + walFile.getPath().toString(), e);
        }
    }

    /**
     * Commits this WAL
     * @throws HarvesterException on failure to commit
     */
    public void commit() throws HarvesterException {
        try {
            walFile.delete();
        } catch (RuntimeException e) {
            throw new HarvesterException("Commit failed for wal file: " + walFile.getPath().toString(), e);
        }
    }

    private BinaryFile getWalFile() throws HarvesterException {
        try {
            return binaryFileStore.getBinaryFile(Paths.get(config.getContent().getUshHarvesterJobId() + ".wal"));
        } catch (RuntimeException e) {
            throw new HarvesterException("Error getting wal file for " + config.getContent().getUshHarvesterJobId(), e);
        }
    }

    public static class WalEntry {
        final long id;
        final long version;
        final long from;
        final long until;

        public static WalEntry create(long id, long version, Date from, Date until) throws NullPointerException {
            return new WalEntry(id, version, from.getTime(), until.getTime());
        }

        public WalEntry(String walEntry) throws NullPointerException, IllegalArgumentException {
            InvariantUtil.checkNotNullNotEmptyOrThrow(walEntry, "walEntry");
            final String[] parts = walEntry.split(":");
            if (parts.length != 5) {
                throw new IllegalArgumentException("Invalid syntax of WalEntry: " + walEntry);
            }
            id = parseLong(parts[1], "id");
            version = parseLong(parts[2], "version");
            from = parseLong(parts[3], "from");
            until = parseLong(parts[4], "until");
        }

        private WalEntry(long id, long version, long from, long until) {
            this.id = id;
            this.version = version;
            this.from = from;
            this.until = until;
        }

        public long getId() {
            return id;
        }

        public long getVersion() {
            return version;
        }

        public Date getFrom() {
            return new Date(from);
        }

        public Date getUntil() {
            return new Date(until);
        }

        @Override
        public String toString() {
            return HARVESTER_ID + ":" + id + ":" + version + ":" + from + ":" + until;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            WalEntry walEntry = (WalEntry) o;

            if (id != walEntry.id) {
                return false;
            }
            if (version != walEntry.version) {
                return false;
            }
            if (from != walEntry.from) {
                return false;
            }
            return until == walEntry.until;

        }

        @Override
        public int hashCode() {
            int result = (int) (id ^ (id >>> 32));
            result = 31 * result + (int) (version ^ (version >>> 32));
            result = 31 * result + (int) (from ^ (from >>> 32));
            result = 31 * result + (int) (until ^ (until >>> 32));
            return result;
        }

        private long parseLong(String s, String fieldName) throws IllegalArgumentException {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(String.format(
                        "WalEntry: string value '%s' of parameter '%s' can not be converted into a long", s, fieldName), e);
            }
        }
    }
}
