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

/** @file Infrastructure support for marc classes */
//------------------------------------------------------------------------------------------------------
/*!
    \file textcodec.js
    \brief 
*/
//------------------------------------------------------------------------------------------------------

//------------------------------------------------------------------------------------------------------
//                      Exported Symbols
//------------------------------------------------------------------------------------------------------

EXPORTED_SYMBOLS = [ 'getMatchField', 'getMatchSubField', 'MatchField', 'MatchSubField',
    'DBCMatchField', 'MatchFunc'
];

//------------------------------------------------------------------------------------------------------
//                            Global help functions
//------------------------------------------------------------------------------------------------------

/**
 * Converts the argument to a MatchField instance.
 * 
 * @param {MatchField|RegExp} arg A MatchField or a regular expression of field names.
 * 
 * @return {MatchField} The MatchField after arg is converted to it.
 */
function getMatchField( arg ) {
    if ( arg == undefined ) {
        return new MatchField( );
    };

    if ( arg instanceof RegExp ) {
        return new MatchField( arg );
    };

    return arg;
};

function getMatchSubField( arg ) {
    if ( arg == undefined ) {
        return new MatchSubField( );
    };

    if ( arg instanceof RegExp ) {
        return new MatchSubField( arg );
    };

    return arg;
};

//------------------------------------------------------------------------------------------------------
/** Defines a matcher object that can match a field.
 * 
 * This object is mainly used with for instance Record.eachField to specify how to 
 * match each field that needs to be processed. 
 *
 * @param {RegExp} fieldPattern Pattern to match a field name.
 * @param {RegExp} indicatorPattern Pattern to match a field indicator.
 * @param {RegExp} subFieldPattern Pattern to match a string with all sub field names 
 * 		  in the field.
 * @param {Number} min Minimum occurrence of the field to be matched.   
 * @param {Number} max Maximum occurrence of the field to be matched.   
 * @constructor
 * @see MarcClasses
 * @see Record
 * @see Field
 * @name MatchField */
function MatchField( fieldPattern, indicatorPattern, subFieldPattern, min, max ) {
    this.fieldPattern = fieldPattern;
    this.indicatorPattern = indicatorPattern;
    this.subFieldPattern = subFieldPattern;
    this.min = min;
    this.max = max;
    if ( this.max === undefined ) {
        this.max = this.min;
    };

    this.matchCounter = new Object;
};

MatchField.prototype.matchField = function( record, field ) {
    if ( this.fieldPattern != undefined && !this.fieldPattern.test( field.name ) ) {
        return false;
    };

    if ( this.indicatorPattern != undefined && !this.indicatorPattern.test( field.indicator ) ) {
        return false;
    };

    if ( this.subFieldPattern != undefined ) {
        var subFields = "";
        for ( var i = 0; i < field.count( ); i++ ) {
            subFields += field.subfield( i ).name;
        };

        if ( !this.subFieldPattern.test( subFields ) ) {
            return false;
        };
    };

    var v = this.matchCounter[ field.name ];
    if ( v === undefined ) {
        v = 0;
    };
    v++;
    this.matchCounter[ field.name ] = v;

    return ( this.min === undefined || this.min <= v ) && ( this.max === undefined || v <= this.max );
};

//------------------------------------------------------------------------------------------------------
/*!
    \brief 
*/

function MatchSubField( namePattern, valuePattern ) {
    this.namePattern = namePattern;
    this.valuePattern = valuePattern;
};

MatchSubField.prototype.matchSubField = function( field, subField ) {
    if ( this.namePattern != undefined ) {
        if ( !this.namePattern.test( subField.name ) ) {
            return false;
        };
    };

    if ( this.valuePattern != undefined ) {
        if ( !this.valuePattern.test( subField.value ) ) {
            return false;
        };
    };

    return true;
};

//------------------------------------------------------------------------------------------------------
/*!
    \brief
*/

function DBCMatchField( fieldPattern, subFieldPattern ) {
    this.impl = new MatchField( fieldPattern, undefined, subFieldPattern );
};

DBCMatchField.prototype.matchField = function( record, field ) {
    return this.impl.matchField( record, field );
};

//------------------------------------------------------------------------------------------------------
/*!
    \brief Object that can match fields in a record and call a function on each of them.    
*/

function MatchFunc( funcMatcher ) {
    this.funcMatcher = funcMatcher;
};

MatchFunc.prototype.matchField = function( record, field ) {
    if ( this.funcMatcher != undefined ) {
        return this.funcMatcher( field );
    };

    return false;
};

MatchFunc.prototype.matchSubField = function( record, field, subField ) {
    if ( this.funcMatcher != undefined ) {
        return this.funcMatcher( subField );
    };

    return false;
};
