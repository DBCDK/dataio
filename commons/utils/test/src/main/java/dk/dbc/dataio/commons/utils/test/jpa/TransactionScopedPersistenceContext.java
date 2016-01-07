/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *                                                  66
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

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
        } finally {
            transaction.commit();
        }
    }

    public void run(CodeBlockVoidExecution codeBlock) {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            codeBlock.execute();
        } finally {
            transaction.commit();
        }
    }

    /**
     * Represents a code block execution with return value
     * @param <T> return type of the code block execution
     */
    @FunctionalInterface
    public interface CodeBlockExecution<T> {
        T execute();
    }

    /**
     * Represents a code block execution without return value
     */
    @FunctionalInterface
    public interface CodeBlockVoidExecution {
        void execute();
    }
}
