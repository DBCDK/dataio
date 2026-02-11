package dk.dbc.dataio.harvester.rr_dm3;

import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.lang.Require;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.HarvesterSourceException;
import dk.dbc.dataio.harvester.types.MarcJSonCollection;
import dk.dbc.dataio.harvester.types.RRV3HarvesterConfig;
import dk.dbc.dataio.harvester.utils.holdingsitems.HoldingsItemsConnector;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepo3Connector;
import dk.dbc.marc.binding.MarcBinding;
import dk.dbc.rawrepo.dto.RecordEntryDTO;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import dk.dbc.vipcore.libraryrules.VipCoreLibraryRulesConnector;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ImsHarvestOperation extends HarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImsHarvestOperation.class);

    private final HoldingsItemsConnector holdingsItemsConnector;

    public ImsHarvestOperation(String workerKey, RRV3HarvesterConfig config, HarvesterJobBuilderFactory harvesterJobBuilderFactory,
                               TaskRepo taskRepo, VipCoreLibraryRulesConnector vipCoreLibraryRulesConnector, MetricRegistry metricRegistry)
            throws QueueException, SQLException, ConfigurationException {

        this(workerKey, config, harvesterJobBuilderFactory, taskRepo,
                new VipCoreConnection(vipCoreLibraryRulesConnector), null, null, null, metricRegistry);
    }

    ImsHarvestOperation(String workerKey, RRV3HarvesterConfig config, HarvesterJobBuilderFactory harvesterJobBuilderFactory, TaskRepo taskRepo,
                        VipCoreConnection vipCoreConnection, RawRepo3Connector rawRepoConnector,
                        HoldingsItemsConnector holdingsItemsConnector, RecordServiceConnector recordServiceConnector,
                        MetricRegistry metricRegistry) throws QueueException, SQLException, ConfigurationException {
        super(workerKey, config, harvesterJobBuilderFactory, taskRepo, vipCoreConnection, rawRepoConnector, recordServiceConnector, metricRegistry);
        this.holdingsItemsConnector = holdingsItemsConnector != null ? holdingsItemsConnector : getHoldingsItemsConnector(config);
    }

    /**
     * Runs this harvest operation, creating dataIO jobs from harvested records.
     * If any non-internal error occurs a record is marked as failed. Only records from
     * IMS agency IDs and DBC library are processed, all others are skipped.
     * Records from DBC library are mapped into IMS libraries with holdings (if any).
     *
     * @return number of records processed
     * @throws HarvesterException on failure to complete harvest operation
     */
    public int execute() throws HarvesterException {
        return execute(vipCoreConnection.getFbsImsLibraries());
    }

    @Override
    void processRecordHarvestTask(RawRepoRecordHarvestTask recordHarvestTask, Set<Integer> imsLibraries) throws HarvesterException {
        for (RawRepoRecordHarvestTask task : unfoldRecordHarvestTask(recordHarvestTask, imsLibraries)) {
            super.processRecordHarvestTask(task, null);
        }
    }

    @Override
    public void close() {
        if (holdingsItemsConnector != null) {
            holdingsItemsConnector.close();
        }
    }

    private boolean isDeletedHeadOrSectionRecord(MarcBinding record) {
        String f004d = record.getSubFieldValue("004", 'r');
        String f004a = record.getSubFieldValue("004", 'a');
        if (f004a != null && "d".equals(f004d)) {
            return "s".equals(f004a) || "h".equals(f004a);
        }
        return false;
    }

    /**
     * This function handles the case where the ims library has a deleted local record attached to a section and/or head record
     * Single records can also have this case, but that is handled in another place (unfoldTaskIMS).
     *
     * @param recordData   Record ids to collect
     * @param addiMetaData Additional record data
     * @return Harvested records
     * @throws HarvesterException Something went wrong getting records
     */
    @Override
    MarcJSonCollection getContentForEnrichedRecord(RecordEntryDTO recordData, AddiMetaData addiMetaData) throws HarvesterException {
        MarcJSonCollection result = super.getContentForEnrichedRecord(recordData, addiMetaData);
        for (MarcBinding record : new ArrayList<>(result.getRecords())) {
            String f001b = record.getSubFieldValue("001", 'b');
            if (f001b != null && !f001b.equals("870970") && isDeletedHeadOrSectionRecord(record)) {
                String f001a = Require.nonNull(record.getSubFieldValue("001", 'a'), () -> new IllegalArgumentException("Record is missing mandatory 001a field"));
                RecordServiceConnector.Params params = new RecordServiceConnector.Params().withExpand(true).withMode(RecordServiceConnector.Params.Mode.EXPANDED);
                RecordIdDTO recordId = new RecordIdDTO(f001a, DBC_LIBRARY);
                try {
                    RecordEntryDTO replaceRecord = recordServiceConnector.getRecordData(recordId, params);
                    result.addMember(replaceRecord.getContent().toString().getBytes(StandardCharsets.UTF_8));
                } catch (RecordServiceConnectorException e) {
                    throw new HarvesterSourceException("Unable to fetch record for " + recordId.getAgencyId() + ":" + recordId.getBibliographicRecordId() + ". " + e.getMessage(), e);
                }
                result.getRecords().remove(record);
            }
        }
        return result;
    }

    private HoldingsItemsConnector getHoldingsItemsConnector(RRV3HarvesterConfig config) throws NullPointerException, IllegalArgumentException {
        return new HoldingsItemsConnector(config.getContent().getImsHoldingsTarget());
    }

    private List<RawRepoRecordHarvestTask> unfoldRecordHarvestTask(RawRepoRecordHarvestTask recordHarvestTask, Set<Integer> imsLibraries) throws HarvesterException {
        final RecordIdDTO recordId = recordHarvestTask.getRecordId();
        List<RawRepoRecordHarvestTask> tasksToProcess = new ArrayList<>();

        if (recordId.getAgencyId() == DBC_LIBRARY) {
            tasksToProcess = unfoldTaskDBC(recordHarvestTask, imsLibraries);
        } else if (imsLibraries.contains(recordId.getAgencyId())) {
            tasksToProcess.add(recordHarvestTask);
        }
        tasksToProcess = unfoldTaskIMS(tasksToProcess);
        return tasksToProcess;
    }


    private List<RawRepoRecordHarvestTask> unfoldTaskDBC(RawRepoRecordHarvestTask recordHarvestTask, Set<Integer> imsLibraries) {
        final List<RawRepoRecordHarvestTask> toProcess = new ArrayList<>();
        final RecordIdDTO recordId = recordHarvestTask.getRecordId();
        System.out.println(recordId);
        final Set<Integer> agenciesWithHoldings = holdingsItemsConnector.hasHoldings(recordId.getBibliographicRecordId(), imsLibraries);
        if (!agenciesWithHoldings.isEmpty()) {
            toProcess.addAll(agenciesWithHoldings.stream()
                    .filter(imsLibraries::contains)
                    .map(agencyId -> new RawRepoRecordHarvestTask()
                            .withRecordId(new RecordIdDTO(recordId.getBibliographicRecordId(), agencyId))
                            .withAddiMetaData(new AddiMetaData()
                                    .withBibliographicRecordId(recordId.getBibliographicRecordId())
                                    .withSubmitterNumber(agencyId)))
                    .toList());
        }
        System.out.println(toProcess);

        return toProcess;
    }

    private List<RawRepoRecordHarvestTask> unfoldTaskIMS(List<RawRepoRecordHarvestTask> recordHarvestTasks) throws HarvesterException {
        int currentRecord = 0;
        final List<RawRepoRecordHarvestTask> toProcess = new ArrayList<>();
        try {
            for (RawRepoRecordHarvestTask repoRecordHarvestTask : recordHarvestTasks) {
                final String bibliographicRecordId = repoRecordHarvestTask.getRecordId().getBibliographicRecordId();
                final int agencyId = repoRecordHarvestTask.getRecordId().getAgencyId();
                RecordEntryDTO record = fetchRecord(repoRecordHarvestTask.getRecordId());
                if (record.isDeleted()) {
                    final boolean hasHolding = !holdingsItemsConnector.hasHoldings(bibliographicRecordId, Set.of(agencyId)).isEmpty();
                    if (hasHolding) {
                        if (recordServiceConnector.recordExists(870970, bibliographicRecordId)) {
                            LOGGER.info("using 870970 record content for deleted record {}", repoRecordHarvestTask.getRecordId());
                            repoRecordHarvestTask.withRecordId(new RecordIdDTO(bibliographicRecordId, 870970));
                            repoRecordHarvestTask.withForceAdd(true);
                            toProcess.add(repoRecordHarvestTask);
                        }
                    } else {
                        LOGGER.info("no holding for deleted record {} - skipping", repoRecordHarvestTask.getRecordId());
                    }
                } else {
                    toProcess.add(repoRecordHarvestTask);
                }
                currentRecord++;
            }
        } catch (RecordServiceConnectorException | HarvesterSourceException e) {
            final RawRepoRecordHarvestTask task = recordHarvestTasks.get(currentRecord);

            // ToDo: 2026-02-10 This is a temporary workaround that should be removed once the RR service is stable.
            final Integer submitter = task.getAddiMetaData().submitterNumber();
            if (submitter == DBC_LIBRARY) {
                // Temporary special case error handling for DBC records.
                // Something broke in the RR service, which means the enrichment trail
                // cannot be deduced. To avoid creating jobs with 191919 submitter,
                // replace with the CATCH_ALL submitter.
                task.getAddiMetaData().withSubmitterNumber(DBC_LIBRARY_CATCH_ALL);
            }

            final String errorMsg = String.format("RawRepo communication failed for %s: %s", task.getRecordId(), e.getMessage());
            task.getAddiMetaData().withDiagnostic(new Diagnostic(Diagnostic.Level.FATAL, errorMsg));
            toProcess.add(task);
        }
        return toProcess;
    }
}
