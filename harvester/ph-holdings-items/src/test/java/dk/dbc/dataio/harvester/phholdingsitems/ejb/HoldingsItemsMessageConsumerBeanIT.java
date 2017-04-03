/*
 * DataIO - Data IO
 * Copyright (C) 2017 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.harvester.phholdingsitems.ejb;

import dk.dbc.dataio.commons.utils.test.jms.MockedJmsObjectMessage;
import dk.dbc.dataio.openagency.OpenAgencyConnector;
import dk.dbc.dataio.openagency.OpenAgencyConnectorException;
import dk.dbc.dataio.openagency.ejb.OpenAgencyConnectorBean;
import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.HoldingsItemsException;
import dk.dbc.holdingsitems.QueueJob;
import dk.dbc.phlog.PhLog;
import org.junit.Test;
import org.mockito.Mockito;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class HoldingsItemsMessageConsumerBeanIT extends PhHarvesterIntegrationTest {

    private final static DataSource holdingsitemsDataSource = mock(
        DataSource.class);
    private final static HoldingsItemsDAO holdingsItemsDao = mock(
        HoldingsItemsDAO.class);

    @Test
    public void onMessage() throws JMSException, HoldingsItemsException,
            OpenAgencyConnectorException {
        Map<String, Integer> statusMap = new HashMap<>();
        statusMap.put("NotForLoan", 17);
        statusMap.put("OnShelf", 44);
        statusMap.put("Decommissioned", 154);
        HoldingsItemsMessageConsumerBean holdingsItemsMDB =
            initHoldingsItemsMDB(statusMap);
        EntityManager entityManager = holdingsItemsMDB.phLogHandler.phLog
                .getEntityManager();
        entityManager.getTransaction().begin();
        String[] recordIds = new String[] {"52568765", "41083924", "85028361"};
        Set<ObjectMessage> messages = new HashSet<>();
        for(String recordId : recordIds)
            messages.add(constructMessage(PHAGENCYID, recordId));
        for(ObjectMessage message : messages)
            holdingsItemsMDB.onMessage(message);
        entityManager.getTransaction().commit();
        Long count = getCount(entityManager);
        boolean deleted = (boolean) runSqlCmdSingleResult(entityManager,
            "select deleted from entry where bibliographicrecordid = '52568765'");
        assertThat("database count after multiple messages", count, is(3L));
        assertThat("delete status with active holdings", deleted, is(false));
    }

    @Test
    public void onMessage_deleted() throws JMSException,
            HoldingsItemsException, OpenAgencyConnectorException {
        Map<String, Integer> statusMap = new HashMap<>();
        statusMap.put("Decommissioned", 80);
        statusMap.put("OnShelf", 0);
        HoldingsItemsMessageConsumerBean holdingsItemsMDB =
                initHoldingsItemsMDB(statusMap);
        EntityManager entityManager = holdingsItemsMDB.phLogHandler.phLog
                .getEntityManager();
        entityManager.getTransaction().begin();
        ObjectMessage message = constructMessage(PHAGENCYID, "53423175");
        holdingsItemsMDB.onMessage(message);
        entityManager.getTransaction().commit();
        boolean deleted = (boolean) runSqlCmdSingleResult(entityManager,
            "select deleted from entry");
        assertThat(deleted, is(true));
    }

    @Test
    public void onMessage_notAPhAgency() throws JMSException,
            HoldingsItemsException, OpenAgencyConnectorException  {
        HoldingsItemsMessageConsumerBean holdingsItemsMDB =
            initHoldingsItemsMDB(new HashMap<>());
        EntityManager entityManager = holdingsItemsMDB.phLogHandler.phLog
            .getEntityManager();
        entityManager.getTransaction().begin();
        ObjectMessage message = constructMessage(150092, "53423175");
        holdingsItemsMDB.onMessage(message);
        entityManager.getTransaction().commit();
        Long count = getCount(entityManager);
        assertThat(count, is(0L));
    }

    private static ObjectMessage constructMessage(int agencyId,
            String bibliographicRecordId) throws JMSException {
        QueueJob queueJob = new QueueJob(bibliographicRecordId, agencyId,
            "17", "");
        ObjectMessage message = new MockedJmsObjectMessage();
        message.setObject(queueJob);
        return message;
    }

    private static HoldingsItemsMessageConsumerBean initHoldingsItemsMDB(
            Map<String, Integer> statusMap) throws HoldingsItemsException,
            OpenAgencyConnectorException {
        HoldingsItemsMessageConsumerBean holdingsItemsMDB = Mockito.spy(
            new HoldingsItemsMessageConsumerBean());
        PhLogHandler phLogHandler = new PhLogHandler();
        phLogHandler.phLog = new PhLog(phLogEntityManager);
        holdingsItemsMDB.phLogHandler = phLogHandler;
        Mockito.when(holdingsItemsDao.getStatusFor(Mockito.anyString(),
            Mockito.anyInt())).thenReturn(statusMap);
        Mockito.when(holdingsItemsMDB.getHoldingsItemsDao(Mockito.any(
            Connection.class))).thenReturn(holdingsItemsDao);

        OpenAgencyConnectorBean openAgencyConnectorBean = mock(
            OpenAgencyConnectorBean.class);
        OpenAgencyConnector openAgencyConnector = mock(
            OpenAgencyConnector.class);
        Set<Integer> set = new HashSet<>();
        set.add(PHAGENCYID);
        Mockito.when(openAgencyConnector.getPHLibraries()).thenReturn(set);
        Mockito.when(openAgencyConnectorBean.getConnector()).thenReturn(
            openAgencyConnector);

        holdingsItemsMDB.dataSource = holdingsitemsDataSource;
        holdingsItemsMDB.openAgencyConnectorBean = openAgencyConnectorBean;
        return holdingsItemsMDB;
    }
}
