/*
 * Copyright (c) 2008, Rickard Öberg. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.qi4j.api.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import org.qi4j.api.concern.Concerns;
import org.qi4j.api.mixin.Mixins;
import org.qi4j.api.sideeffect.SideEffects;
import org.qi4j.api.util.Classes;

import static java.util.Arrays.asList;
import static org.qi4j.api.util.Classes.typesOf;

/**
 * Used to declare and access meta-info.
 * <p>
 * <strong>This is effectively an internal class and should not be used directly.</strong>
 * </p>
 * <p>
 * MetaInfo can be set on composites during the assembly phase, a.k.a the bootstrap
 * process. MetaInfo is any additional data that one wishes to associate at the 'class level' instead of instance
 * level of a composite declaration.
 * </p>
 * <p>
 * To set the MetaInfo on a Composite, call the {@code setMetaInfo()} methods on the various composite declaration
 * types, such as;
 * </p>
 * <pre><code>
 * public void assemble( ModuleAssembly module )
 *     throws AssemblyException
 * {
 *     Map&lt;String,String&gt; properties = ...;
 *     module.services( MyService.class ).setMetaInfo( properties );
 * }
 * </code></pre>
 * <p>
 * which can later be retrieved by calling the {@code metaInfo()} method on the composite itself. For the example
 * above that would be;
 * </p>
 * <pre><code>
 * &#64;Mixins(MyServiceMixin.class)
 * public interface MyService extends ServiceComposite
 * {
 *
 * }
 *
 * public abstract class MyServiceMixin
 *     implements MyService
 * {
 *     private Properties props;
 *
 *     public MyServiceMixin()
 *     {
 *         props = metaInfo( Map.class );
 *     }
 * }
 * </code></pre>
 */
public final class MetaInfo
{
    private final static Collection<Class> ignored;

    static
    {
        ignored = new HashSet<>( 4, 0.8f ); // Optimize size used.
        ignored.addAll( asList( Mixins.class, Concerns.class, SideEffects.class ) );
    }

    private final Map<Class<?>, Object> metaInfoMap;

    public MetaInfo()
    {
        metaInfoMap = new LinkedHashMap<>();
    }

    public MetaInfo( MetaInfo metaInfo )
    {
        metaInfoMap = new LinkedHashMap<>();
        metaInfoMap.putAll( metaInfo.metaInfoMap );
    }

    public void set( Object metaInfo )
    {
        if( metaInfo instanceof Annotation )
        {
            Annotation annotation = (Annotation) metaInfo;
            metaInfoMap.put( annotation.annotationType(), metaInfo );
        }
        else
        {
            Class<?> metaInfoclass = metaInfo.getClass();
            Iterable<Type> types = typesOf( metaInfoclass );
            types.forEach( type -> metaInfoMap.put( Classes.RAW_CLASS.apply( type ), metaInfo ) );
        }
    }

    public <T> T get( Class<T> metaInfoType )
    {
        return metaInfoType.cast( metaInfoMap.get( metaInfoType ) );
    }

    public <T> void add( Class<T> infoType, T info )
    {
        metaInfoMap.put( infoType, info );
    }

    public MetaInfo withAnnotations( AnnotatedElement annotatedElement )
    {
        Arrays.stream( annotatedElement.getAnnotations() )
            .filter( annotation -> !ignored.contains( annotation.annotationType() )
                   && get( annotation.annotationType() ) == null )
            .forEach( this::set );
        return this;
    }

    @Override
    public String toString()
    {
        return metaInfoMap.toString();
    }

    public void remove( Class serviceFinderClass )
    {
        metaInfoMap.remove( serviceFinderClass );
    }
}
