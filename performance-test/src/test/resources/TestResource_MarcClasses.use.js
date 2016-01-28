/** @file UnitTest and documentation for Record, Field and Subfield */
// This is not actually the MarcClasses (Record, Field, Subfield), 
// but rather the unittests for them, and the documentation of them.
// The actual implementation is in MarcClassesCore.
// The unittests comes first, then the documentation last.

use( "System" );
use( "MarcClassesCore" );
EXPORTED_SYMBOLS = [ "MarcClasses" ];

// TODO: Some tests seems to be commented out - I do not know why (mbd, 2013-03-19).


use( "UnitTest" );

UnitTest.addFixture( "marc.MarcClasses module", function( ) {

        ////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////
        // 
        // Operations on qiso2709subfield
        // 
        ////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////

        // Hold variables in this, so we do not have to delete everything.
        __ut = {};

        Assert.equal( "SF: Constructing a subfield with parameters",
            'var subfield = new Subfield( "a", "123 456 789" );', undefined );

        Assert.exception( "SF: Trying to construct subfield with no parameters",
            'var subfield = new Subfield();' );

        // properties name and value
        __ut.subfield = new Subfield( "a", "123 456 789" );
        Assert.equal( "SF: Name of subfield", '__ut.subfield.name;', "a" );
        Assert.equal( "SF: Value of subfield", '__ut.subfield.value;', "123 456 789" );
        __ut.subfield.name = "bif";
        Assert.equal( "SF: Name of subfield", '__ut.subfield.name;', "bif" );
        Assert.equal( "SF: Value of subfield", '__ut.subfield.value;', "123 456 789" );
        __ut.subfield.name = "";
        __ut.subfield.value = "987 654 321";
        Assert.equal( "SF: Name of subfield", '__ut.subfield.name;', "" );
        Assert.equal( "SF: Value of subfield", '__ut.subfield.value;', "987 654 321" );
        __ut.subfield.name = "\u00e5";
        __ut.subfield.value = "";
        Assert.equal( "SF: Name of subfield", '__ut.subfield.name;', "\u00e5" );
        Assert.equal( "SF: Value of subfield", '__ut.subfield.value;', "" );
        Assert.equal( "SF: toString", '__ut.subfield.toString( )', "*\u00e5" );


        ////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////
        // 
        // Operations on qiso2709field
        // 
        ////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////

        // Construction tests
        Assert.equal( "F: Constructing a field with parameters",
            'var field = new Field( "001", "00" );', undefined );

        Assert.exception( "F: Trying to construct field with wrong parameters",
            'var subfield = new Field( "ged" );' );
        // Name and indicator tests
        __ut.emptyField = new Field( );
        Assert.equal( "F: Getting the name of emptyField", '__ut.emptyField.name;', "" );
        Assert.equal( "F: Getting the indicator of emptyField",
            '__ut.emptyField.indicator;', "" );
        __ut.emptyField.name = "043";
        Assert.equal( "F: Getting the name of changed emptyField",
            '__ut.emptyField.name;', "043" );
        __ut.emptyField.name = "042";
        Assert.equal( "F: Getting the name of changed emptyField",
            '__ut.emptyField.name;', "042" );

        __ut.field = new Field( "009", "02" );
        Assert.equal( "F: Getting the name of field", '__ut.field.name;', "009" );
        Assert.equal( "F: Getting the indicator of field", '__ut.field.indicator;', "02" );
        __ut.field.name = "045";
        Assert.equal( "F: Getting the name of field", '__ut.field.name;', "045" );

        // emptyField.setIndicator( "02" );
        //   Assert.equal( "F: Getting the indicator of changed emptyField", 
        // 	  'emptyfield.indicator;', "02" );
        // emptyField.setIndicator( "03" );
        //   Assert.equal( "F: Getting the indicator of changed emptyField", 
        // 	  'emptyfield.indicator;', "03" );

        // Append/GetValue tests
        Assert.equal( "F: Getting the value a subfield with name a on a field with no subfields",
            '__ut.field.subfield( "a"  ).value;', "" );
        Assert.equal( "F: Counting subfield with no subfields", '__ut.field.count( )', 0 );
        Assert.equal( "F: Counting subfield with name a on field with no subfields", '__ut.field.count( "a" )', 0 );
        __ut.field.append( "a", "123 456 789", true );
        Assert.equal( "F: Counting subfields on field with 1 subfield", '__ut.field.count()', 1 );
        Assert.equal( "F: Getting the value of subfield after append",
            '__ut.field.subfield( "a"  ).value;', "123 456 789" );
        Assert.equal( "F: Getting the name of empty field after append",
            '__ut.field.subfield( 0  ).name;', "a" );
        __ut.field.append( "a", "987 654 321a", false );
        Assert.equal( "F: Getting the value of empty field after append",
            '__ut.field.subfield( "a", 1  ).value;', "987 654 321a" );
        Assert.equal( "F: Getting the name of empty field after append",
            '__ut.field.subfield( 0  ).name;', "a" );
        Assert.equal( "F: Getting the name of empty field after append",
            '__ut.field.subfield( 1  ).name;', "a" );
        // Append with true must be replace...
        __ut.field.append( "a", "987 654 321", true );
        Assert.equal( "F: Counting subfield with replaced a subfield", '__ut.field.count( )', 2 );
        Assert.equal( "F: Getting the value of empty field after second append",
            '__ut.field.subfield( "a"  ).value;', "987 654 321" );
        Assert.equal( "F: Getting the value of empty field after append",
            '__ut.field.subfield( "a", 1  ).value;', "987 654 321a" );
        Assert.equal( "F: Getting the name of empty field after append",
            '__ut.field.subfield( 0  ).name;', "a" );
        Assert.equal( "F: Getting the name of empty field after append",
            '__ut.field.subfield( 1  ).name;', "a" );
        // We remove, prematurely...
        __ut.field.remove( "a" );
        // Actually, how does it do replace when no one is there?
        __ut.field.append( "a", "987 654 321", true );
        // Lets count
        Assert.equal( "F: Counting subfield with one subfield", '__ut.field.count( )', 1 );
        Assert.equal( "F: Counting subfields of type a", '__ut.field.count( "a" )', 1 );
        Assert.equal( "F: Counting subfields of type b", '__ut.field.count( "b" )', 0 );
        __ut.field.append( "b", "This is subfield b", true );
        Assert.equal( "F: Counting subfield with one subfield", '__ut.field.count( )', 2 );
        Assert.equal( "F: Counting subfields of type a", '__ut.field.count( "a" )', 1 );
        Assert.equal( "F: Counting subfields of type b", '__ut.field.count( "b" )', 1 );
        __ut.field.append( "a", "Another a subfield @#\xA4$`'~*^%&/()=?", false );
        Assert.equal( "F: Counting subfield with one subfield", '__ut.field.count( )', 3 );
        Assert.equal( "F: Counting subfields of type a", '__ut.field.count( "a" )', 2 );
        Assert.equal( "F: Counting subfields of type b", '__ut.field.count( "b" )', 1 );
        // Append of subfield
        var c = new Subfield( "c", "123456" );
        __ut.field.append( c );
        Assert.equal( "F: Counting subfield with one subfield", '__ut.field.count( )', 4 );
        Assert.equal( "F: Counting subfields of type a", '__ut.field.count( "a" )', 2 );
        Assert.equal( "F: Counting subfields of type b", '__ut.field.count( "b" )', 1 );
        Assert.equal( "F: Counting subfields of type c", '__ut.field.count( "c" )', 1 );
        Assert.equal( "F: Value of subfield c", '__ut.field.subfield( "c"  ).value;', "123456" );
        // Subfields are copied in existing implementation, when appended.
        c.name = "d";
        c.value = "234567";
        Assert.equal( "F: Counting subfield with one subfield 2", '__ut.field.count( )', 4 );
        Assert.equal( "F: Counting subfields of type a 2", '__ut.field.count( "a" )', 2 );
        Assert.equal( "F: Counting subfields of type b 2", '__ut.field.count( "b" )', 1 );
        Assert.equal( "F: Counting subfields of type c 2", '__ut.field.count( "c" )', 1 );
        Assert.equal( "F: Counting subfields of type d 2", '__ut.field.count( "d" )', 0 );
        Assert.equal( "F: Value of subfield c", '__ut.field.subfield( "c"  ).value;', "123456" );

        // Seems we need a toString test here
        Assert.equal( "F: toString test", '__ut.field.toString( )', 
        (typeof(Java) !== "undefined" ? "045 02 *a 987 654 321 *b This is subfield b *a Another a subfield @@#\xA4$@0300'~@*@0302%&/()=? *c 123456 " : "045 02 *a 987 654 321 *b This is subfield b *a Another a subfield @@#\xA4$`'~@*^%&/()=? *c 123456 " ));

        __ut.field.append( new Subfield( "d", "gedeost" ) );
        Assert.equal( "F: Counting subfield with one subfield", '__ut.field.count( )', 5 );
        Assert.equal( "F: Counting subfields of type a", '__ut.field.count( "a" )', 2 );
        Assert.equal( "F: Counting subfields of type b", '__ut.field.count( "b" )', 1 );
        Assert.equal( "F: Counting subfields of type c", '__ut.field.count( "c" )', 1 );
        Assert.equal( "F: Counting subfields of type d", '__ut.field.count( "d" )', 1 );
        Assert.equal( "F: Value of subfield c", '__ut.field.subfield( "c"  ).value;', "123456" );
        Assert.equal( "F: Value of subfield c", '__ut.field.subfield( "d"  ).value;', "gedeost" );
        __ut.field.append( new Subfield( "c", "gedeost" ) );
        Assert.equal( "F: Counting subfield with one subfield", '__ut.field.count( )', 6 );
        Assert.equal( "F: Counting subfields of type a", '__ut.field.count( "a" )', 2 );
        Assert.equal( "F: Counting subfields of type b", '__ut.field.count( "b" )', 1 );
        Assert.equal( "F: Counting subfields of type c", '__ut.field.count( "c" )', 2 );
        Assert.equal( "F: Counting subfields of type d", '__ut.field.count( "d" )', 1 );
        Assert.equal( "F: Value of subfield c0", '__ut.field.subfield( "c"  ).value;', "123456" );
        Assert.equal( "F: Value of subfield c1", '__ut.field.subfield( "c", 1  ).value;', "gedeost" );
        Assert.equal( "F: Value of subfield c", '__ut.field.subfield( "d"  ).value;', "gedeost" );
        // Test names
        Assert.equal( "F: Testing name of subfield (0 )", '__ut.field.subfield( 0 ).name;', "a" );
        Assert.equal( "F: Testing name of subfield (1 )", '__ut.field.subfield( 1 ).name;', "b" );
        Assert.equal( "F: Testing name of subfield (2 )", '__ut.field.subfield( 2 ).name;', "a" );
        Assert.equal( "F: Testing name of subfield (3 )", '__ut.field.subfield( 3 ).name;', "c" );
        Assert.equal( "F: Testing name of subfield (4 )", '__ut.field.subfield( 4 ).name;', "d" );
        Assert.equal( "F: Testing name of subfield (5 )", '__ut.field.subfield( 5 ).name;', "c" );

        // Get value, set Value
        Assert.equal( "F: Get value of first a field, ( )",
            '__ut.field.subfield( "a" ).value;', "987 654 321" );
        Assert.equal( "F: Get value of first a field, (0,  )",
            '__ut.field.subfield( "a", 0 ).value;', "987 654 321" );
        Assert.equal( "F: Get value of second a field, (1,  )",
            '__ut.field.subfield( "a", 1 ).value;',
            "Another a subfield @#\xA4$`'~*^%&/()=?" );
        Assert.equal( "F: 1 Get value of b field, ( )",
            '__ut.field.subfield( "b" ).value;', "This is subfield b" );
        Assert.equal( "F: Get value of b field, (0,  )",
            '__ut.field.subfield( "b", 0 ).value;', "This is subfield b" );
        __ut.field.subfield( "a" ).value = "abcdef";
        Assert.equal( "F: Get value of first a field, ( )",
            '__ut.field.subfield( "a" ).value;', "abcdef" );
        Assert.equal( "F: Get value of first a field, (0,  )",
            '__ut.field.subfield( "a", 0 ).value;', "abcdef" );
        Assert.equal( "F: Get value of second a field, (1,  )",
            '__ut.field.subfield( "a", 1 ).value;',
            "Another a subfield @#\xA4$`'~*^%&/()=?" );
        Assert.equal( "F: 2 Get value of b field, ( )",
            '__ut.field.subfield( "b" ).value;', "This is subfield b" );
        __ut.field.subfield( "a", 0 ).value = "abcdefg";
        Assert.equal( "F: Get value of first a field, ( )",
            '__ut.field.subfield( "a" ).value;', "abcdefg" );
        Assert.equal( "F: Get value of first a field, (0,  )",
            '__ut.field.subfield( "a", 0 ).value;', "abcdefg" );
        Assert.equal( "F: Get value of second a field, (1,  )",
            '__ut.field.subfield( "a", 1 ).value;',
            "Another a subfield @#\xA4$`'~*^%&/()=?" );
        Assert.equal( "F: 3 Get value of b field, ( )",
            '__ut.field.subfield( "b" ).value;', "This is subfield b" );
        __ut.field.subfield( "a", 1 ).value = "abcdefgh";
        Assert.equal( "F: Get value of first a field, ( )",
            '__ut.field.subfield( "a" ).value;', "abcdefg" );
        Assert.equal( "F: Get value of first a field, (0,  )",
            '__ut.field.subfield( "a", 0 ).value;', "abcdefg" );
        Assert.equal( "F: Get value of second a field, (1,  )",
            '__ut.field.subfield( "a", 1 ).value;',
            "abcdefgh" );
        Assert.equal( "F: 4 Get value of b field, ( )",
            '__ut.field.subfield( "b" ).value;', "This is subfield b" );

        // Test remove of non existent subfield - should do nothing.
        Assert.equal( "F: Number of subfields before removing non-existant field", 
                      '__ut.field.count( "a" )', 2 );
        __ut.field.remove( "a", 2 );
        Assert.equal( "F: Number of subfields unchanged after removing non-existant field", 
                      '__ut.field.count( "a" )', 2 );
        Assert.equal( "F: Get value of first a field, ( )", 
                      '__ut.field.subfield( "a" ).value;', "abcdefg");
        Assert.equal( "F: Get value of first a field, (0)", 
                      '__ut.field.subfield( "a", 0 ).value;', "abcdefg");
        Assert.equal( "F: Get value of second a field, (1)", 
                      '__ut.field.subfield( "a", 1 ).value;', 
                      "abcdefgh");
        Assert.equal( "F: Get value of b field, ( )", 
                      '__ut.field.subfield( "b" ).value;', "This is subfield b");
        
        // Test remove of last subfield - should be remove
        __ut.field.remove( "a", 1 );
        Assert.equal( "F: Get value of first a field, ( )", 
                      '__ut.field.subfield( "a" ).value;', "abcdefg");
        Assert.equal( "F: Get value of first a field, (0)", 
                      '__ut.field.subfield( "a", 0 ).value;', "abcdefg");
        Assert.equal( "F: Get value of second a field, (1)", 
                      '__ut.field.subfield( "a", 1 ).value;', 
                      "");
        Assert.equal( "F: Get value of b field, ( )", 
                      '__ut.field.subfield( "b" ).value;', "This is subfield b");
        
        // Insert again, and test removal from front.
        __ut.field.append( "a", "abcdefgh" );

        // Test removal from front
        __ut.field.remove( "a" );
        // First a field should now be equal to second
        Assert.equal( "F: Get value of first a field after remove ( a ), ( )",
            '__ut.field.subfield( "a" ).value;', "abcdefgh" );
        Assert.equal( "F: Get value of first a field (1), (0,  )",
            '__ut.field.subfield( "a", 0 ).value;', "abcdefgh" );
        // Second should be empty
        Assert.equal( "F: Get value of second a field (1), (1,  )",
            '__ut.field.subfield( "a", 1 ).value;',
            "" );
        Assert.equal( "F: 5 Get value of b field, ( )",
            '__ut.field.subfield( "b" ).value;', "This is subfield b" );
        __ut.field.remove( "a" );
        Assert.equal( "F: Get value of first a field (2), ( )",
            '__ut.field.subfield( "a" ).value;', "" );
        Assert.equal( "F: Get value of first a field (3), (0,  )",
            '__ut.field.subfield( "a", 0 ).value;', "" );
        Assert.equal( "F: Get value of second a field, (1,  )",
            '__ut.field.subfield( "a", 1 ).value;',
            "" );
        Assert.equal( "F: 6 Get value of b field, ( )",
            '__ut.field.subfield( "b" ).value;', "This is subfield b" );
        __ut.field.remove( "b" );
        Assert.equal( "F: Get value of first a field (4), ( )",
            '__ut.field.subfield( "a" ).value;', "" );
        Assert.equal( "F: Get value of first a field (5), (0,  )",
            '__ut.field.subfield( "a", 0 ).value;', "" );
        Assert.equal( "F: Get value of second a field (6), (1,  )",
            '__ut.field.subfield( "a", 1 ).value;',
            "" );
        Assert.equal( "F: Get value of b field, ( )",
            '__ut.field.subfield( "b" ).value;', "" );

        // insert
        __ut.field = new Field( "009", "00" );
        __ut.field.insert( 0, "a", "av" );
        Assert.equalValue( "I: Into empty field. Field.size", __ut.field.count(), 1 );
        Assert.equalValue( "I: Into empty field. SF0: Name", __ut.field.subfield(0).name, "a" );
        Assert.equalValue( "I: Into empty field. SF0: Value", __ut.field.subfield(0).value, "av" );
        __ut.field.insert( 0, "b", "bv" );
        Assert.equalValue( "I: Into begin of non-empty field. Field.size", __ut.field.count(), 2 );
        Assert.equalValue( "I: Into begin of non-empty field. SF0: Name", __ut.field.subfield(0).name, "b" );
        Assert.equalValue( "I: Into begin of non-empty field. SF0: Value", __ut.field.subfield(0).value, "bv" );
        Assert.equalValue( "I: Into begin of non-empty field. SF1: Name", __ut.field.subfield(1).name, "a" );
        Assert.equalValue( "I: Into begin of non-empty field. SF1: Value", __ut.field.subfield(1).value, "av" );
        __ut.field.insert( 1, "c", "cv" );
        Assert.equalValue( "I: Into middle of non-empty field. Field.size", __ut.field.count(), 3 );
        Assert.equalValue( "I: Into middle of non-empty field. SF0: Name", __ut.field.subfield(0).name, "b" );
        Assert.equalValue( "I: Into middle of non-empty field. SF0: Value", __ut.field.subfield(0).value, "bv" );
        Assert.equalValue( "I: Into middle of non-empty field. SF1: Name", __ut.field.subfield(1).name, "c" );
        Assert.equalValue( "I: Into middle of non-empty field. SF1: Value", __ut.field.subfield(1).value, "cv" );
        Assert.equalValue( "I: Into middle of non-empty field. SF2: Name", __ut.field.subfield(2).name, "a" );
        Assert.equalValue( "I: Into middle of non-empty field. SF2: Value", __ut.field.subfield(2).value, "av" );

        ////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////
        // Tests for control fields, that is, fields with no subfields and no indicator
        __ut.controlField = new Field( );
        Assert.equal( "CF: Default is not a control field", '__ut.controlField.isControlField()', false );
        Assert.exception( "CF: Default reading of value must throw", '__ut.controlField.value' );
        // Assigning to value of Field, makes it a control field
        __ut.controlField.value = "Now I am a control field";
        Assert.equal( "CF: Now a control field", '__ut.controlField.isControlField()', true );
        Assert.equal( "CF: Value can be read back", '__ut.controlField.value', "Now I am a control field" );
        // Assign a name, check that it can be read
        __ut.controlField.name = "010";
        Assert.equal( "CF: Named field", '__ut.controlField.name', '010' );
        // Lots of fields, must fail now
        Assert.exception( "CF: Assigning to indicator must throw", '__ut.controlField.indicator = "02"' );
        Assert.exception( "CF: Reading from indicator must throw", '__ut.controlField.indicator' );
        Assert.exception( "CF: Append must throw", '__ut.controlField.append( new Subfield( "a", "123 456 789" ) )' );
        Assert.exception( "CF: Insert must throw", '__ut.controlField.insert( 0, "a", "av" )' );
        Assert.exception( "CF: Calling count must throw", '__ut.controlField.count()' );
        Assert.exception( "CF: Calling remove must throw", '__ut.controlField.remove( "a" )' );
        Assert.exception( "CF: Calling subfield must throw", '__ut.controlField.subfield( "a" )' );

        // Check toString
        Assert.equal( "CF: toString", '__ut.controlField.toString()', '010 Now I am a control field' );
        __ut.controlField.name = "011";
        __ut.controlField.value = "Still a control field! Testing @ and *";
        Assert.equal( "CF: toString", '__ut.controlField.toString()', '011 Still a control field! Testing @@ and @*' );

        // Check append
        __ut.cfr = new Record( );
        __ut.cfr.append( __ut.controlField );

        // Check that the field has the same values, when extracted from the record
        __ut.cfe = __ut.cfr.field( "011" );
        Assert.equal( "CFR: Is still a control field", '__ut.cfe.isControlField()', true );
        Assert.equal( "CFR: Value still the same", '__ut.cfe.value', "Still a control field! Testing @ and *" );

        // This can not become a control field, because it has a value in indicator
        __ut.controlField = new Field( "009", "02" );
        Assert.equal( "CF: Named field is not a control field", '__ut.controlField.isControlField()', false );
        Assert.exception( "CF: Named field, default reading of value must throw", '__ut.controlField.value' );
        Assert.exception( "CF: Named field, assignment to value must throw", '__ut.controlField.value = "I want to be a control field"' );
        Assert.equal( "CF: Named field is still not a control field", '__ut.controlField.isControlField()', false );
        Assert.equal( "CF: Still a named field 1", '__ut.controlField.name', '009' );
        __ut.controlField.name = "010";
        Assert.equal( "CF: Still a named field 2", '__ut.controlField.name', '010' );

        // No control field, if contains subfields
        __ut.controlField = new Field( );
        Assert.equal( "CF: Named field is not a control field 3", '__ut.controlField.isControlField()', false );
        Assert.equal( "CF: Count 1, must be 0", '__ut.controlField.count()', 0 );
        __ut.controlField.append( new Subfield( "a", "123 456 789" ) );
        Assert.equal( "CF: Count 2, must be 1", '__ut.controlField.count()', 1 );
        Assert.equal( "CF: Named field is not a control field 4", '__ut.controlField.isControlField()', false );
        Assert.exception( "CF: Named field, assignment to value must throw 2", '__ut.controlField.value = "I want to be a control field"' );
        Assert.equal( "CF: Named field is not a control field 4", '__ut.controlField.isControlField()', false );
        // Check that it still has the field
        Assert.equal( "CF: Count 3, must be 1", '__ut.controlField.count()', 1 );

        ////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////
        // Testing operations on isopost


        // Constructing
        Assert.equal( "R: Constructing an empty record",
            'var rec = new Record();', undefined );

        ////////////////////////////////////////////////////////////
        // Work with a record we construct from scratch
        __ut.record = new Record( );

        // numberOfFields
        Assert.equal( "R: Number of fields in empty record", '__ut.record.numberOfFields( );', 0 );
        // count
        Assert.equal( "R: Number of '001' fields in empty record", '__ut.record.count( "001" );', 0 );
        Assert.equal( "R: Number of '042' fields in empty record", '__ut.record.count( "042" );', 0 );
        // RecordStatus
        Assert.equal( "R: Record status", '__ut.record.recordStatus;', "c" );

        try {
            __ut.record.recordStatus = "g";
        } catch ( e ) {
            println( ">>>>>>>>>> WARNING!!!!!!! EXCEPTION: '" + e + "'" );
        }


        Assert.equal( "R: Record status after set", '__ut.record.recordStatus;', "g" );

        try {
            __ut.record.recordStatus = "n";
        } catch ( e ) {
            println( ">>>>>>>>>> WARNING!!!!!!! EXCEPTION: '" + e + "'" );
        }
        Assert.equal( "R: Record status after set", '__ut.record.recordStatus;', "n" );
        // Implementation codes
        Assert.equal( "R: Implementation Code 1", '__ut.record.implementationCodes;', "    " );
        __ut.record.implementationCodes = "DEAF";
        Assert.equal( "R: Implementation Code 2", '__ut.record.implementationCodes;', "DEAF" );
        __ut.record.implementationCodes = "sse ";
        Assert.equal( "R: Implementation Code 3", '__ut.record.implementationCodes;', "sse " );

        // For user systems
        Assert.equal( "R: For User Systems 1", '__ut.record.forUserSystems;', "   " );
        __ut.record.forUserSystems = "DEA";
        Assert.equal( "R: For User Systems 2", '__ut.record.forUserSystems;', "DEA" );
        __ut.record.forUserSystems = "ss ";
        Assert.equal( "R: For User Systems 3", '__ut.record.forUserSystems;', "ss " );

        // Append
        var newField = new Field( "042", "00" );
        Assert.exception( "R: Only accept 1 or 2 args", '__ut.record.append()' );
        Assert.exception( "R: Only accept 1 or 2 args", '__ut.record.append( 1, 2, 3 )' );
        Assert.exception( "R: Only accept 1 or 2 args", '__ut.record.append( "a", "b", "c" )' );

        __ut.record.append( newField );
        // numberOfFields
        Assert.equal( "R: Number of fields in empty record appended with a field",
            '__ut.record.numberOfFields( );', 1 );

        __ut.record.append( "043", "00" );

        Assert.equal( "R: Number of fields in empty record appended with two fields",
            '__ut.record.numberOfFields( );', 2 );

        // count
        Assert.equal( "R: Number of '001' fields", '__ut.record.count( "001" );', 0 );
        Assert.equal( "R: Number of '042' fields", '__ut.record.count( "042" );', 1 );
        Assert.equal( "R: Number of '043' fields 1", '__ut.record.count( "043" );', 1 );
        newField = new Field( "001", "00" );
        __ut.record.append( newField );
        Assert.equal( "R: Number of fields in empty record appended with three fields",
            '__ut.record.numberOfFields( );', 3 );
        // count
        Assert.equal( "R: Number of '001' fields", '__ut.record.count( "001" );', 1 );
        Assert.equal( "R: Number of '042' fields", '__ut.record.count( "042" );', 1 );
        Assert.equal( "R: Number of '043' fields 2", '__ut.record.count( "043" );', 1 );
        newField = new Field( "001", "00" );
        newField.append( "mbd", "testtest" );
        __ut.record.append( newField );
        Assert.equal( "R: Number of fields in empty record appended with four fields",
            '__ut.record.numberOfFields( );', 4 );
        // Check order
        Assert.equal( "R: Order of 001 field", '__ut.record.field( "001", 0 ).subfield( "mbd" ).value', "" );
        Assert.equal( "R: Order of 001 field", '__ut.record.field( "001", 1 ).subfield( "mbd" ).value', "testtest" );

        // count
        Assert.equal( "R: Number of '001' fields", '__ut.record.count( "001" );', 2 );
        Assert.equal( "R: Number of '042' fields", '__ut.record.count( "042" );', 1 );
        Assert.equal( "R: Number of '043' fields 3", '__ut.record.count( "043" );', 1 );
        // Remove
        Assert.exception( "Only accept 1 or 2 args", '__ut.record.remove()' );
        Assert.exception( "Only accept 1 or 2 args", '__ut.record.remove( "a", 2, "b" )' );
        Assert.exception( "Only accept 1 or 2 args", '__ut.record.remove( "a", 23, 3 )' );
        __ut.record.remove( 0 ); // 042 field
        Assert.equal( "R: Number of fields after remove(0)",
            '__ut.record.numberOfFields();', 3 );

        // Remove the second 001 field, the one with a subfield with value testtest
        // The remaining field should have the empty value
        __ut.record.remove( "001", 1 );

        // Removal of non-existing field - should have no influence.
        __ut.record.remove( "001", 1 );


        // count
        Assert.equal( "R: Number of '001' fields", '__ut.record.count( "001" );', 1 );
        Assert.equal( "R: Number of '042' fields", '__ut.record.count( "042" );', 0 );
        Assert.equal( "R: Number of '043' fields 4", '__ut.record.count( "043" );', 1 );
        Assert.equal( "R: Correct 001 field removed", '__ut.record.field( "001", 0 ).subfield( "mbd" ).value', "" );

        ////////////////////////////////////////////////////////////

        // Originally, this test read a post from a file.
        // However, this is not supported in Java, and we need this file to work
        // with both the java and C++ version. Therefore,
        // the file reading was dropped, and instead a post is create by this function.

        var createIsoPost = function( ) {

            var rec = new Record( );
            rec.implementationCodes = 'sse ';
            rec.recordStatus = 'n';
            var f;
            f = new Field( '001', '00' );
            f.append( 'a', '2 548 263 8' );
            f.append( 'b', '870970' );
            f.append( 'c', '20041117025343' );
            f.append( 'd', '20041101' );
            f.append( 'f', 'a' );
            f.append( 't', 'FAUST' );
            rec.append( f );
            f = new Field( '004', '00' );
            f.append( 'r', 'n' );
            f.append( 'a', 'e' );
            rec.append( f );
            f = new Field( '005', '00' );
            f.append( 'h', 'e' );
            rec.append( f );
            f = new Field( '008', '00' );
            f.append( 't', 's' );
            f.append( 'u', 'f' );
            f.append( 'a', '2004' );
            f.append( 'b', 'gb' );
            f.append( 'l', 'eng' );
            f.append( 'v', '0' );
            rec.append( f );
            f = new Field( '009', '00' );
            f.append( 'a', 's' );
            f.append( 'g', 'xc' );
            rec.append( f );
            f = new Field( '010', '00' );
            f.append( 'a', 'D947837018' );
            rec.append( f );
            f = new Field( '021', '00' );
            f.append( 'd', 'kr. 159,00' );
            rec.append( f );
            f = new Field( '023', '00' );
            f.append( 'a', '0028947564089' );
            rec.append( f );
            f = new Field( '032', '00' );
            f.append( 'x', 'SFG200447' );
            f.append( 'x', 'SFGO200447' );
            rec.append( f );
            f = new Field( '039', '00' );
            f.append( 'a', 'bef' );
            rec.append( f );
            f = new Field( '100', '00' );
            f.append( '0', '' );
            f.append( 'a', 'Richard' );
            f.append( 'h', 'Cliff' );
            rec.append( f );
            f = new Field( '245', '00' );
            f.append( 'a', 'Something\'s goin\' on' );
            f.append( 'e', 'Cliff Richard with Steve Mandile, Tommy Sims, JT Corenflos, Michael Spriggs, Greg Morrow, Nashville String Machine, Tony Harrell, Barry Gibb ... [et al.]' );
            rec.append( f );
            f = new Field( '260', '00' );
            f.append( 'a', 'London' );
            f.append( 'b', 'Decca Music Group' );
            f.append( 'c', 'p 2004' );
            rec.append( f );
            f = new Field( '300', '00' );
            f.append( 'n', '1 cd' );
            rec.append( f );
            f = new Field( '512', '00' );
            f.append( 'a', 'Tekster p\xE5 omslag' );
            rec.append( f );
            f = new Field( '531', '00' );
            f.append( 'a', 'Indhold:' );
            rec.append( f );
            f = new Field( '538', '00' );
            f.append( 'f', 'Decca' );
            f.append( 'g', '4756408' );
            rec.append( f );
            f = new Field( '652', '00' );
            f.append( 'm', '78.794' );
            f.append( 'v', '5' );
            rec.append( f );
            f = new Field( '666', '00' );
            f.append( 'm', 'pop' );
            f.append( 'm', 'rock' );
            f.append( 'n', 'vokal' );
            f.append( 'p', '2000-2009' );
            f.append( 'l', 'England' );
            rec.append( f );
            f = new Field( '700', '00' );
            f.append( '0', '' );
            f.append( 'a', 'Corenflos' );
            f.append( 'h', 'J. T.' );
            rec.append( f );
            f = new Field( '700', '00' );
            f.append( '0', '' );
            f.append( 'a', 'Gibb' );
            f.append( 'h', 'Barry' );
            rec.append( f );
            f = new Field( '700', '00' );
            f.append( '0', '' );
            f.append( 'a', 'Harrell' );
            f.append( 'h', 'Tony' );
            rec.append( f );
            f = new Field( '700', '00' );
            f.append( '0', '' );
            f.append( 'a', 'Lewis' );
            f.append( 'h', 'Ken' );
            rec.append( f );
            f = new Field( '700', '00' );
            f.append( '0', '' );
            f.append( 'a', 'Mandile' );
            f.append( 'h', 'Steve' );
            rec.append( f );
            f = new Field( '700', '00' );
            f.append( '0', '' );
            f.append( 'a', 'Morrow' );
            f.append( 'h', 'Greg' );
            rec.append( f );
            f = new Field( '700', '00' );
            f.append( '0', '' );
            f.append( 'a', 'Sims' );
            f.append( 'h', 'Tommy' );
            rec.append( f );
            f = new Field( '700', '00' );
            f.append( '0', '' );
            f.append( 'a', 'Spriggs' );
            f.append( 'h', 'Michael' );
            rec.append( f );
            f = new Field( '710', '00' );
            f.append( '0', '' );
            f.append( 'a', 'Nashville String Machine' );
            rec.append( f );
            f = new Field( '795', '00' );
            f.append( '\xE5', '11' );
            f.append( '0', '' );
            f.append( 'a', 'Thousand miles to go' );
            f.append( 'a', 'Somethin\' is goin\' on' );
            f.append( 'a', 'I will not be a mistake' );
            f.append( 'a', 'Simplicity' );
            f.append( 'a', 'Sometimes love' );
            f.append( 'a', 'I cannot give you my love' );
            f.append( 'a', 'The \xA4day that I stop loving you' );
            f.append( 'a', 'What car' );
            f.append( 'a', 'How did she get here' );
            f.append( 'a', 'Field of love' );
            f.append( 'a', 'For life' );
            f.append( 'a', 'I don\'t wanna lose you' );
            f.append( 'a', 'Faithful one' );
            rec.append( f );
            f = new Field( '795', '00' );
            f.append( '\xE5', '99' );
            f.append( '0', '' );
            f.append( 'y', '0' );
            f.append( 'a', '1000 miles to go' );
            rec.append( f );

            return rec;
        };

        // Work with a "real record"
        __ut.isopost = createIsoPost( );

        // Test toString
        Assert.equal( "RI: toString test", "__ut.isopost.toString()",
            "001 00 *a 2 548 263 8 *b 870970 *c 20041117025343 *d 20041101 *f a *t FAUST \n" +
            "004 00 *r n *a e \n" +
            "005 00 *h e \n" +
            "008 00 *t s *u f *a 2004 *b gb *l eng *v 0 \n" +
            "009 00 *a s *g xc \n" +
            "010 00 *a D947837018 \n" +
            "021 00 *d kr. 159,00 \n" +
            "023 00 *a 0028947564089 \n" +
            "032 00 *x SFG200447 *x SFGO200447 \n" +
            "039 00 *a bef \n" +
            "100 00 *0 *a Richard *h Cliff \n" +
            "245 00 *a Something's goin' on *e Cliff Richard with Steve Mandile, Tommy Sims, JT Corenflos, Michael Spriggs, Greg Morrow, Nashville String Machine, Tony Harrell, Barry Gibb ... [et al.] \n" +
            "260 00 *a London *b Decca Music Group *c p 2004 \n" +
            "300 00 *n 1 cd \n" +
            "512 00 *a Tekster p\xE5 omslag \n" +
            "531 00 *a Indhold: \n" +
            "538 00 *f Decca *g 4756408 \n" +
            "652 00 *m 78.794 *v 5 \n" +
            "666 00 *m pop *m rock *n vokal *p 2000-2009 *l England \n" +
            "700 00 *0 *a Corenflos *h J. T. \n" +
            "700 00 *0 *a Gibb *h Barry \n" +
            "700 00 *0 *a Harrell *h Tony \n" +
            "700 00 *0 *a Lewis *h Ken \n" +
            "700 00 *0 *a Mandile *h Steve \n" +
            "700 00 *0 *a Morrow *h Greg \n" +
            "700 00 *0 *a Sims *h Tommy \n" +
            "700 00 *0 *a Spriggs *h Michael \n" +
            "710 00 *0 *a Nashville String Machine \n" +
            "795 00 *\xE5 11 *0 *a Thousand miles to go *a Somethin' is goin' on *a I will not be a mistake *a Simplicity *a Sometimes love *a I cannot give you my love *a The \xA4day that I stop loving you *a What car *a How did she get here *a Field of love *a For life *a I don't wanna lose you *a Faithful one \n" +
            "795 00 *\xE5 99 *0 *y 0 *a 1000 miles to go \n" );

        // number of fields
        Assert.equal( "RI: Number of fields", "__ut.isopost.numberOfFields( )", 30 );


        // Counts
        Assert.equal( "RI: Count of field 001", '__ut.isopost.count( "001" )', 1 );
        Assert.equal( "RI: Count of field 666", '__ut.isopost.count( "666" )', 1 );
        Assert.equal( "RI: Count of field 795", '__ut.isopost.count( "795" )', 2 );
        Assert.equal( "RI: Count of field 700", '__ut.isopost.count( "700" )', 8 );

        // Record status
        var tmp = __ut.isopost.recordStatus;
        Assert.equal( "RI: Record status", '__ut.isopost.recordStatus;', "n" );
        try {
            __ut.isopost.recordStatus = "x";
        } catch ( e ) {
            println( ">>>>>>>>>> WARNING!!!!!!! EXCEPTION: '" + e + "'" );
        }
        Assert.equal( "RI: Record status after change", '__ut.isopost.recordStatus;', "x" );
        try {
            __ut.isopost.recordStatus = tmp;
        } catch ( e ) {
            println( ">>>>>>>>>> WARNING!!!!!!! EXCEPTION: '" + e + "'" );
        }
        Assert.equal( "RI: Record status after reset", '__ut.isopost.recordStatus;', tmp );

        tmp = __ut.isopost.implementationCodes;
        Assert.equal( "RI: Implementation codes",
            '__ut.isopost.implementationCodes;', "sse " );
        __ut.isopost.implementationCodes = "BOFH";
        Assert.equal( "RI: Implementation codes after change",
            '__ut.isopost.implementationCodes;', "BOFH" );
        __ut.isopost.implementationCodes = tmp;
        Assert.equal( "RI: Implementation codes after reset",
            '__ut.isopost.implementationCodes;', tmp );

        tmp = __ut.isopost.forUserSystems;
        Assert.equal( "RI: For User Systems",
            '__ut.isopost.forUserSystems;', "   " );
        __ut.isopost.forUserSystems = "BOF";
        Assert.equal( "RI: For User Systems after change",
            '__ut.isopost.forUserSystems;', "BOF" );
        __ut.isopost.forUserSystems = tmp;
        Assert.equal( "RI: For User Systems after reset",
            '__ut.isopost.forUserSystems;', tmp );

        ////////////////////////////////////////////////////////////
        // Field operations

        // Names
        Assert.equal( "RI: Name of field 001", '__ut.isopost.field( "001" ).name', "001" );
        Assert.equal( "RI: Name of field 666", '__ut.isopost.field( "666" ).name', "666" );
        Assert.equal( "RI: Name of (first ) field 700", '__ut.isopost.field( "700" ).name', "700" );
        Assert.equal( "RI: Alternative name of (first ) field 700", '__ut.isopost.field( "700", 0 ).name', "700" );
        Assert.equal( "RI: Alternative name of (second ) field 700", '__ut.isopost.field( "700", 1 ).name', "700" );

        // Indicators
        Assert.equal( "RI: Indicator from field 001",
            '__ut.isopost.field( "001" ).indicator', "00" );
        Assert.equal( "RI: Indicator from field 538",
            '__ut.isopost.field( "538" ).indicator', "00" );
        Assert.equal( "RI: Indicator from last field 795",
            '__ut.isopost.field( "795", 1 ).indicator', "00" );


        // Not existing name
        Assert.equal( "RI: Name of field 042 (not present )", '__ut.isopost.field( "042" ).name', "" );
        Assert.equal( "RI: Count of field 042 (not present )", '__ut.isopost.count( "042" )', 0 );

        // Modify/Set name
        __ut.isopost.field( "001" ).name = "042";
        Assert.equal( "RI: Name of field 001 after rename", '__ut.isopost.field( "001" ).name', "" );
        Assert.equal( "RI: Name of field 042 after rename", '__ut.isopost.field( "042" ).name', "042" );
        Assert.equal( "RI: Count of field 001", '__ut.isopost.count( "001" )', 0 );
        Assert.equal( "RI: Count of field 042", '__ut.isopost.count( "042" )', 1 );

        // Modify back
        __ut.isopost.field( "042" ).name = "001";
        Assert.equal( "RI: Name of field 001 after second rename", '__ut.isopost.field( "001" ).name', "001" );
        Assert.equal( "RI: Name of field 042 after second rename", '__ut.isopost.field( "042" ).name', "" );
        Assert.equal( "RI: Count of field 001", '__ut.isopost.count( "001" )', 1 );
        Assert.equal( "RI: Count of field 042", '__ut.isopost.count( "042" )', 0 );

        ////////////////////////////////////////////////////////////
        // Sub field operations

        Assert.equal( "RI: Count of 001 subfields", '__ut.isopost.field( "001" ).count()', 6 );
        Assert.equal( "RI: Count of 004 subfields", '__ut.isopost.field( "004" ).count()', 2 );
        Assert.equal( "RI: Count of first 700 subfields", '__ut.isopost.field( "700" ).count()', 3 );
        Assert.equal( "RI: Alternative count of first 700 subfields", '__ut.isopost.field( "700", 0 ).count()', 3 );
        Assert.equal( "RI: Count of second 700 subfields", '__ut.isopost.field( "700", 1 ).count()', 3 );
        Assert.equal( "RI: Count of all field 700 subfields",
            '\
var total = 0;\
var i; \
var end = __ut.isopost.count( "700" ); \
for ( i = 0; i < end; i++ ) { \
    total += __ut.isopost.field( "700", i ).count(); \
}; \
total;', 24 );

        ////////////////////////////////////////////////////////////
        // Test fromString on record
        // These tests have been duplicated from the C++ implementation tests
        __ut.fromstringpost = new Record();
        Assert.exception( "R: FromString with incomplete record 1", '__ut.fromstringpost.fromString("100")' );
        Assert.exception( "R: FromString with incomplete record 2", '__ut.fromstringpost.fromString("100 7")' );
        Assert.exception( "R: FromString with incomplete record 3", '__ut.fromstringpost.fromString("100  7")' );
        Assert.exception( "R: FromString with incomplete record 4", '__ut.fromstringpost.fromString("100 79 *")' );
        Assert.exception( "R: FromString with incomplete record 5", '__ut.fromstringpost.fromString("100 00 *aa*bb\\n200 10")' );
        // TODO: Shouldn't this fail, that is, not be accepted?
        // Assert.exception( "R: FromString with incomplete record 11", '__ut.fromstringpost.fromString("100 00 hej *aa*bb")' );

        __ut.fromstringpost.fromString("100    *aa*bb");
        Assert.equal( "R: FromString with indicator: Record 1", '__ut.fromstringpost.field(0).indicator', "  " );

        __ut.fromstringpost.fromString("100 0  *aa*bb");
        Assert.equal( "R: FromString with indicator: Record 2", '__ut.fromstringpost.field(0).indicator', "0 " );

        __ut.fromstringpost.fromString("100  8 *aa*bb");
        Assert.equal( "R: FromString with indicator: Record 3", '__ut.fromstringpost.field(0).indicator', " 8" );

        __ut.fromstringpost.fromString("100 00 *aa*bb\n200  8 *aa*bb");
        Assert.equal( "R: FromString with indicator: Record 4", '__ut.fromstringpost.field(1).indicator', " 8" );

        __ut.fromstringpost.fromString("100 00 *aa*bb\n200  8 *aa*bb\n300 00 *aa*bb\n400 00 *aa*bb");
        Assert.equal( "R: FromString with indicator: Record 5", '__ut.fromstringpost.field(1).indicator', " 8" );

        __ut.fromstringpost.fromString( "100 27 *ab*cd" );
        Assert.equal( "R: FromString 1: Num fields", '__ut.fromstringpost.numberOfFields()', 1);
        Assert.equal( "R: FromString 1: Field 0 name", '__ut.fromstringpost.field(0).name', "100" );
        Assert.equal( "R: FromString 1: Field 0 indicator", '__ut.fromstringpost.field(0).indicator', "27" );
        Assert.equal( "R: FromString 1: Field 0 # subfields", '__ut.fromstringpost.field(0).count()', 2 );
        Assert.equal( "R: FromString 1: Field 0 subfield 0 name", '__ut.fromstringpost.field(0).subfield(0).name', "a" );
        Assert.equal( "R: FromString 1: Field 0 subfield 0 value", '__ut.fromstringpost.field(0).subfield(0).value', "b" );
        Assert.equal( "R: FromString 1: Field 0 subfield 1 name", '__ut.fromstringpost.field(0).subfield(1).name', "c" );
        Assert.equal( "R: FromString 1: Field 0 subfield 1 value", '__ut.fromstringpost.field(0).subfield(1).value', "d" );

        __ut.fromstringpost.fromString( "100 27 *ab*cd\n100 00 *ef*gh" );
        Assert.equal( "R: FromString 2: Num fields", '__ut.fromstringpost.numberOfFields()', 2);
        Assert.equal( "R: FromString 2: Field 0 name", '__ut.fromstringpost.field(0).name', "100" );
        Assert.equal( "R: FromString 2: Field 0 indicator", '__ut.fromstringpost.field(0).indicator', "27" );
        Assert.equal( "R: FromString 2: Field 0 # subfields", '__ut.fromstringpost.field(0).count()', 2 );
        Assert.equal( "R: FromString 2: Field 0 subfield 0 name", '__ut.fromstringpost.field(0).subfield(0).name', "a" );
        Assert.equal( "R: FromString 2: Field 0 subfield 0 value", '__ut.fromstringpost.field(0).subfield(0).value', "b" );
        Assert.equal( "R: FromString 2: Field 0 subfield 1 name", '__ut.fromstringpost.field(0).subfield(1).name', "c" );
        Assert.equal( "R: FromString 2: Field 0 subfield 1 value", '__ut.fromstringpost.field(0).subfield(1).value', "d" );
        Assert.equal( "R: FromString 2: Field 1 name", '__ut.fromstringpost.field(1).name', "100" );
        Assert.equal( "R: FromString 2: Field 1 indicator", '__ut.fromstringpost.field(1).indicator', "00" );
        Assert.equal( "R: FromString 2: Field 1 # subfields", '__ut.fromstringpost.field(1).count()', 2 );
        Assert.equal( "R: FromString 2: Field 1 subfield 0 name", '__ut.fromstringpost.field(1).subfield(0).name', "e" );
        Assert.equal( "R: FromString 2: Field 1 subfield 0 value", '__ut.fromstringpost.field(1).subfield(0).value', "f" );
        Assert.equal( "R: FromString 2: Field 1 subfield 1 name", '__ut.fromstringpost.field(1).subfield(1).name', "g" );
        Assert.equal( "R: FromString 2: Field 1 subfield 1 value", '__ut.fromstringpost.field(1).subfield(1).value', "h" );

        // Note, that a field with no subfields, must not give a field...
        __ut.fromstringpost.fromString( "100 00 *ab*cd*ef*gh" );
        Assert.equal( "R: FromString 3: Num fields", '__ut.fromstringpost.numberOfFields()', 1);
        Assert.equal( "R: FromString 3: Field 0 name", '__ut.fromstringpost.field(0).name', "100" );
        Assert.equal( "R: FromString 3: Field 0 indicator", '__ut.fromstringpost.field(0).indicator', "00" );
        Assert.equal( "R: FromString 3: Field 0 # subfields", '__ut.fromstringpost.field(0).count()', 4 );
        Assert.equal( "R: FromString 3: Field 0 subfield 0 name", '__ut.fromstringpost.field(0).subfield(0).name', "a" );
        Assert.equal( "R: FromString 3: Field 0 subfield 0 value", '__ut.fromstringpost.field(0).subfield(0).value', "b" );
        Assert.equal( "R: FromString 3: Field 0 subfield 1 name", '__ut.fromstringpost.field(0).subfield(1).name', "c" );
        Assert.equal( "R: FromString 3: Field 0 subfield 1 value", '__ut.fromstringpost.field(0).subfield(1).value', "d" );
        Assert.equal( "R: FromString 3: Field 0 subfield 2 name", '__ut.fromstringpost.field(0).subfield(2).name', "e" );
        Assert.equal( "R: FromString 3: Field 0 subfield 2 value", '__ut.fromstringpost.field(0).subfield(2).value', "f" );
        Assert.equal( "R: FromString 3: Field 0 subfield 3 name", '__ut.fromstringpost.field(0).subfield(3).name', "g" );
        Assert.equal( "R: FromString 3: Field 0 subfield 3 value", '__ut.fromstringpost.field(0).subfield(3).value', "h" );

        __ut.fromstringpost.fromString( "100 00 *\u00E6\u00C6*\u00F8\u00D8*\u00E5\u00C5*gh" );
        Assert.equal( "R: FromString 4: Num fields", '__ut.fromstringpost.numberOfFields()', 1);
        Assert.equal( "R: FromString 4: Field 0 name", '__ut.fromstringpost.field(0).name', "100" );
        Assert.equal( "R: FromString 4: Field 0 indicator", '__ut.fromstringpost.field(0).indicator', "00" );
        Assert.equal( "R: FromString 4: Field 0 # subfields", '__ut.fromstringpost.field(0).count()', 4 );
        Assert.equal( "R: FromString 4: Field 0 subfield 0 name", '__ut.fromstringpost.field(0).subfield(0).name', "\u00E6" );
        Assert.equal( "R: FromString 4: Field 0 subfield 0 value", '__ut.fromstringpost.field(0).subfield(0).value', "\u00C6" );
        Assert.equal( "R: FromString 4: Field 0 subfield 1 name", '__ut.fromstringpost.field(0).subfield(1).name', "\u00F8" );
        Assert.equal( "R: FromString 4: Field 0 subfield 1 value", '__ut.fromstringpost.field(0).subfield(1).value', "\u00D8" );
        Assert.equal( "R: FromString 4: Field 0 subfield 2 name", '__ut.fromstringpost.field(0).subfield(2).name', "\u00E5" );
        Assert.equal( "R: FromString 4: Field 0 subfield 2 value", '__ut.fromstringpost.field(0).subfield(2).value', "\u00C5" );
        Assert.equal( "R: FromString 4: Field 0 subfield 3 name", '__ut.fromstringpost.field(0).subfield(3).name', "g" );
        Assert.equal( "R: FromString 4: Field 0 subfield 3 value", '__ut.fromstringpost.field(0).subfield(3).value', "h" );

        // Some stuff needs to be specially treated, @@ => @, @* => *
        // 
        // Test defined as BAD by MB/JA7 2016-01-14
        // 
//        __ut.fromstringpost.fromString( "100 00 *a Another a subfield @@#\xA4$`'~@*^%&/()=? *c 123456" );
//        Assert.equal( "R: FromString 5: Num fields", '__ut.fromstringpost.numberOfFields()', 1);
//        Assert.equal( "R: FromString 5: Field 0 name", '__ut.fromstringpost.field(0).name', "100" );
//        Assert.equal( "R: FromString 5: Field 0 indicator", '__ut.fromstringpost.field(0).indicator', "00" );
//        Assert.equal( "R: FromString 5: Field 0 # subfields", '__ut.fromstringpost.field(0).count()', 2 );
//        Assert.equal( "R: FromString 5: Field 0 subfield 0 name", '__ut.fromstringpost.field(0).subfield(0).name', "a" );
//        Assert.equal( "R: FromString 5: Field 0 subfield 0 value", '__ut.fromstringpost.field(0).subfield(0).value', "Another a subfield @#\xA4$`'~*^%&/()=?" );
//        Assert.equal( "R: FromString 5: Field 0 subfield 1 name", '__ut.fromstringpost.field(0).subfield(1).name', "c" );
//        Assert.equal( "R: FromString 5: Field 0 subfield 1 value", '__ut.fromstringpost.field(0).subfield(1).value', "123456" );
        
        // Records can continue across several lines. Lets check ...
        // TODO: You can't split on spaces???
        __ut.fromstringpost.fromString( "100 42 *a Meget lang l\n    inie her e\r\n    nder @* den" );
        Assert.equal( "R: FromString 7: Num fields", '__ut.fromstringpost.numberOfFields()', 1);
        Assert.equal( "R: FromString 7: Field 0 name", '__ut.fromstringpost.field(0).name', "100" );
        Assert.equal( "R: FromString 7: Field 0 indicator", '__ut.fromstringpost.field(0).indicator', "42" );
        Assert.equal( "R: FromString 7: Field 0 # subfields", '__ut.fromstringpost.field(0).count()', 1 );
        Assert.equal( "R: FromString 7: Field 0 subfield 0 name", '__ut.fromstringpost.field(0).subfield(0).name', "a" );
        Assert.equal( "R: FromString 7: Field 0 subfield 0 value", '__ut.fromstringpost.field(0).subfield(0).value', "Meget lang linie her ender * den" );

        // TODO: Controlfields are not supported.
        // Assert.that( "Test OK with control field", '__ut.fromstringpost.fromString( "010 hej med dig" );true;' );
        

        
        // Test we are OK with \n at the end of a line
        Assert.that( "Test OK with newline at end of line", '__ut.fromstringpost.fromString( "100 42 *a b\\n" );true;' );

        // TODO: C++ fails for this. Should we?
        // Assert.that( "Test OK with newline at end of line and spaces...", '__ut.fromstringpost.fromString( "100 42 *a b\\n   " );true;' );


        // Reuse ISO post tests, sort of.
        __ut.fromstringpost = createIsoPost( );
        __ut.isopost.fromString( __ut.fromstringpost.toString() );
        
        // number of fields
        Assert.equal( "RI2: Number of fields", "__ut.isopost.numberOfFields( )", 30 );

        // Counts
        Assert.equal( "RI2: Count of field 001", '__ut.isopost.count( "001" )', 1 );
        Assert.equal( "RI2: Count of field 666", '__ut.isopost.count( "666" )', 1 );
        Assert.equal( "RI2: Count of field 795", '__ut.isopost.count( "795" )', 2 );
        Assert.equal( "RI2: Count of field 700", '__ut.isopost.count( "700" )', 8 );

        Assert.equal( "RI2: Name of field 001", '__ut.isopost.field( "001" ).name', "001" );
        Assert.equal( "RI2: Name of field 666", '__ut.isopost.field( "666" ).name', "666" );
        Assert.equal( "RI2: Name of (first ) field 700", '__ut.isopost.field( "700" ).name', "700" );
        Assert.equal( "RI2: Alternative name of (first ) field 700", '__ut.isopost.field( "700", 0 ).name', "700" );
        Assert.equal( "RI2: Alternative name of (second ) field 700", '__ut.isopost.field( "700", 1 ).name', "700" );

        // Indicators
        Assert.equal( "RI2: Indicator from field 001",
            '__ut.isopost.field( "001" ).indicator', "00" );
        Assert.equal( "RI2: Indicator from field 538",
            '__ut.isopost.field( "538" ).indicator', "00" );
        Assert.equal( "RI2: Indicator from last field 795",
            '__ut.isopost.field( "795", 1 ).indicator', "00" );

        // Not existing name
        Assert.equal( "RI2: Name of field 042 (not present )", '__ut.isopost.field( "042" ).name', "" );
        Assert.equal( "RI2: Count of field 042 (not present )", '__ut.isopost.count( "042" )', 0 );

        // Modify/Set name
        __ut.isopost.field( "001" ).name = "042";
        Assert.equal( "RI2: Name of field 001 after rename", '__ut.isopost.field( "001" ).name', "" );
        Assert.equal( "RI2: Name of field 042 after rename", '__ut.isopost.field( "042" ).name', "042" );
        Assert.equal( "RI2: Count of field 001", '__ut.isopost.count( "001" )', 0 );
        Assert.equal( "RI2: Count of field 042", '__ut.isopost.count( "042" )', 1 );

        // Modify back
        __ut.isopost.field( "042" ).name = "001";
        Assert.equal( "RI2: Name of field 001 after second rename", '__ut.isopost.field( "001" ).name', "001" );
        Assert.equal( "RI2: Name of field 042 after second rename", '__ut.isopost.field( "042" ).name', "" );
        Assert.equal( "RI2: Count of field 001", '__ut.isopost.count( "001" )', 1 );
        Assert.equal( "RI2: Count of field 042", '__ut.isopost.count( "042" )', 0 );

        ////////////////////////////////////////////////////////////
        // Sub field operations

        Assert.equal( "RI2: Count of 001 subfields", '__ut.isopost.field( "001" ).count()', 6 );
        Assert.equal( "RI2: Count of 004 subfields", '__ut.isopost.field( "004" ).count()', 2 );
        Assert.equal( "RI2: Count of first 700 subfields", '__ut.isopost.field( "700" ).count()', 3 );
        Assert.equal( "RI2: Alternative count of first 700 subfields", '__ut.isopost.field( "700", 0 ).count()', 3 );
        Assert.equal( "RI2: Count of second 700 subfields", '__ut.isopost.field( "700", 1 ).count()', 3 );
        Assert.equal( "RI2: Count of all field 700 subfields",
            '\
var total = 0;\
var i; \
var end = __ut.isopost.count( "700" ); \
for ( i = 0; i < end; i++ ) { \
    total += __ut.isopost.field( "700", i ).count(); \
}; \
total;', 24 );

        ////////////////////////////////////////////////////////////
        // MVS test of illegal return value
        var hest = function( ) {
            var rec = new Record( );
            rec.implementationCodes = 'sse ';
            rec.recordStatus = 'n';
            var f;
            f = new Field( '008', '00' );
            f.append( 'a', '1997' );
            rec.append( f );
            f = new Field( '009', '00' );
            f.append( 'a', 'a' );
            f.append( 'g', 'xx' );
            rec.append( f );
            f = new Field( '021', '00' );
            f.append( 'a', '87-11-15086-6' );
            f.append( 'c', 'hf' );
            f.append( 'd', 'kr. 248,00' );
            rec.append( f );
            return rec;
        };

        __ut.hestpost = hest( );
        Assert.equal( "RI: test of illegal return value", '__ut.hestpost.field("008").subfield("l").value', "" );

        // Pretty ordinary stuff
        Assert.equal( "RI: Content (value of a subfield in 001",
            '__ut.isopost.field( "001" ).subfield( "a" ).value;',
            "2 548 263 8" );
        Assert.equal( "RI: Content (value of b subfield in 001",
            '__ut.isopost.field( "001" ).subfield( "b" ).value;',
            "870970" );
        Assert.equal( "RI: Content (value of t subfield in 001",
            '__ut.isopost.field( "001" ).subfield( "t" ).value;',
            "FAUST" );

        // Iterate over all fields 008 record
        Assert.equal( "RI: Content of all subfields in 008",
            '\
var i; \
var end = __ut.isopost.field( "008" ).count(); \
var total = ""; \
var pre = ""; \
for( i = 0 ; i < end ; i++ ) { \
    total += pre + __ut.isopost.field( "008" ).subfield( i ).value; \
    pre = ":"; \
}; \
total;', "s:f:2004:gb:eng:0" );


        // subfield, multiple a fields
        Assert.equal( "RI: Content (value of \u00e5 subfield in 795",
            '__ut.isopost.field( "795" ).subfield( "\u00e5" ).value;',
            "11" );
        Assert.equal( "RI: Content (value of first a subfield in 795",
            '__ut.isopost.field( "795" ).subfield( "a" ).value;',
            "Thousand miles to go" );
        Assert.equal( "RI: Count of all a subfields in first 795 field",
            '__ut.isopost.field( "795" ).count( "a" )', 13 );
        Assert.equal( "RI: Content of all a subfields in first 795 field",
            '\
var i; \
var end = __ut.isopost.field( "795" ).count( "a" ); \
var total = ""; \
var pre = ""; \
for( i = 0 ; i < end ; i++ ) { \
    total += pre + __ut.isopost.field( "795" ).subfield( "a", i ).value; \
    pre = ":"; \
}; \
total;', "Thousand miles to go:Somethin' is goin' on:I will not be a mistake:Simplicity:Sometimes love:I cannot give you my love:The \u00a4day that I stop loving you:What car:How did she get here:Field of love:For life:I don't wanna lose you:Faithful one" );



        Assert.equal( "RI: Content of second 700 field, subfield a", '__ut.isopost.field( "700", 1 ).subfield( "a" ).value;', "Gibb" );

        ////////////////////////////////////////////////////////////
        // Modify subfield operations


        Assert.equal( "RI: Count of all field 700 subfields before append",
            '\
var total = 0;\
var i; \
var end = __ut.isopost.count( "700" ); \
for ( i = 0; i < end; i++ ) { \
    total += __ut.isopost.field( "700", i ).count(); \
}; \
total', 24 );
        __ut.isopost.field( "700", 1 ).append( "a", "Mads testing" );
        Assert.equal( "RI: Count of second 700 subfields after append",
            '__ut.isopost.field( "700", 1 ).count()', 4 );
        Assert.equal( "RI: Count of number of 700 fields after append",
            '__ut.isopost.count( "700" );', 8 );
        Assert.equal( "RI: Count of all field 700 subfields",
            '\
var total = 0;\
var i; \
var end = __ut.isopost.count( "700" ); \
for ( i = 0; i < end; i++ ) { \
    total += __ut.isopost.field( "700", i ).count(); \
}; \
total', 25 );


        ////////////////////////////////////////////////////////////////////////////////
        //
        // Operations that involve both a ISOPOST, and Recordfield.

        // Append
        // Test append, and rename, because we wish to maintain references, not?
        __ut.field = new Field( "420", "00" );
        __ut.record = new Record( );
        Assert.equal( "Count of empty record", '__ut.record.numberOfFields( )', 0 );
        __ut.record.append( __ut.field );
        Assert.equal( "Count of empty record after append", '__ut.record.numberOfFields( )', 1 );
        Assert.equal( 'Count( "420" ) of empty record after append',
            '__ut.record.count( "420" )', 1 );
        // Change the name
        // This will not influence the record, as the field has been copied into the
        // __ut.record.
        __ut.field.name = "042";
        Assert.equal( 'Count( "420" ) of empty record after append and rename',
            '__ut.record.count( "420" )', 1 );
        Assert.equal( 'Count( "042" ) of empty record after append and rename',
            '__ut.record.count( "042" )', 0 );


        ////////////////////////////////////////////////////////////////////////////////
        // Test what happens, if we assign to something that does not exist...
        // This is to make stuff easier for the users.
        // Note, however, we perhaps should throw an exception.
        __ut.arec = new Record( );    

    
        Assert.equal("Tryed to set name on non existent field returned from record::field",
                         '__ut.arec.field( "009" ).name',"");

        Assert.equal("Tryed to set name on non existent field returned from record::field",
                         '__ut.arec.field( "009" ).indicator',"");

        Assert.exception("Tryed to set name on non existent field returned from record::field",
                         '__ut.arec.field( "009" ).name = "xx"');

        Assert.exception("Tryed to set name on non existent field returned from record::field",
                         '__ut.arec.field( "009" ).value == "xx"');

        Assert.exception("Tryed to set name on non existent field returned from record::field",
                         '__ut.arec.field( "009" ).indicator = "xx"');

        Assert.exception("Tryed to set name on non existent field returned from record::field",
                         '__ut.arec.field( "009" ).value = "xx"');

        Assert.equal("Testing for empty value of non existent subfield in non existant field",
                         '__ut.arec.field( "009" ).subfield("g").value ', "");

        Assert.equal( "arec does have 009 field", '__ut.arec.count( "009" ) == 0',
            true );

        __ut.arec.append( new Field("009","00" ) );


        Assert.equal("Testing for empty value of non existent subfield in field",
                         '__ut.arec.field( "009" ).subfield("g").value ', "");

        Assert.equal("Testing for empty value of non existent subfield in field",
                         '__ut.arec.field( "009" ).subfield("g").name ', "");

        Assert.exception("Tryed to set Subfield Value on non existent sub field",
                         '__ut.arec.field( "009" ).subfield("g").value = "xx"');

        Assert.exception("Tryed to set Subfield name on non existent sub field",
                         '__ut.arec.field( "009" ).subfield("g").name = "xx"');


        __ut.arec.append( new Field( "009", "00" ) );

        Assert.equal( "arec does have 009 field", '__ut.arec.count( "009" ) == 2',
            true );

        ////////////////////////////////////////////////////////////////////////////////
        // Test what happens, if we wish to work on fields, using var references
        __ut.brec = new Record( );
        var afield = new Field( "001", "00" );
        afield.append( "m", "hej med dig" );
        __ut.brec.append( afield );
        Assert.equal( "Using refs on fields",
            '__ut.brec.field( "001" ).subfield( "m" ).value;',
            'hej med dig' );
        delete afield;
        Assert.equal( "Using refs on fields",
            '__ut.brec.field( "001" ).subfield( "m" ).value;',
            'hej med dig' );
        __ut.bfield = __ut.brec.field( "001" );
        __ut.bfield.subfield( "m" ).value = "hej igen";
        Assert.equal( "Using refs on fields",
            '__ut.brec.field( "001" ).subfield( "m" ).value;',
            'hej igen' );
        // Add some more fields.
        __ut.cfield = new Field( "002", "00" );
        __ut.dfield = new Field( "003", "00" );
        __ut.brec.append( __ut.cfield );
        __ut.brec.append( __ut.dfield );
        // Now, construct array of fields.
        __ut.field_array = [ __ut.brec.field( "001" ), __ut.brec.field( "002" ), __ut.brec.field( "003" ) ];
        Assert.equal( 'Accessing fields using array refs',
            '__ut.field_array[1].name',
            '002' );
        // Now, _update_ using field_array, access from record
        __ut.field_array[ 0 ].name = "hej";
        __ut.field_array[ 1 ].name = "med";
        __ut.field_array[ 2 ].name = "dig";
        Assert.equal( 'Checking modified field names using array refs',
            '__ut.brec.count( "hej" ) == 1 && __ut.brec.field( "hej" ).subfield( "m" ).value == "hej igen" && __ut.brec.count( "med" ) == 1 && __ut.brec.count( "dig" ) == 1',
            true );


        // testSession.todo += "Multiple Record objects, Separate Recordfield and ISO2709SubField objects";

        delete __ut;

    } );

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////
// DOCUMENTATION
////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

/** Module to allow representation of Marc objects.
 *
 * In order to allow representations of Marc objects, the MarcClasses
 * module exposes three classes: Record, Field and Subfield. They
 * represent a Marc record, field and subfield respectively.
 *
 * Although MarcClasses is presented as a namespace, the Record, Field
 * and Subfield classes live in the global namespace. This is for
 * historical reasons - these classes were introduced into the DBC
 * JavaScript system before module support was established.
 *
 * @see Record
 * @see Field
 * @see Subfield
 * @name MarcClasses
 * @namespace */
MarcClasses = ( function( ) {
        var exports = {};
        exports.Record = Record;
        exports.Field = Field;
        exports.Subfield = Subfield;
        return exports;
    }( ) );


//////////////////////////////////////////////////////////////////////
// RECORD
//////////////////////////////////////////////////////////////////////

// Note, that a lot properties is assigned to the constructor, to have something
// to attach the documentation to.

/** Abstraction for a Marc record.
 * 
 * The Record class is an abstraction for a Marc record.
 *
 * It contains a number of properties and methods to represent and
 * manipulate a record, including the header.
 *
 * @example // You construct a new Record instance like this:
var r = new Record();
 *
 * @name Record
 * @constructor
 * @see MarcClasses
 * @see Field
 * @see Subfield */
// Do not remove this comment

// PROPERTIES

/** The forUserSystems property of the record.
 *
 * This property holds the forUserSystems string for the record.
 *
 * @type {string}
 * @name Record.forUserSystems
 * @name Record#forUserSystems */
Record.forUserSystems = new String( );

/** The implementation codes of the record.
 *
 * This property holds the implementation codes for the record, as a string.
 *
 * @type {string}
 * @name Record.implementationCodes
 * @name Record#implementationCodes */
Record.implementationCodes = new String( );

/** The status of the record.
 *
 * This property holds the status of the record as a string with a single character in.
 *
 * @type {string}
 * @name Record.recordStatus
 * @name Record#recordStatus */
Record.recordStatus = new String( );

// METHODS

/** Adds a field to the record.
 *
 * This method adds a field to the record.
 *
 * It can be called with either a Field instance, or two Strings. 
 * 
 * If called with a Field instance, the instance is copied to the record.
 *
 * If called with two Strings, a new Field instance will be created,
 * based on the value of the strings, and appended to the record. The
 * first string is then the name of the field, the second the field
 * indicator.
 *
 * **Note:** For the C++ implementation: Any reference to existing
 * fields on this record may be invalidated by append a field. This
 * has a number of potential bad consequences, most of which leads to
 * crashes or unpredictable behaviour. It is easy to fall into the
 * trap of e.g. iterating all fields in a record, and append some
 * while iterating. This is almost certain to make your application
 * crash. Instead, collect the new fields you wish to append, then
 * append them after your main iteration.
 *
 * @example 
// Append an existing field to a record (field will be copied)
aRecord.append( field );
// Create a new field, and append it to the record
aRecord.append( "010", "02" );
 * @example 
// This example illustrates the relation between existing references, and new Fields.
var aRecord = new Record();
aRecord.append( "001", "02" );
var aField = aRecord.field( "001" );
aField.name = "002" // Changes the name of the field in aRecord
var newRecord = new Record();
newRecord.append( aField ); // aField is copied into newRecord
aField.name = "001" // Changes the name of the field in aRecord, but not in newRecord.
 *
 * @param {Field|string} fieldOrFieldName A Field instance to add, or the name of a new field to construct and add
 * @param {string} [fieldIndicator] The indicator value of the new Field to add. *Must be provided, if fieldOrFieldName is a string instance with the name of a field.*
 * @method
 * @name Record.append 
 * @name Record#append */
Record.append = new String( );

/** Create a clone (copy) of this record.
 *
 * All the fields of this field are also cloned by this function.
 * 
 * @return {Record} An exact clone of this record. 
 *
 * @method
 * @name Record.clone
 * @name Record#clone */
Record.clone = new String( );

/** Count the number of fields in a record with a given name.
 *
 * Given the name of a field, this method returns the number of fields
 * with that name.
 *
 * @param {string} fieldName The name of a field.
 * @return {number} The number of fields with the given fieldName 
 *
 * @example 
// Get the number of 545 fields in a record
var num = aRecord.count( "545" );
 * @method
 * @name Record.count
 * @name Record#count */
Record.count = new String( );

/** Iterates over the fields and calls an handler on each field.
 *
 * This function has 2 forms:
 * 
 * If given a matcher and a handler, then the handler is called for each field.
 * 
 * If given a matcher, an array of sorter objects and a handler, then the matched 
 * fields are sorted before the handler is called for each field. 
 * 
 * @param {RegExp|String|Object} matcher An RegExp or string that matches the field name
 * 		  or an object with a function of the norm: function matchField( record, field ).
 *        The function should return a boolean value.
 * @param {Array} sorter An array of objects that has a function: sort( a, b ). a and 
 * 		  b are fields and the function should return -1 (a < b), 0 ( a == b) or 1 (a > b).
 * 		  If the function returns -1 or 1, then the two fields are sorted and any fouther
 * 		  elements in the array is ignored for these two fields.
 * @param {Handler|Function} handler The handler to call for each sub field found 
 * 		  by matcher.    
 * 
 * @return If handler returns a value then all the values are added together into a 
 * 		   single value.
 * 
 * @example
// Print all fields
record.eachField( /./, function( field ) { print( field.toString() + "\n" ); } );

// List field 795 sorted by sub field "å"
record.eachField( /795/, [ new SortBySubFields( /å/ ) ], function( field ) { print( field.toString() + "\n" ); } );

// How to use a custom matcher function with eachField
// The function matches all field "700" with a sub field "a".
record.eachField( { matchField: function( record, field ) {
									return field.name === "700" && field.exists( /a/ )
								} 
				  }, 
				  function( field ) { print( field.toString() + "\n" ); } );

//combine eachField with eachSubField to print all subfield values in all fields
 record.eachField ( /./, function ( field ) {
    field.eachSubField ( /./, function ( field, subfield ) {
        print ( subfield.value + "\n" );
    }); end eachSubField
  }); //end eachField

 *
 * @method
 * @name Record.eachField
 * @name Record#eachField */
Record.eachField = new String();

/** Iterates over the fields and lookup handlers in a map.
 *
 * This method is usefull when you want to handles multiple fields in a record with different functions
 *
 * It is more efficient than making multiple eachField calls, since it only loops through fields once and
 * uses map string lookup in stead of regular expressions.
 * But it can only match on exact field names, because of the map lookup
 *
 * @param {MatchMap} map An object containing mapping between fieldname strings and functions to call
 *
 * @example
var map = new MatchMap();
map.put( "700", function( field ) {
    // handle field 700
});
map.put( "795", function( field ) {
    // handle field 795
});
map.put( "100", "101", function( field ) {
    // handle field 100 and 101
});
record.eachFieldMap(map);
 *
 * @method
 * @name Record.eachFieldMap
 * @name Record#eachFieldMap */
Record.eachFieldMap = new String();


/** Iterates over the fields and lookup handlers in a map.
 *
 * This method is useful when you want to handles multiple fields in a record with different functions and nonspecified
 * fields with a default function. So all fields that are added to the map use a specific function and the remaining fields
 * are called with the default function
 *
 * It is more efficient than making multiple eachField calls, since it only loops through fields once and
 * uses map string lookup in stead of regular expressions.
 * But it can only match on exact field names, because of the map lookup
 *
 * @param {MatchMap} map An object containing mapping between field name strings and functions to call
 * @param {defaultFunction} defaultFunction to call for fields not i MatchMap
 * @param {extraData} Object given to all functions called as second parameter
 *
 *
 * @example
 var map = new MatchMap();

 map.put("001", function( f, resultData) {resultData.push("001 called "+f.name);} );
 map.put("002", function( f, resData) {resData.push("002 called "+f.name);} );

 var defaultFunction = function ( f, resultData ) { resultData.push( "default called " + f.name ); };

 var resultData=[];

 record.eachFieldMapWithDefault( map, defaultFunction, resultData );

 // if record is
 // 001 00 *a
 // 002 00 *a
 // 245 00 *a
 // 300 00 *a
 //result data is now [ "001 called 001", "002 called 002", "default called 245", "default called 300" ]

 *
 * @method
 * @name Record.eachFieldMapWithDefault
 * @name Record#eachFieldMapWithDefault */
Record.eachFieldMapWithDefault = new String();

/** Iterates over the fields and calls an handler on each field as a sequence.
 *
 * A sequence is an array of matchers that is used to extract fields. The handler is then 
 * used on the first field from each matcher, then on the second field from each matcher, 
 * and so on.
 *
 * If the matchers matches on different number of fields, then only the smallest number of matched 
 * fields is used, then we invoke the handler on the sub field.
 * 
 * This function has 2 forms:
 * 
 * If given a matcher and a handler, then the handler is called for each field.
 * 
 * If given a matcher, an array of sorter objects and a handler, then the matched 
 * fields are sorted before the handler is called for each field. 
 * 
 * @param {Array} matchers An Array of RegExp that matches the field name 
 * 		  or an object with a function of the norm: function matchField( record, field ).
 *        The function should return a boolean value.
 * @param {Array} sorter An array of objects that has a function: sort( a, b ). a and 
 * 		  b are fields and the function should return -1 (a < b), 0 ( a == b) or 1 (a > b).
 * 		  If the function returns -1 or 1, then the two fields are sorted and any fouther
 * 		  elements in the array is ignored for these two fields.
 * @param {Handler|Function} handler The handler to call for each sub field found 
 * 		  by matcher.    
 * 
 * @return If handler returns a value then all the values are added together into a 
 * 		   single value.
 * 
 * @example
// Print fields "770" and "795" as pairs.
record.eachFieldSequence( [ /770/, /795/ ], function( field ) { print( field.toString() + "\n" ); } );

// Sequence with a custom matcher.
record.eachFieldSequence( [ { matchField: function( record, field ) { return field.name === "652" && field.exists( /0/ ) }
						    }, /770/, /795/ ], function( field ) { print( field.toString() + "\n" ); } );

 * @method
 * @name Record.eachFieldSequence
 * @name Record#eachFieldSequence */
Record.eachFieldSequence = new String();

/** Checks if the record is empty, that it has zero fields.
 *
 * @return {boolean} True if the record is empty, false otherwise. 
 * 
 * @example
// empty() is many times used as a early check in the begining of a function
function f( record ) {
	if( record.empty() ) {
 		return;
 	}
 	
 	// Normal logic... 	
} 
 * @method
 * @name Record.empty
 * @name Record#empty */
Record.empty = new String( );

/** Checks if this record contains at least one field, that matches a criteria.
 *
 * @param {RegExp|Object} matcher An RegExp that matches the field name 
 * 		  or an object with a function of the norm: function matchField( record, field ).
 *        The function should return a boolean value.
 * 
 * @return {boolean} True if a field matches the matcher argument, False otherwise.
 * 
 * @example
function hasTitle( record ) {
	return record.existField( /245/ );
}  
//The function hasTitle checks if the passed record contains a field with name "245".


var exampleRecord_One = new Record;
var exampleField = new Field ( "245", "00" );
exampleRecord_One.append ( exampleField ); 

hasTitle ( exampleRecord_One ); // <-- this call returns True

var exampleRecord_Two = new Record;
var exampleField = new Field ( "260", "00" );
exampleRecord_Two.append ( exampleField ); 

hasTitle ( exampleRecord_Two ); // <-- this call returns False 
 
 *
 * 
 *   
 * @method
 * @name Record.existField
 * @name Record#existField */
Record.existField = new String();

/** Get a reference to a field in the record.
 *
 * Given the index of a field, or a fieldName, or a fieldName and an
 * index, return a reference to a field.
 *
 * This is the primary method to get access to the fields of a
 * record. The method exists in three versions, depending on the
 * number and type of arguments given.
 *
 * If given a numerical index only, it returns the field at that position
 * in the records internal representation of fields. This is probably
 * only usefull in conjunction with e.g. looping through all fields in
 * the record.
 *
 * If given a name, it returns the first field found, that matches the
 * given name.
 *
 * If given a name and an index, it returns the index'th field with
 * the given name.
 *
 * Note, that if the field is not found, an empty field is
 * returned. This field is not part of the record.
 *
 * @param {number|string} fieldIndexOrFieldName Index of field to find, zero-based, or name of a field to find.
 * @param {number} [fieldNameIndex] Index of named field to find, zero-based. *Only allowed if first argument is a fieldName.*
 * @return {Field} A reference to a field, or an empty field, if not found.
 * @example 
//example record
var record = new Record;
field = new Field ( "021", "00" );
field.append ( "e", "9780618260300" );
record.append( field );
field = new Field ( "021", "00" );
field.append ( "a", "0618162216" );
record.append( field );
field = new Field ( "100", "00" );
field.append ( "a", "Tolkien" );
field.append ( "h", "J.R.R." );
record.append( field );
field = new Field ( "245", "00" );
field.append ( "a", "The hobbit, or, There and back again" );
record.append( field );


// Get the third field in the record
var f1 = record.field( 2 ); //<-- this refers to field 100

// Be aware if you use only the name of the field and there is more than one of that field
// you will get a reference to the first field
var f2 = record.field( "021" );  // <-- this refers to field 021 containing subfield "e" value = 9780618260300

// Get the second 021 field
var f3 = record.field( "021", 1 ); // <-- this refers to field 021 containing subfield "a" value = 0618162216

//Be aware if you refer to a field with name and index that is not there you get an empty field
var f4 = record.field( "021", 2 ); // <-- this refers to an empty field, because there are only two field 021 in this record

 * @method
 * @name Record.field 
 * @name Record#field */
Record.field = new String( );

/** Iterates over the fields and calls an handler on each field. Returns the value of 
 * the first value the handler returns, that is not undefined.
 *
 * This function has 2 forms:
 * 
 * If given a matcher and a handler, then the handler is called for each field.
 * 
 * If given a matcher, an array of sorter objects and a handler, then the matched 
 * fields are sorted before the handler is called for each field. 
 * 
 * @param {RegExp|String|Object} matcher An RegExp or string that matches the field name
 * 		  or an object with a function of the norm: function matchField( record, field ).
 *        The function should return a boolean value.
 * @param {Array} sorter An array of objects that has a function: sort( a, b ). a and 
 * 		  b are fields and the function should return -1 (a < b), 0 ( a == b) or 1 (a > b).
 * 		  If the function returns -1 or 1, then the two fields are sorted and any fouther
 * 		  elements in the array is ignored for these two fields.
 * @param {Handler|Function} handler The handler to call for each sub field found 
 * 		  by matcher.    
 * 
 * @return Returns the first value the handler returns.
 * 
 * @example
// Get the first person name from 795
var personName = record.firstField( /795/, function( field ) { field.getValue( /a/ ) } );

// And with a custom matcher
var personName = record.firstField( { matchField: function( record, field ) { return field.name === "795" } }, function( field ) { field.getValue( /a/ ) } );

// Gets the first name of "795" after the fields have been sorted by sub field "å"
var personName = record.firstField( /795/, [ new SortBySubFields( /å/, Sorter.ASC ) ], function( field ) { field.getValue( /a/ ) } );
 *
 * @method
 * @name Record.firstField
 * @name Record#firstField */
Record.firstField = new String();

/** Returns the first field that matches a given matcher.
 * 
 * @param {RegExp} regExFieldMatcher Regular expression that matches the field name 
 * 				   of the field you want to return.
 * 
 * @return {Field} The first field whos name matches the matcher in regExFieldMatcher.
 * 
 * @example
// This function returns the first occurrence of field 245.
function getTitle() {
	return record.getFirstFieldAsField( /245/ );
}
 * @method
 * @name Record.getFirstFieldAsField
 * @name Record#getFirstFieldAsField */
Record.getFirstFieldAsField = new String();

/** Returns the first sub field value in the record.
 * 
 * @param {RegExp|String|Object} fieldMatcher An RegExp or string that matches the field name
 * 		  or an object with a function of the norm: function matchField( record, field ).
 *        The function should return a boolean value.
 * @param {RegExp|String|Object} subFieldMatcher An RegExp or String that matches the sub field name
 * 		  or an object with a function of the norm: function matchSubField( field, subfield ). 
 *        The function should return a boolean value.
 * 
 * @return {String} The first value found in the record or the empty string.
 * 
 * @example
function firstSubject( record ) {
	return record.getFirstValue( /666/, /f/ );
}
 * 
 * @method
 * @name Record.getFirstValue
 * @name Record#getFirstValue */
Record.getFirstValue = new String();

/** Iterates over the fields and returns the values of any sub field that matches a criteria.
 *
 * @param {RegExp|String|Object} fieldMatcher An RegExp or stringthat matches the field name
 * 		  or an object with a function of the norm: function matchField( record, field ).
 *        The function should return a boolean value.
 * @param {RegExp|String|Object} subFieldMatcher An RegExp or string that matches the sub field name
 * 		  or an object with a function of the norm: function matchSubField( field, subfield ). 
 *        The function should return a boolean value.
 * @param {String} sep A separator to insert between the values, if more than one is found.
 * 
 * @return {String} The found values as a string. If no values are found, the function will 
 * 					return the empty string. 
 * 
 * @example
// Returns all values from field "245a" without any separator.
function getTitles( record ) {
	return record.getValue( /245/, /a/ );
}  

// Returns all values from field "245a" with a comma separator.
function getTitles( record ) {
	return record.getValue( /245/, /a/, ", " );
}  
 *
 * @method
 * @name Record.getValue
 * @name Record#getValue */
Record.getValue = new String();

/** Checks if a matcher only match one field.
 * 
 * @param {RegExp|Object} matcher An RegExp that matches the field name 
 * 		  or an object with a function of the norm: function matchField( record, field ).
 *        The function should return a boolean value.
 * 
 * @return {boolean} True if only one field matches matcher, False otherwise.
 * @method
 * @name Record.isUnique
 * @name Record#isUnique */
Record.isUnique = new String();

/** Iterates over the fields and checks if any of the sub fields has a given value.
 *
 * @param {RegExp|String|Object} fieldMatcher An RegExp or string that matches the field name
 * 		  or an object with a function of the norm: function matchField( record, field ).
 *        The function should return a boolean value.
 * @param {RegExp|String|Object} subFieldMatcher An RegExp or string that matches the sub field name
 * 		  or an object with a function of the norm: function matchSubField( field, subfield ). 
 *        The function should return a boolean value.
 * @param {RegExp} valueRegExp The value that you want to match.
 * 
 * @return {boolean} True if a value is matched, False otherwise. 
 * 
 * @example
// Checks if a record is a book
function isBook( record ) {
	return record.matchValue( /009/, /a/, /xx/ );
}  
 *
 * @method
 * @name Record.matchValue
 * @name Record#matchValue */
Record.matchValue = new String();

/** The number of fields in the record.
 *
 * This method returns the number of fields in the record. It is most
 * often used to iterate the fields of a record.
 * 
 * @return {number} The number of fields in the record.
 * @example // Iterate all fields of a record and print them
for( var i = 0; i < record.numberOfFields(); ++i ) {
  print( record.field( i ).toString() );
}
 * @method
 * @name Record.numberOfFields
 * @name Record#numberOfFields */
Record.numberOfFields = new String( );

/** Remove a field from a record.
 *
 * Given the index of a field, or a fieldName, or a fieldName and an
 * index, removes the field from the record.
 *
 * The method exists in three versions, depending on the
 * number and type of arguments given.
 *
 * If given a numerical index, it removes the field at that position
 * in the records internal representation of fields. 
 *
 * If given a name, it removes the first field found, that matches the
 * given name.
 *
 * If given a name and an index, it removes the index'th field with
 * the given name.
 *
 * If no field matches the parameters, nothing happens.
 *
 * **Note:** For the C++ implementation: Any reference to existing
 * fields on this record may be invalidated by removing a field. This
 * has a number of potential bad consequences, most of which leads to
 * crashes or unpredictable behaviour. It is easy to fall into the
 * trap of e.g. iterating all fields in a record, and remove some of
 * them. This is almost certain to make your application
 * crash. Instead, collect the names or indexes of the fields you wish
 * to remove, then remove them after your main iteration.
 *
 * @param {number|string} fieldIndexOrFieldName Index of field to remove, zero-based, or name of a field to remove
 * @param {number} [fieldNameIndex] Index of named field to remove, zero-based. *Only allowed if first argument is a fieldName.*
 * @example 
// Remove the third field in the record
aRecord.remove( 2 ); 
// Remove the first 545 field
aRecord.remove( "545" );
// Remove the second 545 field
aRecord.remove( "545", 1 ); 
 * @method
 * @memberOf Record
 * @name Record.remove
 * @name Record#remove */
Record.remove = new String( );

/** Remove all matching fields from a record.
 *
 * Given a field name, removes all matching fields from the record.
 *
 * @param {string} fieldName Name of field to remove
 * @example 
// Remove all 545 fields
aRecord.removeAll( "545" );
 * @method
 * @memberOf Record
 * @name Record.removeAll
 * @name Record#removeAll */
Record.removeAll = new String( );

/** Removes any field in the record, that matches a given criteria.
 *
 * @param {RegExp|Object} matcher An RegExp that matches the field name 
 * 		  or an object with a function of the norm: function matchField( record, field ).
 *        The function should return a boolean value.
 * 
 * @return Nothing. 
 * 
 * @example
function removeSubjects( record ) {
	return record.removeWithMatcher( /666/ );
}  
 *
 * @method
 * @name Record.removeWithMatcher
 * @name Record#removeWithMatcher */
Record.removeWithMatcher = new String();

/** Iterates over the fields and returns an array of the selected fields
 * 
 * @param {RegExp|String|Object} matcher An RegExp that matches the field name
 * 		  or an object with a function of the norm: function matchField( record, field ).
 *        The function should return a boolean value.
 * @param {Array} sorter An array of objects that has a function: sort( a, b ). a and 
 * 		  b are fields and the function should return -1 (a < b), 0 ( a == b) or 1 (a > b).
 * 		  If the function returns -1 or 1, then the two fields are sorted and any fouther
 * 		  elements in the array is ignored for these two fields.
 * 
 * @return {Array} An array of the selected fields. Each element is of type Field.
 * 
 * @method
 * @name Record.selectFields
 * @name Record#selectFields */

Record.selectFields = new String();

/** Iterates over the fields and returns an array of the selected fields as a sequence.
 *
 * A sequence is an array of matchers that is used to extract fields. The returned array contains 
 * first the first field from each matcher, then on the second field from each matcher, 
 * and so on.
 *
 * If the matchers matches on different number of fields, then only the smallest number of matched 
 * fields is used.
 * 
 * @param {RegExp|Object} matcher An RegExp that matches the field name 
 * 		  or an object with a function of the norm: function matchField( record, field ).
 *        The function should return a boolean value.
 * @param {Array} sorter An array of objects that has a function: sort( a, b ). a and 
 * 		  b are fields and the function should return -1 (a < b), 0 ( a == b) or 1 (a > b).
 * 		  If the function returns -1 or 1, then the two fields are sorted and any fouther
 * 		  elements in the array is ignored for these two fields.
 * 
 * @return {Array} An array of the selected fields. Each element is of type Field.
 * 
 * @method
 * @name Record.selectFieldsSequence
 * @name Record#selectFieldsSequence */
Record.selectFieldsSequence = new String();

/** Returns the number of fields in this record.
 * 
 *  @return {number} The number of fields.
 *  @example
// This function is mostly used in functions with for-structures like
function printFields( record ) {
	for( var i = 0; i < record.size(); i++ ) {
		var field = record.field( i );
		
		print( field.toString() + "\n" );
	}
}
 *
 * @method
 * @name Record.size
 * @name Record#size */
Record.size = new String();

/** Get a string representation of the record.
 *
 * This method returns a string representation of the record, in a
 * format very close to a standard Marc line format. The string is
 * constructed by calling the toString method of all the fields the
 * record contains.
 *
 * Any values of @ or * in fields/subfields, will be written as 
 * @@ and @* respectively.
 *
 * @return {string} A string representation of the record
 * @method
 * @memberOf Record
 * @name Record.toString
 * @name Record#toString */
Record.toString = new String( );

/** Create a record from a string.
 *
 * This method changes the record to represent the input in the string.
 * 
 * Existing content of the record is cleared. The input is parsed to
 * build a new record in place. The parser honors two "standard" Marc
 * record escape codes: @@ and @\*, which are translated to @ and \*
 * respectively. Other @ codes are not interprented, but are carried
 * through unchanged.
 *
 * **NOTE:** controlFields are *not supported* by this method.
 *
 * @param {String} string A Marc record in line format.
 * @method
 * @memberOf Record
 * @name Record.fromString
 * @name Record#fromString */
Record.fromString = new String( );


//////////////////////////////////////////////////////////////////////
// FIELD
//////////////////////////////////////////////////////////////////////

/** A field in a Marc record.
 *
 * The Field class is an abstraction for a Field in a Marc record. It
 * contains a number of properties and methods to represent and
 * manipulate a field.
 *
 * You can construct a field instance in one of two ways, either as an
 * empty field, or by giving it a name and an indicator.
 *
 * A field can be either a *normal field*, with name, indicator and
 * subfields, or it can be a *control field*, with name and value. All
 * fields are constructed as normal fields. If you want to create a
 * control field, you must use the default constructor (no argument),
 * then set the name and value properties afterwards. Setting the
 * value property on a field with no indicator, and no subfields,
 * turns it into a control field. It is not possibly to turn a control
 * field back into a normal field.
 *
 * @example 
// Syntax
var f1 = new Field();
var f2 = new Field( fieldName, indicator );
 * @example 
// This constructs a normal field, and adds a subfield
var normalField = new Field( "001", "02" );
normalField.append( new Subfield( "a", "value of a subfield" ) );
* @example 
// This constructs a normal field, with no name or indicator, 
// which is then transformed to a control field by settings its value
var controlField = new Field();
controlField.name = "002";
controlField.value = "This is the value of the control field"
 * @param {string} [fieldName] Name of the new field.
 * @param {string} [indicator] Value of the indicator. *Must be present if fieldName is given.*
 * @constructor
 * @see MarcClasses
 * @see Record
 * @see Subfield 
 * @name Field */
// Do not remove this comment

// PROPERTIES

/** The name of the field.
 *
 * This property holds the name of the field.
 *
 * @type {string}
 * @name Field.name
 * @name Field#name */
Field.name = new String( );

/** The indicator for the field.
 *
 * This property holds the indicator for the field, as a string, if
 * the field is a *normal* field.
 *
 * This property is only available, if the field is a normal field,
 * not a control field. Assigning to, or reading from, this property,
 * if the field is a control field, will thrown an exception.
 *
 * @type {string}
 * @name Field.indicator
 * @name Field#indicator */
Field.indicator = new String( );

/** The value of the field.
 *
 * This property holds the value of the field, if the field is a *control* field. 
 * 
 * Assigning to this property, will convert a normal field to a
 * control field, *if* the field does not contain an indicator or any
 * subfields. If it does, an exception will be thrown. Reading from
 * this property will also throw, if the field is a normal field.
 *
 * @type {string}
 * @name Field.value
 * @name Field#value */
Field.value = new String( );

// METHODS

/** Append a subfield to a field.
 *
 * This method appends a subfield to a field. It can be called in
 * three different ways. 
 *
 * If called with a Subfield instance, the subfield is added (copied)
 * to the field. 
 * 
 * If called with a name and value, a new subfield is created and
 * added to the field.
 * 
 * If called with a name, a value, and a bool set to true, a subfield
 * will be created, and it will replace the first subfield with the
 * same name, if present, otherwise it will just be appended.
 *
 * The append operation can only be called on normal fields, not
 * control fields.
 *
 * **Note:** For the C++ implementation: Any reference to existing
 * subfields on this record may be invalidated by append a
 * subfield. This has a number of potential bad consequences, most of
 * which leads to crashes or unpredictable behaviour. It is easy to
 * fall into the trap of e.g. iterating all subfields in a field, and
 * append some while iterating. This is almost certain to make your
 * application crash. Instead, collect the new subfields you wish to
 * append, then append them after your main iteration.
 *
 * @example
// Append an existing feld
aField.append( subfield );
// Create a new subfield, and append it
aField.append( "a", "I am an a subfield" );
// Create a new subfield, and append it to the field, replacing the first similar subfield
aField.append( "a", "I am an a subfield", true );
 * @example // This example illustrates the relation between existing references, and new Subfields.
var aField = new Field();
aField.append( "a", "I am an subfield" );
var aSubfield = aField.subfield( "a" );
aSubfield.name = "b" // Changes the name of the subfield in aField
var newField = new Field();
newField.append( aSubfield ); // aSubfield is copied into newField
aSubfield.name = "a" // Changes the name of the field in aField, but not in newField.
 *
 * @param {Subfield|string} subfieldOrSubfieldName The Subfield to add, or the name of a new subfield to create and add.
 * @param {string} [subfieldValue] The value of the new Subfield to add. *Must be present if the first argument is a subfieldName.*
 * @param {Bool} [replace] If true, replace existing subfields of the same name.
 * @method
 * @name Field.append
 * @name Field#append */
Field.append = new String( );

/** Create a clone (copy) of this field.
 *
 * All the sub fields of this field are also cloned by this function.
 * 
 * @return {Field} An exact clone of this field. 
 *
 * @method
 * @name Field.clone
 * @name Field#clone */
Field.clone = new String( );

/** Count the number of subfields in a field with a given name.
 *
 * Given the name of a subfield, this method returns the number of
 * subfields with that name.
 *
 * @param {string} subfieldName The name of a field.
 * @return {number} The number of subfields with the given subfieldName 
 *
 * @example
// Count the number of "a" subfields
var num = field.count( "a" );
 * @method
 * @name Field.count
 * @name Field#count */
Field.count = new String( );

/** Iterates over the sub fields and calls an handler on each sub field.
 *
 * @param {RegExp|String|Object} matcher An RegExp or string that matches the sub field name
 * 		  or an object with a function of the norm: function matchSubField( field, subfield ). 
 *        The function should return a boolean value.
 * @param {Handler|Function} handler The handler to call for each sub field found 
 * 		  by matcher.    
 * 
 * @return If handler returns a value then all the values are added together into a 
 * 		   single value.
 * 
 * @example
function getValue( field ) {
	return field.eachSubField( /a/, function( field, subField ) { return subField.value; } );
}  
 *
 * The function getValue is passed a field and iterates over a sub fields with name "a" and collects the values of all sub fields with name "a"
 *  
 * @method
 * @name Field.eachSubField
 * @name Field#eachSubField */
Field.eachSubField = new String( );

/** Iterates over the sub fields and calls an handler on each sub field as a sequence.
 *
 * A sequence is an array of matchers that is used to extract sub fields. The handler
 * is then used on the first sub field from each matcher, then on the second sub field 
 * from each matcher, and so on.
 * 
 * If the matchers matches on different number of sub fields, then only the smallest 
 * number of matched sub fields in used, then we invoke the handler on the sub field.
 * 
 * @param {Array} matchers An Array of matcher objects (RegExp's or Objects). 
 * 		  Use RegExp to match the sub field name or use an object with a function 
 * 		  of the norm: function( field, subfield ). The function should return a 
 * 		  boolean value.
 * @param {Handler|Function} handler The handler to call for each sub field found 
 * 		  by matcher.    
 * 
 * @return If handler returns a value then all the values are added together into a 
 * 		   single value.
 * 
 * @example
field.eachSubField( [ /a/, /i/ ], function( field, subField ) { print( subField.value + " " ) } );
 *
 * The function prints the sub fields "a" and "i" in this order: First a, first i, second a, 
 * second i, and so on.
 *  
 * @method
 * @name Field.eachSubFieldSequence
 * @name Field#eachSubFieldSequence */
Field.eachSubFieldSequence = new String( );

Field.eachSubFieldMap = new String();

/** Checks if the field is empty, that it has zero sub fields.
 *
 * @return {boolean} True if the field is empty, false otherwise. 
 * 
 * @example
// empty() is any times used as a early check in the begining of a function
function f( field ) {
	if( field.empty() ) {
 		return;
 	}
 	
 	// Normal logic... 	
} 
 * @method
 * @name Field.empty
 * @name Field#empty */
Field.empty = new String( );

/** Checks if the field contains at least one sub field, that matches a criteria.
 *
 * @param {RegExp|Object} matcher An RegExp that matches the sub field name 
 * 		  or an object with a function of the norm: function matchSubField( field, subfield ). 
 *        The function should return a boolean value.
 * 
 * @return {boolean} True if a sub field matches the matcher argument, False otherwise.
 * 
 * @example
function hasTitle( field ) {
	return field.exists( /a/ );
}  
 *
 * The function hasTitle checks if the passed field contains a sub field with name "a".
 *   
 * @method
 * @name Field.exists
 * @name Field#exists */
Field.exists = new String( );

/** Iterates over the sub fields and returns the value of the first sub field that matches a criteria.
 *
 * @param {RegExp|String|Object} matcher An RegExp or string that matches the sub field name
 * 		  or an object with a function of the norm: function matchSubField( field, subfield ). 
 *        The function should return a boolean value.
 * @param {Handler|Function} handler The handler to call for each sub field found 
 * 		  by matcher.    
 * 
 * @return If handler returns a value then that value is returned by Field.firstSubField
 * 
 * @example
function getTitle( field ) {
	return field.firstSubField( /a/, function( field, subField ) { return subField.value; } );
}  
 *
 * The function getTitle is passed a field and iterates over a sub fields with name "a" and
 * returns the value of the first occurence of sub field "a".
 *   
 * @method
 * @name Field.firstSubField
 * @name Field#firstSubField */
Field.firstSubField = new String( );

/** Returns the value of the first sub field that matches a matcher.
 * 
 * @param {RegExp|String|Object} matcher An RegExp or string that matches the sub field name
 * 		  or an object with a function of the norm: function matchSubField( field, subfield ). 
 *        The function should return a boolean value.
 * 
 * @return {String} If handler returns a value then that value is returned by Field.firstSubField
 * 
 * @example
function getFirstTitle( field ) {
	return field.getFirstValue( /a/ );
}  
 *
 * The function getFirstTitle is passed a matcher and returns the value of the first sub field with name "a".
 *   
 * @method
 * @name Field.getFirstValue
 * @name Field#getFirstValue */
Field.getFirstValue = new String( );

/** Iterates over the sub fields and returns the values of any sub field that matches a criteria.
 *
 * @param {RegExp|Object} matcher An RegExp that matches the sub field name 
 * 		  or an object with a function of the norm: function matchSubField( field, subfield ). 
 *        The function should return a boolean value.
 * 
 * @return {Array} An array with all the found values.
 * 
 * @example
function getTitles( field ) {
	return field.getValueAsArray( /a/ );
}  
 *
 * The function getTitles is passed a field and iterates over a sub fields with name "a" and
 * returns the values of the all sub field with name "a"
 *   
 * @method
 * @name Field.getValueAsArray
 * @name Field#getValueAsArray */
Field.getValueAsArray = new String();

/** Iterates over the sub fields and returns the values of any sub field that matches a criteria.
 *
 * @param {RegExp|String|Object} matcher An RegExp or string that matches the sub field name
 * 		  or an object with a function of the norm: function matchSubField( field, subfield ). 
 *        The function should return a boolean value.
 * @param {String} sep A separator to insert between the values, if more than one is found.
 * 
 * @return {String} The found values as a string. If no values are found, the function will 
 * 					return the empty string. 
 * 
 * @example
// Returns all values from sub field "a" without any separator.
function getTitles( field ) {
	return field.getValue( /a/ );
}  

// Returns all values from sub field "a" with a comma separator.
function getTitles( field ) {
	return field.getValue( /a/, ", " );
}  
 *
 * @method
 * @name Field.getValue
 * @name Field#getValue */
Field.getValue = new String();

/** Is the field a control field or not?.
 * 
 * Returns true if the field is a control field, otherwise false. 
 *
 
 * Call this function to figure out if a field is a control field.
 *
 * @return {boolean} True if the field is a control field, false otherwise.
 * @method
 * @name Field.isControlField
 * @name Field#isControlField */
Field.isControlField = new String( );

/** Checks if a matcher only match one sub field.
 * 
 * @param {RegExp|Object} matcher An RegExp that matches the sub field name 
 * 		  or an object with a function of the norm: function matchSubField( field, subfield ). 
 *        The function should return a boolean value.
 * 
 * @return {boolean} True if only one sub field matches matcher, False otherwise.
 * @method
 * @name Field.isUnique
 * @name Field#isUnique */
Field.isUnique = new String( );

/** Checks if the value of one/more sub fields matches a given value.
 * 
 * @param {RegExp|Object} matcher An RegExp that matches the sub field name 
 * 		  or an object with a function of the norm: function matchSubField( field, subfield ). 
 *        The function should return a boolean value.
 * @param {RegExp} valueRegExp The pattern that the value of the sub fields should match.
 * 
 * @return {boolean} True if the vaue is matched, False otherwise.
 * @method
 * @name Field.matchValue
 * @name Field#matchValue */
Field.matchValue = new String();

/** Remove a subfield from a field.
 *
 * The method exists in three versions, depending on the
 * number and type of arguments given.
 *
 * If given a numerical index, it removes the subfield at that position
 * in the fields internal representation of subfields.
 *
 * If given a name, it removes the first subfield found, that matches the
 * given name.
 *
 * If given a name and an index, it removes the index'th subfield with
 * the given name.
 *
 * If no subfield matches the parameters, nothing happens.
 *
 * **Note:** For the C++ implementation: Any reference to existing
 * subfields on this record may be invalidated by removing a subfield. This
 * has a number of potential bad consequences, most of which leads to
 * crashes or unpredictable behaviour. It is easy to fall into the
 * trap of e.g. iterating all subfields in a field, and remove some of
 * them. This is almost certain to make your application
 * crash. Instead, collect the names or indexes of the subfields you wish
 * to remove, then remove them after your main iteration.
 *
 * @param {string} subfieldName Name of a subfield to remove
 * @param {number} [subfieldNameIndex] Index of named subfield to remove, zero-based.
 * @example 
// Remove the first a subfield
aField.remove( "a" );
// Remove the second a subfield
aField.remove( "a", 1 ); 
 * @method
 * @name Field.remove
 * @name Field#remove */
Field.remove = new String( );

/** Removes all sub fields that matches a matcher.
 * 
 * @param {RegExp|Object} matcher An RegExp that matches the sub field name 
 * 		  or an object with a function of the norm: function matchSubField( field, subfield ). 
 *        The function should return a boolean value.
 * 
 * @return Nothing.
 * 
 * @method
 * @name Field.removeWithMatcher
 * @name Field#removeWithMatcher */
Field.removeWithMatcher = new String();

/** Returns the number of sub fields in this field.
 *
 * This function is a shortcut for Field.count().
 * 
 * @return {number} The number of sub fields in this field. Greater or equal to 0. 
 * 
 * @method
 * @name Field.size
 * @name Field#size */
Field.size = new String( );

/** Get a reference to a subfield in the field.
 *
 * Given the index of a subfield, or a subfieldName, or a subfieldName and an
 * index, return a reference to a subfield.
 *
 * This is the primary method to get access to the subfields of a
 * field. The method exists in three versions, depending on the
 * number and type of arguments given.
 *
 * If given a numerical index only, it returns the subfield at that position
 * in the fields internal representation of subfields. This is probably
 * only usefull in conjunction with e.g. looping through all subfields in
 * the field.
 *
 * If given a name, it returns the first subfield found, that matches the
 * given name.
 *
 * If given a name and an index, it returns the index'th subfield with
 * the given name.
 *
 * Note, that if the subfield is not found, an empty subfield is
 * returned. This subfield is not part of the record.
 *
 * @param {number|string} subfieldIndexOrSubfieldName Index of subfield to find, zero-based, or name of a subfield to find.
 * @param {number} [subfieldNameIndex] Index of named subfield to find, zero-based. *Only allowed if first argument is a subfieldName.*
 * @return {Subfield} A reference to a subfield, or an empty subfield, if not found.
 * @example 
//example record and field
var exampleRecord = new Record;
var exampleField = new Field ( "260", "00" );
exampleField.append ( "a", "Kassel" );
exampleField.append ( "b", "Barenreiter" );
exampleField.append ( "a", "Stuttgart" );
exampleField.append ( "b", "Metzler" );
exampleField.append ( "c", "1994-" );
exampleRecord.append ( exampleField ); 

// Get the third subfield in the example field
var s1 = exampleField.subfield( 2 ); //<-- this refers to subfield "a" with the value "Stuttgart"

// Be aware if you use only the name of the subfield and there is more than one of that subfield
// you will get a reference to the first subfield
var s2 = exampleField.subfield( "a" ); //<-- this refers to subfield "a" with the value "Kassel"

// Get the second "b" subfield
var s3 = exampleField.subfield( "b", 1 );  //<-- this refers to subfield "b" with the value "Metzler"

// Be aware if you refer to a subfield with name and index that is not there you get an empty subfield
var s4 = exampleField.subfield( "b", 2 ); // <-- this refers to an empty subfield, because there are only two subfield "b" in this field

// Combining method field from Record and method subfield from Field
var s5 = exampleRecord.field( "260" ).subfield( "c" ); // <-- this refers to the first subfield "c" in the first field "260" in the example record

 * @method
 * @name Field.subfield 
 * @name Field#subfield */
Field.subfield = new String( );

/** Get a string representation of the Field.
 *
 * This method returns a string representation of the field, in a
 * format very close to a standard Marc line format.
 *
 * If the field is a control field, it will return a string
 * constructed from its name and its value. If the field is a normal
 * field, it will build a string from its name, the indicator, and the
 * combined results from calling toString on all its subfields.
 *
 * Note that the characters "@" and "\*" are converted to "@@" and
 * "@\*" respectively, in the result from toString.
 *
 * @return {string} A string representation of the field.
 * @method
 * @name Field.toString
 * @name Field#toString */
Field.toString = new String( );


//////////////////////////////////////////////////////////////////////
// SUBFIELD
//////////////////////////////////////////////////////////////////////
/** A subfield in a Marc record.
 *
 * The Subield class is an abstraction for a Subfield in a Marc
 * record. It contains a name, value and a toString method, and as
 * such is a pretty simple abstraction.
 *
 * You can construct a subfield instance only by giving it a name and
 * a value as parameters to the constructor.
 *
 * @example 
// Construct a subfield
var subfield = new Subield( "a", "I am a subfield" );
 * @param {string} subfieldName] Name of the new subfield.
 * @param {string} subfieldValue Value of the new subfield
 * @constructor
 * @see MarcClasses
 * @see Record
 * @see Field
 * @name Subfield */
// Do not remove this comment

// PROPERTIES

/** The name of the subfield.
 *
 * This property holds the name of the subfield.
 *
 * @type {string}
 * @name Subfield.name
 * @name Subfield#name */
Subfield.name = new String( );

/** The value of the subfield.
 *
 * This property holds the value of the subfield.
 *
 * @type {string}
 * @name Subfield.value
 * @name Subfield#value */
Subfield.value = new String( );

// METHODS

/** Get a string representation of the subfield.
 *
 * This method returns a string representation of the subfield, in a
 * format very close to a standard Marc line format. This means, that
 * the characters "@" and "\*" are converted to "@@" and "@\*" respectively.
 *
 * @return {string} A string representation of the subfield.
 * @method
 * @name Subfield.toString
 * @name Subfield#toString */
Subfield.toString = new String( );

/**
 * Returns a clone (copy) of this Subfield
 * 
 * @return {Subfield} The new Subfield that is an entire clone of this subfield.
 *  
 * @method
 * @name Subfield.clone
 * @name Subfield#clone */
Subfield.clone = new String( );
