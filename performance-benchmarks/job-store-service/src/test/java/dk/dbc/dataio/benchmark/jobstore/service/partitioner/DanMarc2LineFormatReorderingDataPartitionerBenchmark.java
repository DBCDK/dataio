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

package dk.dbc.dataio.benchmark.jobstore.service.partitioner;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.commons.utils.test.jpa.TransactionScopedPersistenceContext;
import dk.dbc.dataio.jobstore.service.partitioner.DanMarc2LineFormatReorderingDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.JobItemReorderer;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DanMarc2LineFormatReorderingDataPartitionerBenchmark {
    @Test
    public void run_benchmark_74_records() throws RunnerException {
        BenchmarkState.resource = "/test-records-74-danmarc2.lin";
        BenchmarkState.encoding = "latin1";

        final Options benchmarkOptions = new OptionsBuilder()
                // Specify which benchmarks to run.
                // You can be more specific if you'd like to run only one benchmark per test.
                .include(this.getClass().getName() + ".*")
                // Set the following options as needed
                .mode(Mode.SingleShotTime)
                .timeUnit(TimeUnit.MILLISECONDS)
                .warmupIterations(10)
                .warmupBatchSize(100)
                .measurementIterations(5)
                .measurementBatchSize(100)
                .threads(1)
                .forks(0)
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .build();

        new Runner(benchmarkOptions).run().forEach(this::printResult);
    }

    private void printResult(RunResult result) {
        final Result primaryResult = result.getPrimaryResult();
        System.out.println(String.format("%s - %s %.3f %s", getFormattedDateTime(),
                primaryResult.getLabel(), primaryResult.getScore(), primaryResult.getScoreUnit()));
    }

    private String getFormattedDateTime() {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        final LocalDateTime dateTime = LocalDateTime.now();
        return dateTime.format(formatter);
    }

    // The JMH samples are the best documentation for how to use it
    // http://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/
    @State(Scope.Thread)
    public static class BenchmarkState {
        static String resource;
        static String encoding;
        static AtomicInteger jobId = new AtomicInteger(0);

        EntityManager entityManager;
        TransactionScopedPersistenceContext persistenceContext;
        DataPartitioner dataPartitioner;

        @Setup(Level.Invocation)
        public void setDataPartitioner() {
            entityManager = JPATestUtils.createEntityManagerForIntegrationTest("jobstoreIT");
            persistenceContext = new TransactionScopedPersistenceContext(entityManager);
            final JobItemReorderer jobItemReorderer = new JobItemReorderer(jobId.incrementAndGet(), entityManager);
            dataPartitioner = DanMarc2LineFormatReorderingDataPartitioner.newInstance(
                    getClass().getResourceAsStream(resource), encoding, jobItemReorderer);
        }
    }

    @Benchmark
    public void benchmark_74_records(BenchmarkState state) {
        final DataPartitioner dataPartitioner = state.dataPartitioner;
        state.persistenceContext.run(() -> {
            for (Iterator<ChunkItem> iterator = dataPartitioner.iterator(); iterator.hasNext(); ) {
                iterator.next();
            }
        });
    }
}
