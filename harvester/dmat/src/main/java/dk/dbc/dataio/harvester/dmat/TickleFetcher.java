package dk.dbc.dataio.harvester.dmat;

import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dmat.service.persistence.DMatRecord;
import dk.dbc.ticklerepo.TickleRepo;
import dk.dbc.ticklerepo.dto.DataSet;
import dk.dbc.ticklerepo.dto.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class TickleFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(TickleFetcher.class);

    public byte[] getOnixProductFor(DMatRecord dMatRecord, TickleRepo tickleRepo, String dataSetName)
            throws HarvesterException {
        LOGGER.info("Looking up original Publizon record with id '{}' in tickle-repo", dMatRecord.getIsbn());
        Optional<DataSet> dataSet = tickleRepo.lookupDataSet(
                new DataSet()
                        .withName(dataSetName));
        Optional<Record> record;
        if (dataSet.isPresent()) {
            record = tickleRepo.lookupRecord(
                    new Record()
                            .withDataset(dataSet.get().getId())
                            .withLocalId(dMatRecord.getIsbn()));
            if (record.isPresent()) {
                return record.get().getContent();
            } else {
                throw new HarvesterException(String.format(
                        "Unable to fetch dmat record with submitter '%s' and id '%s' from dataset '%s' from ticklerepo",
                        JobSpecificationTemplate.SUBMITTER_NUMBER_PUBLIZON, dMatRecord.getIsbn(), dataSetName));
            }
        } else {
            throw new HarvesterException(String.format(
                    "Unable to look up agencyid '%s' in ticklerepo.",
                    JobSpecificationTemplate.SUBMITTER_NUMBER_PUBLIZON));
        }
    }
}
