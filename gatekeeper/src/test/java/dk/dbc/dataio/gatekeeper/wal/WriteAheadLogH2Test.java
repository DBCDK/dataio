package dk.dbc.dataio.gatekeeper.wal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @BeforeEach
    public void setupMocks() {
        when(entityManager.getTransaction()).thenReturn(entityTransaction);
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
    }

    @Test
    public void constructorStringArg_walFileArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new WriteAheadLogH2((String) null));
    }

    @Test
    public void constructorStringArg_walFileArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new WriteAheadLogH2(" "));
    }

    @Test
    public void add_whenGivenListOfModifications_persistsEachModificationInSingleTransaction() {
        List<Modification> modifications = Arrays.asList(new Modification(1L), new Modification(2L));
        WriteAheadLogH2 wal = new WriteAheadLogH2(entityManager);
        wal.add(modifications);

        verify(entityTransaction).begin();
        verify(entityManager).persist(modifications.get(0));
        verify(entityManager).persist(modifications.get(1));
        verify(entityTransaction).commit();
    }

    @Test
    public void add_whenPersistThrows_transactionRollback() {
        List<Modification> modifications = Arrays.asList(new Modification(1L), new Modification(2L));
        doThrow(new PersistenceException()).when(entityManager).persist(modifications.get(1));

        WriteAheadLogH2 wal = new WriteAheadLogH2(entityManager);
        assertThrows(PersistenceException.class, () -> wal.add(modifications));
        verify(entityTransaction).begin();
        verify(entityManager).persist(modifications.get(0));
        verify(entityTransaction).rollback();
    }

    @Test
    public void next_queryReturnsNull_returnsNull() throws ModificationLockedException {
        when(query.getResultList()).thenReturn(null);
        WriteAheadLogH2 wal = new WriteAheadLogH2(entityManager);
        assertThat(wal.next(), is(nullValue()));
    }

    @Test
    public void next_queryReturnsEmptyList_returnsNull() throws ModificationLockedException {
        when(query.getResultList()).thenReturn(Collections.emptyList());
        WriteAheadLogH2 wal = new WriteAheadLogH2(entityManager);
        assertThat(wal.next(), is(nullValue()));
    }

    @Test
    public void next_queryReturnsModification_locksAndReturnsModification() throws ModificationLockedException {
        Modification modification = new Modification(42L);
        when(query.getResultList()).thenReturn(Collections.singletonList(modification));
        WriteAheadLogH2 wal = new WriteAheadLogH2(entityManager);
        Modification next = wal.next();
        assertThat("next", next, is(modification));
        assertThat("next.isLocked()", next.isLocked(), is(true));

        verify(entityTransaction).begin();
        verify(entityTransaction).commit();
    }

    @Test
    public void next_queryReturnsAlreadyLockedModification_throws() throws ModificationLockedException {
        Modification modification = new Modification(42L);
        modification.lock();
        when(query.getResultList()).thenReturn(Collections.singletonList(modification));
        WriteAheadLogH2 wal = new WriteAheadLogH2(entityManager);
        assertThrows(ModificationLockedException.class, wal::next);
    }

    @Test
    public void delete_modificationArgIsNull_returns() {
        WriteAheadLogH2 wal = new WriteAheadLogH2(entityManager);
        wal.delete(null);

        verify(entityTransaction, times(0)).begin();
        verify(entityManager, times(0)).remove(any(Modification.class));
        verify(entityTransaction, times(0)).commit();
    }

    @Test
    public void delete_modificationArgIsMonNull_deletesModification() {
        Modification modification = new Modification(42L);
        WriteAheadLogH2 wal = new WriteAheadLogH2(entityManager);
        wal.delete(modification);

        verify(entityTransaction).begin();
        verify(entityManager).remove(modification);
        verify(entityTransaction).commit();
    }

    @Test
    public void unlock_modificationArgIsNull_returnsFalse() {
        WriteAheadLogH2 wal = new WriteAheadLogH2(entityManager);
        assertThat(wal.unlock(null), is(false));
    }

    @Test
    public void unlock_modificationIsNotLocked_returnsFalse() {
        Modification modification = new Modification(42L);
        WriteAheadLogH2 wal = new WriteAheadLogH2(entityManager);
        assertThat(wal.unlock(modification), is(false));
    }

    @Test
    public void unlock_modificationIsLocked_unlocksReturnsTrue() {
        Modification modification = new Modification(42L);
        modification.lock();
        WriteAheadLogH2 wal = new WriteAheadLogH2(entityManager);
        assertThat("wal.unlock()", wal.unlock(modification), is(true));
        assertThat("modification.isLocked()", modification.isLocked(), is(false));

        verify(entityTransaction).begin();
        verify(entityTransaction).commit();
    }
}
