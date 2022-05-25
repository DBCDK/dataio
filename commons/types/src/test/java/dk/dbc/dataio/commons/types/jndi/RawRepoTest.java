package dk.dbc.dataio.commons.types.jndi;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class RawRepoTest {
    @Test
    public void fromName() {
        assertThat("basismig", RawRepo.fromString("Basismig"), is(RawRepo.BASISMIG));
        assertThat("boblebad", RawRepo.fromString("BOBLEBAD"), is(RawRepo.BOBLEBAD));
        assertThat("cisterne", RawRepo.fromString("cisterne"), is(RawRepo.CISTERNE));
        assertThat("fbstest", RawRepo.fromString("fBsTeSt"), is(RawRepo.FBSTEST));
    }

    @Test
    public void fromJndiResourceName() {
        assertThat("basismig", RawRepo.fromString("jdbc/dataio/basis-rawrepo"), is(RawRepo.BASISMIG));
        assertThat("boblebad", RawRepo.fromString("jdbc/dataio/rawrepo-boblebad"), is(RawRepo.BOBLEBAD));
        assertThat("cisterne", RawRepo.fromString("jdbc/dataio/rawrepo-cisterne"), is(RawRepo.CISTERNE));
        assertThat("fbstest", RawRepo.fromString("jdbc/dataio/rawrepo-exttest"), is(RawRepo.FBSTEST));
    }

    @Test
    public void getJndiResourceName() {
        assertThat("basismig", RawRepo.BASISMIG.getJndiResourceName(), is("jdbc/dataio/basis-rawrepo"));
        assertThat("boblebad", RawRepo.BOBLEBAD.getJndiResourceName(), is("jdbc/dataio/rawrepo-boblebad"));
        assertThat("cisterne", RawRepo.CISTERNE.getJndiResourceName(), is("jdbc/dataio/rawrepo-cisterne"));
        assertThat("fbstest", RawRepo.FBSTEST.getJndiResourceName(), is("jdbc/dataio/rawrepo-exttest"));
    }
}
