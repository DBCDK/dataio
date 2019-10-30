/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.dpf;

import dk.dbc.dataio.sink.dpf.model.DpfRecord;
import dk.dbc.jsonb.JSONBException;
import dk.dbc.lobby.LobbyConnector;
import dk.dbc.lobby.LobbyConnectorException;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import dk.dbc.oss.ns.catalogingupdate.UpdateStatusEnum;
import dk.dbc.updateservice.UpdateServiceDoubleRecordCheckConnector;
import dk.dbc.updateservice.UpdateServiceDoubleRecordCheckConnectorException;

import javax.ejb.Stateless;
import javax.inject.Inject;

@Stateless
public class ServiceBroker {
    @Inject LobbyConnector lobbyConnector;
    @Inject UpdateServiceDoubleRecordCheckConnector doubleRecordCheckConnector;

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
}
