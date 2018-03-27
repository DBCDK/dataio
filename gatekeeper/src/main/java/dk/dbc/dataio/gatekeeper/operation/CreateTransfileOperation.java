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

package dk.dbc.dataio.gatekeeper.operation;

import dk.dbc.invariant.InvariantUtil;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class CreateTransfileOperation implements Operation {
    private static final Opcode OPCODE = Opcode.CREATE_TRANSFILE;
    private static final long ONE_SECOND = 1000;

    final Path destination;
    final String content;

    public CreateTransfileOperation(Path destination, String content)
            throws NullPointerException, IllegalArgumentException {
        this.destination = InvariantUtil.checkNotNullOrThrow(destination, "destination");
        this.content = InvariantUtil.checkNotNullNotEmptyOrThrow(content, "content");
    }

    @Override
    public Opcode getOpcode() {
        return OPCODE;
    }

    @Override
    public void execute() throws OperationExecutionException {
        try {
            // Wait to overcome old POSTHUS limitation regarding transfile timestamp sorting
            Thread.sleep(ONE_SECOND);

            Files.write(destination, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
        } catch (Exception e) {
            throw new OperationExecutionException(e);
        }
    }

    public Path getDestination() {
        return destination;
    }

    public String getContent() {
        return content;
    }
}
