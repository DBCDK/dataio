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

package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.jobstore.test.types.JobInfoSnapshotBuilder;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.sink.es.entity.EsInFlight;
import org.junit.Test;

import javax.jms.JMSException;
import javax.persistence.EntityTransaction;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class EsCleanupBeanIT extends SinkIT {
    @Test
    public void cleanup_noTaskPackagesAreMarkedAsFinished_leavesEsAndInFlightUnchanged() throws JSONBException, JMSException {
        final List<ChunkItem> items = new ArrayList<>(1);
        items.add(new ChunkItemBuilder().setId(0).setStatus(ChunkItem.Status.SUCCESS).setData(getValidAddi()).build());
        final ExternalChunk processorChunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setItems(items).build();
        final MockedJmsTextMessage sinkMessage = getSinkMessage(processorChunk);

        EntityTransaction transaction = esInFlightEntityManager.getTransaction();
        transaction.begin();
        getEsMessageProcessorBean().onMessage(sinkMessage);
        transaction.commit();

        final EsCleanupBean esCleanupBean = getEsCleanupBean();
        transaction = esInFlightEntityManager.getTransaction();
        transaction.begin();
        esCleanupBean.startup();
        esCleanupBean.cleanup();
        transaction.commit();

        // Assert that one ES task package is still in-flight
        assertThat("Number of ES task packages in-flight", listEsInFlight().size(), is(1));

        // Assert that one task package is still to be found in ES
        assertThat("Number of task packages in ES", findTaskPackages().size(), is(1));
    }

    @Test
    public void cleanup_taskPackagesAreMarkedAsFinished_removedFromEsAndInFlight() throws JSONBException, JMSException, SQLException {
        // Mock JobInfoSnapshot returned from jobStoreServiceConnector.addChunk() call
        jobStoreServiceConnector.jobInfoSnapshots.add(new JobInfoSnapshotBuilder().build());

        final List<ChunkItem> items = new ArrayList<>(1);
        items.add(new ChunkItemBuilder().setId(0).setStatus(ChunkItem.Status.SUCCESS).setData(getValidAddi()).build());
        final ExternalChunk processorChunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setItems(items).build();
        final MockedJmsTextMessage sinkMessage = getSinkMessage(processorChunk);

        EntityTransaction transaction = esInFlightEntityManager.getTransaction();
        transaction.begin();
        getEsMessageProcessorBean().onMessage(sinkMessage);
        transaction.commit();

        final List<EsInFlight> esInFlight = listEsInFlight();

        // Assert that one ES task package is in-flight
        assertThat("Number of ES task packages in-flight", esInFlight.size(), is(1));

        setTaskPackageToSuccess(esInFlight.get(0).getTargetReference());

        final EsCleanupBean esCleanupBean = getEsCleanupBean();
        transaction = esInFlightEntityManager.getTransaction();
        transaction.begin();
        esCleanupBean.startup();
        esCleanupBean.cleanup();
        transaction.commit();

        // Assert that no ES task package are in-flight
        assertThat("Number of ES task packages in-flight", listEsInFlight().size(), is(0));

        // Assert that no task packages are to be found in ES
        assertThat("Number of task packages in ES", findTaskPackages().size(), is(0));
    }

    private void setTaskPackageToSuccess(int targetReference) {
        try (final Connection connection = ES_DATASOURCE.getConnection()) {
            ESTaskPackageIntegrationTestUtil.successfullyCompleteTaskpackage(connection, targetReference);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
