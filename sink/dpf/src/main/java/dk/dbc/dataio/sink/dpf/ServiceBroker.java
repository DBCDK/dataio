/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.dpf;

import dk.dbc.dataio.sink.dpf.model.DpfRecord;
import dk.dbc.dataio.sink.dpf.model.RawrepoRecord;
import dk.dbc.jsonb.JSONBException;
import dk.dbc.lobby.LobbyConnector;
import dk.dbc.lobby.LobbyConnectorException;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import dk.dbc.oss.ns.catalogingupdate.UpdateStatusEnum;
import dk.dbc.rawrepo.RecordData;
import dk.dbc.rawrepo.RecordServiceConnector;
import dk.dbc.rawrepo.RecordServiceConnectorException;
import dk.dbc.updateservice.UpdateServiceDoubleRecordCheckConnector;
import dk.dbc.updateservice.UpdateServiceDoubleRecordCheckConnectorException;
import dk.dbc.weekresolver.WeekResolverResult;
import dk.dbc.weekresolver.WeekresolverConnector;
import dk.dbc.weekresolver.WeekresolverConnectorException;

import javax.ejb.Stateless;
import javax.inject.Inject;

@Stateless
public class ServiceBroker {
    @Inject
    LobbyConnector lobbyConnector;
    @Inject
    UpdateServiceDoubleRecordCheckConnector doubleRecordCheckConnector;
    @Inject
    RecordServiceConnector recordServiceConnector;
    @Inject
    WeekresolverConnector weekresolverConnector;

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

    public RawrepoRecord getMarcRecord(String bibliographicRecordId, int agencyId) throws RecordServiceConnectorException, MarcReaderException {
        final RecordData recordData = recordServiceConnector.getRecordData(agencyId, bibliographicRecordId);
        final MarcRecord marcRecord = MarcRecordFactory.fromMarcXchange(recordData.getContent());
        return new RawrepoRecord(marcRecord);
    }

    public String getCatalogueCode(String catalogueCode) throws WeekresolverConnectorException {
        WeekResolverResult weekResolverResult= weekresolverConnector.getWeekCode(catalogueCode);

        return weekResolverResult.getCatalogueCode();
    }
}
