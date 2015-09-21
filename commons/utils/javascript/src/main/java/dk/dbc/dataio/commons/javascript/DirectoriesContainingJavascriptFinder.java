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

package dk.dbc.dataio.commons.javascript;

import java.nio.file.SimpleFileVisitor;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.FileSystems;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.FileVisitResult;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class DirectoriesContainingJavascriptFinder extends SimpleFileVisitor<Path> {

    private final PathMatcher matcher;
    private final Set<Path> javascriptDirectories;

    DirectoriesContainingJavascriptFinder() {
        matcher = FileSystems.getDefault().getPathMatcher("glob:*.js");
        javascriptDirectories = new HashSet<>();
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        if (isFileAJavascript(file)) {
            javascriptDirectories.add(file.getParent());
        }
        return FileVisitResult.CONTINUE;
    }

    private boolean isFileAJavascript(Path file) {
        Path name = file.getFileName();
        if (name != null && matcher.matches(name)) {
            return true;
        }
        return false;
    }

    public List<Path> getJavascriptDirectories() {
        return new ArrayList<>(javascriptDirectories);
    }
}
