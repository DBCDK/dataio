package dk.dbc.dataio.gatekeeper.wal;

import dk.dbc.invariant.InvariantUtil;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.Persistence;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;

/**
 * WriteAheadLog implementation using a h2 database as backing store
 */
public class WriteAheadLogH2 implements WriteAheadLog {
    final EntityManager entityManager;

    public WriteAheadLogH2() {
        this("./gatekeeper.wal");
    }

    public WriteAheadLogH2(String walFile) throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(walFile, "walFile");
        final Map<String, String> properties = new HashMap<>();
        properties.put(JDBC_USER, "gatekeeper");
        properties.put(JDBC_PASSWORD, "gatekeeper");
        properties.put(JDBC_URL, String.format("jdbc:h2:file:%s", walFile));
        properties.put(JDBC_DRIVER, "org.h2.Driver");
        properties.put("eclipselink.logging.level", "FINE");
        final EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("gatekeeperWAL", properties);
        entityManager = entityManagerFactory.createEntityManager(properties);
        entityManager.setFlushMode(FlushModeType.COMMIT);
    }

    /**
     * Package scoped constructor used for unit testing purposes
     *
     * @param entityManager entity manager to be injected
     */
    WriteAheadLogH2(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void add(List<Modification> modifications) {
        final EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            for (Modification modification : modifications) {
                entityManager.persist(modification);
            }
            transaction.commit();
        } catch (RuntimeException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    @Override
    public Modification next() throws ModificationLockedException {
        final Modification modification = getNextModificationOrNull();
        if (modification != null) {
            if (modification.isLocked()) {
                throw new ModificationLockedException(modification.toString());
            } else {
                final EntityTransaction transaction = entityManager.getTransaction();
                transaction.begin();
                modification.lock();
                transaction.commit();
            }
        }
        return modification;
    }

    @Override
    public void delete(Modification modification) {
        if (modification != null) {
            final EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();
            entityManager.remove(modification);
            transaction.commit();
        }
    }

    @Override
    public boolean unlock(Modification modification) {
        if (modification != null && modification.isLocked()) {
            final EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();
            modification.unlock();
            transaction.commit();
            return true;
        }
        return false;
    }


    private Modification getNextModificationOrNull() {
        @SuppressWarnings("unchecked") final List<Modification> result = entityManager
                .createQuery("SELECT modification FROM Modification modification ORDER BY modification.id ASC")
                .setMaxResults(1)
                .getResultList();
        if (result == null || result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }
}
