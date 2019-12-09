/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.rawrepo.RecordId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * Iterable abstraction around a file containing record IDs
 * with a single record ID per line
 */
public class RecordIdFile implements Iterable<RecordId>, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordFetcher.class);

    private final BufferedReader reader;

    public RecordIdFile(BinaryFile file) {
        this.reader = new BufferedReader(
                new InputStreamReader(file.openInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public Iterator<RecordId> iterator() {
        return new Iterator<RecordId>() {
            @Override
            public boolean hasNext() {
                try {
                    reader.mark(1);
                    if (reader.read() < 0) {
                        return false;
                    }
                    reader.reset();
                    return true;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public RecordId next() {
                try {
                    return toRecordId(reader.readLine());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };
    }

    @Override
    public void close() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                LOGGER.error("Unable to close record ID file", e);
            }
        }
    }

    private static RecordId toRecordId(String line) {
        if (line != null && !line.trim().isEmpty()) {
            final String[] parts = line.split(":");
            if (parts.length == 2) {
                try {
                    return new RecordId(parts[0], Integer.parseInt(parts[1]));
                } catch (NumberFormatException e) {
                    LOGGER.error("line '{}' contained invalid ID", line, e);
                }
            }
        }
        return null;
    }
}
