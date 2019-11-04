/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.dpf;

import dk.dbc.dataio.commons.types.DpfSinkConfig;
import dk.dbc.dataio.sink.dpf.model.DpfRecord;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import dk.dbc.jsonb.JSONBException;
import dk.dbc.lobby.LobbyConnector;
import dk.dbc.lobby.LobbyConnectorException;
import dk.dbc.oss.ns.catalogingupdate.BibliographicRecord;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import dk.dbc.oss.ns.catalogingupdate.UpdateStatusEnum;
import dk.dbc.updateservice.UpdateServiceDoubleRecordCheckConnector;
import dk.dbc.updateservice.UpdateServiceDoubleRecordCheckConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

@Stateless
public class ServiceBroker {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerBean.class);

    @Inject LobbyConnector lobbyConnector;
    @Inject UpdateServiceDoubleRecordCheckConnector doubleRecordCheckConnector;
    OpenUpdateServiceConnector openUpdateServiceConnector;

    @EJB ConfigBean configBean;
    DpfSinkConfig config;

    private BibliographicRecordFactory bibliographicRecordFactory = new BibliographicRecordFactory();

    public void sendToLobby(DpfRecord dpfRecord) throws LobbyConnectorException, JSONBException {
        lobbyConnector.createOrReplaceApplicant(dpfRecord.toLobbyApplicant());
    }

    public boolean isDoubleRecord(DpfRecord dpfRecord)
            throws BibliographicRecordFactoryException, UpdateServiceDoubleRecordCheckConnectorException {
        final UpdateRecordResult updateRecordResult = doubleRecordCheckConnector.doubleRecordCheck(
                bibliographicRecordFactory.toBibliographicRecord(dpfRecord.getBody()));
        return updateRecordResult.getUpdateStatus() != UpdateStatusEnum.OK;
    }

    public UpdateRecordResult sendToUpdate(String groupId, String updateTemplate,
                                           BibliographicRecord bibliographicRecord, String trackingId) {
        return getOpenUpdateServiceConnector()
                .updateRecord(groupId, updateTemplate, bibliographicRecord, trackingId);
    }

    private boolean isConfigUpdated() {
        final DpfSinkConfig latestConfig = configBean.getConfig();
        if (!latestConfig.equals(config)) {
            config = latestConfig;
            return true;
        }
        return false;
    }

    private OpenUpdateServiceConnector getOpenUpdateServiceConnector() {
        if (isConfigUpdated()) {
            LOGGER.debug("Updating update service connector");
            openUpdateServiceConnector = new OpenUpdateServiceConnector("url",
                config.getUpdateServiceUserId(),
                config.getUpdateServicePassword());
        }
        return openUpdateServiceConnector;
    }
}
