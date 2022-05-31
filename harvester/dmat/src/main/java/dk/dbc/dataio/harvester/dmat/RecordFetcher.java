package dk.dbc.dataio.harvester.dmat;

import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.MarcExchangeCollection;
import dk.dbc.dmat.service.persistence.DMatRecord;
import dk.dbc.dmat.service.persistence.enums.Selection;
import dk.dbc.dmat.service.persistence.enums.UpdateCode;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class RecordFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordFetcher.class);

    public static byte[] getRecordCollectionFor(RecordServiceConnector recordServiceConnector, DMatRecord dMatRecord)
            throws RecordServiceConnectorException, HarvesterException {
        String recordId = getAttachedRecordId(dMatRecord);

        // Fetch record and wrap in a collection (although there's always only one record)
        if (recordId != null) {
            LOGGER.info("Fetch attached record {}:191919", recordId);
            return recordServiceConnector.getRecordContentCollection(191919, recordId,
                    new RecordServiceConnector.Params()
                            .withMode(RecordServiceConnector.Params.Mode.EXPANDED)
                            .withKeepAutFields(true)
                            .withUseParentAgency(true)
                            .withExpand(true));
        } else {
            LOGGER.info("No attached record for updateCode {} and selection {}", dMatRecord.getUpdateCode(), dMatRecord.getSelection());
            return new MarcExchangeCollection().emptyCollection();
        }
    }

    private static String getAttachedRecordId(DMatRecord dMatRecord) throws HarvesterException {

        // updateCode NEW and AUTO with selection CREATE => no record
        if (Arrays.asList(UpdateCode.NEW, UpdateCode.AUTO).contains(dMatRecord.getUpdateCode())
                && dMatRecord.getSelection() == Selection.CREATE) {
            return null;
        }

        // updateCode NEW and AUTO with selection CLONE => matched record
        if (Arrays.asList(UpdateCode.NEW, UpdateCode.AUTO).contains(dMatRecord.getUpdateCode())
                && dMatRecord.getSelection() == Selection.CLONE) {
            return dMatRecord.getMatch();
        }

        // updateCode ACT with selection CREATE => no record
        if (dMatRecord.getUpdateCode() == UpdateCode.ACT && dMatRecord.getSelection() == Selection.CREATE) {
            return null;
        }

        // updateCode NNB with selection DROP and AUTODROP => no record
        if (dMatRecord.getUpdateCode() == UpdateCode.NNB
                && Arrays.asList(Selection.DROP, Selection.AUTODROP).contains(dMatRecord.getSelection())) {
            return null;
        }

        // updateCode REVIEW with any selection => LU record
        if (dMatRecord.getUpdateCode() == UpdateCode.REVIEW) {
            return dMatRecord.getReviewId();
        }

        // updateCode UPDATE with any selection => own record
        if (dMatRecord.getUpdateCode() == UpdateCode.UPDATE) {
            return dMatRecord.getRecordId();
        }

        // Invalid combination of updateCode and selection
        LOGGER.error("Attempt to fetch RR record for invalid combination of updateCode {} and selection {}",
                dMatRecord.getUpdateCode(), dMatRecord.getSelection());
        throw new HarvesterException(
                String.format("Attempt to fetch RR record for invalid combination of updateCode %s and selection %s",
                        dMatRecord.getUpdateCode(), dMatRecord.getSelection()));
    }
}
