package dk.dbc.dataio.commons.javascript;

import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
        return name != null && matcher.matches(name);
    }

    public List<Path> getJavascriptDirectories() {
        return new ArrayList<>(javascriptDirectories);
    }
}
