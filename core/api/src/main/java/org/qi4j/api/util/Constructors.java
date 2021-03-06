package org.qi4j.api.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.function.Function;

import static org.qi4j.functional.Iterables.iterable;

/**
 * Useful methods for handling Constructors.
 */
public final class Constructors
{
    public static final Function<Type, Iterable<Constructor<?>>> CONSTRUCTORS_OF = Classes.forClassHierarchy( new Function<Class<?>, Iterable<Constructor<?>>>()
    {
        @Override
        public Iterable<Constructor<?>> apply( Class<?> type )
        {
            return iterable( type.getDeclaredConstructors() );
        }
    } );
}
