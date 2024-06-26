package dk.dbc.dataio.sink.diff;

import dk.dbc.dataio.commons.types.Tools;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


public class ExternalToolDiffGenerator implements DiffGenerator {
    private static final String EMPTY = "";

    /**
     * Creates diff string through external tool, returning
     * empty string   : if the two input parameters are identical or semantic identical.
     * diff as string : if the two input parameters are different from one another.
     *
     * @param kind    the kind of diff to generate
     * @param current the current item data
     * @param next    the next item data
     * @return the diff string
     * @throws DiffGeneratorException on failure to create diff
     */
    @SuppressWarnings({"java:S899", "ResultOfMethodCallIgnored"})
    public String getDiff(Kind kind, byte[] current, byte[] next) throws DiffGeneratorException, InvalidMessageException {
        File tempFile1 = null;
        File tempFile2 = null;
        try {
            tempFile1 = File.createTempFile("doc1", ".tmp.file");
            tempFile2 = File.createTempFile("doc2", ".tmp.file");
            try(FileOutputStream fos1 = new FileOutputStream(tempFile1); FileOutputStream fos2 = new FileOutputStream(tempFile2)) {
                fos1.write(current);
                fos2.write(next);
            }
            AtomicBoolean stdoutDone = new AtomicBoolean();
            AtomicBoolean stderrDone = new AtomicBoolean();
            Process p = Runtime.getRuntime().exec(String.format("%s %s %s\n",
                    kind.getTool(), tempFile1.getAbsolutePath(), tempFile2.getAbsolutePath()));
            StringBuilder out = new StringBuilder();
            StreamHandler outHandler = new StreamHandler(p.getInputStream(),
                    (line) -> out.append(line).append("\n"), () -> stdoutDone.set(true));
            StringBuilder err = new StringBuilder();
            StreamHandler errHandler = new StreamHandler(p.getErrorStream(),
                    (line) -> err.append(line).append("\n"), () -> stderrDone.set(true));

            Thread outputThread = new Thread(outHandler);
            outputThread.start();
            Thread errorThread = new Thread(errHandler);
            errorThread.start();

            int res = p.waitFor();
            // wait a bit until the threads are done
            while (!stderrDone.get() || !stdoutDone.get()) Tools.sleep(20);
            if (!err.isEmpty()) {
                throw new DiffGeneratorException(kind.getTool() +
                        " failed to compare input: " + err);
            }

            if (res != 0 && !out.isEmpty()) {
                return out.toString();
            }
            return EMPTY;
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          return "<interrupted>";
        } catch (IOException e) {
            throw new DiffGeneratorException(kind.getTool() +
                    " failed to compare input", e);
        } catch (RuntimeException e) {
            throw new InvalidMessageException("Unexpected exception", e);
        } finally {
            if (tempFile1 != null) {
                tempFile1.delete();
            }
            if (tempFile2 != null) {
                tempFile2.delete();
            }
        }
    }

    private static class StreamHandler implements Runnable {
        private final InputStream is;
        private final Consumer<String> consumer;
        private final Runnable done;

        public StreamHandler(InputStream is, Consumer<String> consumer, Runnable done) {
            this.is = is;
            this.consumer = consumer;
            this.done = done;
        }

        @Override
        public void run() {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = br.readLine()) != null) {
                    consumer.accept(line);
                }
            } catch (IOException e) {
                consumer.accept("caught exception: " + e);
            } finally {
                done.run();
            }
        }
    }
}
