/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.periodicjobs;

import java.time.LocalDate;

@FunctionalInterface
public interface WeekcodeSupplier {
    String get(String catalogueCode, LocalDate localDate);
}
