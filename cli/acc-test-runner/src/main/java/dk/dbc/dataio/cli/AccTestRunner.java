package dk.dbc.dataio.cli;

import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;

public class AccTestRunner implements Callable<Integer> {
    @CommandLine.Parameters(index = "0", description = "job specification file")
    private Path jobSpec;
    @CommandLine.Parameters(index = "1", description = "data file")
    private Path datafile;
    @CommandLine.Option(names = "-f", description = "Flowstore url")
    private String flowstore;

    public static void main(String[] args) {
        new CommandLine(new AccTestRunner()).setCaseInsensitiveEnumValuesAllowed(true).registerConverter(Path.class, Path::of).execute(args);
    }

    @Override
    public Integer call() throws Exception {
        return 0;
    }
}
