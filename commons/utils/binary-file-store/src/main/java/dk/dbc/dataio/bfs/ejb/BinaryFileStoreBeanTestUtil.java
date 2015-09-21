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

/**
 * Test utility class
 */
public class BinaryFileStoreBeanTestUtil {
    private BinaryFileStoreBeanTestUtil() { }

    /**
     * Creates BinaryFileStoreBean usable outside of application server environment
     * when used in conjunction with the InMemoryInitialContextFactory class.
     * <P>
     * <code>
     *  System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
     *  final static String BFS_BASE_PATH_JNDI_NAME = "bfs/home";
     *  InMemoryInitialContextFactory.bind(BFS_BASE_PATH_JNDI_NAME, someFolder);
     *  final BinaryFileStoreBean bean = BinaryFileStoreBeanTestUtil.getBinaryFileStoreBean(BFS_BASE_PATH_JNDI_NAME);
     * </code>
     * @param basePathJndiName JNDI lookup name of binary file store base path
     * @return BinaryFileStoreBean instance able to read from and write to folder resolved by JNDI lookup
     */
    public static BinaryFileStoreBean getBinaryFileStoreBean(String basePathJndiName) {
        final BinaryFileStoreConfigurationBean binaryFileStoreConfigurationBean = new BinaryFileStoreConfigurationBean();
        binaryFileStoreConfigurationBean.basePathJndiName = basePathJndiName;
        binaryFileStoreConfigurationBean.initialize();
        final BinaryFileStoreBean binaryFileStoreBean = new BinaryFileStoreBean();
        binaryFileStoreBean.configuration = binaryFileStoreConfigurationBean;
        binaryFileStoreBean.initializeBinaryFileStore();
        return binaryFileStoreBean;
    }
}
