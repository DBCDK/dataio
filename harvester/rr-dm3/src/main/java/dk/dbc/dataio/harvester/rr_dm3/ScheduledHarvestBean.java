package dk.dbc.dataio.harvester.rr_dm3;

import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.DependsOn;
import jakarta.ejb.EJB;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timeout;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * This singleton Enterprise Java Bean (EJB) class executes scheduled harvest
 * operations
 */
@Singleton
@Startup
@DependsOn({"TaskRepoDatabaseMigrator", "FlowStoreServiceConnectorBean"})
public class ScheduledHarvestBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledHarvestBean.class);

    final Map<String, Future<Integer>> runningHarvests = new HashMap<>();

    @Resource
    TimerService timerService;

    @EJB
    HarvesterConfigurationBean config;

    @EJB
    HarvesterBean harvester;

    /**
     * Starts default (every 30s) harvest schedule.
     * Note: in the near future this method will probably be removed and
     * scheduled harvesting will not be started by default.
     */
    @PostConstruct
    public void bootstrap() {
        final ScheduleExpression scheduleExpression = new ScheduleExpression();
        scheduleExpression.second("*/5");
        scheduleExpression.minute("*");
        scheduleExpression.hour("*");
        start(scheduleExpression);
    }

    /**
     * (Re)starts harvester with given schedule.
     *
     * @param scheduleExpression harvest schedule
     */
    @Lock(LockType.WRITE)
    public void start(ScheduleExpression scheduleExpression) {
        /* stop current timer (if any) and create new timer
           with given schedule */
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        timerService.createCalendarTimer(scheduleExpression, timerConfig);
    }

    /**
     * Executes harvest operations not already running on each scheduled point in time
     */
    @Timeout
    public void scheduleHarvests() {
        try {
            config.reload();
            final Iterator<Map.Entry<String, Future<Integer>>> iterator = runningHarvests.entrySet().iterator();
            while (iterator.hasNext()) {
                final Map.Entry<String, Future<Integer>> harvest = iterator.next();
                if (harvest.getValue().isDone()) {
                    iterator.remove();
                    try {
                        final Integer recordsHarvested = harvest.getValue().get();
                        LOGGER.info("Scheduled harvest for '{}' harvested {} records",
                                harvest.getKey(), recordsHarvested);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        LOGGER.error("Exception caught from scheduled harvest for '{}'", harvest.getKey(), e);
                    }
                }
            }

            for (RRHarvesterConfig rrHarvesterConfig : config.get()) {
                final String harvesterId = rrHarvesterConfig.getContent().getId();
                if (!runningHarvests.containsKey(harvesterId)) {
                    runningHarvests.put(harvesterId, harvester.harvest(rrHarvesterConfig));
                    LOGGER.debug("Scheduling harvest for '{}'", harvesterId);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Exception caught while scheduling harvests", e);
        }
    }
}
