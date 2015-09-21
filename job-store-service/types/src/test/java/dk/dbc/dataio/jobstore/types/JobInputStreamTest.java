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

package dk.dbc.dataio.jobstore.types;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

public class JobInputStreamTest {

    private static final int PART_NUMBER = 12345678;

    @Test(expected = NullPointerException.class)
    public void constructor_jobSpecificationArgIsNull_throws() {
        new JobInputStream(null, false, PART_NUMBER);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_partNumberLessThanZero_throws() {
        new JobInputStream(new JobSpecificationBuilder().build(), false, -1);
    }

    @Test
    public void setJobSpecification_inputIsValid_jobSpecificationCreated() {
        final String FORMAT = "thisIsATestFormat";
        JobSpecification jobSpecification = new JobSpecificationBuilder()
                .setFormat(FORMAT).build();
        JobInputStream jobInputStream = new JobInputStream(jobSpecification, false, PART_NUMBER);

        assertThat(jobInputStream.getPartNumber(), not(nullValue()));
        assertThat(jobInputStream.getPartNumber(), is(PART_NUMBER));
        assertThat(jobInputStream.getIsEndOfJob(), is(false));
        assertThat(jobInputStream.getJobSpecification(), not(nullValue()));
        assertThat(jobInputStream.getJobSpecification().getFormat(), is(FORMAT));
    }

}
