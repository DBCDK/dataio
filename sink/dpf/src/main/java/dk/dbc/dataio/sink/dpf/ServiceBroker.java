package dk.dbc.dataio.sink.dpf;

import dk.dbc.dataio.commons.types.DpfSinkConfig;
import dk.dbc.dataio.sink.dpf.model.DpfRecord;
import dk.dbc.dataio.sink.dpf.model.RawrepoRecord;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import dk.dbc.jsonb.JSONBException;
import dk.dbc.lobby.LobbyConnector;
import dk.dbc.lobby.LobbyConnectorException;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.opennumberroll.OpennumberRollConnector;
import dk.dbc.opennumberroll.OpennumberRollConnectorException;
import dk.dbc.oss.ns.catalogingupdate.BibliographicRecord;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import dk.dbc.rawrepo.dto.RecordDTO;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import dk.dbc.updateservice.UpdateServiceDoubleRecordCheckConnector;
import dk.dbc.updateservice.UpdateServiceDoubleRecordCheckConnectorException;
import dk.dbc.updateservice.dto.BibliographicRecordDTO;
import dk.dbc.updateservice.dto.RecordDataDTO;
import dk.dbc.updateservice.dto.UpdateRecordResponseDTO;
import dk.dbc.weekresolver.WeekResolverConnector;
import dk.dbc.weekresolver.WeekResolverConnectorException;
import dk.dbc.weekresolver.WeekResolverResult;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@Stateless
public class
ServiceBroker {
    @Inject
    LobbyConnector lobbyConnector;
    @Inject
    UpdateServiceDoubleRecordCheckConnector doubleRecordCheckConnector;
    @Inject
    RecordServiceConnector recordServiceConnector;
    @Inject
    WeekResolverConnector weekResolverConnector;
    @Inject
    private OpennumberRollConnector opennumberRollConnector;
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerBean.class);

    @Inject
    @ConfigProperty(name = "UPDATE_SERVICE_WS_URL")
    private String updateServiceWsUrl;

    OpenUpdateServiceConnector openUpdateServiceConnector;

    @EJB
    ConfigBean configBean;
    DpfSinkConfig config;

    private BibliographicRecordFactory bibliographicRecordFactory = new BibliographicRecordFactory();

    public void sendToLobby(DpfRecord dpfRecord) throws LobbyConnectorException, JSONBException {
        lobbyConnector.createOrReplaceApplicant(dpfRecord.toLobbyApplicant());
    }

    public UpdateRecordResponseDTO isDoubleRecord(DpfRecord dpfRecord)
            throws UpdateServiceDoubleRecordCheckConnectorException, JSONBException {
        final byte[] content = MarcRecordFactory.toMarcXchange(dpfRecord.getBody());
        final RecordDataDTO recordDataDTO = new RecordDataDTO();
        recordDataDTO.setContent(Collections.singletonList(new String(content)));

        final BibliographicRecordDTO bibliographicRecordDTO = new BibliographicRecordDTO();
        bibliographicRecordDTO.setRecordSchema("info:lc/xmlns/marcxchange-v1</recordSchema");
        bibliographicRecordDTO.setRecordPacking("xml");
        bibliographicRecordDTO.setRecordDataDTO(recordDataDTO);
        return doubleRecordCheckConnector.doubleRecordCheck(bibliographicRecordDTO);
    }

    public RawrepoRecord getRawrepoRecord(String bibliographicRecordId, int agencyId) throws RecordServiceConnectorException, MarcReaderException {
        final RecordDTO recordData = recordServiceConnector.getRecordData(agencyId, bibliographicRecordId);
        final MarcRecord marcRecord = MarcRecordFactory.fromMarcXchange(recordData.getContent());
        return new RawrepoRecord(marcRecord);
    }

    public boolean rawrepoRecordExists(String bibliographicRecordId, int agencyId) throws RecordServiceConnectorException {
        return recordServiceConnector.recordExists(agencyId, bibliographicRecordId);
    }

    public String getCatalogueCode(String catalogueCode) throws WeekResolverConnectorException {
        final WeekResolverResult weekResolverResult = weekResolverConnector.getWeekCode(catalogueCode);
        final String weekCode = weekResolverResult.getWeekCode();

        if (weekCode == null || !catalogueCode.equals(weekCode.substring(0, 3))) {
            throw new WeekResolverConnectorException("Mismatch between incoming catalogue code (" + catalogueCode + ") and resulting week code (" + weekCode + ")");
        }

        return weekCode;
    }

    public String getNewFaust() throws OpennumberRollConnectorException {
        OpennumberRollConnector.Params params = new OpennumberRollConnector.Params();
        params.withRollName("faust");
        return opennumberRollConnector.getId(params);
    }

    public UpdateRecordResult sendToUpdate(String groupId, String updateTemplate, DpfRecord dpfRecord,
                                           String trackingId, String queueProvider)
            throws BibliographicRecordFactoryException {
        final BibliographicRecord bibliographicRecord =
                bibliographicRecordFactory.toBibliographicRecord(dpfRecord.getBody(), queueProvider);
        return getOpenUpdateServiceConnector().updateRecord(groupId, updateTemplate, bibliographicRecord, trackingId);
    }

    public List<DataField> getUpdateErrors(String errorFieldTag, UpdateRecordResult result, DpfRecord dpfRecord) {
        return openUpdateServiceConnector.toErrorFields(errorFieldTag, result, dpfRecord.getBody());
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
            openUpdateServiceConnector = new OpenUpdateServiceConnector(
                    updateServiceWsUrl,
                    config.getUpdateServiceUserId(),
                    config.getUpdateServicePassword());
        }
        return openUpdateServiceConnector;
    }

}
