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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.HoldingsItemsDAOPostgreSQLImpl;
import dk.dbc.holdingsitems.HoldingsItemsException;
import dk.dbc.vipcore.exception.VipCoreException;
import dk.dbc.vipcore.libraryrules.VipCoreLibraryRulesConnector;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Collectors;

@MessageDriven
public class HoldingsItemsMessageConsumerBean {
    private JsonFactory jsonFactory = new JsonFactory();

    @EJB
    PhLogHandler phLogHandler;

    @EJB
    VipCoreLibraryRulesConnector vipCoreLibraryRulesConnector;

    @Resource(lookup = "jdbc/search/holdingsitems")
    DataSource dataSource;

    /**
     * Callback for received messages.
     * <p>
     * Gets holdingsitems status based on the object in the message
     * and forwards to the phlog handler.
     *
     * @param message received message
     */
    public void onMessage(Message message) {
        try {
            RecordInfo recordInfo = getRecordInfo((TextMessage) message);

            // filter out non-ph agencies
            if (!vipCoreLibraryRulesConnector.
                    getLibrariesByLibraryRule(VipCoreLibraryRulesConnector.Rule.IMS_LIBRARY, true).stream().
                    map(Integer::parseInt).
                    collect(Collectors.toSet()).
                    contains(recordInfo.getAgencyId()))
                return;

            try (final Connection connection = dataSource.getConnection()) {
                HoldingsItemsDAO holdingsItemsDAO = getHoldingsItemsDao(
                        connection);
                Map<String, Integer> statusMap = holdingsItemsDAO
                        .getStatusFor(recordInfo.getBibliographicRecordId(),
                                recordInfo.getAgencyId());
                phLogHandler.updatePhLogEntry(recordInfo.getAgencyId(),
                        recordInfo.getBibliographicRecordId(), statusMap);
            }
        } catch (JMSException | SQLException | HoldingsItemsException |
                VipCoreException | IOException e) {
            // TODO: håndter fejl rigtigt + håndter RuntimeExceptions
            throw new IllegalStateException(
                    "Exception caught while processing message", e);
        }
    }

    // to enable mocking the dao
    public HoldingsItemsDAO getHoldingsItemsDao(Connection connection) {
        return new HoldingsItemsDAOPostgreSQLImpl(connection,
                "dataio-holdingsItemsMessageConsumer");
    }

    private RecordInfo getRecordInfo(TextMessage message) throws JMSException, IOException {
        String bibliographicRecordId = null;
        int agencyId = -1;

        String json = message.getText();
        try {
            JsonParser parser = jsonFactory.createParser(json);
            while (!parser.isClosed()) {
                JsonToken token = parser.nextToken();
                if (JsonToken.FIELD_NAME.equals(token)) {
                    String fieldName = parser.getCurrentName();
                    parser.nextToken();
                    if (fieldName.equals("bibliographicRecordId"))
                        bibliographicRecordId = parser.getValueAsString();
                    else if (fieldName.equals("agencyId"))
                        agencyId = parser.getValueAsInt();
                }
            }
        } catch (JsonParseException e) {
            throw new IllegalStateException(String.format("Couldn't parse json: %s", json), e);
        }

        if (bibliographicRecordId == null || agencyId == -1) {
            throw new IllegalStateException(String.format(
                    "Couldn't get record id or agency id from json: %s", json));
        }
        return new RecordInfo(bibliographicRecordId, agencyId);
    }

    private class RecordInfo {
        String bibliographicRecordId;
        int agencyId;

        public RecordInfo(String bibliographicRecordId, int agencyId) {
            this.bibliographicRecordId = bibliographicRecordId;
            this.agencyId = agencyId;
        }

        public String getBibliographicRecordId() {
            return bibliographicRecordId;
        }

        public int getAgencyId() {
            return agencyId;
        }
    }
}
