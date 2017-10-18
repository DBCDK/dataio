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

import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.UshHarvesterProperties;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;
import dk.dbc.dataio.harvester.ush.solr.entity.ProgressWal;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import javax.ejb.SessionContext;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HarvesterBeanTest {
    private SessionContext sessionContext = mock(SessionContext.class);
    private HarvesterWalBean wal = mock(HarvesterWalBean.class);
    private BinaryFileStoreBean binaryFileStoreBean = mock(BinaryFileStoreBean.class);
    private FileStoreServiceConnectorBean fileStoreServiceConnectorBean = mock(FileStoreServiceConnectorBean.class);
    private FileStoreServiceConnector fileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
    private FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private JobStoreServiceConnectorBean jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);
    private JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);

    private HarvesterBean harvesterBean;

    private Date lastHarvestFinishedDate = new Date();

    @Before
    public void setupMocks() {
        when(fileStoreServiceConnectorBean.getConnector()).thenReturn(fileStoreServiceConnector);
        when(flowStoreServiceConnectorBean.getConnector()).thenReturn(flowStoreServiceConnector);
        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(jobStoreServiceConnector);
        harvesterBean = createHarvesterBean();
        when(sessionContext.getBusinessObject(HarvesterBean.class)).thenReturn(harvesterBean);
    }

    @Test
    public void harvest() throws HarvesterException, ExecutionException, InterruptedException {
        final UshSolrHarvesterConfig config = createConfig();
        doReturn(config).when(harvesterBean).redoConfigUpdateIfUncommitted(config);
        doReturn(new MockedHarvestOperation(config)).when(harvesterBean).getHarvestOperation(config);

        assertThat(harvesterBean.harvest(config).get(), is(42));

        final InOrder inOrder = Mockito.inOrder(harvesterBean, wal);
        inOrder.verify(harvesterBean).redoConfigUpdateIfUncommitted(config);
        inOrder.verify(wal).write(any(ProgressWal.class));
        inOrder.verify(harvesterBean).execute(any(HarvestOperation.class));
        inOrder.verify(wal).commit(any(ProgressWal.class));
    }

    @Test
    public void redoConfigUpdateIfUncommitted_noUncommittedWalEntryExists_returnsConfigUnchanged()
            throws HarvesterException, JobStoreServiceConnectorException {
        final UshSolrHarvesterConfig config = createConfig();
        when(wal.read(config.getId())).thenReturn(Optional.empty());

        assertThat(harvesterBean.redoConfigUpdateIfUncommitted(config), is(createConfig()));

        verify(jobStoreServiceConnector, times(0)).listJobs(any(JobListCriteria.class));
    }

    @Test
    public void redoConfigUpdateIfUncommitted_uncommittedWalEntryExistsButNoDataIoJobExists_commitsWalReturnsConfigUnchanged()
            throws HarvesterException, JobStoreServiceConnectorException, FlowStoreServiceConnectorException {
        final UshSolrHarvesterConfig config = createConfig();
        final ProgressWal progressWal = new ProgressWal();
        when(wal.read(config.getId())).thenReturn(Optional.of(progressWal));
        when(jobStoreServiceConnector.listJobs(any(JobListCriteria.class))).thenReturn(Collections.emptyList());

        assertThat(harvesterBean.redoConfigUpdateIfUncommitted(config), is(createConfig()));

        verify(flowStoreServiceConnector, times(0)).updateHarvesterConfig(any(UshSolrHarvesterConfig.class));
    }

    @Test
    public void redoConfigUpdateIfUncommitted_uncommitedWalEntryExistsAndDataIoJobExists_commitsWalReturnsUpdatedConfig()
            throws HarvesterException, JobStoreServiceConnectorException, FlowStoreServiceConnectorException {
        final UshSolrHarvesterConfig config = createConfig();
        final ProgressWal progressWal = new ProgressWal()
                .withHarvestedUntil(new Date(42));
        when(wal.read(config.getId())).thenReturn(Optional.of(progressWal));
        when(jobStoreServiceConnector.listJobs(any(JobListCriteria.class))).thenReturn(Collections.singletonList(new JobInfoSnapshot()));

        harvesterBean.redoConfigUpdateIfUncommitted(config);
        assertThat("Config updated", config.getContent().getTimeOfLastHarvest(), is(progressWal.getHarvestedUntil()));

        verify(flowStoreServiceConnector).updateHarvesterConfig(any(UshSolrHarvesterConfig.class));
    }

    private HarvesterBean createHarvesterBean() {
        final HarvesterBean harvesterBean = Mockito.spy(new HarvesterBean());
        harvesterBean.sessionContext = sessionContext;
        harvesterBean.wal = wal;
        harvesterBean.fileStoreServiceConnectorBean = fileStoreServiceConnectorBean;
        harvesterBean.flowStoreServiceConnectorBean = flowStoreServiceConnectorBean;
        harvesterBean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        harvesterBean.binaryFileStoreBean = binaryFileStoreBean;
        return harvesterBean;
    }

    private UshSolrHarvesterConfig createConfig() {
        return new UshSolrHarvesterConfig(1, 1, new UshSolrHarvesterConfig.Content()
            .withTimeOfLastHarvest(new Date(lastHarvestFinishedDate.getTime() - 1000))
            .withUshHarvesterProperties(new UshHarvesterProperties()
                    .withStorageUrl("solrUrl")
                    .withLastHarvestFinishedDate(lastHarvestFinishedDate)));

    }

    private class MockedHarvestOperation extends HarvestOperation {
        public MockedHarvestOperation(UshSolrHarvesterConfig config) throws HarvesterException {
            super(config, flowStoreServiceConnector, binaryFileStoreBean, fileStoreServiceConnector, jobStoreServiceConnector);
        }

        @Override
        public int execute() {
            return 42;
        }
    }
}