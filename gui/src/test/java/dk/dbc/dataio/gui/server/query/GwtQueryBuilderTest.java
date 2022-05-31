package dk.dbc.dataio.gui.server.query;

import dk.dbc.dataio.gui.client.querylanguage.GwtIntegerClause;
import dk.dbc.dataio.gui.client.querylanguage.GwtQueryClause;
import dk.dbc.dataio.gui.client.querylanguage.GwtStringClause;
import dk.dbc.dataio.querylanguage.Ordering;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class GwtQueryBuilderTest {
    @Test
    public void singleGwtStringClause() {
        final GwtStringClause gwtStringClause = new GwtStringClause()
                .withIdentifier("flow_binders:field")
                .withOperator(GwtQueryClause.BiOperator.EQUALS)
                .withValue("value");

        assertThat(new GwtQueryBuilder().add(gwtStringClause).build(),
                is("flow_binders:field = 'value'"));
    }

    @Test
    public void multipleGwtQueryClause() {
        final GwtStringClause gwtStringClause = new GwtStringClause()
                .withIdentifier("flow_binders:field1")
                .withOperator(GwtQueryClause.BiOperator.EQUALS)
                .withValue("value1");
        final GwtIntegerClause gwtIntegerClause = new GwtIntegerClause()
                .withIdentifier("flow_binders:field2")
                .withOperator(GwtQueryClause.BiOperator.GREATER_THAN)
                .withValue(2);
        final GwtIntegerClause gwtBooleanClause = new GwtIntegerClause()
                .withIdentifier("flow_binders:field3")
                .withOperator(GwtQueryClause.BiOperator.EQUALS)
                .withValue(1)
                .withFlag(true);

        assertThat(new GwtQueryBuilder().addAll(Arrays.asList(gwtStringClause, gwtIntegerClause, gwtBooleanClause)).build(),
                is("flow_binders:field1 = 'value1' AND flow_binders:field2 > 2 AND flow_binders:field3 = true"));
    }

    @Test
    public void jsonValue() {
        final GwtStringClause gwtStringClause1 = new GwtStringClause()
                .withIdentifier("flow_binders:field.property1")
                .withOperator(GwtQueryClause.BiOperator.JSON_LEFT_CONTAINS)
                .withValue("value1");
        final GwtStringClause gwtStringClause2 = new GwtStringClause()
                .withIdentifier("flow_binders:field.property2")
                .withOperator(GwtQueryClause.BiOperator.JSON_LEFT_CONTAINS)
                .withValue("value2");

        assertThat(new GwtQueryBuilder().addAll(Arrays.asList(gwtStringClause1, gwtStringClause2)).build(),
                is("flow_binders:field @> '{\"property2\":\"value2\",\"property1\":\"value1\"}'"));
    }

    @Test
    public void jsonValueArrayProperty() {
        final GwtStringClause gwtStringClause1 = new GwtStringClause()
                .withIdentifier("flow_binders:field.property")
                .withOperator(GwtQueryClause.BiOperator.JSON_LEFT_CONTAINS)
                .withValue("value1")
                .withArrayProperty(true);
        final GwtStringClause gwtStringClause2 = new GwtStringClause()
                .withIdentifier("flow_binders:field.property")
                .withOperator(GwtQueryClause.BiOperator.JSON_LEFT_CONTAINS)
                .withValue("value2")
                .withArrayProperty(true);

        assertThat(new GwtQueryBuilder().addAll(Arrays.asList(gwtStringClause1, gwtStringClause2)).build(),
                is("flow_binders:field @> '{\"property\":[\"value1\",\"value2\"]}'"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void identifierWithTooManyLevels() {
        final GwtStringClause gwtStringClause = new GwtStringClause()
                .withIdentifier("flow_binders:field.property1.sub1")
                .withOperator(GwtQueryClause.BiOperator.JSON_LEFT_CONTAINS)
                .withValue("value1");
        new GwtQueryBuilder().add(gwtStringClause).build();
    }

    @Test
    public void notClause() {
        final GwtStringClause gwtStringClause = new GwtStringClause()
                .withIdentifier("flow_binders:field")
                .withOperator(GwtQueryClause.BiOperator.EQUALS)
                .withValue("value")
                .withNegated(true);

        assertThat(new GwtQueryBuilder().add(gwtStringClause).build(),
                is("NOT flow_binders:field = 'value'"));
    }

    @Test
    public void notJsonValue() {
        final GwtStringClause gwtStringClause1 = new GwtStringClause()
                .withIdentifier("flow_binders:field.property1")
                .withOperator(GwtQueryClause.BiOperator.JSON_LEFT_CONTAINS)
                .withValue("value1")
                .withNegated(true);
        final GwtStringClause gwtStringClause2 = new GwtStringClause()
                .withIdentifier("flow_binders:field.property2")
                .withOperator(GwtQueryClause.BiOperator.JSON_LEFT_CONTAINS)
                .withValue("value2");

        assertThat(new GwtQueryBuilder().addAll(Arrays.asList(gwtStringClause1, gwtStringClause2)).build(),
                is("flow_binders:field @> '{\"property2\":\"value2\"}' AND NOT flow_binders:field @> '{\"property1\":\"value1\"}'"));
    }

    @Test
    public void sortBy() {
        final GwtStringClause gwtStringClause = new GwtStringClause()
                .withIdentifier("flow_binders:field")
                .withOperator(GwtQueryClause.BiOperator.EQUALS)
                .withValue("value");

        assertThat(new GwtQueryBuilder()
                        .add(gwtStringClause)
                        .sortBy(new Ordering()
                                .withIdentifier("flow_binders:content.name")
                                .withOrder(Ordering.Order.ASC)
                                .withSortCase(Ordering.SortCase.LOWER))
                        .build(),
                is("flow_binders:field = 'value' ORDER BY LOWER flow_binders:content.name ASC"));
    }
}
