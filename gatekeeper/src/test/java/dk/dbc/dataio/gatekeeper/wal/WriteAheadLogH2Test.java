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

package dk.dbc.dataio.gatekeeper.wal;

import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WriteAheadLogH2Test {
    private final EntityManager entityManager = mock(EntityManager.class);
    private final EntityTransaction entityTransaction = mock(EntityTransaction.class);
    private final Query query = mock(Query.class);

    @Before
    public void setupMocks() {
        when(entityManager.getTransaction()).thenReturn(entityTransaction);
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
    }

    @Test(expected = NullPointerException.class)
    public void constructorStringArg_walFileArgIsNull_throws() {
        new WriteAheadLogH2((String) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorStringArg_walFileArgIsEmpty_throws() {
        new WriteAheadLogH2(" ");
    }

    @Test
    public void add_whenGivenListOfModifications_persistsEachModificationInSingleTransaction() {
        final List<Modification> modifications = Arrays.asList(new Modification(1L), new Modification(2L));
        final WriteAheadLogH2 wal = new WriteAheadLogH2(entityManager);
        wal.add(modifications);

        verify(entityTransaction).begin();
        verify(entityManager).persist(modifications.get(0));
        verify(entityManager).persist(modifications.get(1));
        verify(entityTransaction).commit();
    }

    @Test
    public void add_whenPersistThrows_transactionRollback() {
        final List<Modification> modifications = Arrays.asList(new Modification(1L), new Modification(2L));
        doThrow(new PersistenceException()).when(entityManager).persist(modifications.get(1));

        final WriteAheadLogH2 wal = new WriteAheadLogH2(entityManager);
        try {
            wal.add(modifications);
            fail("No PersistenceException thrown");
        } catch (PersistenceException e) {
        }

        verify(entityTransaction).begin();
        verify(entityManager).persist(modifications.get(0));
        verify(entityTransaction).rollback();
    }

    @Test
    public void next_queryReturnsNull_returnsNull() throws ModificationLockedException {
        when(query.getResultList()).thenReturn(null);
        final WriteAheadLogH2 wal = new WriteAheadLogH2(entityManager);
        assertThat(wal.next(), is(nullValue()));
    }

    @Test
    public void next_queryReturnsEmptyList_returnsNull() throws ModificationLockedException {
        when(query.getResultList()).thenReturn(Collections.emptyList());
        final WriteAheadLogH2 wal = new WriteAheadLogH2(entityManager);
        assertThat(wal.next(), is(nullValue()));
    }

    @Test
    public void next_queryReturnsModification_locksAndReturnsModification() throws ModificationLockedException {
        final Modification modification = new Modification(42L);
        when(query.getResultList()).thenReturn(Collections.singletonList(modification));
        final WriteAheadLogH2 wal = new WriteAheadLogH2(entityManager);
        final Modification next = wal.next();
        assertThat("next", next, is(modification));
        assertThat("next.isLocked()", next.isLocked(), is(true));

        verify(entityTransaction).begin();
        verify(entityTransaction).commit();
    }

    @Test
    public void next_queryReturnsAlreadyLockedModification_throws() throws ModificationLockedException {
        final Modification modification = new Modification(42L);
        modification.lock();
        when(query.getResultList()).thenReturn(Collections.singletonList(modification));
        final WriteAheadLogH2 wal = new WriteAheadLogH2(entityManager);
        try {
            wal.next();
            fail("No ModificationLockedException thrown");
        } catch (ModificationLockedException e) {
        }
    }

    @Test
    public void delete_modificationArgIsNull_returns() {
        final WriteAheadLogH2 wal = new WriteAheadLogH2(entityManager);
        wal.delete(null);

        verify(entityTransaction, times(0)).begin();
        verify(entityManager, times(0)).remove(any(Modification.class));
        verify(entityTransaction, times(0)).commit();
    }

    @Test
    public void delete_modificationArgIsMonNull_deletesModification() {
        final Modification modification = new Modification(42L);
        final WriteAheadLogH2 wal = new WriteAheadLogH2(entityManager);
        wal.delete(modification);

        verify(entityTransaction).begin();
        verify(entityManager).remove(modification);
        verify(entityTransaction).commit();
    }

    @Test
    public void unlock_modificationArgIsNull_returnsFalse() {
        final WriteAheadLogH2 wal = new WriteAheadLogH2(entityManager);
        assertThat(wal.unlock(null), is(false));
    }

    @Test
    public void unlock_modificationIsNotLocked_returnsFalse() {
        final Modification modification = new Modification(42L);
        final WriteAheadLogH2 wal = new WriteAheadLogH2(entityManager);
        assertThat(wal.unlock(modification), is(false));
    }

    @Test
    public void unlock_modificationIsLocked_unlocksReturnsTrue() {
        final Modification modification = new Modification(42L);
        modification.lock();
        final WriteAheadLogH2 wal = new WriteAheadLogH2(entityManager);
        assertThat("wal.unlock()", wal.unlock(modification), is(true));
        assertThat("modification.isLocked()", modification.isLocked(), is(false));

        verify(entityTransaction).begin();
        verify(entityTransaction).commit();
    }
}
