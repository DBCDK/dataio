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
import javax.ejb.TransactionRolledbackLocalException;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

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
        assertThat("premature end of data failure",
                partitioning.hasKnownFailure(Partitioning.KnownFailure.PREMATURE_END_OF_DATA), is(true));
        assertThat("failure", partitioning.getFailure(), is(prematureEndOfDataException));
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
        assertThat("premature end of data failure",
                partitioning.hasKnownFailure(Partitioning.KnownFailure.PREMATURE_END_OF_DATA), is(true));
        assertThat("failure", partitioning.getFailure(), is(prematureEndOfDataException));
    }

    @Test
    public void partitioningFailedWithTransactionRolledBackLocalException() {
        final TransactionRolledbackLocalException transactionRolledbackLocalException =
                new TransactionRolledbackLocalException("Something terrible happened");
        final EJBTransactionRolledbackException ejbTransactionRolledbackException =
                new EJBTransactionRolledbackException("test", transactionRolledbackLocalException);

        final Partitioning partitioning = new Partitioning()
                .withFailure(ejbTransactionRolledbackException);
        assertThat("hasFailedUnexpectedly", partitioning.hasFailedUnexpectedly(), is(true));
        assertThat("transaction rolled back local",
                partitioning.hasKnownFailure(Partitioning.KnownFailure.TRANSACTION_ROLLED_BACK_LOCAL), is(true));
        assertThat("failure", partitioning.getFailure(), is(transactionRolledbackLocalException));
    }

    @Test
    public void partitioningFailedWithoutKnownCause() {
        final IOException ioException = new IOException();
        final EJBTransactionRolledbackException ejbTransactionRolledbackException =
                new EJBTransactionRolledbackException("test", ioException);

        final Partitioning partitioning = new Partitioning()
                .withFailure(ejbTransactionRolledbackException);
        assertThat("hasFailedUnexpectedly", partitioning.hasFailedUnexpectedly(), is(true));
        assertThat("hasKnownFailure", partitioning.hasKnownFailure(), is(false));
        assertThat("failure", partitioning.getFailure(), is(ioException));
    }

    @Test
    public void partitioningFailedWithoutCause() {
        final EJBTransactionRolledbackException ejbTransactionRolledbackException =
                new EJBTransactionRolledbackException("test", null);

        final Partitioning partitioning = new Partitioning()
                .withFailure(ejbTransactionRolledbackException);
        assertThat("hasFailedUnexpectedly", partitioning.hasFailedUnexpectedly(), is(true));
        assertThat("hasKnownFailure", partitioning.hasKnownFailure(), is(false));
        assertThat("failure", partitioning.getFailure(), is(ejbTransactionRolledbackException));
    }

    @Test
    public void nullValuedFailure() {
        final Partitioning partitioning = new Partitioning()
                .withFailure(null);
        assertThat("hasFailedUnexpectedly", partitioning.hasFailedUnexpectedly(), is(false));
        assertThat("hasKnownFailure", partitioning.hasKnownFailure(), is(false));
        assertThat("failure", partitioning.getFailure(), is(nullValue()));
    }
}
