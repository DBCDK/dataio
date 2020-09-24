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

package dk.dbc.dataio.sink.diff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;


public class ExternalToolDiffGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalToolDiffGenerator.class);
    private static final String EMPTY = "";
    static String path = "";

    public enum Kind {
        JSON("jsondiff"),
        PLAINTEXT("plaintextdiff"),
        XML("xmldiff");

        private final String tool;

        Kind(String tool) {
            this.tool = tool;
        }

        public String getTool() {
            return path + this.tool;
        }
    }

    /**
     * Creates diff string through external tool, returning
     * empty string   : if the two input parameters are identical or semantic identical.
     * diff as string : if the two input parameters are different from one another.
     * @param kind the kind of diff to generate
     * @param current the current item data
     * @param next the next item data
     * @return the diff string
     * @throws DiffGeneratorException on failure to create diff
     */
    public String getDiff(Kind kind, byte[] current, byte[] next) throws DiffGeneratorException {
        File tempFile1 = null;
        File tempFile2 = null;
        try {
            tempFile1 = File.createTempFile("doc1", ".tmp.file");
            tempFile2 = File.createTempFile("doc2", ".tmp.file");

            final FileOutputStream fos1 = new FileOutputStream(tempFile1);
            final FileOutputStream fos2 = new FileOutputStream(tempFile2);
            fos1.write(current);
            fos2.write(next);
            fos1.close();
            fos2.close();

            final BooleanHolder stdoutDone = new BooleanHolder();
            final BooleanHolder stderrDone = new BooleanHolder();
            final Process p = Runtime.getRuntime().exec(String.format("%s %s %s\n",
                    kind.getTool(), tempFile1.getAbsolutePath(), tempFile2.getAbsolutePath()));
            final StringBuilder out = new StringBuilder();
            final StreamHandler outHandler = new StreamHandler(p.getInputStream(),
                (line) -> out.append(line).append("\n"), stdoutDone::setTrue);
            final StringBuilder err = new StringBuilder();
            final StreamHandler errHandler = new StreamHandler(p.getErrorStream(),
                (line) -> err.append(line).append("\n"), stderrDone::setTrue);

            //Thread outputThread = threadFactory.newThread(outHandler);
            final Thread outputThread = new Thread(outHandler);
            outputThread.start();
            //Thread errorThread = threadFactory.newThread(errHandler);
            final Thread errorThread = new Thread(errHandler);
            errorThread.start();

            final int res = p.waitFor();
            // wait a bit until the threads are done
            while (!stderrDone.value || !stdoutDone.value)
                Thread.sleep(20);

            if (err.length() > 0) {
                throw new DiffGeneratorException(kind.getTool() +
                        " failed to compare input: " + err.toString());
            }

            if (res != 0 && out.length() > 0) {
                return out.toString();
            }
            return EMPTY;
        } catch (IOException | InterruptedException e) {
            throw new DiffGeneratorException(kind.getTool() +
                    " failed to compare input", e);
        } catch(RuntimeException e) {
            LOGGER.error("Unexpected exception: ", e);
            throw e;
        } finally {
            if (tempFile1 != null) {
                tempFile1.delete();
            }
            if (tempFile2 != null) {
                tempFile2.delete();
            }
        }
    }

    private class StreamHandler implements Runnable {
        private InputStream is;
        private Consumer<String> consumer;
        private Runnable done;
        public StreamHandler(InputStream is, Consumer<String> consumer, Runnable done) {
            this.is = is;
            this.consumer = consumer;
            this.done = done;
        }

        @Override
        public void run() {
            try(BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = br.readLine()) != null) {
                    consumer.accept(line);
                }
            } catch(IOException e) {
                consumer.accept("caught exception: " + e.toString());
            } finally {
                done.run();
            }
        }
    }
    // convenience class because Boolean is immutable
    private class BooleanHolder {
        boolean value;
        public void setTrue() {
            value = true;
        }
    }
}
