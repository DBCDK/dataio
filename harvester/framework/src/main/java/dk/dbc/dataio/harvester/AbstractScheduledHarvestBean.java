package dk.dbc.dataio.harvester;

import dk.dbc.dataio.harvester.types.HarvesterConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.Timeout;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Abstract base class for time scheduled harvests
 *
 * @param <T> type parameter for harvester bean
 * @param <U> type parameter for harvester configuration type
 * @param <V> type parameter for harvester configuration bean
 */
public abstract class AbstractScheduledHarvestBean<T extends AbstractHarvesterBean<T, U>, U extends HarvesterConfig<?>, V extends AbstractHarvesterConfigurationBean<U>> {
    final protected Map<String, Future<Integer>> runningHarvests = new HashMap<>();

    @Resource
    TimerService timerService;

    protected T harvesterBeanImpl;
    protected V harvesterConfigurationBeanImpl;

    /**
     * Starts default harvest schedule.
     */
    @PostConstruct
    public void bootstrap() {
        harvesterBeanImpl = getHarvesterBeanImpl();
        harvesterConfigurationBeanImpl = getHarvesterConfigurationBeanImpl();
        start(getTimerSchedule());
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
            final Iterator<Map.Entry<String, Future<Integer>>> iterator = runningHarvests.entrySet().iterator();
            while (iterator.hasNext()) {
                final Map.Entry<String, Future<Integer>> harvest = iterator.next();
                if (harvest.getValue().isDone()) {
                    iterator.remove();
                    try {
                        final Integer recordsHarvested = harvest.getValue().get();
                        getLogger().info("Scheduled harvest for '{}' harvested {} records",
                                harvest.getKey(), recordsHarvested);
                    } catch (Exception e) {
                        getLogger().error("Exception caught from scheduled harvest for '{}'", harvest.getKey(), e);
                    }
                }
            }

            harvesterConfigurationBeanImpl.reload();

            for (U config : harvesterConfigurationBeanImpl.getConfigs()) {
                final String harvesterId = config.getLogId();
                if (!runningHarvests.containsKey(harvesterId) && canRun(config)) {
                    runningHarvests.put(harvesterId, harvesterBeanImpl.harvest(config));
                    getLogger().debug("Scheduling harvest for '{}'", harvesterId);
                }
            }
        } catch (Exception e) {
            getLogger().error("Exception caught while scheduling harvests", e);
        }
    }

    /**
     * Can be overridden if additional tests needs to
     * be executed in order to determine if a scheduled
     * harvest may run
     *
     * @param config harvest config
     * @return always true in this default implementation
     */
    public boolean canRun(U config) {
        return true;
    }

    /**
     * @return AbstractHarvesterBean implementation
     */
    public abstract T getHarvesterBeanImpl();

    /**
     * @return AbstractHarvesterConfigurationBean implementation
     */
    public abstract V getHarvesterConfigurationBeanImpl();

    /**
     * @return timer schedule
     */
    public abstract ScheduleExpression getTimerSchedule();

    /**
     * @return Logger
     */
    public abstract Logger getLogger();
}
