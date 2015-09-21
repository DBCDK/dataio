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

/** @file System module. Provides System class, that can be populated by other modules. */
EXPORTED_SYMBOLS = [ 'System' ];

/**
 * Provides a namespace to add lowlevel/system like functionality to.
 * 
 * Methods and modules related to low-level stuff is added into this
 * namespace. 
 *
 * To actually use most of the functions in this namespace, you need
 * to load additional modules. E.g. to be able to use the 
 * {@link System.readFile} function, you must use the module `ReadFile`. to
 * use the {@link System.print} function you must use the module
 * `Print`. For all functions, it is indicated which module they are
 * part of, that is, which module you should use to get access to the
 * function.
 * 
 * @namespace 
 * @name System */
var System = function( ) {
    var that = {};

    /**
     * Array containing arguments passed to the system, if invoked interactively.
     * 
     * Contains any arguments passed to the system, if used interactively. Is
     * an empty array if not. 
     *
     * @example
// Get the first argument to a script when called in a shell
use( "System" );
if ( System.arguments.length > 0 ) {
  var firstArg = System.arguments[0];
}
     *
     * @name System.arguments
     * @type {String[]}
     * @property */
    that.arguments = [ ];

    /** 
     * String containing the name of the called script, if invoked interactively.
     * 
     * @example
// Print the name of the calling script
use( "System" );
use( "Print" );
print( System.scriptname + "\n" );
     * 
     * @name System.scriptname
     * @type {String}
     * @property */
    that.scriptname = "";

    return that;
}( );
