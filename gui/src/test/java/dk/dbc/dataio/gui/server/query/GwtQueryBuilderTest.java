/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.gui.server.query;

import dk.dbc.dataio.gui.client.querylanguage.GwtNotClause;
import dk.dbc.dataio.gui.client.querylanguage.GwtQueryClause;
import dk.dbc.dataio.gui.client.querylanguage.GwtStringClause;
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
        final GwtStringClause gwtStringClause1 = new GwtStringClause()
                .withIdentifier("flow_binders:field1")
                .withOperator(GwtQueryClause.BiOperator.EQUALS)
                .withValue("value1");
        final GwtStringClause gwtStringClause2 = new GwtStringClause()
                .withIdentifier("flow_binders:field2")
                .withOperator(GwtQueryClause.BiOperator.GREATER_THAN)
                .withValue("value2");

        assertThat(new GwtQueryBuilder().addAll(Arrays.asList(gwtStringClause1, gwtStringClause2)).build(),
                is("flow_binders:field1 = 'value1' AND flow_binders:field2 > 'value2'"));
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
                .withValue("value");

        assertThat(new GwtQueryBuilder().add(new GwtNotClause().withGwtQueryClause(gwtStringClause)).build(),
                is("NOT flow_binders:field = 'value'"));
    }

    @Test
    public void notJsonValue() {
        final GwtStringClause gwtStringClause1 = new GwtStringClause()
                .withIdentifier("flow_binders:field.property1")
                .withOperator(GwtQueryClause.BiOperator.JSON_LEFT_CONTAINS)
                .withValue("value1");
        final GwtStringClause gwtStringClause2 = new GwtStringClause()
                .withIdentifier("flow_binders:field.property2")
                .withOperator(GwtQueryClause.BiOperator.JSON_LEFT_CONTAINS)
                .withValue("value2");

        assertThat(new GwtQueryBuilder().addAll(Arrays.asList(
                new GwtNotClause().withGwtQueryClause(gwtStringClause1), gwtStringClause2)).build(),
                is("flow_binders:field @> '{\"property2\":\"value2\"}' AND NOT flow_binders:field @> '{\"property1\":\"value1\"}'"));
    }
}