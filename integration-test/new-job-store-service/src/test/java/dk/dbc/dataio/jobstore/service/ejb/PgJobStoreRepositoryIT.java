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
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import org.junit.Test;

import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class PgJobStoreRepositoryIT extends AbstractJobStoreIT {
    /**
     * Given: a job repository containing chunks from multiple jobs
     * When : the chunks are purged for a job
     * Then : all the chunks associated with the given job are deleted
     * And  : the remaining chunks do not belong to the purged job
     */
    @Test
    public void purgeChunks() {
        // Given...
        final int job1 = 1;
        final int job2 = 2;
        newPersistedChunkEntity(new ChunkEntity.Key(0, job1));
        newPersistedChunkEntity(new ChunkEntity.Key(1, job1));
        newPersistedChunkEntity(new ChunkEntity.Key(0, job2));
        newPersistedChunkEntity(new ChunkEntity.Key(1, job2));
        newPersistedChunkEntity(new ChunkEntity.Key(2, job2));

        // When...
        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreRepository();
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        assertThat("Number of chunks purged", pgJobStoreRepository.purgeChunks(job2), is(3));
        transaction.commit();

        // Then...
        final List<ChunkEntity> remainingChunks = findAllChunks();
        assertThat("Number of remaining chunks", remainingChunks.size(), is(2));

        // And...
        for (ChunkEntity entity : remainingChunks) {
            assertThat("Job ID of remaining chunk", entity.getKey().getJobId(), is(not(job2)));
        }
    }

    /**
     * Given: a job repository containing items from multiple jobs
     * When : the items are purged for a job
     * Then : all the items associated with the given job are deleted
     * And  : the remaining items do not belong to the purged job
     */
    @Test
    public void purgeItems() {
        // Given...
        final int job1 = 1;
        final int job2 = 2;
        final int chunkId = 0;
        newPersistedItemEntity(new ItemEntity.Key(job1, chunkId, (short)0));
        newPersistedItemEntity(new ItemEntity.Key(job1, chunkId, (short)1));
        newPersistedItemEntity(new ItemEntity.Key(job2, chunkId, (short)0));
        newPersistedItemEntity(new ItemEntity.Key(job2, chunkId, (short)1));
        newPersistedItemEntity(new ItemEntity.Key(job2, chunkId, (short)2));

        // When...
        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreRepository();
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        assertThat("Number of items purged", pgJobStoreRepository.purgeItems(job2), is(3));
        transaction.commit();

        // Then...
        final List<ItemEntity> remainingItems = findAllItems();
        assertThat("Number of remaining items", remainingItems.size(), is(2));

        // And...
        for (ItemEntity entity : remainingItems) {
            assertThat("Job ID of remaining item", entity.getKey().getJobId(), is(not(job2)));
        }
    }

    private PgJobStoreRepository newPgJobStoreRepository() {
        final PgJobStoreRepository pgJobStoreRepository = new PgJobStoreRepository();
        pgJobStoreRepository.entityManager = entityManager;
        return pgJobStoreRepository;
    }

    public List<ChunkEntity> findAllChunks() {
        final Query query = entityManager.createQuery("SELECT e FROM ChunkEntity e");
        return (List<ChunkEntity>) query.getResultList();
    }

    public List<ItemEntity> findAllItems() {
        final Query query = entityManager.createQuery("SELECT e FROM ItemEntity e");
        return (List<ItemEntity>) query.getResultList();
    }
}
