package dk.dbc.dataio.harvester.corepo;

import dk.dbc.corepo.access.CORepoDAO;
import dk.dbc.corepo.access.CORepoProvider;
import dk.dbc.dataio.commons.types.Pid;
import dk.dbc.opensearch.commons.repository.IRepositoryIdentifier;
import dk.dbc.opensearch.commons.repository.RepositoryException;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.function.Predicate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CORepoConnectorTest {
    private final CORepoProvider coRepoProvider = mock(CORepoProvider.class);
    private final CORepoDAO coRepoDAO = mock(CORepoDAO.class);
    private final CORepoConnector coRepoConnector = new CORepoConnector(coRepoProvider);
    private final Instant from = new Date(1485326980959L).toInstant();
    private final Instant to = new Date(1485339690529L).toInstant();

    @Before
    public void setupMocks() throws RepositoryException {
        when(coRepoProvider.getRepository()).thenReturn(coRepoDAO);
    }

    @Test
    public void noChangesInRepository() throws RepositoryException {
        when(coRepoDAO.searchRepository(anyString())).thenReturn(new IRepositoryIdentifier[0]);

        assertThat(coRepoConnector.getChangesInRepository(from, to, new PidPredicate()),
                is(Collections.emptyList()));
    }

    @Test
    public void predicateAppliedWhenReturningChangesInRepository() throws RepositoryException {
        when(coRepoDAO.searchRepository(anyString())).thenReturn(new IRepositoryIdentifier[] {
                new Identifier("unit:135"),
                new Identifier("870970-basis:test1"),
                new Identifier("work:246"),
                new Identifier("invalid"),
                new Identifier("870970-basis:test2")});

        assertThat(coRepoConnector.getChangesInRepository(from, to, new PidPredicate()),
                is(Arrays.asList(Pid.of("870970-basis:test1"), Pid.of("870970-basis:test2"))));
    }

    @Test
    public void queryForChangesInRepository() throws RepositoryException {
        when(coRepoDAO.searchRepository(anyString())).thenReturn(new IRepositoryIdentifier[0]);

        coRepoConnector.getChangesInRepository(from, to, new PidPredicate());
        verify(coRepoDAO).searchRepository("modified >= 2017-01-25T06:49:40Z AND modified < 2017-01-25T10:21:30Z");
    }

    public static class PidPredicate implements Predicate<Pid> {
        @Override
        public boolean test(Pid pid) {
            return !pid.toString().contains("unit") && !pid.toString().contains("work");
        }
    }

    static class Identifier implements IRepositoryIdentifier {
        private final String value;

        Identifier(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        @Override
        public ObjectType getObjectType() {
            return null;
        }
    }
}
