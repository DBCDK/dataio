/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.dpf;

import dk.dbc.dataio.sink.dpf.model.DpfRecord;
import dk.dbc.lobby.LobbyConnector;
import dk.dbc.lobby.LobbyConnectorException;

import javax.ejb.Stateless;
import javax.inject.Inject;

@Stateless
public class ServiceBroker {
    @Inject LobbyConnector lobbyConnector;

    public void sendToLobby(DpfRecord dpfRecord) throws LobbyConnectorException {
        lobbyConnector.createOrReplaceApplicant(dpfRecord.toLobbyApplicant());
    }
}
