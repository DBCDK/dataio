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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileDeleteOperation implements Operation {
    private static final Opcode OPCODE = Opcode.DELETE_FILE;

    final Path file;

    public FileDeleteOperation(Path file) throws NullPointerException {
        this.file = InvariantUtil.checkNotNullOrThrow(file, "file");
    }

    @Override
    public Opcode getOpcode() {
        return OPCODE;
    }

    @Override
    public void execute() throws OperationExecutionException {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new OperationExecutionException(e);
        }
    }

    public Path getFile() {
        return file;
    }
}
