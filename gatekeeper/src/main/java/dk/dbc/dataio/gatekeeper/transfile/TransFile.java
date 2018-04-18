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

import dk.dbc.dataio.gatekeeper.EncodingDetector;
import dk.dbc.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
import java.util.stream.Collectors;

/**
 * Simple transfile parser
 */
public class TransFile {
    public static final Pattern END_OF_FILE = Pattern.compile("slut|finish", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern DATAFILE_NAME = Pattern.compile("^[\\p{L}0-9._-]*$");
    private static final Logger LOGGER = LoggerFactory.getLogger(TransFile.class);

    private final Path path;
    private boolean isComplete = false;
    private boolean isValid = true;
    private String causeForInvalidation;
    private List<Line> lines = new ArrayList<>();

    /**
     * Return new transfile representation
     * @param transfile path of transfile to parse
     * @throws NullPointerException if given null-valued transfile path
     */
    public TransFile(Path transfile) throws NullPointerException {
        path = InvariantUtil.checkNotNullOrThrow(transfile, "transfile");
        if (Files.exists(transfile)) {
            final EncodingDetector encodingDetector = new EncodingDetector();
            try (final Scanner fileScanner = new Scanner(transfile, encodingDetector.detect(transfile)
                    .orElse(StandardCharsets.UTF_8).name())) {
                fileScanner.findWithinHorizon(EncodingDetector.BOM, 1);
                parse(fileScanner);
            } catch (IOException e) {
                invalidate("Trans fil kunne ikke læses: " + e.getMessage());
            }
        }
    }

    private void parse(Scanner scanner) {
        while (scanner.hasNextLine()) {
            if (scanner.hasNext(END_OF_FILE)) {
                scanner.nextLine();
                isComplete = true;
                break;
            }
            final String nextLine = scanner.nextLine();
            if (!nextLine.trim().isEmpty()) {
                lines.add(new Line(nextLine));
            }
        }
        verify();
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
     * @return true if transfile is valid, otherwise false
     */
    public boolean isValid() {
        return isValid;
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
     * @return cause for transfile invalidation or null if transfile is valid
     */
    public String getCauseForInvalidation() {
        return causeForInvalidation;
    }

    @Override
    public String toString() {
        return lines.stream()
                .map(TransFile.Line::getLine)
                .collect(Collectors.joining("\n"));
    }

    private void invalidate(String cause) {
        isValid = false;
        causeForInvalidation = cause;
    }

    private void verify() {
        if (isValid) { // only verify if transfile has not already been invalidated
            if (!isComplete) {
                invalidate("Transfil mangler slut-linje");
                return;
            }

            if (WHITESPACE.matcher(path.getFileName().toString()).find()) {
                invalidate("Transfilnavn indeholder blanktegn");
                return;
            }

            for (Line line : lines) {
                final String f = line.getField("f");
                if (f != null && !f.isEmpty() && !DATAFILE_NAME.matcher(f).matches()) {
                    invalidate(String.format("Datafilnavn <%s> indeholder ulovlige tegn", f));
                    return;
                }
            }
        }
    }

    /**
     * Transfile line representation
     */
    public static class Line {
        private final String line;
        private final Map<String, String> fields = new HashMap<>();
        private boolean isModified = false;

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

        /* Copy constructor */
        public Line(Line line) throws NullPointerException {
            this(line.getLine());
        }

        /**
         * @return string representation of line
         */
        public String getLine() {
            if (isModified) {
                return fields.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .collect(Collectors.joining(","));
            }
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

        /**
         * Sets value of field
         * @param fieldName name of field
         * @param fieldValue value of field
         */
        public void setField(String fieldName, String fieldValue) {
            if (fieldName != null) {
                isModified = true;
                fields.put(fieldName, fieldValue);
            }
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
