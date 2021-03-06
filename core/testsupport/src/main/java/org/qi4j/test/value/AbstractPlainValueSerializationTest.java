/*
 * Copyright (c) 2011, Niclas Hehdman. All Rights Reserved.
 * Copyright (c) 2012, Paul Merlin. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *     You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.qi4j.test.value;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import org.junit.Before;
import org.junit.Test;
import org.qi4j.api.entity.EntityReference;
import org.qi4j.api.injection.scope.Service;
import org.qi4j.api.value.ValueSerialization;
import org.qi4j.test.AbstractQi4jTest;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

/**
 * Assert that ValueSerialization behaviour on plain values is correct.
 */
public abstract class AbstractPlainValueSerializationTest
    extends AbstractQi4jTest
{

    @Before
    public void before()
    {
        module.injectTo( this );
    }
    @Service
    @SuppressWarnings( "ProtectedField" )
    protected ValueSerialization valueSerialization;

    @Test
    public void givenCharacterValueWhenSerializingAndDeserializingExpectEquals()
    {
        String serialized = valueSerialization.serialize( 'q' );
        assertThat( "Serialized", serialized, equalTo( "q" ) );

        Character deserialized = valueSerialization.deserialize( Character.class, serialized );
        assertThat( "Deserialized", deserialized, equalTo( 'q' ) );
    }

    @Test
    public void givenEmptyStringValueWhenSerializingAndDeserializingExpectEquals()
    {
        String serialized = valueSerialization.serialize( "" );
        assertThat( "Serialized", serialized, equalTo( "" ) );

        String deserialized = valueSerialization.deserialize( String.class, serialized );
        assertThat( "Deserialized", deserialized, equalTo( "" ) );
    }

    @Test
    public void givenStringValueWhenSerializingAndDeserializingExpectEquals()
    {
        String serialized = valueSerialization.serialize( "test" );
        assertThat( serialized, equalTo( "test" ) );

        String deserialized = valueSerialization.deserialize( String.class, serialized );
        assertThat( deserialized, equalTo( "test" ) );
    }

    @Test
    public void givenBooleanValueWhenSerializingAndDeserializingExpectEquals()
    {
        String serialized = valueSerialization.serialize( Boolean.TRUE );
        assertThat( serialized, equalTo( "true" ) );

        Boolean deserialized = valueSerialization.deserialize( Boolean.class, serialized );
        assertThat( deserialized, equalTo( Boolean.TRUE ) );
    }

    @Test
    public void givenIntegerValueWhenSerializingAndDeserializingExpectEquals()
    {
        String serialized = valueSerialization.serialize( 42 );
        assertThat( serialized, equalTo( "42" ) );
        Integer deserialized = valueSerialization.deserialize( Integer.class, serialized );
        assertThat( deserialized, equalTo( 42 ) );
    }

    @Test
    public void givenLongValueWhenSerializingAndDeserializingExpectEquals()
    {
        String serialized = valueSerialization.serialize( 42L );
        assertThat( serialized, equalTo( "42" ) );

        Long deserialized = valueSerialization.deserialize( Long.class, serialized );
        assertThat( deserialized, equalTo( 42L ) );
    }

    @Test
    public void givenShortValueWhenSerializingAndDeserializingExpectEquals()
    {
        String serialized = valueSerialization.serialize( (short) 42 );
        assertThat( serialized, equalTo( "42" ) );

        Short deserialized = valueSerialization.deserialize( Short.class, serialized );
        assertThat( deserialized, equalTo( (short) 42 ) );
    }

    @Test
    public void givenByteValueWhenSerializingAndDeserializingExpectEquals()
    {
        String serialized = valueSerialization.serialize( (byte) 42 );
        assertThat( serialized, equalTo( "42" ) );
        Byte deserialized = valueSerialization.deserialize( Byte.class, serialized );
        assertThat( deserialized, equalTo( (byte) 42 ) );
    }

    @Test
    public void givenFloatValueWhenSerializingAndDeserializingExpectEquals()
    {
        String serialized = valueSerialization.serialize( 42F );
        assertThat( serialized, equalTo( "42.0" ) );

        Float deserialized = valueSerialization.deserialize( Float.class, serialized );
        assertThat( deserialized, equalTo( 42F ) );
    }

    @Test
    public void givenDoubleValueWhenSerializingAndDeserializingExpectEquals()
    {
        String serialized = valueSerialization.serialize( 42D );
        assertThat( serialized, equalTo( "42.0" ) );

        Double deserialized = valueSerialization.deserialize( Double.class, serialized );
        assertThat( deserialized, equalTo( 42D ) );
    }

    @Test
    public void givenBigIntegerValueWhenSerializingAndDeserializingExpectEquals()
    {
        BigInteger bigInteger = new BigInteger( "42424242424242424242424242" );
        assertThat( bigInteger, not( equalTo( BigInteger.valueOf( bigInteger.longValue() ) ) ) );

        String serialized = valueSerialization.serialize( bigInteger );
        assertThat( serialized, equalTo( "42424242424242424242424242" ) );

        BigInteger deserialized = valueSerialization.deserialize( BigInteger.class, serialized );
        assertThat( deserialized, equalTo( bigInteger ) );
    }

    @Test
    public void givenBigDecimalValueWhenSerializingAndDeserializingExpectEquals()
    {
        BigDecimal bigDecimal = new BigDecimal( "42.2376931348623157e+309" );
        assertThat( bigDecimal.doubleValue(), equalTo( Double.POSITIVE_INFINITY ) );
        
        String serialized = valueSerialization.serialize( bigDecimal );
        assertThat( serialized, equalTo( "4.22376931348623157E+310" ) );

        BigDecimal deserialized = valueSerialization.deserialize( BigDecimal.class, serialized );
        assertThat( deserialized, equalTo( bigDecimal ) );
    }

    @Test
    public void givenDateValueWhenSerializingAndDeserializingExpectEquals()
    {
        ZonedDateTime refTime = ZonedDateTime.parse( "2020-03-04T13:24:35Z" );
        String serialized = valueSerialization.serialize( refTime );
        assertThat( serialized, equalTo( "2020-03-04T13:24:35Z" ) );

        ZonedDateTime deserialized = valueSerialization.deserialize( ZonedDateTime.class, serialized );
        assertThat( deserialized, equalTo( refTime ) );
        assertThat( deserialized, equalTo( refTime ) );
    }

    @Test
    public void givenDateTimeValueWhenSerializingAndDeserializingExpectEquals()
    {
        OffsetDateTime refTime = OffsetDateTime.parse( "2020-03-04T13:24:35+08:00" );
        String serialized = valueSerialization.serialize( refTime );
        assertThat( serialized, equalTo( "2020-03-04T13:24:35+08:00" ) );
        OffsetDateTime deserialized = valueSerialization.deserialize( OffsetDateTime.class, serialized );
        assertThat( deserialized, equalTo( refTime ));
    }

    @Test
    public void givenLocalDateTimeValueWhenSerializingAndDeserializingExpectEquals()
    {
        // Serialized without TimeZone
        LocalDateTime refTime = LocalDateTime.parse( "2020-03-04T13:23:10" );
        String serialized = valueSerialization.serialize( refTime );
        assertThat( serialized, equalTo( "2020-03-04T13:23:10" ) );

        LocalDateTime deserialized = valueSerialization.deserialize( LocalDateTime.class, serialized );
        assertThat( deserialized, equalTo( refTime ) );
    }

    @Test
    public void givenLocalDateValueWhenSerializingAndDeserializingExpectEquals()
    {
        LocalDate refTime = LocalDate.parse( "2020-03-04" );
        String serialized = valueSerialization.serialize( refTime );
        assertThat( serialized, equalTo( "2020-03-04" ) );

        LocalDate deserialized = valueSerialization.deserialize( LocalDate.class, serialized );
        assertThat( deserialized, equalTo( refTime ) );
    }

    @Test
    public void givenEntityReferenceValueWhenSerializingAndDeserializingExpectEquals()
    {
        String serialized = valueSerialization.serialize( EntityReference.parseEntityReference( "ABCD-1234" ) );
        assertThat( serialized, equalTo( "ABCD-1234" ) );

        EntityReference deserialized = valueSerialization.deserialize( EntityReference.class, serialized );
        assertThat( deserialized, equalTo( EntityReference.parseEntityReference( "ABCD-1234" ) ) );
    }
}
