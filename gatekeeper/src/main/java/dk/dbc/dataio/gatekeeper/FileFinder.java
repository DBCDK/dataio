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

package dk.dbc.dataio.gatekeeper;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FileFinder {
    /**
     * Finds all files in given directory ending with the specified
     * file extension(s)
     * @param dir directory to search
     * @param extensions only match files with given extensions (dot is not required)
     * @return list of matching files
     * @throws NullPointerException if given null-valued argument
     * @throws IOException if directory does not exists or similar error condition
     */
    public static List<Path> findFilesWithExtension(Path dir, Set<String> extensions)
            throws NullPointerException, IOException {
        InvariantUtil.checkNotNullOrThrow(dir, "dir");
        InvariantUtil.checkNotNullOrThrow(extensions, "extensions");
        final FindFilesWithExtensionVisitor findFilesWithExtensionVisitor =
                new FindFilesWithExtensionVisitor(extensions);
        Files.walkFileTree(dir, findFilesWithExtensionVisitor);
        return findFilesWithExtensionVisitor.getMatchingFilesFound();
    }

    static class FindFilesWithExtensionVisitor extends SimpleFileVisitor<Path> {
        private final List<Path> matchingFilesFound;
        private final Set<String> extensions;

        public FindFilesWithExtensionVisitor(Set<String> extensions) {
            this.matchingFilesFound = new ArrayList<>();
            this.extensions = extensions;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            matchingFilesFound.addAll(extensions.stream()
                    .filter(extension -> file.toString().endsWith(extension))
                    .map(extension -> file)
                    .collect(Collectors.toList()));

            return FileVisitResult.CONTINUE;
        }

        public List<Path> getMatchingFilesFound() {
            return Collections.unmodifiableList(matchingFilesFound);
        }
    }
}
