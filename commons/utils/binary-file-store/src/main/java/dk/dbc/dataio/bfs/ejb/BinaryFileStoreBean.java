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

package dk.dbc.dataio.bfs.ejb;

import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.bfs.api.BinaryFileStoreFsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.nio.file.Path;

/**
 * This Enterprise Java Bean (EJB) stateless bean is used to gain
 * access to binary file representations.
 * <p>
 * It is understood that the direct file system access used by the
 * current BinaryFileStore implementation is in violation of the
 * EJB specification.
 * </p>
 */
@Stateless
@LocalBean
public class BinaryFileStoreBean implements BinaryFileStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(BinaryFileStoreBean.class);
    private BinaryFileStore binaryFileStore;

    @EJB
    BinaryFileStoreConfigurationBean configuration;

    /**
     * Initializes BinaryFileStore implementation
     */
    @PostConstruct
    public void initializeBinaryFileStore() {
        LOGGER.debug("Initializing binary file store");
        binaryFileStore = new BinaryFileStoreFsImpl(configuration.getBasePath());
    }

    /**
     * Returns binary file representation associated with given path
     * @param path binary file path
     * @return binary file representation
     */
    @Override
    public BinaryFile getBinaryFile(Path path) {
        return binaryFileStore.getBinaryFile(path);
    }
}
