/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.types;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

/*
    This stinks beyond reason - GWT serialization won't work with
    an interface or an abstract class, so this needs to be
    actual instantiable type.
*/

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, property="type")
public class Pickup implements Serializable {
    Pickup() {}
}
