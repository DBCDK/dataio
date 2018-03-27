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

package dk.dbc.dataio.bfs.api;

import dk.dbc.invariant.InvariantUtil;

import java.nio.file.Path;

/**
 * Access to binary files stored in file system
 */
public class BinaryFileStoreFsImpl implements BinaryFileStore {
    private final Path base;

    /**
     * Class constructor
     * @param base base path of file system store
     * @throws NullPointerException if given null-valued base argument
     * @throws IllegalArgumentException if given base path is non-absolute
     */
    public BinaryFileStoreFsImpl(Path base) throws NullPointerException, IllegalArgumentException {
        this.base = InvariantUtil.checkNotNullOrThrow(base, "base");
        if (!this.base.isAbsolute()) {
            throw new IllegalArgumentException(String.format(
                    "Unable to initialize binary file store - base path is not absolute: %s", base));
        }
    }

    /**
     * Returns file system binary file representation associated with given path
     * @param path binary file path relative to base path specified in constructor
     * @return binary file representation
     * @throws NullPointerException if given null-valued path argument
     * @throws IllegalArgumentException if given absolute path
     */
    @Override
    public BinaryFile getBinaryFile(Path path) throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullOrThrow(path, "path");
        if (path.isAbsolute()) {
            throw new IllegalArgumentException("Value of path parameter can not be absolute path " + path);
        }
        return new BinaryFileFsImpl(base.resolve(path));
    }
}
