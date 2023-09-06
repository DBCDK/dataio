package dk.dbc.dataio.harvester.task;

import dk.dbc.dataio.harvester.task.entity.HarvestTask;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.Optional;

/**
 * This class contains the harvester task repository API
 */
@Stateless
public class TaskRepo {
    @PersistenceContext(unitName = "taskrepo_PU")
    EntityManager entityManager;

    public TaskRepo() {
    }

    public TaskRepo(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    /**
     * Returns next available harvest for specified harvester
     *
     * @param configId configuration ID of specific harvester
     * @return task or empty of none available
     */
    public Optional<HarvestTask> findNextHarvestTask(long configId) {
        return entityManager.createNamedQuery(HarvestTask.QUERY_FIND_NEXT, HarvestTask.class)
                .setParameter("configId", configId)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst();
    }
}
