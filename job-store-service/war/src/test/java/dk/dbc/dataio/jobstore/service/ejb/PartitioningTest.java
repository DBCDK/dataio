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

package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.types.PrematureEndOfDataException;
import org.junit.Test;

import javax.ejb.EJBTransactionRolledbackException;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class PartitioningTest {
    @Test
    public void partitioningFailedWithPrematureEndOfDataExceptionWithCause() {
        final IOException ioException = new IOException();
        final PrematureEndOfDataException prematureEndOfDataException =
                new PrematureEndOfDataException(ioException);
        final EJBTransactionRolledbackException ejbTransactionRolledbackException =
                new EJBTransactionRolledbackException("test", prematureEndOfDataException);

        final Partitioning partitioning = new Partitioning()
                .withFailure(ejbTransactionRolledbackException);
        assertThat("hasFailedUnexpectedly", partitioning.hasFailedUnexpectedly(), is(true));
        assertThat("hasFailedPossiblyDueToLostFileStoreConnection", partitioning.hasFailedPossiblyDueToLostFileStoreConnection(), is(true));
        assertThat("failure", partitioning.getFailure(), is(ioException));
    }

    @Test
    public void partitioningFailedWithPrematureEndOfDataExceptionWithoutCause() {
        final PrematureEndOfDataException prematureEndOfDataException =
                new PrematureEndOfDataException(null);
        final EJBTransactionRolledbackException ejbTransactionRolledbackException =
                new EJBTransactionRolledbackException("test", prematureEndOfDataException);

        final Partitioning partitioning = new Partitioning()
                .withFailure(ejbTransactionRolledbackException);
        assertThat("hasFailedUnexpectedly", partitioning.hasFailedUnexpectedly(), is(true));
        assertThat("hasFailedPossiblyDueToLostFileStoreConnection", partitioning.hasFailedPossiblyDueToLostFileStoreConnection(), is(false));
        assertThat("failure", partitioning.getFailure(), is(prematureEndOfDataException));
    }

    @Test
    public void partitioningFailedWithoutPrematureEndOfDataException() {
        final IOException ioException = new IOException();
        final EJBTransactionRolledbackException ejbTransactionRolledbackException =
                new EJBTransactionRolledbackException("test", ioException);

        final Partitioning partitioning = new Partitioning()
                .withFailure(ejbTransactionRolledbackException);
        assertThat("hasFailedUnexpectedly", partitioning.hasFailedUnexpectedly(), is(true));
        assertThat("hasFailedPossiblyDueToLostFileStoreConnection", partitioning.hasFailedPossiblyDueToLostFileStoreConnection(), is(false));
        assertThat("failure", partitioning.getFailure(), is(ioException));
    }

    @Test
    public void partitioningFailedWithoutCause() {
        final EJBTransactionRolledbackException ejbTransactionRolledbackException =
                new EJBTransactionRolledbackException("test", null);

        final Partitioning partitioning = new Partitioning()
                .withFailure(ejbTransactionRolledbackException);
        assertThat("hasFailedUnexpectedly", partitioning.hasFailedUnexpectedly(), is(true));
        assertThat("hasFailedPossiblyDueToLostFileStoreConnection", partitioning.hasFailedPossiblyDueToLostFileStoreConnection(), is(false));
        assertThat("failure", partitioning.getFailure(), is(ejbTransactionRolledbackException));
    }

    @Test
    public void nullValuedFailure() {
        final Partitioning partitioning = new Partitioning()
                .withFailure(null);
        assertThat("hasFailedUnexpectedly", partitioning.hasFailedUnexpectedly(), is(false));
        assertThat("hasFailedPossiblyDueToLostFileStoreConnection", partitioning.hasFailedPossiblyDueToLostFileStoreConnection(), is(false));
        assertThat("failure", partitioning.getFailure(), is(nullValue()));
    }
}