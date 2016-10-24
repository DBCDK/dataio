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

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


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
        assertThat(place.getUrl(), is(""));
        assertThat(place.getKeys().isEmpty(), is(true));
    }

    @Test
    public void constructorWithUrl_nullUrl_emptyUrlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace((String)null);

        // Test validation
        assertThat(place.getUrl(), is(""));
        assertThat(place.getKeys().isEmpty(), is(true));
    }

    @Test
    public void constructorWithUrl_emptyUrl_emptyUrlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("");

        // Test validation
        assertThat(place.getUrl(), is(""));
        assertThat(place.getKeys().isEmpty(), is(true));
    }

    @Test
    public void constructorWithUrl_validUrlOnlyKey_urlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key");

        // Test validation
        assertThat(place.getUrl(), is("key"));
        assertThat(place.getKeys().size(), is(1));
        assertThat(place.getValue("key"), is(""));
    }

    @Test
    public void constructorWithUrl_validUrlOnePair_urlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key=value");

        // Test validation
        assertThat(place.getUrl(), is("key=value"));
        assertThat(place.getKeys().size(), is(1));
        assertThat(place.getValue("key"), is("value"));
    }

    @Test
    public void constructorWithUrl_validUrlOnePairDoubleEqualSign_urlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key=val=ue");

        // Test validation
        assertThat(place.getUrl(), is("key=val=ue"));
        assertThat(place.getKeys().size(), is(1));
        assertThat(place.getValue("key"), is("val=ue"));
    }

    @Test
    public void constructorWithUrl_validUrlOneAndAHalfPairs_urlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key=value&key2=");

        // Test validation
        assertThat(place.getUrl(), is("key=value&key2"));
        assertThat(place.getKeys().size(), is(2));
        assertThat(place.getValue("key"), is("value"));
        assertThat(place.getValue("key2"), is(""));
    }

    @Test
    public void constructorWithUrl_validUrlOneAndAHalfPairsWithoutEqualSign_urlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key=value&key2");

        // Test validation
        assertThat(place.getUrl(), is("key=value&key2"));
        assertThat(place.getKeys().size(), is(2));
        assertThat(place.getValue("key"), is("value"));
        assertThat(place.getValue("key2"), is(""));
    }

    @Test
    public void constructorWithUrl_validUrlTwoPairs_urlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key=value&key2=value2");

        // Test validation
        assertThat(place.getUrl(), is("key=value&key2=value2"));
        assertThat(place.getKeys().size(), is(2));
        assertThat(place.getValue("key"), is("value"));
        assertThat(place.getValue("key2"), is("value2"));
    }


    // Test constructor with token parameters

    @Test
    public void constructorWithTokens_nullKeyNullValue_emptyUrlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace(null, null);

        // Test validation
        assertThat(place.getUrl(), is(""));
        assertThat(place.getKeys().isEmpty(), is(true));
    }

    @Test
    public void constructorWithTokens_emptyKeyNullValue_emptyUrlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("", null);

        // Test validation
        assertThat(place.getUrl(), is(""));
        assertThat(place.getKeys().isEmpty(), is(true));
    }

    @Test
    public void constructorWithTokens_validKeyNullValue_validUrlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key", null);

        // Test validation
        assertThat(place.getUrl(), is("key"));
        assertThat(place.getKeys().size(), is(1));
        assertThat(place.getValue("key"), is(""));
    }

    @Test
    public void constructorWithTokens_validKeyEmptyValue_validUrlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key", "");

        // Test validation
        assertThat(place.getUrl(), is("key"));
        assertThat(place.getKeys().size(), is(1));
        assertThat(place.getValue("key"), is(""));
    }

    @Test
    public void constructorWithTokens_validKeyValidValue_validUrlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key", "value");

        // Test validation
        assertThat(place.getUrl(), is("key=value"));
        assertThat(place.getKeys().size(), is(1));
        assertThat(place.getValue("key"), is("value"));
    }

    @Test
    public void constructorWithTokens_twoValidKeysOnlyOneValidValue_validUrlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key", "value", "key2");

        // Test validation
        assertThat(place.getUrl(), is("key=value&key2"));
        assertThat(place.getKeys().size(), is(2));
        assertThat(place.getValue("key"), is("value"));
        assertThat(place.getValue("key2"), is(""));
    }

    @Test
    public void constructorWithTokens_twoValidParameters_validUrlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("key", "value", "key2", "value2");

        // Test validation
        assertThat(place.getUrl(), is("key=value&key2=value2"));
        assertThat(place.getKeys().size(), is(2));
        assertThat(place.getValue("key"), is("value"));
        assertThat(place.getValue("key2"), is("value2"));
    }


    // Test uppercase

    @Test
    public void constructorWithUrl_validUrlTwoPairsWithUppercase_urlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("Key=valuE&kEY2=vALue2");

        // Test validation
        assertThat(place.getUrl(), is("key=valuE&key2=vALue2"));
        assertThat(place.getKeys().size(), is(2));
        assertThat(place.getValue("key"), is("valuE"));
        assertThat(place.getValue("key2"), is("vALue2"));
    }

    @Test
    public void constructorWithTokens_twoValidParametersWithUppercase_validUrlStored() {
        // Subject under test
        AbstractBasePlace place = new ConcreteAbstractBasePlace("kEy", "vaLue", "keY2", "Value2");

        // Test validation
        assertThat(place.getUrl(), is("key=vaLue&key2=Value2"));
        assertThat(place.getKeys().size(), is(2));
        assertThat(place.getValue("key"), is("vaLue"));
        assertThat(place.getValue("key2"), is("Value2"));
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


}
