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

package dk.dbc.dataio.sink.worldcat;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.WorldCatSinkConfig;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.oclc.wciru.WciruServiceConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ws.rs.ProcessingException;
import java.util.HashSet;

/**
 * This Enterprise Java Bean (EJB) singleton is used as a config container for the the worldCat web-service sink
 * to the worldCat web-service.
 */

@Singleton
public class WorldCatConfigBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldCatConfigBean.class);

    @EJB
    FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;

    private long highestVersionSeen = 0;
    WciruServiceConnector wciruServiceConnector;
    private final int maxNumberOfRetries = 1;
    private final int miliSecondsToSleepBetweenRetries = 1000; // 1 second

    /**
     *
     * @param consumedMessage the consumed message
     * @return configured wciruServiceConnector
     * @throws SinkException on error to retrieve property or sink
     */
    public WciruServiceConnector getConnector(ConsumedMessage consumedMessage) throws SinkException {
        configureConnector(consumedMessage);
        return wciruServiceConnector;
    }

    /*
     * Private methods
     */

    /**
     * This method determines if the current instance of the wciru service connector is configured
     * with the latest version config values.
     *
     * If the current config is outdated:
     * The latest version of the config is retrieved through the referenced sink.
     * A new wciru service connector is created with the new config values.

     * @param consumedMessage consumed message containing the version and the id of the sink containing the version
     * @throws SinkException on error to retrieve property for id or version or on error on fetching sink
     */
    private void configureConnector(ConsumedMessage consumedMessage) throws SinkException, NullPointerException, IllegalArgumentException, ProcessingException {
        try {
            final long sinkId = consumedMessage.getHeaderValue(JmsConstants.SINK_ID_PROPERTY_NAME, Long.class);
            final long sinkVersion = consumedMessage.getHeaderValue(JmsConstants.SINK_VERSION_PROPERTY_NAME, Long.class);

            LOGGER.trace("Sink version of message {} vs highest version seen {}", sinkVersion, highestVersionSeen);
            if (sinkVersion > highestVersionSeen) {
                final Sink sink = flowStoreServiceConnectorBean.getConnector().getSink(sinkId);
                final WorldCatSinkConfig config = (WorldCatSinkConfig) sink.getContent().getSinkConfig();
                LOGGER.info("Current sink config: {}", config);
                highestVersionSeen = sink.getVersion();

                final WciruServiceConnector.RetryScheme retryScheme = new WciruServiceConnector.RetryScheme(
                        maxNumberOfRetries,
                        miliSecondsToSleepBetweenRetries,
                        new HashSet<>(config.getRetryDiagnostics()));

                wciruServiceConnector = new WciruServiceConnector(
                        config.getEndpoint(),
                        config.getUserId(),
                        config.getPassword(),
                        config.getProjectId(),
                        retryScheme);
            }
        } catch (FlowStoreServiceConnectorException e) {
            throw new SinkException(e.getMessage(), e);
        }
    }
}