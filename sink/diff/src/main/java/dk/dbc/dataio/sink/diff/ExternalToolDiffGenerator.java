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

import javax.ejb.Singleton;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;


@Singleton
public class ExternalToolDiffGenerator {
    // this should be the preferred way of handling threads in an ejb
    // but it dies occasionally with a nullpointerexception without stacktrace
    // [2017-06-09 08:51:28,414] [ERROR] [concurrent/__defaultManagedThreadFactory-Thread-282] [] org.glassfish.enterprise.concurrent - java.lang.NullPointerException
    // [2017-06-09 08:51:28,415] [ERROR] [concurrent/__defaultManagedThreadFactory-Thread-281] [] org.glassfish.enterprise.concurrent - java.lang.NullPointerException
    //@Resource(name = "concurrent/__defaultManagedThreadFactory")
    //protected ManagedThreadFactory threadFactory;

    protected String xmlDiffPath = "xmldiff";

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalToolDiffGenerator.class);

    private static final String EMPTY = "";

    /**
     * Creates diff string through XmlDiff.
     *
     * Diff as empty string     : if the two input parameters are identical or semantic identical.
     * Diff with xml as string  : if the two input parameters are different from one another.
     *
     * @param current the current item data
     * @param next the next item data
     * @return the diff string
     *
     * @throws DiffGeneratorException on failure to create diff
     */
    public String getDiff(byte[] current, byte[] next) throws DiffGeneratorException {
        File tempFile1 = null;
        File tempFile2 = null;
        try {
            tempFile1 = File.createTempFile("xml1", ".tmp.xml");
            tempFile2 = File.createTempFile("xml2", ".tmp.xml");

            FileOutputStream fos1 = new FileOutputStream(tempFile1);
            FileOutputStream fos2 = new FileOutputStream(tempFile2);
            fos1.write(current);
            fos2.write(next);
            fos1.close();
            fos2.close();

            BooleanHolder stdoutDone = new BooleanHolder();
            BooleanHolder stderrDone = new BooleanHolder();
            Process p = Runtime.getRuntime().exec(String.format(
                "%s %s %s\n", xmlDiffPath,
                tempFile1.getAbsolutePath(), tempFile2.getAbsolutePath()));
            StringBuilder out = new StringBuilder();
            StreamHandler outHandler = new StreamHandler(p.getInputStream(),
                (line) -> out.append(line).append("\n"), stdoutDone::setTrue);
            StringBuilder err = new StringBuilder();
            StreamHandler errHandler = new StreamHandler(p.getErrorStream(),
                (line) -> err.append(line).append("\n"), stderrDone::setTrue);

            //Thread outputThread = threadFactory.newThread(outHandler);
            Thread outputThread = new Thread(outHandler);
            outputThread.start();
            //Thread errorThread = threadFactory.newThread(errHandler);
            Thread errorThread = new Thread(errHandler);
            errorThread.start();

            int res = p.waitFor();
            // wait a bit until the threads are done
            while(!stderrDone.value || !stdoutDone.value)
                Thread.sleep(20);

            if(err.length() > 0) {
                throw new DiffGeneratorException(
                    "XmlDiffGenerator failed to compare input: " + err.toString());
            }

            if(res != 0 && out.length() > 0)
                return out.toString();
            else
                return EMPTY;
        } catch (IOException | InterruptedException e) {
            throw new DiffGeneratorException("XmlDiff Failed to compare input", e);
        } catch(RuntimeException e) {
            LOGGER.error("Unexpected exception: ", e);
            throw e;
        } finally {
            if(tempFile1 != null)
                tempFile1.delete();
            if(tempFile2 != null)
                tempFile2.delete();
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
