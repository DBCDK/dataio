/** @file Various utilities */

EXPORTED_SYMBOLS = [ 'Util' ];
use( "Underscore" );

/**
 * Various utilities.
 * 
 * Various utilities, mostly related to objects.
 * 
 * @name Util
 * @namespace 
 */
var Util = function( ) {
    /**
     * Map a function across the matches of a RegExp. 
     *
     * **TODO:** RJE needs to document this, and give at least one example on how to use it.
     *
     * @type {function}
     * @method
     * @param {string} str The string to search for matches.
     * @param {RegExp} re Regular expression to match against.
     * @param {function} fn Function to map, this gets the matched regex-groups as parameters (the full match as the first of them).
     * @name Util.forEachGroupedRegExpMatch
     */

    function forEachGroupedRegExpMatch( str, re, fn ) {
        // JavaScript quirck: the easiest way to map a function across the _groups_ of the matches of a regex is to call replace and throw the result away. 
        str.replace( re, fn );
    };

    return {
        forEachGroupedRegExpMatch: forEachGroupedRegExpMatch,
        /**
         * Get the type of an object.
         * 
         * Get the type of an object, using some tricks from the bag of RJE...
         * 
         * @type {function}
         * @syntax Util.getType(obj);
         * @param {object} obj The object to get the type for.
         * @return {string} typeof obj, or name of obj.constructor if available.
         * @name Util.getType
         * @method
         */
        getType: function( obj ) {
            var type = typeof( obj );
            if ( type === "object" && obj !== null && obj.constructor ) {
                var match = RegExp( "function ([^(][^(]*)" )( obj.constructor.toSource( ) );
                if ( match && match.length == 2 ) {
                    return match[ 1 ];
                }
            }
            return type;
        },
        /**
         * Get the keys from an object as an array.
         * 
         * Get an array of keys of the object, using some tricks from the bag of
         * RJE 
         * 
         * @type {function}
         * @syntax Util.arrayOfKeys(obj);
         * @param {object} obj The object to get the keys for.
         * @return {object[]} Returns a new array containing all the keys of obj.
         * @name Util.arrayOfKeys
         * @method */
        arrayOfKeys: function( obj ) {
            var result = [ ];
            for ( var elem in obj ) if ( obj.hasOwnProperty( elem ) ) {
                    result.push( elem );
                }
            return result;
        },
        /**
         * Is this an object.
         * 
         * Figures out if this is an object, using some tricks from the bag of RJE.
         * 
         * @type {function}
         * @syntax Util.getType(obj);
         * @param {object} obj The object to test for beeing an object.
         * @return {boolean} true if obj is an object (not null or array)
         * @name Util.isObject
         * @method */
        isObject: function( obj ) {
            throw "deprecated";
            return obj != null && typeof( obj ) === "object" && !( obj instanceof Array ) && !( obj instanceof Date );
        },
        /**
         * Get a new object with sorted keys.
         * 
         * Get a new object with the same keys/values as the obj, but inserted in
         * alphabetical order, using some tricks from the bag of RJE.
         * 
         * @type {function}
         * @syntax Util.keySortedObject(obj);
         * @param {object} obj The object to sort keys for.
         * @return {object} A new object with the same keys/values as the obj, but inserted in alphabetical order
         * @name Util.keySortedObject
         * @method */
        keySortedObject: function( obj ) {
            if ( obj === null || typeof( obj ) !== "object" || ( obj instanceof Array ) || ( obj instanceof Date ) ) {
                return obj;
            }
            var result = {};
            _.keys( obj ).sort( ).forEach( function( key ) {
                    result[ key ] = obj[ key ];
                } );
            return result;
        }
    }
}( );
