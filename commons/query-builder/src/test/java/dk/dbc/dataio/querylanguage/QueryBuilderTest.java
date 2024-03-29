package dk.dbc.dataio.querylanguage;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class QueryBuilderTest {
    @Test
    public void singleBiClause() {
        BiClause clause = new BiClause()
                .withIdentifier("job:id")
                .withOperator(BiClause.Operator.EQUALS)
                .withValue(42);
        QueryBuilder queryBuilder = new QueryBuilder(clause);
        assertThat(queryBuilder.buildQuery(), is("job:id = 42"));
    }

    @Test
    public void withClause() {
        WithClause clause = new WithClause()
                .withIdentifier("job:timeofcompletion");
        QueryBuilder queryBuilder = new QueryBuilder(clause);
        assertThat(queryBuilder.buildQuery(), is("WITH job:timeofcompletion"));
    }

    @Test
    public void multipleClauses() {
        QueryBuilder queryBuilder = new QueryBuilder(new BiClause()
                .withIdentifier("job:id")
                .withOperator(BiClause.Operator.EQUALS)
                .withValue(42))
                .or(new BiClause()
                        .withIdentifier("job:timeofcreation")
                        .withOperator(BiClause.Operator.GREATER_THAN)
                        .withValue("2017-09-06"))
                .and(new BiClause()
                        .withIdentifier("job:enabled")
                        .withOperator(BiClause.Operator.EQUALS)
                        .withValue(true))
                .and(new WithClause()
                        .withIdentifier("job:timeofcompletion"));
        assertThat(queryBuilder.buildQuery(),
                is("job:id = 42 OR job:timeofcreation > '2017-09-06' AND job:enabled = true AND WITH job:timeofcompletion"));
    }

    @Test
    public void countQuery() {
        BiClause clause = new BiClause()
                .withIdentifier("job:id")
                .withOperator(BiClause.Operator.GREATER_THAN)
                .withValue(42);
        QueryBuilder queryBuilder = new QueryBuilder(clause);
        assertThat(queryBuilder.buildCountQuery(), is("COUNT job:id > 42"));
    }

    @Test
    public void notBiClause() {
        BiClause clause = new BiClause()
                .withIdentifier("job:id")
                .withOperator(BiClause.Operator.LESS_THAN_OR_EQUAL_TO)
                .withValue(42);
        QueryBuilder queryBuilder = new QueryBuilder(new NotClause().withClause(clause));
        assertThat(queryBuilder.buildQuery(), is("NOT job:id <= 42"));
    }

    @Test
    public void notWithClause() {
        WithClause clause = new WithClause()
                .withIdentifier("job:timeofcompletion");
        QueryBuilder queryBuilder = new QueryBuilder(new NotClause().withClause(clause));
        assertThat(queryBuilder.buildQuery(), is("NOT WITH job:timeofcompletion"));
    }

    @Test
    public void limit() {
        BiClause clause = new BiClause()
                .withIdentifier("job:id")
                .withOperator(BiClause.Operator.GREATER_THAN)
                .withValue(42);
        QueryBuilder queryBuilder = new QueryBuilder(clause).limit(10);
        assertThat(queryBuilder.buildQuery(), is("job:id > 42 LIMIT 10"));
    }

    @Test
    public void offset() {
        BiClause clause = new BiClause()
                .withIdentifier("job:id")
                .withOperator(BiClause.Operator.GREATER_THAN)
                .withValue(42);
        QueryBuilder queryBuilder = new QueryBuilder(clause).offset(10);
        assertThat(queryBuilder.buildQuery(), is("job:id > 42 OFFSET 10"));
    }

    @Test
    public void orderBy() {
        BiClause clause = new BiClause()
                .withIdentifier("job:timeofcreation")
                .withOperator(BiClause.Operator.GREATER_THAN)
                .withValue("2017-09-06");
        QueryBuilder queryBuilder = new QueryBuilder(clause)
                .orderBy(new Ordering()
                        .withIdentifier("job:id")
                        .withOrder(Ordering.Order.ASC));
        assertThat(queryBuilder.buildQuery(), is("job:timeofcreation > '2017-09-06' ORDER BY job:id ASC"));
    }

    @Test
    public void multipleOrderBy() {
        BiClause clause = new BiClause()
                .withIdentifier("job:timeofcreation")
                .withOperator(BiClause.Operator.GREATER_THAN)
                .withValue("2017-09-06");
        QueryBuilder queryBuilder = new QueryBuilder(clause)
                .orderBy(new Ordering()
                        .withIdentifier("job:id")
                        .withOrder(Ordering.Order.ASC))
                .orderBy(new Ordering()
                        .withIdentifier("job:keyX")
                        .withOrder(Ordering.Order.DESC)
                        .withSortCase(Ordering.SortCase.UPPER))
                .orderBy(new Ordering()
                        .withIdentifier("job:keyY")
                        .withOrder(Ordering.Order.ASC));
        assertThat(queryBuilder.buildQuery(),
                is("job:timeofcreation > '2017-09-06' ORDER BY job:id ASC UPPER job:keyX DESC job:keyY ASC"));
    }

    @Test
    public void characterEscaping() {
        BiClause clause = new BiClause()
                .withIdentifier("cartoon:quote")
                .withOperator(BiClause.Operator.EQUALS)
                .withValue("What's Up, Doc?");
        QueryBuilder queryBuilder = new QueryBuilder(clause);
        assertThat(queryBuilder.buildQuery(), is("cartoon:quote = 'What\\'s Up, Doc?'"));
    }

    @Test
    public void jsonLeftContainsOperator() {
        BiClause clause = new BiClause()
                .withIdentifier("job:specification")
                .withOperator(BiClause.Operator.JSON_LEFT_CONTAINS)
                .withValue(new Spec().withDataFile("urn:dataio-fs:1268210"));
        QueryBuilder queryBuilder = new QueryBuilder(clause);
        assertThat(queryBuilder.buildQuery(),
                is("job:specification @> '{\"dataFile\":\"urn:dataio-fs:1268210\"}'"));
    }

    private static class Spec {
        private String dataFile;

        public String getDataFile() {
            return dataFile;
        }

        public Spec withDataFile(String dataFile) {
            this.dataFile = dataFile;
            return this;
        }
    }
}
