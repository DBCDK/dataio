package dk.dbc.dataio.sink.dpf;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.DpfSinkConfig;
import dk.dbc.dataio.sink.dpf.model.DpfRecord;
import dk.dbc.dataio.sink.dpf.model.RawrepoRecord;
import dk.dbc.dataio.sink.dpf.transform.BibliographicRecordFactory;
import dk.dbc.dataio.sink.dpf.transform.BibliographicRecordFactoryException;
import dk.dbc.dataio.sink.dpf.transform.MarcRecordFactory;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
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
import dk.dbc.weekresolver.connector.WeekResolverConnector;
import dk.dbc.weekresolver.connector.WeekResolverConnectorException;
import dk.dbc.weekresolver.model.WeekResolverResult;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import static dk.dbc.dataio.sink.dpf.SinkConfig.LOBBY_SERVICE_URL;
import static dk.dbc.dataio.sink.dpf.SinkConfig.OPENNUMBERROLL_SERVICE_URL;
import static dk.dbc.dataio.sink.dpf.SinkConfig.RAWREPO_RECORD_SERVICE_URL;
import static dk.dbc.dataio.sink.dpf.SinkConfig.UPDATE_SERVICE_URL;
import static dk.dbc.dataio.sink.dpf.SinkConfig.UPDATE_SERVICE_WS_URL;
import static dk.dbc.dataio.sink.dpf.SinkConfig.WEEKRESOLVER_SERVICE_URL;

@SuppressWarnings("PMD")
public class ServiceBroker {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumer.class);
    private static final String UPDATE_SERVICE_WS = UPDATE_SERVICE_WS_URL.asString();
    private final BibliographicRecordFactory bibliographicRecordFactory = new BibliographicRecordFactory();
    private final LobbyConnector lobbyConnector;
    private final UpdateServiceDoubleRecordCheckConnector doubleRecordCheckConnector;
    private final RecordServiceConnector recordServiceConnector;
    private final WeekResolverConnector weekResolverConnector;
    private final OpennumberRollConnector opennumberRollConnector;
    private OpenUpdateServiceConnector openUpdateConnector;
    public final ConfigBean configBean;
    private DpfSinkConfig config;

    @SuppressWarnings("java:S2095")
    public ServiceBroker() {
        configBean = new ConfigBean();
        Client client = ClientBuilder.newClient().register(new JacksonFeature());
        lobbyConnector = new LobbyConnector(client, LOBBY_SERVICE_URL.asString());
        doubleRecordCheckConnector = new UpdateServiceDoubleRecordCheckConnector(client, UPDATE_SERVICE_URL.asString());
        recordServiceConnector = new RecordServiceConnector(client, RAWREPO_RECORD_SERVICE_URL.asString());
        weekResolverConnector = new WeekResolverConnector(client, WEEKRESOLVER_SERVICE_URL.asString());
        opennumberRollConnector = new OpennumberRollConnector(client, OPENNUMBERROLL_SERVICE_URL.asString());
    }

    public void sendToLobby(DpfRecord dpfRecord) throws LobbyConnectorException, JsonProcessingException {
        lobbyConnector.createOrReplaceApplicant(dpfRecord.toLobbyApplicant());
    }

    public UpdateRecordResponseDTO isDoubleRecord(DpfRecord dpfRecord) throws UpdateServiceDoubleRecordCheckConnectorException, JSONBException {
        byte[] content = MarcRecordFactory.toMarcXchange(dpfRecord.getBody());
        RecordDataDTO recordDataDTO = new RecordDataDTO();
        recordDataDTO.setContent(Collections.singletonList(new String(content)));

        BibliographicRecordDTO bibliographicRecordDTO = new BibliographicRecordDTO();
        bibliographicRecordDTO.setRecordSchema("info:lc/xmlns/marcxchange-v1</recordSchema");
        bibliographicRecordDTO.setRecordPacking("xml");
        bibliographicRecordDTO.setRecordDataDTO(recordDataDTO);
        return doubleRecordCheckConnector.doubleRecordCheck(bibliographicRecordDTO);
    }

    public RawrepoRecord getRawrepoRecord(String bibliographicRecordId, int agencyId) throws RecordServiceConnectorException, MarcReaderException {
        RecordDTO recordData = recordServiceConnector.getRecordData(agencyId, bibliographicRecordId);
        MarcRecord marcRecord = MarcRecordFactory.fromMarcXchange(recordData.getContent());
        return new RawrepoRecord(marcRecord);
    }

    public boolean rawrepoRecordExists(String bibliographicRecordId, int agencyId) throws RecordServiceConnectorException {
        return recordServiceConnector.recordExists(agencyId, bibliographicRecordId);
    }

    public String getCatalogueCode(String catalogueCode) throws WeekResolverConnectorException {
        WeekResolverResult weekResolverResult = weekResolverConnector.getWeekCode(catalogueCode);
        String weekCode = weekResolverResult.getWeekCode();

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

    public UpdateRecordResult sendToUpdate(String groupId, String updateTemplate, DpfRecord dpfRecord, String trackingId, String queueProvider) throws BibliographicRecordFactoryException {
        BibliographicRecord bibliographicRecord = bibliographicRecordFactory.toBibliographicRecord(dpfRecord.getBody(), queueProvider);
        return getOpenUpdateConnector().updateRecord(groupId, updateTemplate, bibliographicRecord, trackingId);
    }

    public List<DataField> getUpdateErrors(String errorFieldTag, UpdateRecordResult result, DpfRecord dpfRecord) {
        return openUpdateConnector.toErrorFields(errorFieldTag, result, dpfRecord.getBody());
    }

    private boolean isConfigUpdated() {
        DpfSinkConfig latestConfig = configBean.getConfig();
        if (!latestConfig.equals(config)) {
            config = latestConfig;
            return true;
        }
        return false;
    }

    private OpenUpdateServiceConnector getOpenUpdateConnector() {
        if (isConfigUpdated()) {
            LOGGER.debug("Updating update service connector");
            openUpdateConnector = new OpenUpdateServiceConnector(UPDATE_SERVICE_WS, config.getUpdateServiceUserId(), config.getUpdateServicePassword());
        }
        return openUpdateConnector;
    }
}
