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

package dk.dbc.dataio.commons.utils.transfile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TransFile {
    private static final String ENCODING = "UTF-8";
    public static class UnexpectedEndOfFileException extends RuntimeException {}

    /**
     * TransFile constructor
     */
    private TransFile() {}  // The constructor is private to make the class static
    
    /**
     * Processes all lines in a TransFile input stream
     * 
     * @param inputStream the input stream
     * @return list of trans file data
     *
     * @throws UnexpectedEndOfFileException if the end of file was reached unexpectedly
     * @throws IllegalArgumentException if an illegal argument was found
     */
    public static List<TransFileData> process(InputStream inputStream) throws UnexpectedEndOfFileException, IllegalArgumentException {
        List<TransFileData> transFile = new ArrayList<>();
        Scanner fileScanner = new Scanner(inputStream, ENCODING);

        while (fileScanner.hasNextLine()) {
            if (fileScanner.hasNext("slut")) {
                return transFile;
            }
            transFile.add(new TransFileData(fileScanner.nextLine()));
        }
        throw new UnexpectedEndOfFileException();
    }
    
}
