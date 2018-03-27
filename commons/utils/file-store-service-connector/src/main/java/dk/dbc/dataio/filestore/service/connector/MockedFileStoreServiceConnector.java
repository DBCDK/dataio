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

package dk.dbc.dataio.filestore.service.connector;

import dk.dbc.httpclient.HttpClient;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Mocked FileStoreServiceConnector implementation able to intercept
 * calls to addFile() writing file content to local destinations instead
 */
public class MockedFileStoreServiceConnector extends FileStoreServiceConnector {
    public static final String FILE_ID = "42";
    public final Queue<Path> destinations;

    public MockedFileStoreServiceConnector() {
        super(HttpClient.newClient(), "baseurl");
        this.destinations = new LinkedList<>();
    }

    @Override
    public String addFile(InputStream inputStream) {
        final Path destination = destinations.remove();
        try (final FileOutputStream fos = new FileOutputStream(destination.toFile())) {
            final byte[] buf = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buf)) > 0) {
                fos.write(buf, 0, bytesRead);
            }
            fos.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write file " + destination, e);
        }
        return FILE_ID;
    }

    @Override
    public void deleteFile(String fileId) {}
}
