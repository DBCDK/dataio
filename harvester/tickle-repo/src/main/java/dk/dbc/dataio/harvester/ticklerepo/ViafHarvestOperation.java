
package dk.dbc.dataio.harvester.ticklerepo;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.Field;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.reader.MarcXchangeV1Reader;
import dk.dbc.marc.writer.MarcXchangeV1Writer;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import dk.dbc.ticklerepo.TickleRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static dk.dbc.marc.binding.MarcRecord.hasTag;

public class ViafHarvestOperation extends HarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(ViafHarvestOperation.class);

    private final static String DBC_AGENCY = "191919";
    private final RecordServiceConnector recordServiceConnector;

    public ViafHarvestOperation(TickleRepoHarvesterConfig config,
                                FlowStoreServiceConnector flowStoreServiceConnector,
                                BinaryFileStore binaryFileStore,
                                FileStoreServiceConnector fileStoreServiceConnector,
                                JobStoreServiceConnector jobStoreServiceConnector,
                                TickleRepo tickleRepo, TaskRepo taskRepo,
                                RecordServiceConnector recordServiceConnector) {
        super(config, flowStoreServiceConnector, binaryFileStore, fileStoreServiceConnector,
                jobStoreServiceConnector, tickleRepo, taskRepo);
        this.recordServiceConnector = recordServiceConnector;
    }

    @Override
    AddiRecord createAddiRecord(AddiMetaData addiMetaData, byte[] content) throws HarvesterException {
        try {
            final List<MarcRecord> marcRecords = new ArrayList<>();
            final MarcRecord viafRecord = marcXchangeToMarcRecord(content);
            marcRecords.add(viafRecord);
            final List<MarcRecord> rawRepoRecords = lookupInRawRepo(viafRecord);
            marcRecords.addAll(rawRepoRecords);
            final MarcXchangeV1Writer marcXchangeWriter = new MarcXchangeV1Writer();
            return new AddiRecord(getBytes(addiMetaData),
                    marcXchangeWriter.writeCollection(marcRecords, StandardCharsets.UTF_8));
        } catch (MarcReaderException e) {
            return new AddiRecord(getBytes(addiMetaData
                    .withDiagnostic(new Diagnostic(Diagnostic.Level.FATAL, e.getMessage(), e))),
                    null);
        }
    }

    private MarcRecord marcXchangeToMarcRecord(byte[] bytes) throws MarcReaderException {
        final MarcXchangeV1Reader marcXchangeReader = new MarcXchangeV1Reader(
                    new ByteArrayInputStream(bytes), StandardCharsets.UTF_8);
        return marcXchangeReader.read();
    }

    private List<MarcRecord> lookupInRawRepo(MarcRecord viafRecord)
            throws HarvesterException, MarcReaderException {
        try {
            final List<MarcRecord> rawRepoRecords = new ArrayList<>();
            List<DataField> dataFields = new ArrayList<>();
            dataFields.addAll(viafRecord.getFields(DataField.class, hasTag("700")
                    .and(hasSubFieldValueStartingWith('0', "(DBC)"))));
            dataFields.addAll(viafRecord.getFields(DataField.class, hasTag("710")
                    .and(hasSubFieldValueStartingWith('0', "(DBC)"))));
            for (DataField dataField : dataFields) {
                final String dbcRecordId = getDbcRecordId(dataField);
                LOGGER.info("Looking up 870979/{}", dbcRecordId);
                if (recordServiceConnector.recordExists(DBC_AGENCY, dbcRecordId)) {
                    final byte[] dbcRecord = recordServiceConnector
                            .getRecordContent(DBC_AGENCY, dbcRecordId);
                    rawRepoRecords.add(marcXchangeToMarcRecord(dbcRecord));
                }
            }
            return rawRepoRecords;
        } catch (RuntimeException | RecordServiceConnectorException e) {
            throw new HarvesterException(e);
        }
    }

    private String getDbcRecordId(DataField dataField) {
        for (SubField subField : dataField.getSubfields()) {
            if (subField.getCode() == '0' && subField.getData().startsWith("(DBC)")) {
                return subField.getData().replaceFirst("^\\(DBC\\)870979", "");
            }
        }
        return null;
    }

    private static HasSubFieldValueStartingWith hasSubFieldValueStartingWith(Character code, String prefix) {
        return new HasSubFieldValueStartingWith(code, prefix);
    }

    private static class HasSubFieldValueStartingWith implements Predicate<Field> {
        private final Character code;
        private final String prefix;

        private HasSubFieldValueStartingWith(Character code, String prefix) {
            this.code = code;
            this.prefix = prefix;
        }

        @Override
        public boolean test(Field field) {
            if (!(field instanceof DataField)) {
                return false;
            }
            for (SubField subField : ((DataField) field).getSubfields()) {
                if (subField.getCode() == code && subField.getData().startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }
    }

}
