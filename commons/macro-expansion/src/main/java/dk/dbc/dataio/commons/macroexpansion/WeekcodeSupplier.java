/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.commons.macroexpansion;

import java.time.LocalDate;

@FunctionalInterface
public interface WeekcodeSupplier {
    String get(String catalogueCode, LocalDate localDate);
}
