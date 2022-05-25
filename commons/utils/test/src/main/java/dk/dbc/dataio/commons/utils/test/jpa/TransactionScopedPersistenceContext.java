package dk.dbc.dataio.commons.utils.test.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

/**
 * Class used to execute blocks of code given as lambda expressions
 * inside a transaction scoped persistence context
 */
public class TransactionScopedPersistenceContext {
    private final EntityManager entityManager;

    public TransactionScopedPersistenceContext(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public <T> T run(CodeBlockExecution<T> codeBlock) {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            return codeBlock.execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            transaction.commit();
        }
    }

    public void run(CodeBlockVoidExecution codeBlock) {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            codeBlock.execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            transaction.commit();
        }
    }

    /**
     * Represents a code block execution with return value
     *
     * @param <T> return type of the code block execution
     */
    @FunctionalInterface
    public interface CodeBlockExecution<T> {
        T execute() throws Exception;
    }

    /**
     * Represents a code block execution without return value
     */
    @FunctionalInterface
    public interface CodeBlockVoidExecution {
        void execute() throws Exception;
    }
}
