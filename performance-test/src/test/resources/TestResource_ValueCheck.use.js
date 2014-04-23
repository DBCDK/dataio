/** @file Module to handle argument checking in a neater way. */

use( "UnitTest" );
// use( "Log" );

EXPORTED_SYMBOLS = [ 'ValueCheck' ];

/**
 * Functions to check arguments presence and value.
 * 
 * This module/namespace provides a number of functions that can be used
 * to systematically check that parameters/variables are set and check
 * that the values fullfill certain criteria. The functions provided are
 * chainable, which leads to easy-to-read expressions. For more
 * information about the actual constraints, see {@link ValueCheck.CheckObject}.
 * 
 * If a caller passes parameters to the function that does not match
 * the constraints, a suitable exception will be thrown. This ensures
 * that you can write the rest of the function with the knowledge that
 * the parameters are within the values the function can handle.
 *
 * @type {namespace}
 * @borrows ValueCheck.check as checkThat 
 * @borrows ValueCheck.check as requireThat 
 * @example 
// Say that you want to check the arguments foo, bar and baz to a function. You want to check that
// 
//   a) foo is a non-empty string,
//   b) bar must be a number between 0 and 100 (inclusive), and
//   c) baz should be either the string "success", "suspend" or the string "failure" 
// 
//   You could then write the function somewhat like this:
function myTestFunction( foo, bar, baz ) {
    ValueCheck.checkThat( "foo", foo ).is.defined.and.has.type( "string" ).and.value.that.is.not.equalTo( "" );
    ValueCheck.checkThat( "bar", bar ).is.defined.with.type( "number" ).and.inside( 0, 100 );
    ValueCheck.checkThat( "baz", baz ).is.defined.and.has.type( "string" ).and.is.in( [ "success", "suspend", "failure" ] );

    // Rest of function
}
// The tests could also be written a lot more compact, if you wish:
ValueCheck.checkThat( "foo", foo ).defined.type( "string" ).not.equalTo( "" );
ValueCheck.checkThat( "bar", bar ).defined.type( "number" ).inside( 0, 100 );
ValueCheck.checkThat( "baz", baz ).defined.type( "string" ).in( [ "success", "suspend", "failure" ] );
 * 
 * @namespace 
 * @name ValueCheck
 */
var ValueCheck = function( ) {

    var that = {};

    // Throw helper function
    var throwError = function( message ) {
        throw Error( "ValueCheckError: The following check failed: " + message );
    };

    var checkthrowNeed1ArgError = function( functionName, length ) {
        if ( length != 1 ) {
            throwError( "Error in calling checkObject." + functionName + ". Requires exactly 1 argument, got " + length );
        }
    };

    var checkthrowNeed2ArgsError = function( functionName, length ) {
        if ( length != 2 ) {
            throwError( "Error in calling checkObject." + functionName + ". Requires exactly 2 arguments, got " + length );
        }
    };



    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    // Object, that exposes all the constraints, etc.
    // NOTE: if name is set to undefined, the getters does not return this - this is to facilitate the interactive help system.
    // Only CheckObject can work with help, normal objects can not work with help, due to the way getters work.
    var checkObject = function( name_, value_ ) {
        var that = {};

        // Private stuff

        // Variable
        var value = value_;
        var name = name_;
        var notflag = false;

        // Helper method
        // Define operators
        var operator = {
            // todo: Use === or not... ???
            equality: function( a, b ) {
                return a == b;
            },
            // inequality : function( a, b ) { return ! equality( a, b ); },
            greaterthan: function( a, b ) {
                return a > b;
            },
            lessthan: function( a, b ) {
                return a < b;
            }
        };
        // And this is for the descriptions.
        operator.equality.desc = "==";
        operator.greaterthan.desc = ">";
        operator.lessthan.desc = "<";

        // Compar other stuff to value, using an operator
        var compare = function( operator, b ) {
            var compres = operator( value, b );
            var notstring = "not ";
            if ( notflag ) {
                compres = !compres;
                notflag = false;
                notstring = "";
            }
            if ( compres ) {
                return that;
            } else {
                throwError( "Value of '" + name + "' : '" + value + "' is " + notstring + operator.desc + " '" + b + "'" );
            }
        };

        // Helper function to create link works.
        // Words, that we want
        // is, and, has, have, with, of, value, must, be

        var defineLink = function( onObject, linkname ) {
            onObject.__defineGetter__( linkname, function( ) {
                    if ( name === undefined ) {
                        return onObject.__lookupGetter__( linkname );
                    } else {
                        return onObject;
                    }
                } );
            onObject.__lookupGetter__( linkname ).__doc__ = <doc type="link"><brief>Link one or more check/constraints together</brief>
      <syntax>CheckObjectInstance.{linkname}.someCheck;</syntax>
      <description>This link word does nothing, but is provided to allow an expressive, easy-to-read syntax.</description>
      <returns type="Object">CheckObject instance suitable for chaining constraints</returns>
      <examples>ValueCheck.checkThat( "foo", foo ).is.of.type( "string" ).and.with.value.not.equalTo( "something" );
  ValueCheck.requireThat( "foo", foo ).must.be.of.type( "number" ).and.have.value.inside( 42, 78 );
  ValueCheck.checkThat( "foo", foo ).is.defined.and.has.type( "string" ).and.that.it.has.value.in( [ "biblotek.dk", "pallesgavebod" ] );
  ValueCheck.requireThat( "address", foo ).has.type( "string" ).and.that.it.is.an.email.address;
</examples>
      </doc>;
        };

        // Public stuff
        // Define all the links.
        ( function( ) {
                [ "is", "and", "has", "have", "with", "of", "value", "must", "be", "that", "it", "does", "address", "an" ]
                    .forEach( function( v ) {
                        defineLink( that, v );
                    } );
            }( ) );


        // Here comes stuff that actually does something.
        that.__defineGetter__( "not", function( ) {
                // Hack to fix the help system
                if ( name === undefined ) {
                    return this.__lookupGetter__( "not" );
                } else {
                    notflag = !notflag;
                    return this;
                }
            } );
        that.__lookupGetter__( "not" ).__doc__ = <doc type="flag"><brief>Toogle the semantics of the next check</brief>
    <syntax>CheckObjectInstance.not;</syntax>
    <description>This flag toogles the semantics of the next check/constraint, and is provided to allow an expressive, easy-to-read syntax. So, writing CheckObjectInstance.not.equalTo( 42 ) is equivalent to writing CheckObjectInstance.unequalTo( 42 ). Note, that for inside and outside, there are special cornercase meanings for writing not (see the help for these checks, for more information).</description>
    <returns type="Object">CheckObject instance suitable for chaining constraints</returns>
    <examples>ValueCheck.check( "foo", foo ).is.not.type( "string" ).and.value.is.not.in( [ 23, 34, 45 ] );</examples>
  </doc>;

        ////////////////////////////////////////////////////////////////////////////////
        // defined and undefined

        that.__defineGetter__( "undefined", function( ) {
                // Hack to fix the help system
                if ( name === undefined ) {
                    return this.__lookupGetter__( "undefined" );
                } else {
                    return this.equalTo( undefined );
                }
            } );
        that.__lookupGetter__( "undefined" ).__doc__ = <doc type="check"><brief>Check if the value is undefined</brief>
    <syntax>CheckObjectInstance.undefined;</syntax>
    <description>This constraint requires that the value is undefined. If the constraint is not fullfilled, it will throw an error.</description>
    <returns type="Object">CheckObject instance suitable for chaining constraints</returns>
    <examples>ValueCheck.check( "foo", foo ).is.undefined;</examples>
  </doc>;


        that.__defineGetter__( "defined", function( ) {
                // Hack to fix the help system
                if ( name === undefined ) {
                    return this.__lookupGetter__( "defined" );
                } else {
                    return this.not.undefined;
                }
            } );
        that.__lookupGetter__( "defined" ).__doc__ = <doc type="check"><brief>Check if the value is defined</brief>
  <syntax>CheckObjectInstance.defined;</syntax>
  <description>This constraint requires that the value is defined. If the constraint is not fullfilled, it will throw an error.</description>
  <returns type="Object">CheckObject instance suitable for chaining constraints</returns>
  <examples>ValueCheck.requireThat( "foo", foo ).is.defined.with.type( "string" ).and.that.it.is.not.equalTo( "" );
  ValueCheck.check( "foo", foo ).defined;
</examples>
  </doc>;

        ////////////////////////////////////////////////////////////////////////////////
        // type

        /**
         * Check if the type of a the value matches type.
         * 
         * This constraint requires that the typeof the value is equal to the type
         * string passed as parameter to the constraint. If the constraint is not
         * fullfilled, it will throw an error. 
         * 
         * @type {check}
         * @syntax CheckObjectInstance.type( type );
         * @param {String} type The type as a string to check the encapsulated value against
         * @return {Object} CheckObject instance suitable for chaining constraints
         * @example ValueCheck.check( "foo", foo ).has.type( "number" );
         *     ValueCheck.check( "foo", foo ).type( "string" );
         * @name ValueCheck.CheckObject.type
         * @method
         */
        that.type = function( type ) {
            checkthrowNeed1ArgError( "type", arguments.length );
            var rescomp = typeof value == type;
            var notstring1 = "not ";
            var notstring2 = "";
            if ( notflag ) {
                notflag = false;
                rescomp = !rescomp;
                notstring1 = "";
                notstring2 = "not ";
            }
            if ( rescomp ) {
                return this;
            } else {
                throwError( "Value '" + name + "' is " + notstring1 + "of type '" + type + "', but " + notstring2 + "of type '" + typeof value + "'" );
            }
        };

        ////////////////////////////////////////////////////////////////////////////////
        // instanceOf
        /**
         * Check if the value is an instance of a type.
         * 
         * This constraint requires that the value is an instance of the
         * type/constructor passed as parameter to the constraint. If the
         * constraint is not fullfilled, it will throw an error. 
         * 
         * @type {check}
         * @syntax CheckObjectInstance.instanceOf( type );
         * @param {String} type The type/constructor to check the encapsulated value against
         * @return {Object} CheckObject instance suitable for chaining constraints
         * @example ValueCheck.check( "somedate", somedate ).is.an.instanceOf( Date );
         *     ValueCheck.check( "foo", foo ).instanceOf( String );
         * @name ValueCheck.CheckObject.instanceOf
         * @method
         */
        that.instanceOf = function( type ) {
            checkthrowNeed1ArgError( "type", arguments.length );
            var rescomp = value instanceof type;
            var notstring1 = "not ";
            var notstring2 = "";
            if ( notflag ) {
                notflag = false;
                rescomp = !rescomp;
                notstring1 = "";
                notstring2 = "not ";
            }
            if ( rescomp ) {
                return this;
            } else {
                throwError( "Value '" + name + "' is " + notstring1 + " instanceof '" + type + "'" );
            }
        };

        ////////////////////////////////////////////////////////////////////////////////
        // RegExp support
        /**
         * Check if the value contains a regular expression.
         * 
         * This constraint requires that the value, converted to a string,
         * contains the regular expression passed as parameter to the constraint.
         * It differs from match in that the regular expression only needs to
         * match some part of the value (after string conversion). If the
         * constraint is not fullfilled, it will throw an error. 
         * 
         * @type {check}
         * @syntax CheckObjectInstance.contain( regexp );
         * @param {RegExp} regexp The regular expression to check the encapsulated value against
         * @return {Object} CheckObject instance suitable for chaining constraints
         * @example The following example will be OK for names such as Beth, beth and Elizabeth
         *   ValueCheck.checkThat( "name", name ).is.of.type( "string" ).and.contain( /beth/i );
         * @name ValueCheck.CheckObject.contain
         * @method
         */
        that.contain = function( regexp ) {
            checkthrowNeed1ArgError( "equalTo", arguments.length );
            var vs = value.toString( );
            var rescomp = ( -1 != vs.search( regexp ) );
            var notstring = "not ";
            if ( notflag ) {
                notflag = false;
                rescomp = !rescomp;
                notstring = "";
            }
            if ( rescomp ) {
                return this;
            } else {
                throwError( "Value '" + name + "' does " + notstring + " contain '" + regexp.toString( ) + "'" );
            }
        }

        /**
         * Check if the value matches a regular expression.
         * 
         * This constraint requires that the value, converted to a string, matches
         * the regular expression passed as parameter to the constraint. It
         * differs from contains in that the regular expression must match the
         * entire value (after string conversion). If the constraint is not
         * fullfilled, it will throw an error. 
         * 
         * @type {check}
         * @syntax CheckObjectInstance.match( regexp );
         * @param {RegExp} regexp The regular expression to check the encapsulated value against
         * @return {Object} CheckObject instance suitable for chaining constraints
         * @example The following example matches only Beth and beth, not Elizabeth
         *   ValueCheck.checkThat( "name", foo ).matches( /beth/i );
         * @name ValueCheck.CheckObject.match
         * @method
         */
        that.match = function( regexp ) {
            checkthrowNeed1ArgError( "equalTo", arguments.length );
            var vs = value.toString( );
            var rescomp = ( vs == vs.match( regexp )[ 0 ] );
            var notstring = "not ";
            if ( notflag ) {
                notflag = false;
                rescomp = !rescomp;
                notstring = "";
            }
            if ( rescomp ) {
                return this;
            } else {
                throwError( "Value '" + name + "' does " + notstring + " match '" + regexp.toString( ) + "'" );
            }
        }

        ////////////////////////////////////////////////////////////////////////////////
        // Check if it looks like an email. Checking email addresses with 
        // regular expressions is complicated, check
        // http://www.regular-expressions.info/email.html
        that.__defineGetter__( "email", function( ) {
                // Hack to fix the help system
                if ( name === undefined ) {
                    return this.__lookupGetter__( "email" );
                } else {
                    return this.contain( /^[^@]+@[^@]+\.\w{2,}$/ );
                }
            } );
        that.__lookupGetter__( "email" ).__doc__ = <doc type="check"><brief>Check if the value may is an email address</brief>
  <syntax>CheckObjectInstance.defined;</syntax>
    <description>This constraint requires that the value is on a form that matches most common forms of email address. It is quite relaxed, and may accept values that truly are not email address. However, it should fail on most invalid addresses. If the constraint is not fullfilled, it will throw an error.</description>
  <returns type="Object">CheckObject instance suitable for chaining constraints</returns>
  <examples>ValueCheck.requireThat( "foo", foo ).with.type( "string" ).is.an.email.address;
</examples>
  </doc>;


        ////////////////////////////////////////////////////////////////////////////////
        // Check if it looks like an url.
        that.__defineGetter__( "url", function( ) {
                // Hack to fix the help system			       
                if ( name === undefined ) {
                    return this.__lookupGetter__( "url" );
                } else {
                    return this.contain( /^([^:\/]+:\/\/)?([^:\/]+)(:\d+)?(\/.*)?$/ );
                    //                     protocol       host      port   path 
                }
            } );
        that.__lookupGetter__( "url" ).__doc__ = <doc type="check"><brief>Check if the value may is an url address</brief>
  <syntax>CheckObjectInstance.defined;</syntax>
    <description>This constraint requires that the value is on a form that matches most common forms of url address. It is quite relaxed, and may accept values that truly are not url address. However, it should fail on most invalid addresses. If the constraint is not fullfilled, it will throw an error.</description>
  <returns type="Object">CheckObject instance suitable for chaining constraints</returns>
  <examples>ValueCheck.requireThat( "foo", foo ).with.type( "string" ).is.an.url;
</examples>
  </doc>;




        ////////////////////////////////////////////////////////////////////////////////
        // Various comparison operators

        /**
         * Check if the value is = a specific value.
         * 
         * This constraint requires that the value is equal to the value passed as
         * parameter to the constraint. If the constraint is not fullfilled, it
         * will throw an error. 
         * 
         * @type {check}
         * @syntax CheckObjectInstance.equalTo( value );
         * @param {Any} value The value to check the encapsulated value against
         * @return {Object} CheckObject instance suitable for chaining constraints
         * @example ValueCheck.checkThat( "foo", foo ).is.defined.with.type( "number" ).and.value.that.is.equalTo( 42 );
         *   ValueCheck.check( "foo", foo ).type( "string" ).equalTo( "kanonkonge" );
         * @name ValueCheck.CheckObject.equalTo
         * @method
         */
        that.equalTo = function( value ) {
            checkthrowNeed1ArgError( "equalTo", arguments.length );
            return compare( operator.equality, value );
        };

        /**
         * Check if the value is != a specific value.
         * 
         * This constraint requires that the value is not equal to the value
         * passed as parameter to the constraint. If the constraint is not
         * fullfilled, it will throw an error. 
         * 
         * @type {check}
         * @syntax CheckObjectInstance.unequalTo( value );
         * @param {Any} value The value to check the encapsulated value against
         * @return {Object} CheckObject instance suitable for chaining constraints
         * @example ValueCheck.checkThat( "foo", foo ).is.defined.with.type( "number" ).and.value.that.is.unequalTo( 42 );
         *   ValueCheck.check( "foo", foo ).type( "string" ).unequalTo( "kanonkonge" );
         * @name ValueCheck.CheckObject.unequalTo
         * @method
         */
        that.unequalTo = function( value ) {
            return this.not.equalTo( value );
        };

        /**
         * Check if the value is > than a specific value.
         * 
         * This constraint requires that the value is greater than the value
         * passed as parameter to the constraint. If the constraint is not
         * fullfilled, it will throw an error. 
         * 
         * @type {check}
         * @syntax CheckObjectInstance.greaterThan( minValue );
         * @param {Any} minValue The minimum value of the encapsulated value
         * @return {Object} CheckObject instance suitable for chaining constraints
         * @example ValueCheck.checkThat( "foo", foo ).is.defined.with.type( "number" ).and.value.greaterThan( 10 );
         *   ValueCheck.check( "foo", foo ).greaterThan( "kanonkonge" );
         * @name ValueCheck.CheckObject.greaterThan
         * @method
         */
        that.greaterThan = function( minValue ) {
            checkthrowNeed1ArgError( "greaterThan", arguments.length );
            return compare( operator.greaterthan, minValue );
        };


        /**
         * Check if the value is <= a specific value.
         * 
         * This constraint requires that the value is less than or equal to the
         * value passed as parameter to the constraint. If the constraint is not
         * fullfilled, it will throw an error. 
         * 
         * @type {check}
         * @syntax CheckObjectInstance.lessEqualThan( maxValue );
         * @param {Any} maxValue The maximum value of the encapsulated value
         * @return {Object} CheckObject instance suitable for chaining constraints
         * @example ValueCheck.checkThat( "foo", foo ).is.defined.with.type( "number" ).and.value.lessEqualThan( -10 );
         *   ValueCheck.check( "foo", foo ).is.defined.lessEqualThan( "kanonkonge" );
         * @name ValueCheck.CheckObject.lessEqualThan
         * @method
         */
        that.lessEqualThan = function( maxValue ) {
            return this.not.greaterThan( maxValue );
        };

        /**
         * Check if the value is < than a specific value.
         * 
         * This constraint requires that the value is less than the value passed
         * as parameter to the constraint. If the constraint is not fullfilled, it
         * will throw an error. 
         * 
         * @type {check}
         * @syntax CheckObjectInstance.lessThan( maxValue );
         * @param {Any} maxValue The maximum value of the encapsulated value
         * @return {Object} CheckObject instance suitable for chaining constraints
         * @example ValueCheck.checkThat( "foo", foo ).is.defined.with.type( "number" ).and.value.lessThan( -10 );
         *   ValueCheck.check( "foo", foo ).type( "string" ).lessThan( "kanonkonge" );
         * @name ValueCheck.CheckObject.lessThan
         * @method
         */
        that.lessThan = function( maxValue ) {
            checkthrowNeed1ArgError( "lessThan", arguments.length );
            return compare( operator.lessthan, maxValue );
        };

        /**
         * Check if the value is >= a specific value.
         * 
         * This constraint requires that the value is greater than or equal to the
         * value passed as parameter to the constraint. If the constraint is not
         * fullfilled, it will throw an error. 
         * 
         * @type {check}
         * @syntax CheckObjectInstance.greaterEqualThan( minValue );
         * @param {Any} minValue The minimum value of the encapsulated value
         * @return {Object} CheckObject instance suitable for chaining constraints
         * @example ValueCheck.checkThat( "foo", foo ).is.defined.with.type( "number" ).and.value.greaterEqualThan( 10 );
         *   ValueCheck.check( "foo", foo ).greaterEqualThan( "kanonkonge" );
         * @name ValueCheck.CheckObject.greaterEqualThan
         * @method
         */
        that.greaterEqualThan = function( minValue ) {
            return this.not.lessThan( minValue );
        };

        /**
         * Check if the value is inside a specific interval.
         * 
         * This constraint requires that the value is greater than or equal to the
         * minValue passed and less than or equal to the maxValue passed as
         * parameter to the constraint. If the constraint is not fullfilled, it
         * will throw an error. Note, that this constraint is inclusive, even for
         * not. That is, not.inside will return the same result as outside. 
         * 
         * @type {check}
         * @syntax CheckObjectInstance.inside( minValue, maxValue );
         * @param {Any} minValue The minimum value of the encapsulated value (inclusive)
         * @param {Any} maxValue The maximum value of the encapsulated value (inclusive)
         * @return {Object} CheckObject instance suitable for chaining constraints
         * @example ValueCheck.checkThat( "foo", foo ).is.defined.with.type( "number" ).and.value.inside( 10, 20 );
         *     ValueCheck.check( "foo", foo ).inside( "kanonkonge", "leopard" );
         * @name ValueCheck.CheckObject.inside
         * @method
         */
        that.inside = function( minValue, maxValue ) {
            checkthrowNeed2ArgsError( "inRange", arguments.length );
            if ( !notflag ) {
                return this.greaterEqualThan( minValue ).lessEqualThan( maxValue );
            } else {
                // Not changes this to an or. Check if we are outside.
                notflag = false;
                try {
                    // If this succeds, it was outside top.
                    return this.greaterThan( maxValue );
                } catch ( error1 ) {
                    try {
                        // Try if outside bottom.
                        return this.lessThan( minValue );
                    } catch ( error2 ) {
                        // Provide reasonably error message
                        throwError( "Value '" + name + "' : '" + value + "' is not outside range '" + minValue + "', '" + maxValue + "'" );
                    }
                }
            }
        };

        /**
         * Check if the value is not inside a specific interval.
         * 
         * This constraint requires that the value is either less than the
         * minValue passed or greater than the the maxValue passed as parameter to
         * the constraint. If the constraint is not fullfilled, it will throw an
         * error. Note, that calling outside( a, b ) is semantically equivalent to
         * calling not.inside( a, b ). 
         * 
         * @type {check}
         * @syntax CheckObjectInstance.outside( minValue, maxValue );
         * @param {Any} minValue The minimum value of the interval that the encapsulated value must no be part of (inclusive)
         * @param {Any} maxValue The maximum value of the interval that the encapsulated value must not be part of(inclusive)
         * @return {Object} CheckObject instance suitable for chaining constraints
         * @example ValueCheck.checkThat( "foo", foo ).is.defined.with.type( "number" ).and.value.outside( 10, 20 );
         *     ValueCheck.check( "foo", foo ).outside( "kanonkonge", "leopard" );
         * @name ValueCheck.CheckObject.outside
         * @method
         */
        that.outside = function( minValue, maxValue ) {
            return this.not.inside( minValue, maxValue );
        };


        /**
         * Check if the value is is one of several specific values.
         * 
         * This constraint requires that the value is found in the array passed as
         * parameter to the constraint. If the constraint is not fullfilled, it
         * will throw an error. 
         * 
         * @type {check}
         * @syntax CheckObjectInstance.outside( legalValues );
         * @param {Array} legalValues An array of legal values for the encapsulated value
         * @return {Object} CheckObject instance suitable for chaining constraints
         * @example ValueCheck.checkThat( "foo", foo ).is.defined.with.type( "number" ).and.value.in( [ 10, 17, 20, 42, 450 ] );
         *     ValueCheck.check( "foo", foo ).in([  "kanonkonge", "superhelt", "softwareudvikler" ] );
         * @name ValueCheck.CheckObject.in
         * @method
         */
        that. in = function( legalValues ) {
            checkthrowNeed1ArgError( "in", arguments.length );
            var res = ( -1 !== legalValues.indexOf( value ) );
            var notstring = "not ";
            if ( notflag ) {
                res = !res;
                notstring = "";
                notflag = false;
            }
            if ( !res ) {
                throwError( "Value of '" + name + "' : '" + value + "' is " + notstring + "in " + legalValues );
            } else {
                return this;
            }
        };

        // Document instance

        // Return it
        return that;
    };
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////

    // To make available for documentation, a dummy object
    // This keeps name === undefined, which is NOT allowed, but here we use it to change the behaviour to allow online help.
    // Tricky, tricky behaviour... indeed :-) (Will probably bite me sometime in the future).
    /**
     * Object that encapsulates a value, and allow to specify constraints.
     * 
     * Instances of this kind of object holds a value, passed to it during
     * construction, and provides methods to check constraints on it. It is
     * designed to provide an easy mechanism to check e.g. values passed to a
     * function or similar. You speficy constraints by calling methods on the
     * object that encapsulates the value. All methods throw if the constraint
     * is not fullfilled, and return an instanse of this object otherwise,
     * enabling chaining the constraints arbitrarely. Construct instances of
     * this object, by calling ValueCheck.check* methods. 
     *
     * **TODO:** MBD Should fix the constraints documentation for this module.
     * 
     * @type {Object}
     * @example ValueCheck.checkThat( "foo", foo ).is.defined.and.has.type( "string" ).and.is.not.equalTo( "" );
     * @name ValueCheck.CheckObject
     * @namespace */
    that.CheckObject = checkObject( undefined, undefined );


    /** Check a specific parameter/variable, even for undefined. 
   *
   * This method constructs an instance of an CheckObject object, which
   * can be used to check constraints on the parameter/variable passed. The
   * constructor differs from the other check\* methods in that no
   * defined/undefined check is performed. This allow checking e.g. that the
   * value is undefined or some specific value. The intention is that you
   * chain additional constraints. The syntax is quite flexible and can be
   * written very readable. 
   *  
   * @method
   * @example 
// The examples below illustrates some of the checks that is possibly with this method
ValueCheck.checkValue( "foo", foo ).is.defined.and.has.type( "string" ).and.value.that.is.not.equalTo( "" );
ValueCheck.checkValue( "foo", foo ).is.in.( [ undefined, "bibliotekdk", "pallesgavebod" ] );
   * @syntax ValueCheck.checkValue( paramname, paramvalue );
   * @param {string} paramname The name of the parameter/variable to check. Used to construct meaningful error reports
   * @param {object} paramvalue The value of the parameter/variable to check.
   * @return {ValueCheck.CheckObject} A CheckObject instance, that can be used to check the value for more constraints
   * @name ValueCheck.checkValue
   */
    that.checkValue = function( paramname, paramvalue ) {
        if ( arguments.length != 2 ) {
            throwError( "CheckValue needs exactly 2 arguments" );
        }
        // Abuse a temporary object, to check paramname
        ( function( ) {
                var tmp = checkObject( "paramname", paramname );
                tmp.defined.type( "string" ).unequalTo( "" );
            }( ) );
        // Now construct the real object and return it.
        return checkObject( paramname, paramvalue );
    };




    /**
     * Check a specific parameter/variable, with implicit check for undefined.
     * 
     * This constructor constructs an instance of an CheckObject object, which
     * can be used to check constraints on the parameter/variable passed. The
     * first check is implicit: the value must not be undefined. The intention
     * is that you chain additional constraints. The syntax is quite flexible
     * and can be written very readable. The various forms does the same, and
     * you are free to choose whichever best suits your style. 
     *
     * **Note:** The functions check, checkThat, and requireThat are aliases for the same function.
     * 
     * @syntax ValueCheck.check( paramname, paramvalue );
     *   ValueCheck.checkThat( paramname, paramvalue );
     *   ValueCheck.requireThat( paramname, paramvalue );
     * @param {string} paramname The name of the parameter/variable to check. Used to construct meaningful error reports
     * @param {Any} paramvalue The value of the parameter/variable to check.
     * @return {ValueCheck.CheckObject} A CheckObject instance, that can be used to check the value for more constraints
     * @example The examples below are semantically equal, but the first one is more explicit than the second. Which one to prefer is a matter of taste:
     *   ValueCheck.checkThat( "foo", foo ).has.type( "string" ).and.that.value.is.not.equalTo( "" );
     *   ValueCheck.check( "foo", foo ).type( "string" ).unequalTo( "" );
     *   ValueCheck.requireThat( "foo", foo).value.has.type( "string" ).and.is.not.equalTo( "" );
     * @name ValueCheck.check
     * @method
     */
    that.check = function( paramname, paramvalue ) {
        if ( arguments.length != 2 ) {
            throwError( "Check needs exactly 2 arguments" );
        }
        return this.checkValue( paramname, paramvalue ).defined;
    }
    // Set up some shortHands
    that.checkThat = that.check;
    that.requireThat = that.check;


    /**
     * Check a specific parameter/variable for being undefined.
     * 
     * This constructor constructs an instance of an CheckObject object, that
     * does a single test on the value, that it is undefined. Nothing is
     * returned. 
     *
     * @syntax ValueCheck.checkUndefined( paramname, paramvalue );
     * @param {string} paramname The name of the parameter/variable to check. Used to construct meaningful error reports
     * @param {Any} paramvalue The value of the parameter/variable to check.
     * @example ValueCheck.checkUndefined( "foo", foo );
     * @name ValueCheck.checkUndefined
     * @method
     */
    that.checkUndefined = function( paramname, paramvalue ) {
        if ( arguments.length != 2 ) {
            throwError( "CheckUndefined needs exactly 2 arguments" );
        }
        this.checkValue( paramname, paramvalue ).undefined;
    }


    // Return that
    return that;
}( );

UnitTest.addFixture( "util.ValueCheck module", function( ) {
        // Basic check of test
        Assert.exception( "Calling check with no arguments", 'ValueCheck.check();' );
        Assert.exception( "Calling check with one argument", 'ValueCheck.check( "param" );' );
        Assert.exception( "Calling check with param1 empty string", 'ValueCheck.check( "", 42 );' );
        Assert.exception( "Calling check with param1 not a string 1", 'ValueCheck.check( 42, 42 );' );
        Assert.exception( "Calling check with param1 not a string 2", 'ValueCheck.check( {}, 42 );' );

        // Check that we can actually create a check object
        // Todo: this check may actually not work?
        Assert.that( "Construction check object 1", 'ValueCheck.check( "foo", 42 ) != undefined' );

        // Create a checkObject to use for some number checking
        co = ValueCheck.check( "foo", 42 );
        // Check the 'empty' words note the two nots, to toggle back.
        Assert.equal( "Check fill words 1", 'co.with', co );
        Assert.equal( "Checking fill words", 'co.with.is.and.has.not.not', co );


        // Defined and undefined must be explicitly set now.
        Assert.exception( "Checking undefined passed to check", 'ValueCheck.check( "foo", undefined )' );
        Assert.exception( "Checking undefined passed to check", 'ValueCheck.check( "foo", null )' );
        Assert.exception( "Checking defined passed to checkUndefined", 'ValueCheck.checkUndefined( "foo", 42 )' );
        Assert.equal( "Checking undefined OK with null", 'ValueCheck.checkUndefined( "foo", null )', undefined );

        // type test
        Assert.equal( "Checking a number for type 1", 'co.type( "number" )', co );
        Assert.exception( "Checking a number for type 2", 'co.has.type( "string" )' );
        Assert.exception( "Checking a number for type 3", 'co.type( "function" )' );
        Assert.exception( "Checking a number for type 4", 'co.has.type( "object" )' );
        Assert.equal( "Checking is number isDefined", 'co.defined', co );
        Assert.exception( "Checking is number isUndefined", 'co.is.undefined' );

        // compare tests
        Assert.equal( "Check number for equality", 'co.equalTo( 42 )', co );
        Assert.equal( "Check number for inequality", 'co.not.equalTo( 43 )', co );
        Assert.equal( "Check number for inequality", 'co.not.equalTo( 41 )', co );
        Assert.equal( "Check number for inequality", 'co.unequalTo( 43 )', co );
        Assert.equal( "Check number for inequality", 'co.unequalTo( 41 )', co );
        Assert.exception( "Check number for equality, fail 1", 'co.equal( 41 )' );
        Assert.exception( "Check number for equality, fail 1", 'co.equal( 43 )' );

        Assert.equal( "Check a number for greaterThan", 'co.is.greaterThan( 41 )', co );
        Assert.exception( "Check a number for greaterThan at", 'co.is.greaterThan( 42 )' );
        Assert.equal( "Check a number for greaterThan at", 'co.is.greaterThan( 41 )', co );
        Assert.equal( "Check a number for greaterThan at", 'co.is.not.greaterThan( 42 )', co );
        Assert.exception( "Check a number for greaterThan no", 'co.greaterThan( 43 )' );
        Assert.equal( "Check a number for greaterEqualThan at", 'co.is.greaterEqualThan( 42 )', co );
        Assert.exception( "Check a number for greaterEqualThan no", 'co.greaterEqualThan( 43 )' );
        Assert.equal( "Check a number for lessThan", 'co.is.lessThan( 43 )', co );
        Assert.exception( "Check a number for lessThan at", 'co.lessThan( 42 )' );
        Assert.exception( "Check a number for lessThan no", 'co.is.lessThan( 41 )' );
        Assert.equal( "Check a number for lessEqualThan at", 'co.lessEqualThan( 42 )', co );
        Assert.exception( "Check a number for lessEqualThan no", 'co.is.lessEqualThan( 41 )' );
        // Chain test
        Assert.equal( "Check a number for lessThan, greaterThan", 'co.greaterThan( 41 ).and.is.lessThan( 43 )', co );

        // Check range
        Assert.equal( "Check a number in a range 1", 'co.inside( 42, 42 ) ', co );
        Assert.equal( "Check a number in a range 2", 'co.is.inside( 41, 43 ) ', co );
        Assert.equal( "Check a number in a range 3", 'co.inside( -300, 400 ) ', co );
        Assert.exception( "Check a number in range 4, fail", 'co.is.inside( 0, 30 )' );
        Assert.exception( "Check a number in range 5, fail", 'co.inside( 44, 42 )' );
        Assert.exception( "Check a number in range 6, fail", 'co.is.inside( 43, 43 )' );
        Assert.exception( "Check a number in range 7, fail", 'co.inside( 41, 41 )' );
        // not
        Assert.equal( "Check a number not in a range 1", 'co.not.inside( 0, 30 ) ', co );
        Assert.equal( "Check a number not in a range 2", 'co.is.not.inside( 44, 42 ) ', co );
        Assert.equal( "Check a number not in a range 3", 'co.not.inside( 43, 400 ) ', co );
        Assert.exception( "Check a number not in range 4, fail", 'co.is.not.inside( 42, 42 )' );
        Assert.exception( "Check a number not in range 5, fail", 'co.not.inside( 41, 43 )' );
        Assert.exception( "Check a number not in range 6, fail", 'co.is.not.inside( -300, 400 )' );
        Assert.equal( "Check a number not in a range 1", 'co.outside( 0, 30 ) ', co );
        Assert.equal( "Check a number not in a range 2", 'co.is.outside( 44, 42 ) ', co );
        Assert.equal( "Check a number not in a range 3", 'co.outside( 43, 400 ) ', co );
        Assert.exception( "Check a number not in range 4, fail", 'co.is.outside( 42, 42 )' );
        Assert.exception( "Check a number not in range 5, fail", 'co.outside( 41, 43 )' );
        Assert.exception( "Check a number not in range 6, fail", 'co.is.outside( -300, 400 )' );

        // Check in
        Assert.equal( "Check a number in 1", 'co.is.in( [ 23, 42, 567 ] ) ', co );
        Assert.equal( "Check a number in 2", 'co.in( [ 42 ] ) ', co );
        Assert.equal( "Check a number in 3", 'co.is.in( [ 0, 34, 42 ] ) ', co );
        Assert.exception( "Check a number in 4, fail", 'co.is.in( [] )' );
        Assert.exception( "Check a number in 5, fail", 'co.in( [ 41, 43 ] )' );
        Assert.exception( "Check a number in 6, fail", 'co.is.in( [ 23, 34, -42 ] )' );
        // not in
        Assert.equal( "Check a number not in 1", 'co.is.not.in( [ 23, 34, -42] ) ', co );
        Assert.equal( "Check a number not in 2", 'co.not.in( [ 41, 43 ] ) ', co );
        Assert.equal( "Check a number not in 3", 'co.is.not.in( [ ] ) ', co );
        Assert.exception( "Check a number not in 4, fail", 'co.is.not.in( [23, 42, 567 ] )' );
        Assert.exception( "Check a number not in 5, fail", 'co.not.in( [ 42 ] )' );
        Assert.exception( "Check a number not in 6, fail", 'co.is.not.in( [ 0, 34, 42 ] )' );

        // Not on type
        Assert.equal( "Check not number type 1", 'co.is.not.type( "string" )', co );
        Assert.equal( "Check not number type 2", 'co.is.not.type( "object" )', co );
        Assert.equal( "Check not number type 3", 'co.is.not.type( "object" ).is.not.type( "string" )', co );

        Assert.equal( "Check chain 2", 'co.is.greaterThan( 41 ).and.not.greaterThan( 44 ).and.inside( 34, 56 ).and.outside( 67,80 ).and.not.in( [ 0, 1, 2, 3, 4 ] ).and.not.equalTo( 50 ).and.with.type( "number" ).and.equalTo( 42 )', co );

        // Some more undefined tests
        co = ValueCheck.checkValue( "foo", undefined );
        Assert.equal( "Checking checkValue with foo and in 1", 'co.is.in( [ undefined, "va", 42, "vb" ])', co );
        co = ValueCheck.checkValue( "foo", "va" );
        Assert.equal( "Checking checkValue with foo and in 2", 'co.is.in( [ undefined, "va", 42, "vb" ])', co );
        co = ValueCheck.checkValue( "foo", 42 );
        Assert.equal( "Checking checkValue with foo and in 3", 'co.is.in( [ undefined, "va", 42, "vb" ])', co );
        co = ValueCheck.checkValue( "foo", "vbc" );
        Assert.exception( "Checking checkValue with foo and in 4 fail", 'co.is.in( [ undefined, "va", 42, "vb" ])' );


        ////////////////////////////////////////////////////////////////////////////////
        // Check with strings.
        co = ValueCheck.checkThat( "foo", "ggggggggg MyTestString" );
        // type test
        Assert.equal( "Checking a string for type 1", 'co.type( "string" )', co );
        Assert.exception( "Checking a string for type 2", 'co.has.type( "number" )' );
        Assert.exception( "Checking a string for type 3", 'co.type( "function" )' );
        Assert.exception( "Checking a string for type 4", 'co.has.type( "object" )' );
        Assert.equal( "Checking is number is defined", 'co.is.defined', co );
        Assert.exception( "Checking is string is undefined", 'co.is.undefined', co );

        // compare tests
        Assert.equal( "Check string for equality", 'co.equalTo( "ggggggggg MyTestString" )', co );
        Assert.equal( "Check string for not equality", 'co.not.equalTo( "MyTestString" )', co );
        Assert.equal( "Check string for inequality", 'co.not.equalTo( "" )', co );
        Assert.equal( "Check string for inequality", 'co.not.equalTo( "gggggg" )', co );
        Assert.equal( "Check string for inequality", 'co.unequalTo( "   " )', co );
        Assert.exception( "Check string for equality, fail 1", 'co.equal( 41 )' );
        Assert.exception( "Check string for equality, fail 1", 'co.equal( 43 )' );
        Assert.equal( "Check string for greaterThan", 'co.greaterThan( "aaaaaa" )', co );
        Assert.equal( "Check string for greaterThan 2", 'co.greaterThan( "ggggggggg MyTest" )', co );
        Assert.equal( "Check string for lessThan", 'co.lessThan( "h" )', co );

        Assert.equal( "Check string in options", 'co.not.in( [ "hej", "med", "dig" ] )', co );
        Assert.exception( "Check string in options", 'co.in( [ "hej", "med", "dig" ] )' );
        Assert.equal( "Check string in options", 'co.in( [ "hej", "med", "ggggggggg MyTestString", "dig" ] )', co );
        Assert.exception( "Check string in options", 'co.not.in( [ "hej", "med", "ggggggggg MyTestString", "dig" ] )' );

        // Test with undefined value. Function check. Check undefined/defined on array.
        co = ValueCheck.checkUndefined( "foo", undefined );
        /// FIXME: unitests fails, should be rewritten
        Assert.equal( "Check object returned by checkUndefined is undefined", 'co', undefined );

        co = ValueCheck.check( "foo", [ 42, 43 ] );
        Assert.equal( "Check array is defined", 'co.is.defined', co );
        Assert.exception( "Check array is undefined fail", 'co.is.not.defined' );

        // Some xml, perhaps.
        co = ValueCheck.check( "foo", <doc><brief>somedoc</brief></doc> );
        Assert.equal( "Check xml is defined", 'co.is.defined', co );
        Assert.exception( "Check xml is undefined fail", 'co.is.undefined' );
        Assert.equal( "Check xml is not empty", 'co.is.not.equalTo( "" )', co );

        // Links
        co = ValueCheck.check( "foo", "noget" );
        Assert.equal( "Check link 1", 'co.is.of.type( "string" ).and.with.value.not.equalTo( "something" );', co );
        co = ValueCheck.check( "foo", 50 );
        Assert.equal( "Check link 2", 'co.must.be.of.type( "number" ).and.does.have.value.inside( 42, 78 );', co );
        co = ValueCheck.check( "foo", "pallesgavebod" );
        Assert.equal( "Check link 3", 'co.has.type( "string" ).and.has.value.in( [ "biblotek.dk", "pallesgavebod" ] );', co );

        // Regular expressions
        co = ValueCheck.check( "foo", "somevalue" );
        Assert.equal( "Check regexp1 1", 'co.contain( /adam|eva/ )', co );
        Assert.equal( "Check regexp1 2", 'co.contain( "adam|eva" )', co );
        Assert.equal( "Check regexp1 3", 'co.not.contain( /adam/ )', co );
        Assert.equal( "Check regexp1 4", 'co.not.match( "eva" )', co );
        Assert.equal( "Check regexp1 5", 'co.match( "somevalue" )', co );
        Assert.equal( "Check regexp1 6", 'co.match( /somevalue/ )', co );
        // Opposites should fail.
        Assert.exception( "Check regexp1 fail 1", 'co.not.contain( /adam|eva/ )' );
        Assert.exception( "Check regexp1 fail 2", 'co.not.contain( "adam|eva" )' );
        Assert.exception( "Check regexp1 fail 3", 'co.contain( /adam/ )' );
        Assert.exception( "Check regexp1 fail 4", 'co.match( "eva" )' );
        Assert.exception( "Check regexp1 fail 5", 'co.not.match( "somevalue" )' );
        Assert.exception( "Check regexp1 fail 6", 'co.not.match( /somevalue/ )' );

        co = ValueCheck.check( "name", "Elizabeth" );
        Assert.equal( "Check regexp2 1", 'co.does.not.match( /beth/i )', co );
        Assert.equal( "Check regexp2 2", 'co.does.contain( /Beth/i )', co );

        // Email
        co = ValueCheck.check( "address1", "mbd@dbc.dk" );
        Assert.equal( "Check email 2.1", 'co.is.an.email.address', co );
        Assert.exception( "Check email 2.2", 'co.is.not.an.email' );
        co = ValueCheck.check( "address2", "@mbd@dbc.dk" );
        Assert.equal( "Check email 2.1", 'co.is.not.an.email.address', co );
        Assert.exception( "Check email 2.2", 'co.is.an.email' );
        co = ValueCheck.check( "address3", "" );
        Assert.equal( "Check email 3.1", 'co.is.not.an.email.address', co );
        Assert.exception( "Check email 3.2", 'co.is.an.email' );

        // Url
        var testvalidurl = function( url ) {
            co = ValueCheck.check( url, url );
            Assert.equal( "Check valid: " + url, 'co.is.an.url', co );
            Assert.exception( "Check valid: " + url, 'co.is.not.an.url' );
        };
        testvalidurl( "http://www.dbc.dk" );
        testvalidurl( "http://www.dbc.dk:80" );
        testvalidurl( "www.dbc.dk:8080/foo/bar.html" );
        testvalidurl( "http://vision.dbc.dk/~fvs/broend/OpenLibrary/OpenAgency/trunk/server.php" );
        testvalidurl( "lakitre.dbc.dk:21044" );
        testvalidurl( "soya.kk.dk:210/default" );
        testvalidurl( "lakitre:2105" );

        var testinvalidurl = function( url ) {
            co = ValueCheck.check( url, url );
            Assert.equal( "Check invalid: " + url, 'co.is.not.an.url', co );
            Assert.exception( "Check invalid: " + url, 'co.is.an.url' );
        };
        testinvalidurl( ":dbc.dk" );
        testinvalidurl( ":2110" );
        testinvalidurl( "dbc.dk:" );

        // InstanceOf
        co = ValueCheck.check( "someDate", new Date( ) );
        Assert.equal( "instanceOf 1.1", 'co.is.an.instanceOf( Date )', co );
        Assert.exception( "instanceOf 1.2", 'co.is.an.instanceOf( String )' );
        Assert.equal( "instanceOf 1.3", 'co.is.not.an.instanceOf( String )', co );
        Assert.exception( "instanceOf 1.4", 'co.is.not.an.instanceOf( Date )' );
        co = ValueCheck.check( "someDate", new String( "" ) );
        Assert.equal( "instanceOf 2.1", 'co.is.an.instanceOf( String )', co );
        Assert.exception( "instanceOf 2.2", 'co.is.an.instanceOf( Date )' );


        delete this.co;
        delete this.foo;
    } );

// TODO: ScriptDoc possibly missing @name in other file for ValueCheck.CheckObject.is
// TODO: ScriptDoc possibly missing @name in other file for ValueCheck.CheckObject.and
// TODO: ScriptDoc possibly missing @name in other file for ValueCheck.CheckObject.has
// TODO: ScriptDoc possibly missing @name in other file for ValueCheck.CheckObject.have
// TODO: ScriptDoc possibly missing @name in other file for ValueCheck.CheckObject.with
// TODO: ScriptDoc possibly missing @name in other file for ValueCheck.CheckObject.of
// TODO: ScriptDoc possibly missing @name in other file for ValueCheck.CheckObject.value
// TODO: ScriptDoc possibly missing @name in other file for ValueCheck.CheckObject.must
// TODO: ScriptDoc possibly missing @name in other file for ValueCheck.CheckObject.be
// TODO: ScriptDoc possibly missing @name in other file for ValueCheck.CheckObject.that
// TODO: ScriptDoc possibly missing @name in other file for ValueCheck.CheckObject.it
// TODO: ScriptDoc possibly missing @name in other file for ValueCheck.CheckObject.does
// TODO: ScriptDoc possibly missing @name in other file for ValueCheck.CheckObject.address
// TODO: ScriptDoc possibly missing @name in other file for ValueCheck.CheckObject.an
// TODO: ScriptDoc possibly missing @name in other file for ValueCheck.CheckObject.not
// TODO: ScriptDoc possibly missing @name in other file for ValueCheck.CheckObject.undefined
// TODO: ScriptDoc possibly missing @name in other file for ValueCheck.CheckObject.defined
// TODO: ScriptDoc possibly missing @name in other file for ValueCheck.CheckObject.email
// TODO: ScriptDoc possibly missing @name in other file for ValueCheck.CheckObject.url
