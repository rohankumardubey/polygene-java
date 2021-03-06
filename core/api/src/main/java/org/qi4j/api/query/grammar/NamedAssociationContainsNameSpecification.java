/*
 * Copyright 2011-2012 Niclas Hedhman.
 * Copyright 2014 Paul Merlin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * ied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.qi4j.api.query.grammar;

import java.util.function.Predicate;
import org.qi4j.api.association.NamedAssociation;
import org.qi4j.api.composite.Composite;

/**
 * NamedAssociation Contains Specification.
 */
public class NamedAssociationContainsNameSpecification<T>
    implements Predicate<Composite>
{
    private final NamedAssociationFunction<T> namedAssociationFunction;
    private final String name;

    public NamedAssociationContainsNameSpecification( NamedAssociationFunction<T> namedAssociationFunction, String name )
    {
        this.namedAssociationFunction = namedAssociationFunction;
        this.name = name;
    }

    public NamedAssociationFunction<T> namedAssociation()
    {
        return namedAssociationFunction;
    }

    public String name()
    {
        return name;
    }

    @Override
    public boolean test( Composite item )
    {
        NamedAssociation<T> collection = namedAssociationFunction.apply( item );
        if( collection == null )
        {
            return false;
        }
        return collection.containsName( name );
    }

    @Override
    public String toString()
    {
        return namedAssociationFunction + " contains name:" + name;
    }
}
