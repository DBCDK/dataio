package dk.dbc.dataio.harvester.rr_dm3;

import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRV3HarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepo3Connector;
import dk.dbc.ocnrepo.OcnRepo;
import dk.dbc.ocnrepo.dto.WorldCatEntity;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.vipcore.libraryrules.VipCoreLibraryRulesConnector;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class WorldCatHarvestOperation extends HarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldCatHarvestOperation.class);

    private final OcnRepo ocnRepo;

    public WorldCatHarvestOperation(String workerKey, RRV3HarvesterConfig config, HarvesterJobBuilderFactory harvesterJobBuilderFactory,
                                    TaskRepo taskRepo, VipCoreLibraryRulesConnector vipCoreLibraryRulesConnector, OcnRepo ocnRepo, MetricRegistry metricRegistry)
            throws SQLException, QueueException, ConfigurationException {
        this(workerKey, config, harvesterJobBuilderFactory, taskRepo,
                new VipCoreConnection(vipCoreLibraryRulesConnector), null, ocnRepo, null, metricRegistry);
    }

    WorldCatHarvestOperation(String workerKey, RRV3HarvesterConfig config, HarvesterJobBuilderFactory harvesterJobBuilderFactory, TaskRepo taskRepo,
                             VipCoreConnection vipCoreConnection, RawRepo3Connector rawRepoConnector,
                             OcnRepo ocnRepo, RecordServiceConnector recordServiceConnector, MetricRegistry metricRegistry)
            throws SQLException, QueueException, ConfigurationException {
        super(workerKey, config, harvesterJobBuilderFactory, taskRepo, vipCoreConnection, rawRepoConnector, recordServiceConnector, metricRegistry);
        this.ocnRepo = ocnRepo;
    }

    @Override
    void processRecordHarvestTask(RawRepoRecordHarvestTask recordHarvestTask, Set<Integer> agencyFilter) throws HarvesterException {
        for (RawRepoRecordHarvestTask task : preprocessRecordHarvestTask(recordHarvestTask)) {
            LOGGER.info("handling pid {} ocn {}", task.getAddiMetaData().pid(), task.getAddiMetaData().ocn());
            super.processRecordHarvestTask(task, agencyFilter);
        }
    }

    /* One record harvest task may be expanded into multiple tasks based on ocn-repo lookup */
    public List<RawRepoRecordHarvestTask> preprocessRecordHarvestTask(RawRepoRecordHarvestTask task) {
        final List<RawRepoRecordHarvestTask> tasks = getWorldCatEntities(task).stream()
                .map(worldCatEntity -> mergeTaskWithWorldCatEntity(task, worldCatEntity))
                .filter(t -> hasPid(t.getAddiMetaData()))
                .collect(Collectors.toList());

        if (tasks.isEmpty() && hasPid(task.getAddiMetaData())) {
            // no existing entry in ocn-repo, use original task
            tasks.add(task);
        }

        return tasks;
    }

    private List<WorldCatEntity> getWorldCatEntities(RawRepoRecordHarvestTask task) {
        final AddiMetaData addiMetaData = task.getAddiMetaData();
        if (hasPid(addiMetaData)) {
            // single entity from exact ocn-repo key lookup
            return ocnRepo.lookupWorldCatEntity(new WorldCatEntity()
                    .withPid(addiMetaData.pid()));
        }
        // potentially multiple entities from agencyId/bibliographicRecordId query
        return ocnRepo.lookupWorldCatEntity(new WorldCatEntity()
                .withAgencyId(addiMetaData.submitterNumber())
                .withBibliographicRecordId(addiMetaData.bibliographicRecordId()));
    }

    private RawRepoRecordHarvestTask mergeTaskWithWorldCatEntity(RawRepoRecordHarvestTask task, WorldCatEntity worldCatEntity) {
        return new RawRepoRecordHarvestTask()
                .withRecordId(task.getRecordId())
                .withAddiMetaData(new AddiMetaData()
                        .withPid(worldCatEntity.getPid())
                        .withOcn(worldCatEntity.getOcn())
                        .withSubmitterNumber(worldCatEntity.getAgencyId())
                        .withBibliographicRecordId(worldCatEntity.getBibliographicRecordId()));
    }

    private boolean hasPid(AddiMetaData addiMetaData) {
        return addiMetaData.pid() != null && !addiMetaData.pid().trim().isEmpty();
    }
}
