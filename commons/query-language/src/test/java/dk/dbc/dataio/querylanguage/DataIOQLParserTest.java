package dk.dbc.dataio.querylanguage;

import org.junit.jupiter.api.Test;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DataIOQLParserTest {
    private final DataIOQLParser ioqlParser = new DataIOQLParser();

    @Test
    public void equalsOperator() throws ParseException {
        String query = ioqlParser.parse("job:id = 42");
        assertThat(query, is("SELECT * FROM job WHERE id = 42"));
    }

    @Test
    public void greaterThanOperator() throws ParseException {
        String query = ioqlParser.parse("job:id > 42");
        assertThat(query, is("SELECT * FROM job WHERE id > 42"));
    }

    @Test
    public void greaterThanOrEqualToOperator() throws ParseException {
        String query = ioqlParser.parse("job:id >= 42");
        assertThat(query, is("SELECT * FROM job WHERE id >= 42"));
    }

    @Test
    public void jsonLeftContainsOperator() throws ParseException {
        String query = ioqlParser.parse("job:specification @> '{\"dataFile\": \"urn:dataio-fs:1268210\"}'");
        assertThat(query, is("SELECT * FROM job WHERE specification @> '{\"dataFile\": \"urn:dataio-fs:1268210\"}'"));
    }

    @Test
    public void lessThanOperator() throws ParseException {
        String query = ioqlParser.parse("job:id < 42");
        assertThat(query, is("SELECT * FROM job WHERE id < 42"));
    }

    @Test
    public void lessThanOrEqualToOperator() throws ParseException {
        String query = ioqlParser.parse("job:id <= 42");
        assertThat(query, is("SELECT * FROM job WHERE id <= 42"));
    }

    @Test
    public void notEqualsOperator() throws ParseException {
        String query = ioqlParser.parse("job:id != 42");
        assertThat(query, is("SELECT * FROM job WHERE id != 42"));
    }

    @Test
    public void withOperator() throws ParseException {
        String query = ioqlParser.parse("WITH job:timeofcompletion");
        assertThat(query, is("SELECT * FROM job WHERE timeofcompletion IS NOT NULL"));
    }

    @Test
    public void quotedValue() throws ParseException {
        String query = ioqlParser.parse("job:timeofcreation > '2017-09-06'");
        assertThat(query, is("SELECT * FROM job WHERE timeofcreation > '2017-09-06'"));
    }

    @Test
    public void multipleTerms() throws ParseException {
        String query = ioqlParser.parse("job:id = 42 OR job:id = 43 AND job:timeofcreation > '2017-09-06' AND WITH job:timeofcompletion");
        assertThat(query, is("SELECT * FROM job WHERE id = 42 OR id = 43 AND timeofcreation > '2017-09-06' AND timeofcompletion IS NOT NULL"));
    }

    @Test
    public void logicalGroupings() throws ParseException {
        String query = ioqlParser.parse("job:id = 42 OR (job:id = 43 AND job:timeofcreation > '2017-09-06' AND WITH job:timeofcompletion)");
        assertThat(query, is("SELECT * FROM job WHERE id = 42 OR (id = 43 AND timeofcreation > '2017-09-06' AND timeofcompletion IS NOT NULL)"));
    }

    @Test
    public void characterEscaping() throws ParseException {
        String query = ioqlParser.parse("cartoon:quote = 'What\\'s Up, Doc?'");
        assertThat(query, is("SELECT * FROM cartoon WHERE quote = 'What\\'s Up, Doc?'"));
    }

    @Test
    public void jsonField() throws ParseException {
        String query = ioqlParser.parse("job:specification.ancestry.previousJobId = '42'");
        assertThat(query, is("SELECT * FROM job WHERE specification->'ancestry'->>'previousJobId' = '42'"));
    }

    @Test
    public void countQuery() throws ParseException {
        String query = ioqlParser.parse("COUNT job:id > 42");
        assertThat(query, is("SELECT COUNT(*) FROM job WHERE id > 42"));
    }

    @Test
    public void notOperator() throws ParseException {
        String query = ioqlParser.parse("NOT job:id <= 42");
        assertThat(query, is("SELECT * FROM job WHERE NOT id <= 42"));
    }

    @Test
    public void notWith() throws ParseException {
        String query = ioqlParser.parse("NOT WITH job:timeofcompletion");
        assertThat(query, is("SELECT * FROM job WHERE NOT timeofcompletion IS NOT NULL"));
    }

    @Test
    public void notLogicalGrouping() throws ParseException {
        String query = ioqlParser.parse("job:id = 42 OR NOT (job:id = 43 AND job:timeofcreation > '2017-09-06' AND WITH job:timeofcompletion)");
        assertThat(query, is("SELECT * FROM job WHERE id = 42 OR NOT (id = 43 AND timeofcreation > '2017-09-06' AND timeofcompletion IS NOT NULL)"));
    }

    @Test
    public void multipleResourcesInQuery() throws ParseException {
        assertThat(() -> ioqlParser.parse("job:id = 42 AND chunk:id = 0"), isThrowing(ParseException.class));
    }

    @Test
    public void limit() throws ParseException {
        String query = ioqlParser.parse("job:id > 42 LIMIT 10");
        assertThat(query, is("SELECT * FROM job WHERE id > 42 LIMIT 10"));
    }

    @Test
    public void countWithLimit() throws ParseException {
        String query = ioqlParser.parse("COUNT job:id > 42 LIMIT 10");
        assertThat(query, is("SELECT COUNT(*) FROM job WHERE id > 42 LIMIT 10"));
    }

    @Test
    public void offset() throws ParseException {
        String query = ioqlParser.parse("job:id > 42 OFFSET 10");
        assertThat(query, is("SELECT * FROM job WHERE id > 42 OFFSET 10"));
    }

    @Test
    public void countWithOffset() throws ParseException {
        String query = ioqlParser.parse("COUNT job:id > 42 OFFSET 10");
        assertThat(query, is("SELECT COUNT(*) FROM job WHERE id > 42 OFFSET 10"));
    }

    @Test
    public void orderBy() throws ParseException {
        String query = ioqlParser.parse("job:timeofcreation > '2017-09-06' ORDER BY job:id ASC");
        assertThat(query, is("SELECT * FROM job WHERE timeofcreation > '2017-09-06' ORDER BY id ASC"));
    }

    @Test
    public void orderByJsonPath() throws ParseException {
        String query = ioqlParser.parse("job:timeofcreation > '2017-09-06' ORDER BY LOWER job:specification.name ASC");
        assertThat(query, is("SELECT * FROM job WHERE timeofcreation > '2017-09-06' ORDER BY lower(specification->>'name') ASC"));
    }

    @Test
    public void multipleOrderBy() throws ParseException {
        String query = ioqlParser.parse("job:timeofcreation > '2017-09-06' ORDER BY job:id ASC UPPER job:keyX DESC job:keyY ASC");
        assertThat(query, is("SELECT * FROM job WHERE timeofcreation > '2017-09-06' ORDER BY id ASC, upper(keyX) DESC, keyY ASC"));
    }

    @Test
    public void combineKeywords() throws ParseException {
        String query = ioqlParser.parse("COUNT job:id > 42 ORDER BY job:id ASC LIMIT 10 OFFSET 1000");
        assertThat(query, is("SELECT COUNT(*) FROM job WHERE id > 42 ORDER BY id ASC LIMIT 10 OFFSET 1000"));
    }

    /*
        Standard SQL injections are caught by the parser.
            - line comments
            - stacked queries
            - CHR() attacks
            - blind SLEEP() injections
            etc.
     */

    @Test
    public void sqlInjections_lineComment() throws ParseException {
        assertThrows(TokenMgrError.class, () -> ioqlParser.parse("members:username = admin--"));
    }

    @Test
    public void sqlInjections_stackedQueries() throws ParseException {
        assertThat(() -> ioqlParser.parse("members:username = admin;DROP members;--"), isThrowing(ParseException.class));
    }
}
