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

package dk.dbc.dataio.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import dk.dbc.dataio.cli.command.CreateCommand;
import dk.dbc.dataio.cli.options.CreateOptions;
import dk.dbc.dataio.cli.options.Options;

/**
 * dataIO flow component command line interface
 * <p>
 *     To show usage run
 *
 *     %> java -jar dataio-cli-flow-component.jar
 * </p>
 */
public class FlowComponentCli {
    private final Options options;
    private final CreateOptions createOptions;

    public static void main(String[] args) {
        final FlowComponentCli cli = new FlowComponentCli();
        cli.run(args);
    }

    private FlowComponentCli() {
        options = new Options();
        createOptions = new CreateOptions();
    }

    private void run(String[] args) {
        final JCommander argParser = new JCommander(options);
        try {
            argParser.setProgramName("dataio-flow-component");
            argParser.addCommand("create", createOptions);
            argParser.parse(args);

            if (args.length == 0 || options.help || createOptions.help) {
                argParser.usage();
                System.exit(0);
            }

            switch (argParser.getParsedCommand()) {
                case "create": new CreateCommand(createOptions).execute();
                    break;
                default:
                    argParser.usage();
            }
        } catch (ParameterException e) {
            System.out.println(e.getMessage());
            argParser.usage();
            System.exit(-1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        System.exit(0);
    }
}
