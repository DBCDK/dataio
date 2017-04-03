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

import dk.dbc.dataio.openagency.OpenAgencyConnectorException;
import dk.dbc.dataio.openagency.ejb.OpenAgencyConnectorBean;
import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.HoldingsItemsDAOPostgreSQLImpl;
import dk.dbc.holdingsitems.HoldingsItemsException;
import dk.dbc.holdingsitems.QueueJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

@MessageDriven
public class HoldingsItemsMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(
        HoldingsItemsMessageConsumerBean.class);

    @EJB
    PhLogHandler phLogHandler;

    @EJB
    OpenAgencyConnectorBean openAgencyConnectorBean;

    @Resource(lookup = "jdbc/search/holdingsitems")
    DataSource dataSource;

    /**
     * Callback for received messages.
     *
     * Gets holdingsitems status based on the object in the message
     * and forwards to the phlog handler.
     *
     * @param message received message
     */
    public void onMessage(Message message) {
        try {
            ObjectMessage objectMessage = (ObjectMessage) message;
            QueueJob queueJob = (QueueJob) objectMessage.getObject();

            // filter out non-ph agencies
            if(openAgencyConnectorBean.getConnector().getPHLibraries()
                    .stream().noneMatch(e -> e == queueJob.getAgencyId()))
                return;

            try(final Connection connection = dataSource.getConnection()) {
                HoldingsItemsDAO holdingsItemsDAO = getHoldingsItemsDao(
                    connection);
                Map<String, Integer> statusMap = holdingsItemsDAO
                    .getStatusFor(queueJob.getBibliographicRecordId(),
                    queueJob.getAgencyId());
                phLogHandler.updatePhLogEntry(queueJob.getAgencyId(),
                    queueJob.getBibliographicRecordId(), statusMap);
            }
        } catch(JMSException | SQLException | HoldingsItemsException |
                OpenAgencyConnectorException e) {
            // TODO: håndter fejl rigtigt + håndter RuntimeExceptions
            throw new IllegalStateException(
                "Exception caught while processing message", e);
        }
    }

    // to enable mocking the dao
    protected HoldingsItemsDAO getHoldingsItemsDao(Connection connection) {
        return new HoldingsItemsDAOPostgreSQLImpl(connection,
            "dataio-holdingsItemsMessageConsumer");
    }
}
