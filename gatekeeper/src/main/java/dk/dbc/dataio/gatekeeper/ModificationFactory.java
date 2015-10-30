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
import dk.dbc.dataio.gatekeeper.operation.Opcode;
import dk.dbc.dataio.gatekeeper.transfile.TransFile;
import dk.dbc.dataio.gatekeeper.wal.Modification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class derives the necessary WAL modifications from a given transfile
 */
public class ModificationFactory {
    private static final String EMPTY_STRING = "";

    enum Type {
        DATAIO_EXCLUSIVE,
        POSTHUS_EXCLUSIVE,
        PARALLEL
    }

    private final TransFile transfile;
    private final StringBuilder newTransfile = new StringBuilder();

    /**
     * Class Constructor
     * @param transfile transfile for which modifications are to be listed
     * @throws NullPointerException if given null-valued transfile
     */
    public ModificationFactory(TransFile transfile) throws NullPointerException {
        this.transfile = InvariantUtil.checkNotNullOrThrow(transfile, "transfile");
    }

    /**
     * Pacakage scoped constructor used for unit testing
     */
    ModificationFactory() {
        this.transfile = null;
    }

    /**
     * @return list of modifications
     */
    public List<Modification> getModifications() {
        newTransfile.setLength(0);

        final ArrayList<Modification> modifications = new ArrayList<>();
        if (transfile.getLines().isEmpty()) {
            // Handle special case where transfile is empty.

            // For now just move to posthus...
            modifications.add(getFileMoveModification(transfile.getPath().getFileName().toString()));
        } else if (!transfile.isComplete()) {
            modifications.add(getCreateIncompleteTransfileNotificationModification());
            modifications.add(getFileDeleteModification(transfile.getPath().getFileName().toString()));
        } else {
            for (TransFile.Line line : transfile.getLines()) {
                modifications.addAll(processLine(line));
            }
            if (newTransfile.length() > 0) {
                // Since lines may be excluded due to exclusivity we simply write a
                // new transfile with the content from the newTransfile buffer.
                newTransfile.append("slut");
                modifications.add(getCreateTransfileModification(newTransfile.toString()));
            }
            // Delete the original transfile...
            modifications.add(getFileDeleteModification(
                    transfile.getPath().getFileName().toString()));
        }
        return modifications;
    }

    /* Returns modifications for given transfile line based on type
     */
    List<Modification> processLine(TransFile.Line line) {
        final List<Modification> modifications;
        switch (determineType(line)) {
            case PARALLEL:
                modifications = getParallelModifications(line);
                break;
            case POSTHUS_EXCLUSIVE:
                modifications = getPosthusExclusiveModifications(line);
                break;
            case DATAIO_EXCLUSIVE:
                modifications = getDataioExclusiveModifications(line);
                break;
            default:
                modifications = Collections.emptyList();
        }
        return modifications;
    }

    /* Returns modifications when going exclusively to posthus
     */
    List<Modification> getPosthusExclusiveModifications(TransFile.Line line) {
        final ArrayList<Modification> modifications = new ArrayList<>();
        final String dataFilename = getDataFilename(line);
        if (!dataFilename.isEmpty()) {
            modifications.add(getFileMoveModification(dataFilename));
        }
        newTransfile.append(line.getLine()).append(System.lineSeparator());
        return modifications;
    }

    /* Returns modifications when going exclusively to dataio
     */
    List<Modification> getDataioExclusiveModifications(TransFile.Line line) {
        final ArrayList<Modification> modifications = new ArrayList<>();
        modifications.add(getCreateJobModification(line.getLine()));
        final String dataFilename = getDataFilename(line);
        if (!dataFilename.isEmpty()) {
            modifications.add(getFileDeleteModification(dataFilename));
        }
        return modifications;
    }

    /* Returns modifications when going to both dataio and posthus
     */
    List<Modification> getParallelModifications(TransFile.Line line) {
        final ArrayList<Modification> modifications = new ArrayList<>();
        modifications.add(getCreateJobModification(line.getLine()));
        final String dataFilename = getDataFilename(line);
        if (!dataFilename.isEmpty()) {
            modifications.add(getFileMoveModification(dataFilename));
        }
        newTransfile.append(line.getLine()).append(System.lineSeparator());
        return modifications;
    }

    /* Determines type of transfile line
     */
    Type determineType(TransFile.Line line) {
        final String b = line.getField("b");
        final String t = line.getField("t");
        final String o = line.getField("o");

        if ("danbib".equals(b) && "lin".equals(t) && "marc2".equals(o)) {
            return Type.PARALLEL;
        }
        return Type.POSTHUS_EXCLUSIVE;
    }

    String getDataFilename(TransFile.Line line) {
        final String f = line.getField("f");
        if (f == null) {
            return EMPTY_STRING;
        }
        return f.trim();
    }

    Modification getFileDeleteModification(String filename) {
        final Modification fileDelete = new Modification();
        fileDelete.setOpcode(Opcode.DELETE_FILE);
        fileDelete.setTransfileName(transfile.getPath().getFileName().toString());
        fileDelete.setArg(filename);
        return fileDelete;
    }

    Modification getFileMoveModification(String filename) {
        final Modification fileMove = new Modification();
        fileMove.setOpcode(Opcode.MOVE_FILE);
        fileMove.setTransfileName(transfile.getPath().getFileName().toString());
        fileMove.setArg(filename);
        return fileMove;
    }

    Modification getCreateJobModification(String arg) {
        final Modification createTransfile = new Modification();
        createTransfile.setOpcode(Opcode.CREATE_JOB);
        createTransfile.setTransfileName(transfile.getPath().getFileName().toString());
        createTransfile.setArg(arg);
        return createTransfile;
    }

    Modification getCreateTransfileModification(String arg) {
        final Modification createTransfile = new Modification();
        createTransfile.setOpcode(Opcode.CREATE_TRANSFILE);
        createTransfile.setTransfileName(transfile.getPath().getFileName().toString());
        createTransfile.setArg(arg);
        return createTransfile;
    }

    Modification getCreateIncompleteTransfileNotificationModification() {
        final Modification createNotification = new Modification();
        createNotification.setOpcode(Opcode.CREATE_INCOMPLETE_TRANSFILE_NOTIFICATION);
        createNotification.setTransfileName(transfile.getPath().getFileName().toString());
        createNotification.setArg(transfile.getLines().stream()
                .map(TransFile.Line::getLine)
                .collect(Collectors.joining("\n")));
        return createNotification;
    }
}
