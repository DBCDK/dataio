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

package dk.dbc.dataio.gui.client.places;

import com.google.gwt.activity.shared.Activity;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;


/**
 * PresenterCreateImpl unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class AbstractBasePlaceTest {

    class ConcreteAbstractBasePlace extends AbstractBasePlace {
        ConcreteAbstractBasePlace(String... tokens) {
            super(tokens);
        }
        ConcreteAbstractBasePlace(String url) {
            super(url);
        }
        @Override
        public Activity createPresenter(ClientFactory clientFactory) {
            return null;
        }
    }


    // Test constructor with URL parameter

    @Test
    public void constructor_noArgs_emptyUrlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace();

        // Test validation
        assertThat(place.getToken(), is(""));
        assertThat(place.getKeys().isEmpty(), is(true));
    }

    @Test
    public void constructorWithUrl_nullUrl_emptyUrlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace((String)null);

        // Test validation
        assertThat(place.getToken(), is(""));
        assertThat(place.getKeys().isEmpty(), is(true));
    }

    @Test
    public void constructorWithUrl_emptyUrl_emptyUrlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("");

        // Test validation
        assertThat(place.getToken(), is(""));
        assertThat(place.getKeys().isEmpty(), is(true));
    }

    @Test
    public void constructorWithUrl_validUrlOnlyKey_urlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key");

        // Test validation
        assertThat(place.getToken(), is("key"));
        assertThat(place.getKeys().size(), is(1));
        assertThat(place.getParameter("key"), is(""));
    }

    @Test
    public void constructorWithUrl_validUrlOnePair_urlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key=value");

        // Test validation
        assertThat(place.getToken(), is("key=value"));
        assertThat(place.getKeys().size(), is(1));
        assertThat(place.getParameter("key"), is("value"));
    }

    @Test
    public void constructorWithUrl_validUrlOnePairDoubleEqualSign_urlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key=val=ue");

        // Test validation
        assertThat(place.getToken(), is("key=val=ue"));
        assertThat(place.getKeys().size(), is(1));
        assertThat(place.getParameter("key"), is("val=ue"));
    }

    @Test
    public void constructorWithUrl_validUrlOneAndAHalfPairs_urlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key=value&key2=");

        // Test validation
        assertThat(place.getToken(), is("key=value&key2"));
        assertThat(place.getKeys().size(), is(2));
        assertThat(place.getParameter("key"), is("value"));
        assertThat(place.getParameter("key2"), is(""));
    }

    @Test
    public void constructorWithUrl_validUrlOneAndAHalfPairsWithoutEqualSign_urlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key=value&key2");

        // Test validation
        assertThat(place.getToken(), is("key=value&key2"));
        assertThat(place.getKeys().size(), is(2));
        assertThat(place.getParameter("key"), is("value"));
        assertThat(place.getParameter("key2"), is(""));
    }

    @Test
    public void constructorWithUrl_validUrlTwoPairs_urlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key=value&key2=value2");

        // Test validation
        assertThat(place.getToken(), is("key=value&key2=value2"));
        assertThat(place.getKeys().size(), is(2));
        assertThat(place.getParameter("key"), is("value"));
        assertThat(place.getParameter("key2"), is("value2"));
    }


    // Test constructor with token parameters

    @Test
    public void constructorWithTokens_nullKeyNullValue_emptyUrlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace(null, null);

        // Test validation
        assertThat(place.getToken(), is(""));
        assertThat(place.getKeys().isEmpty(), is(true));
    }

    @Test
    public void constructorWithTokens_emptyKeyNullValue_emptyUrlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("", null);

        // Test validation
        assertThat(place.getToken(), is(""));
        assertThat(place.getKeys().isEmpty(), is(true));
    }

    @Test
    public void constructorWithTokens_validKeyNullValue_validUrlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key", null);

        // Test validation
        assertThat(place.getToken(), is("key"));
        assertThat(place.getKeys().size(), is(1));
        assertThat(place.getParameter("key"), is(""));
    }

    @Test
    public void constructorWithTokens_validKeyEmptyValue_validUrlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key", "");

        // Test validation
        assertThat(place.getToken(), is("key"));
        assertThat(place.getKeys().size(), is(1));
        assertThat(place.getParameter("key"), is(""));
    }

    @Test
    public void constructorWithTokens_validKeyValidValue_validUrlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key", "value");

        // Test validation
        assertThat(place.getToken(), is("key=value"));
        assertThat(place.getKeys().size(), is(1));
        assertThat(place.getParameter("key"), is("value"));
    }

    @Test
    public void constructorWithTokens_twoValidKeysOnlyOneValidValue_validUrlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key", "value", "key2");

        // Test validation
        assertThat(place.getToken(), is("key=value&key2"));
        assertThat(place.getKeys().size(), is(2));
        assertThat(place.getParameter("key"), is("value"));
        assertThat(place.getParameter("key2"), is(""));
    }

    @Test
    public void constructorWithTokens_twoValidParameters_validUrlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key", "value", "key2", "value2");

        // Test validation
        assertThat(place.getToken(), is("key=value&key2=value2"));
        assertThat(place.getKeys().size(), is(2));
        assertThat(place.getParameter("key"), is("value"));
        assertThat(place.getParameter("key2"), is("value2"));
    }


    // Test uppercase

    @Test
    public void constructorWithUrl_validUrlTwoPairsWithUppercase_urlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("Key=valuE&kEY2=vALue2");

        // Test validation
        assertThat(place.getToken(), is("Key=valuE&kEY2=vALue2"));
        assertThat(place.getKeys().size(), is(2));
        assertThat(place.getParameter("Key"), is("valuE"));
        assertThat(place.getParameter("kEY2"), is("vALue2"));
    }

    @Test
    public void constructorWithTokens_twoValidParametersWithUppercase_validUrlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("kEy", "vaLue", "keY2", "Value2");

        // Test validation
        assertThat(place.getToken(), is("kEy=vaLue&keY2=Value2"));
        assertThat(place.getKeys().size(), is(2));
        assertThat(place.getParameter("kEy"), is("vaLue"));
        assertThat(place.getParameter("keY2"), is("Value2"));
    }


    // Test hasValue

    @Test
    public void hasValue_twoValidParameters_testCorrectness() {
        // Test preparation
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key", "value", "key2", "value2");

        // Subject under test
        assertThat(place.hasValue("key"), is(true));
        assertThat(place.hasValue("key2"), is(true));
        assertThat(place.hasValue("key3"), is(false));
    }


    // Test getParameters

    @Test
    public void getParameters_threeValidParameters_validDataFetched() {
        // Test preparation
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key1", "value1", "key3", "value3", "key2", "value2");

        // Subject under test
        Map<String, String> parameters = place.getParameters();

        // Test validation
        assertThat(parameters.size(), is(3));
        assertThat(parameters.get("key1"), is("value1"));
        assertThat(parameters.get("key2"), is("value2"));
        assertThat(parameters.get("key3"), is("value3"));
    }


    // Test setParameters

    @Test
    public void setParameters_threeValidParameters_validDataStored() {
        // Test preparation
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key1", "value1", "key3", "value3", "key2", "value2");

        // Subject under test
        Map<String, String> newParameters = new LinkedHashMap<>();
        newParameters.put("key6", "value6");
        newParameters.put("key5", "value5");
        place.setParameters(newParameters);

        // Test validation
        Map<String, String> parameters = place.getParameters();
        assertThat(parameters.size(), is(2));
        assertThat(parameters.get("key6"), is("value6"));
        assertThat(parameters.get("key5"), is("value5"));
        assertThat(place.getToken(), is("key6=value6&key5=value5"));
    }


    // Test getParameter

    @Test
    public void getParameter_null_nullValue() {
        // Test preparation
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key1", "value1", "key3", "value3", "key2", "value2");

        // Subject under test
        String value = place.getParameter(null);

        // Test validation
        assertThat(value, is(nullValue()));
    }

    @Test
    public void getParameter_notFound_nullValue() {
        // Test preparation
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key1", "value1", "key3", "value3", "key2", "value2");

        // Subject under test
        String value = place.getParameter("key7");

        // Test validation
        assertThat(value, is(nullValue()));
    }

    @Test
    public void getParameter_found_value() {
        // Test preparation
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key1", "value1", "key3", "value3", "key2", "value2");

        // Subject under test
        String value = place.getParameter("key3");

        // Test validation
        assertThat(value, is("value3"));
    }


    // Test addParameter

    @Test
    public void addParameter_null_stored() {
        // Test preparation
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key1", "value1", "key3", "value3", "key2", "value2");

        // Subject under test
        place.addParameter(null, "fido");

        // Test validation
        Map<String, String> parameters = place.getParameters();
        assertThat(parameters.size(), is(4));
        assertThat(parameters.get(null), is("fido"));
        assertThat(place.getToken(), is("key1=value1&key3=value3&key2=value2&null=fido"));
    }

    @Test
    public void addParameter_empty_stored() {
        // Test preparation
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key1", "value1", "key3", "value3", "key2", "value2");

        // Subject under test
        place.addParameter("", "monkey");

        // Test validation
        Map<String, String> parameters = place.getParameters();
        assertThat(parameters.size(), is(4));
        assertThat(parameters.get(""), is("monkey"));
        assertThat(place.getToken(), is("key1=value1&key3=value3&key2=value2&=monkey"));
    }

    @Test
    public void addParameter_valid_stored() {
        // Test preparation
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key1", "value1", "key3", "value3", "key2", "value2");

        // Subject under test
        place.addParameter("high", "fidelity");

        // Test validation
        Map<String, String> parameters = place.getParameters();
        assertThat(parameters.size(), is(4));
        assertThat(parameters.get("high"), is("fidelity"));
        assertThat(place.getToken(), is("key1=value1&key3=value3&key2=value2&high=fidelity"));
    }

    @Test
    public void addParameter_validKeyNullValue_stored() {
        // Test preparation
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key1", "value1", "key3", "value3", "key2", "value2");

        // Subject under test
        place.addParameter("high", null);

        // Test validation
        Map<String, String> parameters = place.getParameters();
        assertThat(parameters.size(), is(4));
        assertThat(parameters.get("high"), is(nullValue()));
        assertThat(place.getToken(), is("key1=value1&key3=value3&key2=value2&high"));
    }

    @Test
    public void addParameter_existingKey_newValueStored() {
        // Test preparation
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key1", "value1", "key3", "value3", "key2", "value2");

        // Subject under test
        place.addParameter("key1", "New Value");

        // Test validation
        Map<String, String> parameters = place.getParameters();
        assertThat(parameters.size(), is(3));
        assertThat(parameters.get("key1"), is("New Value"));
        assertThat(place.getToken(), is("key1=New Value&key3=value3&key2=value2"));
    }


    // Test removeParameter

    @Test
    public void removeParameter_null_noAction() {
        // Test preparation
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key1", "value1", "key3", "value3", "key2", "value2");

        // Subject under test
        final String formerValue = place.removeParameter(null);

        // Test validation
        assertThat(formerValue, is(nullValue()));
        Map<String, String> parameters = place.getParameters();
        assertThat(parameters.size(), is(3));
        assertThat(parameters.get("key1"), is("value1"));
        assertThat(parameters.get("key3"), is("value3"));
        assertThat(parameters.get("key2"), is("value2"));
        assertThat(place.getToken(), is("key1=value1&key3=value3&key2=value2"));
    }

    @Test
    public void removeParameter_emptyNotFound_noAction() {
        // Test preparation
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key1", "value1", "key3", "value3", "key2", "value2");

        // Subject under test
        final String formerValue = place.removeParameter("");

        // Test validation
        assertThat(formerValue, is(nullValue()));
        Map<String, String> parameters = place.getParameters();
        assertThat(parameters.size(), is(3));
        assertThat(parameters.get("key1"), is("value1"));
        assertThat(parameters.get("key3"), is("value3"));
        assertThat(parameters.get("key2"), is("value2"));
        assertThat(place.getToken(), is("key1=value1&key3=value3&key2=value2"));
    }

    @Test
    public void removeParameter_emptyFound_removed() {
        // Test preparation
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key3", "value3", "key2", "value2");
        place.addParameter("", "empty");

        // Subject under test
        final String formerValue = place.removeParameter("");

        // Test validation
        assertThat(formerValue, is("empty"));
        Map<String, String> parameters = place.getParameters();
        assertThat(parameters.size(), is(2));
        assertThat(parameters.get("key3"), is("value3"));
        assertThat(parameters.get("key2"), is("value2"));
        assertThat(place.getToken(), is("key3=value3&key2=value2"));
    }

    @Test
    public void removeParameter_validNotFound_noAction() {
        // Test preparation
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key1", "value1", "key3", "value3", "key2", "value2");

        // Subject under test
        final String formerValue = place.removeParameter("what");

        // Test validation
        assertThat(formerValue, is(nullValue()));
        Map<String, String> parameters = place.getParameters();
        assertThat(parameters.size(), is(3));
        assertThat(parameters.get("key1"), is("value1"));
        assertThat(parameters.get("key3"), is("value3"));
        assertThat(parameters.get("key2"), is("value2"));
        assertThat(place.getToken(), is("key1=value1&key3=value3&key2=value2"));
    }

    @Test
    public void removeParameter_validFound_removed() {
        // Test preparation
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key1", "value1", "key3", "value3", "key2", "value2");

        // Subject under test
        final String formerValue = place.removeParameter("key3");

        // Test validation
        assertThat(formerValue, is("value3"));
        Map<String, String> parameters = place.getParameters();
        assertThat(parameters.size(), is(2));
        assertThat(parameters.get("key1"), is("value1"));
        assertThat(parameters.get("key2"), is("value2"));
        assertThat(place.getToken(), is("key1=value1&key2=value2"));
    }

}