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

public class HarvesterWalBeanTest {
    /*
    private final int ushHarvesterJobId = 42;
    private final UshSolrHarvesterConfig ushSolrHarvesterConfig = newUshSolrHarvesterConfig();

    private HarvesterWalBean harvesterWalBean;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setObjectUnderTest() throws HarvesterException {
        harvesterWalBean = new HarvesterWalBean(ushSolrHarvesterConfig, new BinaryFileStoreFsImpl(folder.getRoot().toPath()));
    }

    @Test(expected = NullPointerException.class)
    public void constructor_configArgIsNull_throw() throws HarvesterException {
        new HarvesterWalBean(null, new BinaryFileStoreFsImpl(folder.getRoot().toPath()));
    }

    @Test(expected = NullPointerException.class)
    public void constructor_binaryFileStoreArgIsNull_throw() throws HarvesterException {
        new HarvesterWalBean(ushSolrHarvesterConfig, null);
    }

    @Test
    public void read_walFileDoesNotExist_returnsEmpty() throws HarvesterException {
        assertThat(harvesterWalBean.read(), is(Optional.empty()));
    }

    @Test
    public void write_walFileExists_throws() throws HarvesterException, IOException {
        folder.newFile(ushHarvesterJobId + ".wal");
        assertThat(() -> harvesterWalBean.write(HarvesterWalBean.WalEntry.create(1, 2, new Date(3), new Date(4))), isThrowing(HarvesterException.class));
    }

    @Test
    public void commit_walFileExists_removesWalFile() throws HarvesterException, IOException {
        final File walFile = folder.newFile(ushHarvesterJobId + ".wal");
        harvesterWalBean.commit();
        assertThat(walFile.exists(), is(false));
    }

    @Test
    public void wal_operations() throws HarvesterException {
        final HarvesterWalBean.WalEntry walEntry = HarvesterWalBean.WalEntry.create(1, 2, new Date(3), new Date(4));
        harvesterWalBean.write(walEntry);
        assertThat("First read before commit", harvesterWalBean.read().orElse(null), is(walEntry));
        assertThat("Second read before commit", harvesterWalBean.read().orElse(null), is(walEntry));
        harvesterWalBean.commit();
        assertThat("Read after commit", harvesterWalBean.read(), is(Optional.empty()));
        harvesterWalBean.write(walEntry);
        assertThat("Read after second write", harvesterWalBean.read().orElse(null), is(walEntry));
    }

    private UshSolrHarvesterConfig newUshSolrHarvesterConfig() {
        return new UshSolrHarvesterConfig(1, 1, new UshSolrHarvesterConfig.Content()
                .withUshHarvesterJobId(ushHarvesterJobId));
    }
    */
}