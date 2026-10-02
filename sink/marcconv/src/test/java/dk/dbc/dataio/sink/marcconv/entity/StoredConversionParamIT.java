package dk.dbc.dataio.sink.marcconv.entity;

import dk.dbc.dataio.commons.conversion.ConversionParam;
import dk.dbc.dataio.sink.marcconv.IntegrationTest;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class StoredConversionParamIT extends IntegrationTest {
    private static final int JOB_ID = 42;

    private final ConversionParam first = new ConversionParam().withPackaging("iso").withEncoding("danmarc2");
    private final ConversionParam second = new ConversionParam().withPackaging("iso").withEncoding("latin1");

    @Test
    public void storesParamsWhenTheJobHasNoneStoredYet() {
        int stored = env().getPersistenceContext().run(() ->
                StoredConversionParam.insertIfAbsent(env().getEntityManager(), JOB_ID, first));

        assertThat("rows stored", stored, is(1));
        assertThat("ConversionParam", findParam().getParam(), is(first));
    }

    @Test
    public void discardsASecondWritersParamsRatherThanFailing() {
        // The statement the racing writers rely on: the loser is answered with a row
        // count rather than a constraint violation, which would abort its whole
        // transaction and with it the item it was converting.
        env().getPersistenceContext().run(() ->
                StoredConversionParam.insertIfAbsent(env().getEntityManager(), JOB_ID, first));

        int stored = env().getPersistenceContext().run(() ->
                StoredConversionParam.insertIfAbsent(env().getEntityManager(), JOB_ID, second));

        assertThat("rows stored", stored, is(0));
        assertThat("ConversionParam", findParam().getParam(), is(first));
    }

    private StoredConversionParam findParam() {
        return env().getPersistenceContext().run(() ->
                env().getEntityManager().find(StoredConversionParam.class, JOB_ID));
    }
}
