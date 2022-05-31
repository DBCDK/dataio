package dk.dbc.dataio.commons.utils.service;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class HowRUTest {
    @Test
    public void defaultOk() {
        assertThat(new HowRU().toJson(), is("{\"ok\":true}"));
    }

    @Test
    public void withException() {
        try {
            throw new NullPointerException("death by NPE");
        } catch (NullPointerException e) {
            final HowRU howRU = new HowRU().withException(e);
            assertThat("isOK()", howRU.isOk(), is(false));
            assertThat("getErrorText()", howRU.getErrorText(), is("death by NPE"));
            assertThat("getError()", howRU.getError(), is(notNullValue()));
            assertThat("getError().getMessage()", howRU.getError().getMessage(), is("death by NPE"));
            assertThat("getError().getStacktrace()", howRU.getError().getStacktrace(), is(notNullValue()));
        }
    }
}
