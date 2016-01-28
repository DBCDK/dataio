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
     * Add an object to an array, if the object is not already in the array
     * 
     * @type {function}
     * @syntax Util.addUniqueObjectToArray( array, obj );
     * @param {Array} array An array of objects to push an unique object to.
     * @param {Object} obj The object to push to the array.
     * @return {Array } the updated array.
     * @name Util.addUniqueObjectToArray
     * @example Util.addUniqueObjectToArray( [{ "firstName": "Hans", "familyName": "Jensen" }, { "firstName": "Jens", "familyName": "Hansen"}, { "firstName": "Jens", "familyName": "Jensen" }], { "firstName": "Hans","familyName": "Hansen" } ); 
     * //returns [{ "firstName": "Hans", "familyName": "Jensen" }, { "firstName": "Jens", "familyName": "Hansen" }, { "firstName": "Jens", "familyName": "Jensen" }, { "firstName": "Hans","familyName": "Hansen" } ]
     * 
     * @method 
     */

    function addUniqueObjectToArray( array, obj ) {

        var isUnique = true;
        for ( var i = 0; i < array.length; i++ ) {
            if ( 0 === array.length || true === _.isEqual( array[ i ], obj ) ) {
                var isUnique = false;
                break;
            }
        }
        if ( true === isUnique ) {
            array.push( obj );
        }
        return array;
    }

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
     * @example
     * Util.arrayOfKeys( { 'elementName': 'title', 'elementValue': 'Den gamle mand og havet' } ),
     * //returns [ "elementName", "elementValue" ]
     *  
     * @method */

    function arrayOfKeys( obj ) {

        var result = [ ];
        for ( var elem in obj )
            if ( obj.hasOwnProperty( elem ) ) {
                result.push( elem );
            }
        return result;
    }

    /**
     * Create object that has the argument as prototype
     * 
     * See Crockford page 22. In some of Crockfords notes, this function is called 
     * "beget". This will become Object.create in ES5.
     *  
     * @type {function}
     * @syntax Util.keySortedObject( prototype_object );
     * @param {Object} 
     * @return {Object} 
     * @name Util.create
     * @method */

    function create( prototype_object ) {
        var F = function( ) {};
        F.prototype = prototype_object;
        return new F( );
    }

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
    }



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

    function getType( obj ) {

        var type = typeof( obj );
        if ( type === "object" && obj !== null && obj.constructor ) {
            var match = RegExp( "function ([^(][^(]*)" ).exec( obj.constructor.toSource( ) );
            if ( match && match.length == 2 ) {
                return match[ 1 ];
            }
        }
        return type;
    }

    /**
     * Are two objects the same.
     * 
     * Compares two objects to determine if they are equal.
     * 
     * @type {function}
     * @syntax Util.isEqual(object1, object2)
     * @param {object} object1 The first object for the comparison test
     * @param {object} object2 The second object for the comparison test
     * @return {boolean} true if the two objects are equal, otherwise false
     * @name Util.isEqual
     * @example
     * Util.isEqual( { elementName: 'title', elementValue: 'Den gamle mand og havet' }, { elementName: 'title', elementValue: 'Den gamle mand og havet' } )
     * //returns true
     * 
     * Util.isEqual( { elementValue: 'Den gamle mand og havet', elementName: 'title' }, { elementName: 'title', elementValue: 'Den gamle mand og havet' } )
     * //returns true
     * 
     * Util.isEqual( { elementName: 'title', elementValue: 'Den gamle mand og havet' }, { elementName: 'title', elementValue: 'Den gamle mand og havet', elementProperty: "full" } )
     * //returns false
     * 
     * @method */

    function isEqual( object1, object2 ) {

        return _.isEqual( object1, object2 );

    }



    /**
     * Is this an object. Do not use this function in new code.
     * 
     * Figures out if this is an object, using some tricks from the bag of RJE.
     * 
     * @type {function}
     * @syntax Util.isObject(obj);
     * @param {object} obj The object to test for beeing an object.
     * @return {boolean} true if obj is an object (not null or array)
     * @name Util.isObject
     * @method */

    function isObject( obj ) {
        throw "deprecated"; //This is probably inserted to prevent any use of the function
        return obj != null && typeof( obj ) === "object" && !( obj instanceof Array ) && !( obj instanceof Date );
    }

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
     * @example Util.keySortedObject( { elementName: 'title', elementValue: 'Den gamle mand og havet', elementProperty: "full" });
     * //returns { elementName: 'title', elementProperty: "full", elementValue: 'Den gamle mand og havet' }
     * 
     * @method */

    function keySortedObject( obj ) {

        if ( obj === null || typeof( obj ) !== "object" || ( obj instanceof Array ) || ( obj instanceof Date ) ) {
            return obj;
        }
        var result = {};
        _.keys( obj ).sort( ).forEach( function( key ) {
            result[ key ] = obj[ key ];
        } );
        return result;
    }

    /**
     * Method that handles numeric sorting of an array.
     *
     * @type {function}
     * @syntax Util.sortArrayOfNumbers( array, [mode] )
     * @param {Array} array An array containing numbers. The numbers are also allowed as strings.
     * @param {string} [mode] A "d" giving descending order or anything else giving ascending order
     * @return {Array} A new array containing sorted numbers
     * @example Util.sortArrayOfNumbers( [ 3, 7, 12, 1, 100, 47 ], "d" )
     * //returns [ 100, 47, 12, 7, 3, 1 ]
     *  
     * @example Util.sortArrayOfNumbers( [ "020", "3", "75" ,"301", "001" ] )
     * // returns [ "001", "3", "020", "75" ,"301" ]
     * 
     * @method
     * @name Util.sortArrayOfNumbers
     */
    function sortArrayOfNumbers( array, mode ) {

        array.sort( function( a, b ) {
            if ( mode !== "d" ) {
                return a - b;
            } else {
                return b - a;
            }
        } );

        return array;

    }

    /**
     * Method that handles alphabetical sorting on a specified property for an array of objects.
     *
     * @type {function}
     * @syntax Util.sortArrayOfObjects( array, property )
     * @param {Array} array An array containing objects
     * @param {string} property The name of the property used for sorting
     * @return {Array} A new array containing sorted objects
     * @example Util.sortArrayOfObjects( [ { "value": "hest", "type": "animal" },{ "value": "ko", "type": "animal" },{ "value": "gris", "type": "animal" } ], "value" ) 
     * // returns [ { "value": "gris", "type": "animal" }, { "value": "hest", "type": "animal" },{ "value": "ko", "type": "animal" } ]
     * 
     * @method
     * @name Util.sortArrayOfObjects
     */
    function sortArrayOfObjects( array, property ) {

        Log.trace( "Entering: Util.sortArrayOfObjects method" );

        var sortedArray = array.sort( function( a, b ) {
            if ( a[ property ] < b[ property ] ) {
                return -1;
            }
            if ( a[ property ] > b[ property ] ) {
                return 1;
            }
            return 0;
        } );

        Log.trace( "Leaving: Util.sortArrayOfObjects method" );

        return sortedArray;

    }

    /**
     * Method that removes duplicate objects in an array.
     *
     * @type {function}
     * @syntax Util.uniqueObjectsInArray( arrayOfObjects )
     * @param {Array} array An array containing Objects
     * @return {Array} A new array containing only unique objects
     * @example Util.uniqueObjectsInArray( [ { "animal": "horse", "colour": "brown" },{ "animal": "horse", "colour": "black" },{ "animal": "horse", "colour": "brown" } ] )
     * //returns [ { "animal": "horse", "colour": "black" },{ "animal": "horse", "colour": "brown" } ]
     * 
     * @method
     * @name Util.uniqueObjectsInArray
     */

    function uniqueObjectsInArray( arrayOfObjects ) {

        var numberOfObjects = arrayOfObjects.length;
        var arrayOfUniqueObjects = [ ];
        for ( var j = 0; j < arrayOfObjects.length; j++ ) {
            var rest = arrayOfObjects.slice( j + 1, numberOfObjects )
            var currentObject = arrayOfObjects[ j ];
            var isUnique = true;
            for ( var i = 0; i < rest.length; i++ ) {
                if ( _.isEqual( currentObject, rest[ i ] ) ) {
                    var isUnique = false;
                    break;
                }
            };
            if ( true === isUnique ) {
                arrayOfUniqueObjects.push( currentObject );
            }
        }
        return arrayOfUniqueObjects;
    }


    /**
     * Method that removes duplicate values in an array.
     * 
     * @type {function}
     * @syntax Util.uniqueValues( array )
     * @param {Array} array The sorted array where duplicates need to be removed
     * @return {Array} An array containing unique values
     * @example Util.uniqueValues( [ "207", "217", "217", "583", "217"] )
     * //returns [ "207", "583", "217"]
     * 
     * @method
     * @name Util.uniqueValues
     */
    function uniqueValues( array ) {

        Log.trace( "Entering: Util.uniqueValues method" );

        var uniqueValueArray = [ ];
        for ( var i = 0; i < array.length; i++ ) {
            if ( uniqueValueArray.indexOf( array[ i ] ) < 0 ) {
                uniqueValueArray.push( array[ i ] );
            }
        }

        Log.trace( "Leaving: Util.uniqueValues method" );

        return uniqueValueArray;

    }

    return {

        addUniqueObjectToArray: addUniqueObjectToArray,
        arrayOfKeys: arrayOfKeys,
        create: create,		
        forEachGroupedRegExpMatch: forEachGroupedRegExpMatch,
        getType: getType,
        isEqual: isEqual,
		isObject: isObject,
        keySortedObject: keySortedObject,
		sortArrayOfNumbers: sortArrayOfNumbers,			
		sortArrayOfObjects: sortArrayOfObjects,	
        uniqueObjectsInArray: uniqueObjectsInArray,
        uniqueValues: uniqueValues

    };

}( );

// unittests are in TestUtil.use.js
// (because the UnitTest module is not available from this module)
