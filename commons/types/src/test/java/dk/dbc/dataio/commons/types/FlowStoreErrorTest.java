/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class FlowStoreErrorTest {
    private static final FlowStoreError.Code CODE = FlowStoreError.Code.NONEXISTING_SUBMITTER;
    private static final int STATUS_CODE = 123;
    private static final String DESCRIPTION = "description";
    private static final String STACKTRACE = "stacktrace";

    @Test(expected = NullPointerException.class)
    public void constructor_codeArgIsNull_throws() {
        new FlowStoreError(null, STATUS_CODE, DESCRIPTION, STACKTRACE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_statusCodeArgIsZero_throws() {
        new FlowStoreError(CODE, 0, DESCRIPTION, STACKTRACE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_descriptionArgIsNull_throws() {
        new FlowStoreError(CODE, STATUS_CODE, null, STACKTRACE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_descriptionArgIsEmpty_throws() {
        new FlowStoreError(CODE, STATUS_CODE, "", STACKTRACE);
    }

    @Test
    public void constructor_stacktraceIsNull_returnsNewInstanceWithEmptyStacktrace() {
        final FlowStoreError FlowStoreError = new FlowStoreError(CODE, STATUS_CODE, DESCRIPTION, null);
        assertThat(FlowStoreError, is(notNullValue()));
        assertThat(FlowStoreError.getStacktrace().isEmpty(), is(true));
    }

    @Test
    public void constructor_stacktraceIsEmpty_returnsNewInstanceWithEmptyStacktrace() {
        final FlowStoreError FlowStoreError = new FlowStoreError(CODE, STATUS_CODE, DESCRIPTION, "");
        assertThat(FlowStoreError, is(notNullValue()));
        assertThat(FlowStoreError.getStacktrace().isEmpty(), is(true));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final FlowStoreError FlowStoreError = new FlowStoreError(CODE, STATUS_CODE, DESCRIPTION, STACKTRACE);
        assertThat(FlowStoreError, is(notNullValue()));
        assertThat(FlowStoreError.getCode(), is(CODE));
        assertThat(FlowStoreError.getStatusCode(), is(STATUS_CODE));
        assertThat(FlowStoreError.getDescription(), is(DESCRIPTION));
        assertThat(FlowStoreError.getStacktrace(), is(STACKTRACE));
    }

    @Test
    public void jsonBinding_marshallingFollowedByUnmarshalling_returnsNewInstanceWithMatchingFields() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final FlowStoreError FlowStoreError = new FlowStoreError(CODE, STATUS_CODE, DESCRIPTION, STACKTRACE);
        final String marshalled = jsonbContext.marshall(FlowStoreError);
        final FlowStoreError unmarshalled = jsonbContext.unmarshall(marshalled, FlowStoreError.class);
        assertThat(unmarshalled, is(notNullValue()));
        assertThat(unmarshalled.getCode(), is(FlowStoreError.getCode()));
        assertThat(unmarshalled.getStatusCode(), is(FlowStoreError.getStatusCode()));
        assertThat(unmarshalled.getDescription(), is(FlowStoreError.getDescription()));
        assertThat(unmarshalled.getStacktrace(), is(FlowStoreError.getStacktrace()));
    }

    @Test
    public void equals_equality_ok() {
        FlowStoreError fse1 = new FlowStoreError(
                FlowStoreError.Code.NONEXISTING_SUBMITTER,
                234,
                "desci",
                "traci"
        );
        FlowStoreError fse2 = new FlowStoreError(
                FlowStoreError.Code.NONEXISTING_SUBMITTER,
                234,
                "desci",
                "traci"
        );
        assertThat(fse1, equalTo(fse2));
    }

    @Test
    public void equals_codeNotEqual_notOk() {
        FlowStoreError fse1 = new FlowStoreError(
                FlowStoreError.Code.NONEXISTING_SUBMITTER,
                234,
                "desci",
                "traci"
        );
        FlowStoreError fse2 = new FlowStoreError(
                FlowStoreError.Code.EXISTING_SUBMITTER_EXISTING_DESTINATION_NONEXISTING_TOC,
                234,
                "desci",
                "traci"
        );
        assertThat(fse1, not(equalTo(fse2)));
    }

    @Test
    public void equals_statusCodeNotEqual_notOk() {
        FlowStoreError fse1 = new FlowStoreError(
                FlowStoreError.Code.NONEXISTING_SUBMITTER,
                234,
                "desci",
                "traci"
        );
        FlowStoreError fse2 = new FlowStoreError(
                FlowStoreError.Code.NONEXISTING_SUBMITTER,
                235,
                "desci",
                "traci"
        );
        assertThat(fse1, not(equalTo(fse2)));
    }

    @Test
    public void equals_descriptionNotEqual_notOk() {
        FlowStoreError fse1 = new FlowStoreError(
                FlowStoreError.Code.NONEXISTING_SUBMITTER,
                234,
                "desci",
                "traci"
        );
        FlowStoreError fse2 = new FlowStoreError(
                FlowStoreError.Code.NONEXISTING_SUBMITTER,
                234,
                "desciX",
                "traci"
        );
        assertThat(fse1, not(equalTo(fse2)));
    }

    @Test
    public void equals_stackTraceNotEqual_notOk() {
        FlowStoreError fse1 = new FlowStoreError(
                FlowStoreError.Code.NONEXISTING_SUBMITTER,
                234,
                "desci",
                "traci"
        );
        FlowStoreError fse2 = new FlowStoreError(
                FlowStoreError.Code.NONEXISTING_SUBMITTER,
                234,
                "desci",
                "traciX"
        );
        assertThat(fse1, not(equalTo(fse2)));
    }

    @Test
    public void toString_normal_ok() {
        FlowStoreError fse = new FlowStoreError(
                FlowStoreError.Code.NONEXISTING_SUBMITTER,
                234,
                "desci",
                "traci"
        );
        assertThat(fse.toString(), is("FlowStoreError{code='NONEXISTING_SUBMITTER', statusCode=234, description='desci', stacktrace='traci'}"));
    }

}