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

package dk.dbc.dataio.cli;

import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jsonb.JSONBException;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.client.Client;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Class managing all interactions with the dataIO file-store needed for acceptance test operation
 */
public class FileManager {

    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final String props = ".props";

    public FileManager(String fileStoreEndpoint) {
        final Client client = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature()));
        fileStoreServiceConnector = new FileStoreServiceConnector(client, fileStoreEndpoint);
    }

    public List<Path> getTestSuite(String dir) throws IOException {
        final List<Path> testSuite = Files.walk(Paths.get(dir))
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());
        if(testSuite.size() == 2) {
            return testSuite;
        } else {
            throw new IllegalStateException(String.format("Test suite contains %d file(s). Expected 2", testSuite.size()));
        }
    }

    public String addDataFile(List<Path> testSuite) throws FileStoreServiceConnectorException, IOException {
        final Path dataFile = getDataFilePath(testSuite);
        try (final InputStream is = new FileInputStream(dataFile.toFile())) {
            String fileId = fileStoreServiceConnector.addFile(is);
            return fileId;
        }
    }

    public Properties getJobProperties(List<Path> paths) throws IOException, JSONBException {
        final Path jobPropertiesPath = getPropsFilePath(paths);
        final Properties properties = new Properties();
        properties.load(new FileInputStream(jobPropertiesPath.toFile()));
        return properties;
    }

    /*
     * Private methods
     */

    private Path getPropsFilePath(List<Path> paths) {
        final Path jobPropertiesPath = paths.stream().filter(path -> path.toString().endsWith(props)).findFirst().orElse(null);
        if(jobPropertiesPath != null ) {
            return jobPropertiesPath;
        } else {
            throw new IllegalStateException("Required property file not found");
        }
    }

    private Path getDataFilePath(List<Path> paths) {
        Path dataFilePath = null;
        for (Path path : paths) {
            if (!path.toString().endsWith(props)) {
                dataFilePath = path;
            }
        }
        if(dataFilePath != null) {
            return dataFilePath;
        } else {
            throw new IllegalStateException("Required data file not found");
        }
    }
}