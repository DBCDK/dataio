package dk.dbc.dataio.commons.types;

import org.junit.Test;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class HarvesterTokenTest {
    @Test
    public void tokenOK() {
        final HarvesterToken token = HarvesterToken.of("raw-repo:42:1:re:main:der");
        assertThat("variant", token.getHarvesterVariant(), is(HarvesterToken.HarvesterVariant.RAW_REPO));
        assertThat("id", token.getId(), is(42L));
        assertThat("version", token.getVersion(), is(1L));
        assertThat("remainder", token.getRemainder(), is("re:main:der"));
    }

    @Test
    public void tokenWithoutRemainder() {
        final HarvesterToken token = HarvesterToken.of("raw-repo:42:1");
        assertThat("remainder", token.getRemainder(), is(nullValue()));
    }

    @Test
    public void tokenWithTooFewParts_throws() {
        assertThat(() -> HarvesterToken.of("raw-repo:42"), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void tokenWithUnknownVariant_throws() {
        assertThat(() -> HarvesterToken.of("unknown:42:1"), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void tokenWithIllegalId() {
        assertThat(() -> HarvesterToken.of("raw-repo:illegalId:1"), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void tokenWithIllegalVersion() {
        assertThat(() -> HarvesterToken.of("raw-repo:42:illegalVersion"), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void builder() {
        final HarvesterToken token = new HarvesterToken()
                .withHarvesterVariant(HarvesterToken.HarvesterVariant.RAW_REPO)
                .withId(42L)
                .withVersion(1L)
                .withRemainder("re:main:der");
        assertThat(token.toString(), is("raw-repo:42:1:re:main:der"));
    }
}
