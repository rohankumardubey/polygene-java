/*
 * Copyright (c) 2007, Rickard Öberg. All Rights Reserved.
 * Copyright (c) 2010, Niclas Hehdman. All Rights Reserved.
 * Copyright (c) 2012-2014, Paul Merlin. All Rights Reserved.
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
package org.qi4j.spi.value;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.qi4j.api.Qi4j;
import org.qi4j.api.association.Association;
import org.qi4j.api.association.AssociationDescriptor;
import org.qi4j.api.association.AssociationStateHolder;
import org.qi4j.api.association.ManyAssociation;
import org.qi4j.api.association.NamedAssociation;
import org.qi4j.api.composite.CompositeInstance;
import org.qi4j.api.entity.EntityComposite;
import org.qi4j.api.entity.EntityReference;
import org.qi4j.api.entity.Identity;
import org.qi4j.api.property.Property;
import org.qi4j.api.property.PropertyDescriptor;
import org.qi4j.api.util.Base64Encoder;
import org.qi4j.api.value.ValueComposite;
import org.qi4j.api.value.ValueDescriptor;
import org.qi4j.api.value.ValueSerializationException;
import org.qi4j.api.value.ValueSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.qi4j.functional.Iterables.first;

/**
 * Adapter for pull-parsing capable ValueSerializers.
 *
 * <p>
 * Among Plain values (see {@link ValueSerializer}) some are considered primitives to underlying serialization
 * mechanisms and by so handed/come without conversion to/from implementations. Primitive values can be one of:
 * </p>
 * <ul>
 * <li>String,</li>
 * <li>Character or char,</li>
 * <li>Boolean or boolean,</li>
 * <li>Integer or int,</li>
 * <li>Long or long,</li>
 * <li>Short or short,</li>
 * <li>Byte or byte,</li>
 * <li>Float or float,</li>
 * <li>Double or double.</li>
 * </ul>
 * <p>
 * Some other Plain values are transformed before being handed to implementations:
 * </p>
 * <ul>
 * <li>BigInteger and BigDecimal depends on ValueSerializer.{@link Options};</li>
 * <li>Date as a ISO-8601 UTC String;</li>
 * </ul>
 *
 * @param <OutputType> Implementor output type
 */
public abstract class ValueSerializerAdapter<OutputType>
    implements ValueSerializer
{

    public static interface ComplexSerializer<T, OutputType>
    {
        void serialize( Options options, T object, OutputType output )
            throws Exception;
    }

    private static final Logger LOG = LoggerFactory.getLogger( ValueSerializerAdapter.class );
    private static final String UTF_8 = "UTF-8";

    private static <TO, FROM extends TO> BiFunction<Options, FROM, TO> identitySerializer()
    {
        return new BiFunction<Options, FROM, TO>()
        {
            @Override
            public TO apply( Options options, FROM from )
            {
                return from;
            }
        };
    }

    private final Map<Class<?>, BiFunction<Options, Object, Object>> serializers = new HashMap<>( 16 );
    private final Map<Class<?>, ComplexSerializer<Object, OutputType>> complexSerializers = new HashMap<>( 2 );

    /**
     * Register a Plain Value type serialization Function.
     *
     * @param <T>        Plain Value parametrized Type
     * @param type       Plain Value Type
     * @param serializer Serialization Function
     */
    @SuppressWarnings("unchecked")
    protected final <T> void registerSerializer( Class<T> type, BiFunction<Options, T, Object> serializer )
    {
        serializers.put( type, (BiFunction<Options, Object, Object>) serializer );
    }

    /**
     * Register a Complex Value type serialization Function.
     *
     * @param <T>        Complex Value parametrized Type
     * @param type       Complex Value Type
     * @param serializer Serialization Function
     */
    @SuppressWarnings("unchecked")
    protected final <T> void registerComplexSerializer( Class<T> type, ComplexSerializer<T, OutputType> serializer )
    {
        complexSerializers.put( type, (ComplexSerializer<Object, OutputType>) serializer );
    }

    public ValueSerializerAdapter()
    {
        // Primitive Value types
        registerSerializer( String.class, ValueSerializerAdapter.<Object, String>identitySerializer() );
        registerSerializer( Character.class, ValueSerializerAdapter.<Object, Character>identitySerializer() );
        registerSerializer( Boolean.class, ValueSerializerAdapter.<Object, Boolean>identitySerializer() );
        registerSerializer( Integer.class, ValueSerializerAdapter.<Object, Integer>identitySerializer() );
        registerSerializer( Long.class, ValueSerializerAdapter.<Object, Long>identitySerializer() );
        registerSerializer( Short.class, ValueSerializerAdapter.<Object, Short>identitySerializer() );
        registerSerializer( Byte.class, ValueSerializerAdapter.<Object, Byte>identitySerializer() );
        registerSerializer( Float.class, ValueSerializerAdapter.<Object, Float>identitySerializer() );
        registerSerializer( Double.class, ValueSerializerAdapter.<Object, Double>identitySerializer() );

        // Number types
        registerSerializer( BigDecimal.class, new BiFunction<Options, BigDecimal, Object>()
        {
            @Override
            public Object apply( Options options, BigDecimal bigDecimal )
            {
                return bigDecimal.toString();
            }
        } );
        registerSerializer( BigInteger.class, new BiFunction<Options, BigInteger, Object>()
        {
            @Override
            public Object apply( Options options, BigInteger bigInteger )
            {
                return bigInteger.toString();
            }
        } );

        // Date types
        registerSerializer( ZonedDateTime.class, new BiFunction<Options, ZonedDateTime, Object>()
        {
            @Override
            public Object apply( Options options, ZonedDateTime dateTime )
            {
                return dateTime.toString();
            }
        } );
        registerSerializer( LocalDateTime.class, new BiFunction<Options, LocalDateTime, Object>()
        {
            @Override
            public Object apply( Options options, LocalDateTime dateTime )
            {
                return dateTime.toString();
            }
        } );
        registerSerializer( LocalTime.class, new BiFunction<Options, LocalTime, Object>()
        {
            @Override
            public Object apply( Options options, LocalTime time )
            {
                return time.toString();
            }
        } );
        registerSerializer( LocalDate.class, new BiFunction<Options, LocalDate, Object>()
        {
            @Override
            public Object apply( Options options, LocalDate date )
            {
                return date.toString();
            }
        } );
        registerSerializer( OffsetDateTime.class, new BiFunction<Options, OffsetDateTime, Object>()
        {
            @Override
            public Object apply( Options options, OffsetDateTime time )
            {
                return time.toString();
            }
        } );
        registerSerializer( OffsetTime.class, new BiFunction<Options, OffsetTime, Object>()
        {
            @Override
            public Object apply( Options options, OffsetTime time )
            {
                return time.toString();
            }
        } );
        registerSerializer( Duration.class, new BiFunction<Options, Duration, Object>()
        {
            @Override
            public Object apply( Options options, Duration time )
            {
                return time.toString();
            }
        } );
        registerSerializer( Period.class, new BiFunction<Options, Period, Object>()
        {
            @Override
            public Object apply( Options options, Period time )
            {
                return time.toString();
            }
        } );
        registerSerializer( Instant.class, new BiFunction<Options, Instant, Object>()
        {
            @Override
            public Object apply( Options options, Instant time )
            {
                return time.toString();
            }
        } );
        registerSerializer( MonthDay.class, new BiFunction<Options, MonthDay, Object>()
        {
            @Override
            public Object apply( Options options, MonthDay monthDay )
            {
                return monthDay.toString();
            }
        } );
        registerSerializer( Year.class, new BiFunction<Options, Year, Object>()
        {
            @Override
            public Object apply( Options options, Year year )
            {
                return year.toString();
            }
        } );
        registerSerializer( YearMonth.class, new BiFunction<Options, YearMonth, Object>()
        {
            @Override
            public Object apply( Options options, YearMonth yearMonth )
            {
                return yearMonth.toString();
            }
        } );
        registerSerializer( ZoneId.class, new BiFunction<Options, ZoneId, Object>()
        {
            @Override
            public Object apply( Options options, ZoneId id )
            {
                return id.toString();
            }
        } );
        registerSerializer( ZoneOffset.class, new BiFunction<Options, ZoneOffset, Object>()
        {
            @Override
            public Object apply( Options options, ZoneOffset offset )
            {
                return offset.toString();
            }
        } );

        // Other supported types
        registerSerializer( EntityReference.class, new BiFunction<Options, EntityReference, Object>()
        {
            @Override
            public Object apply( Options options, EntityReference ref )
            {
                return ref.toString();
            }
        } );
    }

    @Override
    public final <T> Function<T, String> serialize()
    {
        return new Function<T, String>()
        {
            @Override
            public String apply( T object )
            {
                return serialize( object );
            }
        };
    }

    @Override
    public final <T> Function<T, String> serialize( final Options options )
    {
        return new Function<T, String>()
        {
            @Override
            public String apply( T object )
            {
                return serialize( options, object );
            }
        };
    }

    @Override
    @Deprecated
    public final <T> Function<T, String> serialize( final boolean includeTypeInfo )
    {
        return new Function<T, String>()
        {
            @Override
            public String apply( T object )
            {
                return serialize( includeTypeInfo ? new Options().withTypeInfo() : new Options().withoutTypeInfo(),
                                  object );
            }
        };
    }

    @Override
    public final String serialize( Object object )
        throws ValueSerializationException
    {
        return serialize( new Options(), object );
    }

    @Override
    public final String serialize( Options options, Object object )
        throws ValueSerializationException
    {
        try
        {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            serializeRoot( options, object, output );
            return output.toString( UTF_8 );
        }
        catch( ValueSerializationException ex )
        {
            throw ex;
        }
        catch( Exception ex )
        {
            throw new ValueSerializationException( "Could not serialize value", ex );
        }
    }

    @Override
    @Deprecated
    public final String serialize( Object object, boolean includeTypeInfo )
        throws ValueSerializationException
    {
        return serialize( includeTypeInfo ? new Options().withTypeInfo() : new Options().withoutTypeInfo(),
                          object );
    }

    @Override
    public final void serialize( Object object, OutputStream output )
        throws ValueSerializationException
    {
        serialize( new Options(), object, output );
    }

    @Override
    public final void serialize( Options options, Object object, OutputStream output )
        throws ValueSerializationException
    {
        try
        {
            serializeRoot( options, object, output );
        }
        catch( ValueSerializationException ex )
        {
            throw ex;
        }
        catch( Exception ex )
        {
            throw new ValueSerializationException( "Could not serialize value", ex );
        }
    }

    @Override
    @Deprecated
    public final void serialize( Object object, OutputStream output, boolean includeTypeInfo )
        throws ValueSerializationException
    {
        serialize( includeTypeInfo ? new Options().withTypeInfo() : new Options().withoutTypeInfo(),
                   object, output );
    }

    private void serializeRoot( Options options, Object object, OutputStream output )
        throws Exception
    {
        if( object != null )
        {
            // System.out.println( ">>>>>>>>>>>> " + ( object == null ? "null" : object.getClass() ) );
            if( serializers.get( object.getClass() ) != null )
            {
                // Plain Value
                Object serialized = serializers.get( object.getClass() ).apply( options, object );
                output.write( serialized.toString().getBytes( UTF_8 ) );
            }
            else if( object.getClass().isEnum() )
            {
                // Enum Value
                output.write( object.toString().getBytes( UTF_8 ) );
            }
            else if( object.getClass().isArray() )
            {
                // Array Value
                output.write( serializeBase64Serializable( object ).getBytes( UTF_8 ) );
            }
            else
            {
                // Complex Value
                OutputType adaptedOutput = adaptOutput( output );
                onSerializationStart( object, adaptedOutput );
                doSerialize( options, object, adaptedOutput, true );
                onSerializationEnd( object, adaptedOutput );
            }
        }
    }

    private void doSerialize( Options options, Object object, OutputType output, boolean rootPass )
        throws Exception
    {
        if( object == null )
        {
            LOG.trace( "Null object -> onValue( null )" );
            onValue( output, null );
            return;
        }

        Class<?> type = object.getClass();
        if( ( !type.equals( ZoneOffset.class ) ) && ZoneId.class.isAssignableFrom( type ) )
        {
            type = ZoneId.class;
        }
        if( serializers.get( type ) != null )
        {
            LOG.trace( "Registered serializer matches -> onValue( serialized )" );
            onValue( output, serializers.get( type ).apply( options, object ) );
        }
        else if( complexSerializers.get( type ) != null )
        {
            complexSerializers.get( type ).serialize( options, object, output );
        }
        else if( ValueComposite.class.isAssignableFrom( type ) )
        {
            LOG.trace( "ValueComposite assignable -> serializeValueComposite( object )" );
            serializeValueComposite( options, object, output, rootPass );
        }
        else if( EntityComposite.class.isAssignableFrom( type ) )
        {
            LOG.trace( "EntityComposite assignable -> serializeEntityComposite( object )" );
            serializeEntityComposite( object, output );
        }
        else if( Iterable.class.isAssignableFrom( type ) )
        {
            LOG.trace( "Iterable assignable -> serializeIterable( object )" );
            serializeIterable( options, object, output );
        }
        else if( type.isArray() )
        {
            LOG.trace( "Object isArray -> serializeBase64Serializable( object )" );
            serializeBase64Serializable( object, output );
        }
        else if( Map.class.isAssignableFrom( type ) )
        {
            LOG.trace( "Map assignable -> serializeMap( object )" );
            serializeMap( options, object, output );
        }
        else if( type.isEnum() )
        {
            LOG.trace( "Object is an enum -> onValue( object.toString() )" );
            onValue( output, object.toString() );
        }
        else // Fallback to Base64 encoded Java Serialization
        {
            LOG.trace( "Unknown object type -> serializeBase64Serializable( object )" );
            serializeBase64Serializable( object, output );
        }
    }

    private void serializeValueComposite( Options options, Object object, OutputType output, boolean rootPass )
        throws Exception
    {
        CompositeInstance valueInstance = Qi4j.FUNCTION_COMPOSITE_INSTANCE_OF.apply( (ValueComposite) object );
        ValueDescriptor descriptor = (ValueDescriptor) valueInstance.descriptor();
        AssociationStateHolder state = (AssociationStateHolder) valueInstance.state();

        onObjectStart( output );

        if( options.getBoolean( Options.INCLUDE_TYPE_INFO ) && !rootPass )
        {
            onFieldStart( output, "_type" );
            onValueStart( output );
            onValue( output, first( descriptor.valueType().types() ).getName() );
            onValueEnd( output );
            onFieldEnd( output );
        }

        for( PropertyDescriptor persistentProperty : descriptor.valueType().properties() )
        {
            Property<?> property = state.propertyFor( persistentProperty.accessor() );
            onFieldStart( output, persistentProperty.qualifiedName().name() );
            onValueStart( output );
            doSerialize( options, property.get(), output, false );
            onValueEnd( output );
            onFieldEnd( output );
        }
        for( AssociationDescriptor associationDescriptor : descriptor.valueType().associations() )
        {
            Association<?> association = state.associationFor( associationDescriptor.accessor() );
            Object instance = association.get();
            onFieldStart( output, associationDescriptor.qualifiedName().name() );
            onValueStart( output );
            if( instance == null )
            {
                onValue( output, null );
            }
            else
            {
                onValue( output, ( (Identity) instance ).identity().get() );
            }
            onValueEnd( output );
            onFieldEnd( output );
        }
        for( AssociationDescriptor associationDescriptor : descriptor.valueType().manyAssociations() )
        {
            ManyAssociation<?> manyAssociation = state.manyAssociationFor( associationDescriptor.accessor() );
            onFieldStart( output, associationDescriptor.qualifiedName().name() );
            onValueStart( output );
            onArrayStart( output );
            for( Object instance : manyAssociation )
            {
                onValueStart( output );
                onValue( output, ( (Identity) instance ).identity().get() );
                onValueEnd( output );
            }
            onArrayEnd( output );
            onValueEnd( output );
            onFieldEnd( output );
        }
        for( AssociationDescriptor associationDescriptor : descriptor.valueType().namedAssociations() )
        {
            NamedAssociation<?> namedAssociation = state.namedAssociationFor( associationDescriptor.accessor() );
            onFieldStart( output, associationDescriptor.qualifiedName().name() );
            onValueStart( output );
            onObjectStart( output );
            for( String name : namedAssociation )
            {
                onFieldStart( output, name );
                onValueStart( output );
                onValue( output, ( (Identity) namedAssociation.get( name ) ).identity().get() );
                onValueEnd( output );
                onFieldEnd( output );
            }
            onObjectEnd( output );
            onValueEnd( output );
            onFieldEnd( output );
        }

        onObjectEnd( output );
    }

    private void serializeEntityComposite( Object object, OutputType output )
        throws Exception
    {
        onValue( output, EntityReference.entityReferenceFor( object ) );
    }

    private void serializeIterable( Options options, Object object, OutputType output )
        throws Exception
    {
        @SuppressWarnings("unchecked")
        Iterable<Object> collection = (Iterable<Object>) object;
        onArrayStart( output );
        for( Object item : collection )
        {
            onValueStart( output );
            doSerialize( options, item, output, false );
            onValueEnd( output );
        }
        onArrayEnd( output );
    }

    private void serializeMap( Options options, Object object, OutputType output )
        throws Exception
    {
        @SuppressWarnings("unchecked")
        Map<Object, Object> map = (Map<Object, Object>) object;
        onArrayStart( output );
        for( Map.Entry<Object, Object> entry : map.entrySet() )
        {
            onObjectStart( output );

            onFieldStart( output, "key" );
            onValueStart( output );
            onValue( output, entry.getKey().toString() );
            onValueEnd( output );
            onFieldEnd( output );

            onFieldStart( output, "value" );
            onValueStart( output );
            doSerialize( options, entry.getValue(), output, false );
            onValueEnd( output );
            onFieldEnd( output );

            onObjectEnd( output );
        }
        onArrayEnd( output );
    }

    private void serializeBase64Serializable( Object object, OutputType output )
        throws Exception
    {
        onValue( output, serializeBase64Serializable( object ) );
    }

    private String serializeBase64Serializable( Object object )
        throws Exception
    {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream( bout ))
        {
            out.writeUnshared( object );
        }
        byte[] bytes = Base64Encoder.encode( bout.toByteArray(), true );
        return new String( bytes, UTF_8 );
    }

    protected abstract OutputType adaptOutput( OutputStream output )
        throws Exception;

    protected void onSerializationStart( Object object, OutputType output )
        throws Exception
    {
        // NOOP
    }

    protected void onSerializationEnd( Object object, OutputType output )
        throws Exception
    {
        // NOOP
    }

    protected abstract void onArrayStart( OutputType output )
        throws Exception;

    protected abstract void onArrayEnd( OutputType output )
        throws Exception;

    protected abstract void onObjectStart( OutputType output )
        throws Exception;

    protected abstract void onObjectEnd( OutputType output )
        throws Exception;

    protected abstract void onFieldStart( OutputType output, String fieldName )
        throws Exception;

    protected void onFieldEnd( OutputType output )
        throws Exception
    {
        // NOOP
    }

    protected void onValueStart( OutputType output )
        throws Exception
    {
        // NOOP
    }

    protected abstract void onValue( OutputType output, Object value )
        throws Exception;

    protected void onValueEnd( OutputType output )
        throws Exception
    {
        // NOOP
    }
}
