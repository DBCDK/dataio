package dk.dbc.dataio.harvester.rr_dm3;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.HarvesterRecord;
import dk.dbc.dataio.harvester.types.HarvesterSourceException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.utils.holdingsitems.HoldingsItemsConnector;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.marc.binding.MarcRecord;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ImsHarvestOperation extends HarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImsHarvestOperation.class);

    private final HoldingsItemsConnector holdingsItemsConnector;

    public ImsHarvestOperation(RRHarvesterConfig config,
                               HarvesterJobBuilderFactory harvesterJobBuilderFactory,
                               TaskRepo taskRepo, VipCoreLibraryRulesConnector vipCoreLibraryRulesConnector, MetricRegistry metricRegistry)
            throws NullPointerException, IllegalArgumentException, QueueException, SQLException, ConfigurationException {
        this(config, harvesterJobBuilderFactory, taskRepo,
                new VipCoreConnection(vipCoreLibraryRulesConnector), null, null, null, metricRegistry);
    }

    ImsHarvestOperation(RRHarvesterConfig config, HarvesterJobBuilderFactory harvesterJobBuilderFactory, TaskRepo taskRepo,
                        VipCoreConnection vipCoreConnection, RawRepoConnector rawRepoConnector,
                        HoldingsItemsConnector holdingsItemsConnector, RecordServiceConnector recordServiceConnector,
                        MetricRegistry metricRegistry)
            throws QueueException, SQLException, ConfigurationException {
        super(config, harvesterJobBuilderFactory, taskRepo, vipCoreConnection, rawRepoConnector, recordServiceConnector, metricRegistry);
        this.holdingsItemsConnector = holdingsItemsConnector != null ? holdingsItemsConnector : getHoldingsItemsConnector(config);
    }

    /**
     * Runs this harvest operation, creating dataIO jobs from harvested records.
     * If any non-internal error occurs a record is marked as failed. Only records from
     * IMS agency IDs are processed and DBC library are process, all others are skipped.
     * Records from DBC library are mapped into IMS libraries with holdings (if any).
     *
     * @return number of records processed
     * @throws HarvesterException on failure to complete harvest operation
     */
    @Override
    public int execute() throws HarvesterException {
        final StopWatch stopWatch = new StopWatch();
        final RecordHarvestTaskQueue recordHarvestTaskQueue = createTaskQueue();
        // Since we might (re)run batches with a size larger than the one currently configured
        final int batchSize = Math.max(configContent.getBatchSize(), recordHarvestTaskQueue.estimatedSize());

        if (recordHarvestTaskQueue.isEmpty()) return 0;
        Set<Integer> imsLibraries = vipCoreConnection.getFbsImsLibraries();

        int itemsProcessed = 0;
        RawRepoRecordHarvestTask recordHarvestTask = recordHarvestTaskQueue.poll();
        while (recordHarvestTask != null) {
            LOGGER.info("{} ready for harvesting", recordHarvestTask.getRecordId());

            // There is quite a bit of waisted effort being done here when
            // the workload contains more than one record, since we will actually be
            // fetching/merging the same record more than once. Fixing this entails
            // either an ImsHarvestOperation implementation having very little code
            // in common with HarvestOperation or a complete rewrite of the
            // HarvestOperation class, neither of which we have the time for
            // currently.
            for (RawRepoRecordHarvestTask task : unfoldRecordHarvestTask(recordHarvestTask, imsLibraries)) {
                processRecordHarvestTask(task);
            }

            if (++itemsProcessed == batchSize) {
                break;
            }
            recordHarvestTask = recordHarvestTaskQueue.poll();
        }
        flushHarvesterJobBuilders();

        recordHarvestTaskQueue.commit();

        LOGGER.info("Processed {} items from {} queue in {} ms",
                itemsProcessed, configContent.getConsumerId(), stopWatch.getElapsedTime());

        return itemsProcessed;
    }

    @Override
    public void close() {
        if (holdingsItemsConnector != null) {
            holdingsItemsConnector.close();
        }
    }

    private boolean isDeletedHeadOrSectionRecord(MarcRecord record) {
        final Optional<String> f004d = record.getSubFieldValue("004", 'r');
        final Optional<String> f004a = record.getSubFieldValue("004", 'a');
        if (f004d.isPresent() && f004a.isPresent() && "d".equals(f004d.get())) {
            return "s".equals(f004a.get()) || "h".equals(f004a.get());
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
    HarvesterRecord getContentForEnrichedRecord(RecordEntryDTO recordData, AddiMetaData addiMetaData) throws HarvesterException {
        HarvesterRecord result = super.getContentForEnrichedRecord(recordData, addiMetaData);
        for (MarcRecord record : new ArrayList<>(result.getRecords())) {
            final Optional<String> f001b = record.getSubFieldValue("001", 'b');
            if (f001b.isPresent() && !f001b.get().equals("870970") && isDeletedHeadOrSectionRecord(record)) {
                String f001a = record.getSubFieldValue("001", 'a').orElseThrow(() -> new IllegalArgumentException("Record is missing mandatory 001a field"));
                RecordServiceConnector.Params params = new RecordServiceConnector.Params().withExpand(true).withMode(RecordServiceConnector.Params.Mode.EXPANDED);
                RecordIdDTO recordId = new RecordIdDTO(f001a, HarvestOperation.DBC_LIBRARY);
                try {
                    RecordEntryDTO replaceRecord = rawRepoRecordServiceConnector.getRecordData(recordId, params);
                    result.addMember(replaceRecord.getContent().toString().getBytes(StandardCharsets.UTF_8));
                } catch (RecordServiceConnectorException e) {
                    throw new HarvesterSourceException("Unable to fetch record for " + recordId.getAgencyId() + ":" + recordId.getBibliographicRecordId() + ". " + e.getMessage(), e);
                }
                result.getRecords().remove(record);
            }
        }
        return result;
    }

    private HoldingsItemsConnector getHoldingsItemsConnector(RRHarvesterConfig config) throws NullPointerException, IllegalArgumentException {
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
                    final boolean hasHolding = !holdingsItemsConnector.hasHoldings(bibliographicRecordId, new HashSet<>(Collections.singletonList(agencyId))).isEmpty();
                    if (hasHolding) {
                        if (rawRepoRecordServiceConnector.recordExists(870970, bibliographicRecordId)) {
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
            final String errorMsg = String.format("RawRepo communication failed for %s: %s", task.getRecordId(), e.getMessage());
            task.getAddiMetaData().withDiagnostic(new Diagnostic(Diagnostic.Level.FATAL, errorMsg));
            toProcess.add(task);
        }
        return toProcess;
    }
}
