// Pure JavaScript implementation of Record, Field and Subfield classes.
// Placed in javacore for now, to avoid clashes with the C++ implementation.

// Note, the documentation of these classes is the MarcClasses.use.js
// module, such that it can be shared between the Java and C++
// implementations.

EXPORTED_SYMBOLS = [ "Record", "Field", "Subfield" ];

// The pattern used to create Record, Field and Subfield objects in
// this module differs from most other modules in DBC, because it is a
// true constructor that we wish for, which can be called multiple
// times.

var Subfield = function( namearg, valuearg ) {

    // checking that we are called with exactly two arguments, if not throw exception
    if ( arguments.length != 2 ) {
        throw Error( "subfield must be initialized with exactly 2 arguments" );
        /* Commented out, pending exception module implementation.  08-06-12- Mvs
		 * {
        name: 'initializationError',
                  message:'subfield must be initialized with exactly 2 arguments'
                  };	
        */
    }

    // Encapsulate object
    var that = Object.create( Subfield.prototype );

    // Private value of name and value
    var name = namearg;
    var value = valuearg;

    that.__defineGetter__( "name", function( ) {
        return name;

    } );
    that.__defineSetter__( "name", function( val ) {
        name = val;
    } );

    that.__defineGetter__( "value", function( ) {
        return value;

    } );
    that.__defineSetter__( "value", function( val ) {
        value = val;
    } );

    // Function that returns either name or name and value.
    // If value is undefined or empty when the function is called, only name is returned.
    that.toString = function( ) {
        if ( typeof value === "undefined" || value === "" ) {
            return "*" + name;
        } else {
            return "*" + name + " " + value.replace( "@", "@@" ).replace( "*", "@*" );
        }
    };
    return that;
};


////////////////////////////////////////////////////////////////////////////////
// Field implementation
// array of subfields
////////////////////////////////////////////////////////////////////////////////
var Field = function( namearg, indicatorarg ) {

    // checking that we are called with exactly zero or two arguments, if not throw exception
    if ( arguments.length != 2 && arguments.length !== 0 ) {
        throw Error( "Field must be initialized with exactly 0 or 2 arguments" );

        /* Commented out, pending exception module implementation.  08-06-12- Mvs
		{	
        name: 'initializationError',
                  message:'Field must be initialized with exactly 0 or 2 arguments'
                  };*/
    }
    // Encapsulate object
    var that = Object.create( Field.prototype );

    var subfieldArray = [ ]; //used for storing the subfields

    var isControlFieldBool = false; // Used to determine if field is a control field
    var value = ""; // Only valid if

    // Private value of name and value
    var name = namearg;
    var indicator = indicatorarg;

    /**
     * Internal helper
     */
    var __createReadOnlySubField = function( ) {
                var res = Subfield("","");
                res.__defineSetter__("name",function(val) {
                    throw Error ("Illegal operation assigment to name on 'nonExisting' field");
                    }
                );
                res.__defineSetter__( "value", function( val ) {
                    throw Error ("Illegal operation assignment to value on 'nonExisting' field")
                } );
                return res;
            }

    that.__defineGetter__( "name", function( ) {
        if ( typeof name == "undefined" ) {
            name = "";
        }
        return name;

    } );
    that.__defineSetter__( "name", function( val ) {
        name = val;
    } );

    that.__defineGetter__( "indicator", function( ) {
        if ( isControlFieldBool ) {
            throw Error( "Illegal operation on control field: reading indicator attribute is not allowed" );
        }
        if ( typeof indicator == "undefined" ) {
            indicator = "";
        }
        return indicator;

    } );
    that.__defineSetter__( "indicator", function( val ) {
        if ( isControlFieldBool ) {
            throw Error( "Illegal operation on control field: setting indicator attribute is not allowed" );
        }
        indicator = val;
    } );

    that.isControlField = function( ) {
        return isControlFieldBool;
    };

    // Field.value
    // Value can only be read on control fields, and only be set on control fields, or empty ordinary fields
    that.__defineGetter__( "value", function( ) {
        if ( !isControlFieldBool ) {
            throw Error( "Illegal operation on non-control field: reading value attribute is not allowed" );
        }
        return value;

    } );
    that.__defineSetter__( "value", function( val ) {
        if ( isControlFieldBool ||
            ( ( typeof indicator == "undefined" || indicator == "" ) &&
                subfieldArray.length == 0 ) ) {
            isControlFieldBool = true;
            value = val;
        } else {
            throw Error( "Illegal operation on non-control field: setting value attribute is not allowed" );
        }
    } );

    // Field.append
    // Function for appending or adding subfields
    // nameArg is a subfield
    // valueArg is the value of the subfield to be appended.
    // replaceBool dictates wheter a subield should be appended or replaced.
    // If replaceBool is true we add to the array otherwise the subfield is appended to the array
    that.append = function( nameArg, valueArg, replaceBool ) {
        if ( isControlFieldBool ) {
            throw Error( "Illegal operation on control field: calling append method is not allowed" );
        }

        switch ( arguments.length ) {
            case 1:
                subfieldArray[ subfieldArray.length ] = Subfield( nameArg.name, nameArg.value );
                break;

            case 2:
                subfieldArray[ subfieldArray.length ] = Subfield( nameArg, valueArg );
                break;

            case 3:
                if ( replaceBool ) {
                    var index = -1;
                    for ( var i = 0; i < subfieldArray.length; i++ ) {
                        if ( subfieldArray[ i ].name == nameArg ) {
                            index = i;
                            break;
                        }
                    }
                    if ( index != -1 ) { //we found a subfield
                        subfieldArray[ index ] = Subfield( nameArg, valueArg );
                    } else {
                        createAndStoreSubfield( nameArg, valueArg );
                    }
                } else {
                    createAndStoreSubfield( nameArg, valueArg );
                }

                break;

            default:

                throw Error( "function takes 1, 2 or 3 arguments" );
                /* Commented out, pending exception module implementation.  08-06-12- Mvs
			{
            name: "invalidArgumentNumberError",
                      message: "function takes 1, 2 or 3 arguments"
                      };*/
        }
    };

    // Field.remove
    // Function that removes a subfield from the arrays of subfield. Can be called with either one or two arguments
    // nameArg = name of subfield
    // indexArg = index of the subfields, starts with 0.
    that.remove = function( nameArg, indexArg ) {
        if ( isControlFieldBool ) {
            throw Error( "Illegal operation on control field: calling remove method is not allowed" );
        }

        if ( arguments.length == 1 ) { //name of subfield
            for ( var i = 0; i < subfieldArray.length; i++ ) {
                if ( subfieldArray[ i ].name == nameArg ) {
                    removeIndexOfSubfieldArray( i );
                    break;
                }
            }
        } else if ( arguments.length == 2 ) {
            var itemCount = -1;
            for ( var y = 0; y < subfieldArray.length; y++ ) {
                if ( subfieldArray[ y ].name == nameArg ) {
                    itemCount = itemCount + 1;
                    if ( itemCount == indexArg ) {
                        removeIndexOfSubfieldArray( y );
                        break;
                    }
                }
            }
        } else {
            throw Error( "function takes 1 or 2 arguments" );
            /* Commented out, pending exception module implementation.  08-06-12- Mvs
			{
            name:"invalidArgumentNumberError",
                     message:"function takes 1 or 2 arguments"
                     };*/
        }
    };

    // Field:count
    // Function that returns the number of instances of subfields in the array.
    // takes one argument = name of subfields.
    that.count = function( nameArg ) {
        if ( isControlFieldBool ) {
            throw Error( "Illegal operation on control field: calling count method is not allowed" );
        }

        if ( arguments.length === 0 ) {
            return subfieldArray.length;
        } else if ( arguments.length == 1 ) {
            var itemCount = 0;
            var item = "";
            for ( var i = 0; i < subfieldArray.length; i++ ) {
                item = subfieldArray[ i ];
                if ( item.name == nameArg ) {
                    itemCount = itemCount + 1;
                }
            }
            return itemCount;
        } else {
            throw Error( "function takes 0 or 1 argument" );
            /* Commented out, pending exception module implementation.  08-06-12- Mvs
			{
            name:"invalidArgumentNumberError",
                     message:"function takes 0 or 1 argument"
                     }; */
        }


    };

//ja6    // Field:subfield
    // Function that returns a subfield from the array
    // Takes 2 arguments. First argument can be either a name of a subfield object or a number of a subifield, which is zero based.
    // second argument is index of subfield. The index number determines which occurance of the subfield should be returned.
    // If the second argument is omitted, the argument is checked for type.
    that.subfield = function( nameArg, indexArg ) {
        if ( isControlFieldBool ) {
            throw Error( "Illegal operation on control field: calling subfield method is not allowed" );
        }

        if ( arguments.length == 1 ) {
            if ( typeof nameArg == "number" ) {
                if ( nameArg >= subfieldArray.length ) {
                    return Subfield( "", "" );
                } else {
                    return subfieldArray[ nameArg ];
                }
            } else if ( typeof nameArg == "string" ) {
                for ( var y = 0; y < subfieldArray.length; y++ ) {
                    var item = subfieldArray[ y ];
                    if ( item.name == nameArg ) {
                        return item;
                    }
                }
                return __createReadOnlySubField();
            }
        } else if ( arguments.length == 2 ) {
            var itemCount = -1;
            var item = "";
            for ( var x = 0; x < subfieldArray.length; x++ ) {
                item = subfieldArray[ x ];
                if ( item.name == nameArg ) {
                    itemCount = itemCount + 1;
                    if ( itemCount == indexArg ) {
                        return item;
                    }
                }
            }
            return __createReadOnlySubField();
        } else {
            throw Error( "function takes 1 or 2 arguments" );
            /* Commented out, pending exception module implementation.  08-06-12- Mvs
			{
                name : "invalidArgumentNumberError",
                    message: "function takes 1 or 2 arguments"
                    };*/
        }
    };


    // Field:toString
    // Function that returns the contents of field as a string.
    that.toString = function( ) {
        if ( isControlFieldBool ) {
            return name + " " + value.replace( "@", "@@" ).replace( "*", "@*" );;
        } else {
            // Traverses the subfields and return each as a string	
            var subfieldString = " ";
            for ( var i = 0; i < subfieldArray.length; i++ ) {
                subfieldString = subfieldString + subfieldArray[ i ].toString( ) + " ";
            }
            return name + " " + indicator + subfieldString;
        }
    };


    //private helperfunctions

    // Function for copying a field and all its subfields. Needed to save copies, not references
    that.__copy = function( ) {
        var res = new Field( );
        res.name = name;
        res.isControlFieldBool = isControlFieldBool;
        if ( isControlFieldBool ) {
            res.value = value;
        } else {
            res.indicator = indicator;
            for ( var i = 0; i < subfieldArray.length; i++ ) {
                res.append( subfieldArray[ i ] );
            }
        }
        return res;
    };
    
    // Function used for creating a subfield in the subfield array 
    function createAndStoreSubfield( name, val ) {
        subfieldArray[ subfieldArray.length ] = Subfield( name, val );
    };

    // Function that removes a subfield
    function removeIndexOfSubfieldArray( indexArg ) {
        if ( indexArg === 0 ) {
            subfieldArray.splice( 0, 1 );
            //subfieldArray = subfieldArray.slice( 1 );
        } else if ( indexArg == subfieldArray.length - 1 ) {
            subfieldArray.splice( ( subfieldArray.length - 1 ), 1 );
            // subfieldArray = subfieldArray.slice( 0, subfieldArray.length -1 );
        } else {

            subfieldArray.splice( indexArg, 1 );
            /* commented out by mvs, replaced by splice
                var part1 = subfieldArray.slice( 0, indexArg);
                var part2 = subfieldArray.slice( indexArg+1);
                subfieldArray = part1.concat( part2);
            */
        }
    };
    return that;
};


////////////////////////////////////////////////////////////////////////////////
// Record implementation
// Array of fields: fieldArray
////////////////////////////////////////////////////////////////////////////////
var Record = function( filenameArg ) {

    if ( arguments.length === 0 ) {} else if ( arguments.length == 1 ) {
        throw Error( "Call to function that creates a record from a file is not implemented in javascript" );

        /* Commented out, pending exception module implementation.  08-06-12- Mvs
		{
        name:"notImplementedError",
                 message:"Call to function that creates a record from a file is not implemented in javascript"
                 };*/
    } else {
        throw Error( "Record attempted created with too many arguments" );
        /* Commented out, pending exception module implementation.  08-06-12- Mvs
		{
        name:"invalidArgumentNumberError",
                 message:"Record attempted created with too many arguments"
                 };
             */
    }

    // Encapsulate object
    var that = Object.create( Record.prototype );
    var fieldArray = [ ];
    var filename;

    // These are fields from the iso 2709 header.
    // Private value of implementationCodes and recordStatus
    var implementationCodes = "    ";
    var recordStatus = "c";
    var forUserSystems = "   ";

    that.__defineGetter__( "implementationCodes", function( ) {
        return implementationCodes;
    } );

    that.__defineSetter__( "implementationCodes", function( val ) {
        implementationCodes = val;
    } );

    that.__defineGetter__( "recordStatus", function( ) {
        return recordStatus;
    } );

    that.__defineSetter__( "recordStatus", function( val ) {
        recordStatus = val;
    } );

    that.__defineGetter__( "forUserSystems", function( ) {
        return forUserSystems;
    } );

    that.__defineSetter__( "forUserSystems", function( val ) {
        forUserSystems = val;
    } );


    /**
     * internal heldper
     * @returns Record object with owerwritten setter Methods that throws{}
     * @private
     */
    var __createReadOnlyField = function () {
        var res = Field("","");
        res.__defineSetter__("name", function( value ) {
            throw Error("Illegal operation on non existent field: setting name attribute is not allowed");
        });
        res.__defineSetter__("indicator", function( value ) {
            throw Error("Illegal operation on non existent field: setting indicator attribute is not allowed")
        });
        res.__defineSetter__("value", function( value ) {
            throw Error("Illegal operation on non existent field: setting value attribute is not allowed")
        });
        res.append = function( ) {
            throw Error("Illegal operation on non existent field: setting append attribute is not allowed");
        }
        return res;
    }

    // Record:numberOfFields
    // Function which returns the number of fields in the field array
    that.numberOfFields = function( ) {
        return fieldArray.length;
    };


    // Record:count
    // Function that returns the given number of field occurances in the field array
    // Takes one argument, nameArg which is the field id. 
    that.count = function( nameArg ) {
        if ( arguments.length == 1 ) {
            var itemCount = 0;
            var item = "";
            for ( var i = 0; i < fieldArray.length; i++ ) {
                item = fieldArray[ i ];
                if ( fieldArray[ i ].name == nameArg ) {
                    itemCount = itemCount + 1;
                }
            }
            return itemCount;
        } else {
            throw Error( "Function takes only 1 argument" );
            /* Commented out, pending exception module implementation.  08-06-12- Mvs
			{
            name:"invalidArgumentNumberError",
                     message:"Function takes 1 argument"
                     };*/
        }

    };

    // Record:append
    // Function that either appends to an excisting field or adds a new field to the fieldArray
    // Takes two arguments
    // First argument can be either a field or the name of the field. Its assumed to be a field if second argument is omitted.
    // If there are two arguments its assumed first argument is a namestring and an indicator string of the field in the fieldArray.
    that.append = function( nameArg, indicatorArg ) {
        if ( arguments.length == 1 ) { //its a field 
            fieldArray[ fieldArray.length ] = nameArg.__copy( ); //Stores the new field in the fieldarray
        } else if ( arguments.length == 2 ) {
            fieldArray[ fieldArray.length ] = new Field( nameArg, indicatorArg );
        } else {
            throw Error( "function takes 1 or 2 arguments" );
            /* Commented out, pending exception module implementation.  08-06-12- Mvs
		{
            name:"invalidArgumentNumberError",
                     message:"function takes 1 or 2 arguments"
                     };*/
        }
    };

    // Record:remove
    // Function that removes a field from the record instance
    // Takes two arguments
    // First argument can be either the name or the zero based ID number of the field to remove.
    // Second argument is a string representation of the index numbers of fields in the fieldarray.
    // If there is no field matching the ID or name, nothing happens 
    that.remove = function( nameArg, indexArg ) {
        switch ( arguments.length ) {

            case 1:
                if ( arguments.length == 1 && typeof nameArg == "number" ) {
                    for ( var i = 0; i < fieldArray.length; i++ ) {
                        if ( fieldArray[ i ].indicator == nameArg ) {
                            removeIndexOfFieldArray( i );
                            break;
                        }
                    }
                } else
                if ( arguments.length == 1 && typeof nameArg == "string" ) {
                    for ( var y = 0; y < fieldArray.length; y++ ) {
                        if ( fieldArray[ i ].name == nameArg ) {
                            removeIndexOfFieldArray( y );
                            break;
                        }
                    }
                }
                break;

            case 2:
                var itemCount = -1;
                for ( var i = 0; i < fieldArray.length; i++ ) {
                    if ( fieldArray[ i ].name == nameArg ) {
                        itemCount = itemCount + 1;
                        if ( itemCount == indexArg ) {
                            removeIndexOfFieldArray( i );
                            break;
                        }
                    }
                }
                break;

            default:
                throw Error( "function takes 1 or 2 arguments" );
                /* Commented out, pending exception module implementation.  08-06-12- Mvs
			{
            name: "invalidArgumentNumberError",
                      message: "function takes 1 or 2 arguments"
				                      };*/
        }
    };

    // Is in the documentation, but has no unit test.
    that.removeAll = function( name ) {
        var num = that.count( name );
        for ( var i = 0; i < num; i++ ) {
            that.remove( name );
        }
    };

    // Record:field          Sub
    // Function that returns a field from a record
    // Takes 2 arguments
    // First argument can be either the name or the zero based ID number of the field to return 
    that.field = function( nameArg, indexArg ) {
        if ( arguments.length == 1 ) {
            if ( typeof nameArg == "number" ) {
                if ( nameArg >= fieldArray.length ) {
                    return Field( "", "" );
                } else {
                    return fieldArray[ nameArg ];
                }
            } else if ( typeof nameArg == "string" ) {
                for ( var i = 0; i < fieldArray.length; i++ ) {
                    if ( fieldArray[ i ].name == nameArg ) {
                        return fieldArray[ i ];
                    }
                }
                return __createReadOnlyField();
            }
        } else if ( arguments.length == 2 ) {
            var itemCount = -1;

            var len = fieldArray.length;
            for ( var i = 0; i < len; i++ ) {
                if ( fieldArray[ i ].name == nameArg ) {
                    itemCount = itemCount + 1;
                    if ( itemCount == indexArg ) {
                        return fieldArray[ i ];
                    }
                }
            }
            return __createReadOnlyField();
        } else {
            throw Error( "Function field is called with 1 or 2 arguments" );
            /* Commented out, pending exception module implementation.  08-06-12- Mvs
		{
            name:"invalidArgumentNumberError",
                     message:"Function field is called with 1 or 2 arguments"
                     }; */
        }
    };

    // Record:toString
    // Function that returns the contents of fieldArray as strings.
    // Calls field.toString which calls subfield.toString
    that.toString = function( ) {
        var fieldString = "";
        for ( var i = 0; i < fieldArray.length; i++ ) {
            fieldString = fieldString + fieldArray[ i ].toString( ) + "\n";
        }
        return fieldString;
    };


    // private helper function
    // Function is used for removing a field from the fieldarray.
    // Takes 1 argument which is the index number of the array position pending removal
    function removeIndexOfFieldArray( indexArg ) {
        if ( indexArg === 0 ) {
            fieldArray.splice( 0, 1 );
        } else if ( indexArg == fieldArray.length - 1 ) {
            fieldArray.splice( ( fieldArray.length - 1 ), 1 );
        } else {
            fieldArray.splice( indexArg, 1 );
        }
    };
    return that;
};
