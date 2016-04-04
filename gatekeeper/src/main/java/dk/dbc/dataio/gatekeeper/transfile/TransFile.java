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

package dk.dbc.dataio.gatekeeper.transfile;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Simple non-validating transfile parser
 */
public class TransFile {
    public static final Pattern END_OF_FILE = Pattern.compile("slut|finish", Pattern.CASE_INSENSITIVE);

    private static final Logger LOGGER = LoggerFactory.getLogger(TransFile.class);

    private final Path path;
    private boolean isComplete = false;
    private List<Line> lines = new ArrayList<>();

    /**
     * Return new transfile representation
     * @param transfile path of transfile to parse
     * @throws NullPointerException if given null-valued transfile path
     * @throws UncheckedIOException if unable to read transfile
     */
    public TransFile(Path transfile) throws NullPointerException, IllegalArgumentException {
        path = InvariantUtil.checkNotNullOrThrow(transfile, "transfile");
        parse(path);
    }

    private void parse(Path transfile) throws UncheckedIOException {
        LOGGER.info("Parsing transfile {}", transfile.toAbsolutePath());
        if (Files.exists(transfile)) {
            try (final Scanner fileScanner = new Scanner(transfile, StandardCharsets.UTF_8.name())) {
                while (fileScanner.hasNextLine()) {
                    if (fileScanner.hasNext(END_OF_FILE)) {
                        fileScanner.nextLine();
                        isComplete = true;
                        break;
                    }
                    final String nextLine = fileScanner.nextLine();
                    if (!nextLine.trim().isEmpty()) {
                        lines.add(new Line(nextLine));
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * @return true if transfile exists in the file system, otherwise false
     */
    public boolean exists() {
        return Files.exists(path);
    }

    /**
     * @return true if transfile contained end-of-file marker, otherwise false
     */
    public boolean isComplete() {
        return isComplete;
    }

    /**
     * @return Transfile lines as unmodifiable list
     */
    public List<Line> getLines() {
        return Collections.unmodifiableList(lines);
    }

    /**
     * @return path of transfile
     */
    public Path getPath() {
        return path;
    }

    /**
     * Transfile line representation
     */
    public static class Line {
        private final String line;
        private final Map<String, String> fields = new HashMap<>();

        /**
         * Constructor
         * @param line transfile raw line value
         * @throws NullPointerException if given null-valued line
         * @throws IllegalArgumentException if given empty-valued line
         */
        public Line(String line) throws NullPointerException, IllegalArgumentException {
            this.line = InvariantUtil.checkNotNullNotEmptyOrThrow(line, "line");
            parse();
        }

        /**
         * @return line in its raw input form
         */
        public String getLine() {
            return line;
        }

        /**
         * @return list of field names extracted from line
         */
        public Set<String> getFieldNames() {
            return fields.keySet();
        }

        /**
         * Gets value of field
         * @param fieldName name of field
         * @return value of field or null if field did not exist in line
         */
        public String getField(String fieldName) {
            return fields.get(fieldName);
        }

        private void parse() {
            final String trimmedLine = line.replaceAll("\\s", "");
            final StringTokenizer lineTokenizer = new StringTokenizer(trimmedLine, ",");
            while (lineTokenizer.hasMoreElements()) {
                final String keyValuePair = lineTokenizer.nextToken();
                final StringTokenizer keyValueTokenizer = new StringTokenizer(keyValuePair, "=");
                if (keyValueTokenizer.countTokens() == 2) {
                    fields.put(keyValueTokenizer.nextToken(), keyValueTokenizer.nextToken());
                } else {
                    LOGGER.error("Invalid key/value pair '{}'", keyValuePair);
                }
            }
        }
    }
}
