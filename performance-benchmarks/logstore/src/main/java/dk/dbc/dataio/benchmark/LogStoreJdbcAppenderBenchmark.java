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

package dk.dbc.dataio.benchmark;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import dk.dbc.dataio.logstore.types.LogStoreTrackingId;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
public class LogStoreJdbcAppenderBenchmark {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogStoreJdbcAppenderBenchmark.class);

    private static final int FORKS = 1;
    private static final int ITERATIONS = 20;
    private static final int BATCH_SIZE = 1000;

    private static final String LOGBACK_CONFIG = "/logstoreJdbcAppender.logback.xml";
    private static final Exception EXCEPTION = new IllegalStateException("Benchmark exception");

    private int jobIdCounter = 0;

    @Setup(Level.Trial)
    public void configureLogger() throws JoranException {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        final JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        // Call context.reset() to clear any previous configuration, e.g. default
        // configuration. For multi-step configuration, omit calling context.reset().
        context.reset();
        configurator.doConfigure(LogStoreJdbcAppenderBenchmark.class.getResourceAsStream(LOGBACK_CONFIG));
    }

    @Setup(Level.Invocation)
    public void setLogContext() {
        final String jobId = String.format("benchmark%d", ++jobIdCounter);
        MDC.put(LogStoreTrackingId.LOG_STORE_TRACKING_ID_MDC_KEY, LogStoreTrackingId.create(jobId, 1, 1).toString());
    }

    @TearDown(Level.Trial)
    public void tearDownLogContext() {
        MDC.remove(LogStoreTrackingId.LOG_STORE_TRACKING_ID_MDC_KEY);
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Fork(FORKS)
    @Warmup(iterations = 10, batchSize = BATCH_SIZE)
    @Measurement(iterations = ITERATIONS, batchSize = BATCH_SIZE)
    public void measureAppender() {
        // 10 log statements to simulate JavaScript processing
        LOGGER.info("Log 1");
        LOGGER.info("Log 2");
        LOGGER.info("Log 3");
        LOGGER.info("Loch Lomond");
        LOGGER.info("Before warning");
        LOGGER.warn("Be warned");
        LOGGER.info("After warning");
        LOGGER.info("Before error");
        LOGGER.error("You are in error", EXCEPTION);
        MDC.put(LogStoreTrackingId.LOG_STORE_TRACKING_ID_COMMIT_MDC_KEY, "true");
        LOGGER.info("After error");
        MDC.remove(LogStoreTrackingId.LOG_STORE_TRACKING_ID_COMMIT_MDC_KEY);
    }
}

