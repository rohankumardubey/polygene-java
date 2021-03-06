/*
 * Copyright (c) 2007-2011, Rickard Öberg. All Rights Reserved.
 * Copyright (c) 2008-2013, Niclas Hedhman. All Rights Reserved.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package org.qi4j.runtime.bootstrap;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import org.qi4j.api.common.MetaInfo;
import org.qi4j.api.common.Optional;
import org.qi4j.api.common.QualifiedName;
import org.qi4j.api.common.UseDefaults;
import org.qi4j.api.common.Visibility;
import org.qi4j.api.composite.InvalidCompositeException;
import org.qi4j.api.concern.Concerns;
import org.qi4j.api.constraint.Constraint;
import org.qi4j.api.constraint.ConstraintDeclaration;
import org.qi4j.api.constraint.Constraints;
import org.qi4j.api.constraint.Name;
import org.qi4j.api.entity.Lifecycle;
import org.qi4j.api.injection.scope.State;
import org.qi4j.api.injection.scope.This;
import org.qi4j.api.mixin.Initializable;
import org.qi4j.api.mixin.Mixins;
import org.qi4j.api.property.GenericPropertyInfo;
import org.qi4j.api.property.Immutable;
import org.qi4j.api.property.Property;
import org.qi4j.api.sideeffect.SideEffects;
import org.qi4j.api.type.HasTypes;
import org.qi4j.api.util.Annotations;
import org.qi4j.api.util.Classes;
import org.qi4j.api.util.Fields;
import org.qi4j.bootstrap.StateDeclarations;
import org.qi4j.functional.ForEach;
import org.qi4j.functional.HierarchicalVisitorAdapter;
import org.qi4j.functional.Iterables;
import org.qi4j.functional.Visitor;
import org.qi4j.runtime.composite.AbstractConstraintModel;
import org.qi4j.runtime.composite.CompositeConstraintModel;
import org.qi4j.runtime.composite.CompositeMethodModel;
import org.qi4j.runtime.composite.CompositeMethodsModel;
import org.qi4j.runtime.composite.ConcernModel;
import org.qi4j.runtime.composite.ConcernsModel;
import org.qi4j.runtime.composite.ConstraintModel;
import org.qi4j.runtime.composite.ConstraintsModel;
import org.qi4j.runtime.composite.GenericSpecification;
import org.qi4j.runtime.composite.MixinModel;
import org.qi4j.runtime.composite.MixinsModel;
import org.qi4j.runtime.composite.SideEffectModel;
import org.qi4j.runtime.composite.SideEffectsModel;
import org.qi4j.runtime.composite.StateModel;
import org.qi4j.runtime.composite.ValueConstraintsInstance;
import org.qi4j.runtime.composite.ValueConstraintsModel;
import org.qi4j.runtime.injection.DependencyModel;
import org.qi4j.runtime.property.PropertiesModel;
import org.qi4j.runtime.property.PropertyModel;

import static java.util.Arrays.asList;
import static org.qi4j.api.util.Annotations.hasAnnotation;
import static org.qi4j.api.util.Annotations.isType;
import static org.qi4j.api.util.Annotations.type;
import static org.qi4j.api.util.Classes.classHierarchy;
import static org.qi4j.api.util.Classes.interfacesOf;
import static org.qi4j.api.util.Classes.isAssignableFrom;
import static org.qi4j.api.util.Classes.typeOf;
import static org.qi4j.api.util.Classes.typesOf;
import static org.qi4j.api.util.Classes.wrapperClass;
import static org.qi4j.functional.Iterables.addAll;
import static org.qi4j.functional.Iterables.cast;
import static org.qi4j.functional.Iterables.empty;
import static org.qi4j.functional.Iterables.filter;
import static org.qi4j.functional.Iterables.first;
import static org.qi4j.functional.Iterables.flatten;
import static org.qi4j.functional.Iterables.flattenIterables;
import static org.qi4j.functional.Iterables.iterable;
import static org.qi4j.functional.Iterables.map;
import static org.qi4j.functional.Iterables.matchesAny;
import static org.qi4j.functional.Iterables.toList;
import static org.qi4j.functional.Specifications.and;
import static org.qi4j.functional.Specifications.in;
import static org.qi4j.functional.Specifications.not;
import static org.qi4j.functional.Specifications.or;
import static org.qi4j.functional.Specifications.translate;

/**
 * Declaration of a Composite.
 */
public abstract class CompositeAssemblyImpl
    implements HasTypes
{
    protected List<Class<?>> concerns = new ArrayList<>();
    protected List<Class<?>> sideEffects = new ArrayList<>();
    protected List<Class<?>> mixins = new ArrayList<>();
    protected List<Class<?>> types = new ArrayList<>();
    protected MetaInfo metaInfo = new MetaInfo();
    protected Visibility visibility = Visibility.module;

    protected boolean immutable;
    protected PropertiesModel propertiesModel;
    protected StateModel stateModel;
    protected MixinsModel mixinsModel;
    protected CompositeMethodsModel compositeMethodsModel;
    private AssemblyHelper helper;
    protected StateDeclarations stateDeclarations;

    protected Set<String> registeredStateNames = new HashSet<>();

    public CompositeAssemblyImpl( Class<?> mainType )
    {
        types.add( mainType );
    }

    @Override
    public Iterable<Class<?>> types()
    {
        return types;
    }

    protected StateModel createStateModel()
    {
        return new StateModel( propertiesModel );
    }

    protected MixinsModel createMixinsModel()
    {
        return new MixinsModel();
    }

    protected void buildComposite( AssemblyHelper helper,
                                   StateDeclarations stateDeclarations
    )
    {
        this.stateDeclarations = stateDeclarations;
        this.helper = helper;
        for( Class<?> compositeType : types )
        {
            metaInfo = new MetaInfo( metaInfo ).withAnnotations( compositeType );
            addAnnotationsMetaInfo( compositeType, metaInfo );
        }

        immutable = metaInfo.get( Immutable.class ) != null;
        propertiesModel = new PropertiesModel();
        stateModel = createStateModel();
        mixinsModel = createMixinsModel();
        compositeMethodsModel = new CompositeMethodsModel( mixinsModel );

        // Implement composite methods
        ArrayList<Type> allTypes = getTypes( this.types );
        Iterable<Class<? extends Constraint<?, ?>>> constraintClasses = constraintDeclarations( getTypes( this.types ) );
        Iterable<Class<?>> concernClasses = flatten( concerns, concernDeclarations( allTypes ) );
        Iterable<Class<?>> sideEffectClasses = flatten( sideEffects, sideEffectDeclarations( allTypes ) );
        Iterable<Class<?>> mixinClasses = flatten( mixins, mixinDeclarations( this.types ) );
        implementMixinType( types, constraintClasses, concernClasses, sideEffectClasses, mixinClasses );

        // Add state from methods and fields
        addState( constraintClasses );
    }

    protected void addAnnotationsMetaInfo( Class<?> type, MetaInfo compositeMetaInfo )
    {
        Class[] declaredInterfaces = type.getInterfaces();
        for( int i = declaredInterfaces.length - 1; i >= 0; i-- )
        {
            addAnnotationsMetaInfo( declaredInterfaces[ i], compositeMetaInfo );
        }
        compositeMetaInfo.withAnnotations( type );
    }

    protected void implementMixinType( Iterable<? extends Class<?>> types,
                                       Iterable<Class<? extends Constraint<?, ?>>> constraintClasses,
                                       Iterable<Class<?>> concernClasses,
                                       Iterable<Class<?>> sideEffectClasses,
                                       Iterable<Class<?>> mixinClasses
    )
    {
        Set<Class<?>> thisDependencies = new HashSet<>();
        for( Class<?> mixinType : types )
        {
            for( Method method : mixinType.getMethods() )
            {
                if( !compositeMethodsModel.isImplemented( method )
                    && !Proxy.class.equals( method.getDeclaringClass().getSuperclass() )
                    && !Proxy.class.equals( method.getDeclaringClass() )
                    && !Modifier.isStatic( method.getModifiers() ) )
                {
                    MixinModel mixinModel = implementMethod( method, mixinClasses );
                    ConcernsModel concernsModel = concernsFor(
                        method,
                        mixinModel.mixinClass(),
                        Iterables.<Class<?>>flatten( concernDeclarations( mixinModel.mixinClass() ),
                                                     concernClasses )
                    );
                    SideEffectsModel sideEffectsModel = sideEffectsFor(
                        method,
                        mixinModel.mixinClass(),
                        Iterables.<Class<?>>flatten( sideEffectDeclarations( mixinModel.mixinClass() ),
                                                     sideEffectClasses )
                    );
                    method.setAccessible( true );
                    ConstraintsModel constraints = constraintsFor(
                        method,
                        Iterables.<Class<? extends Constraint<?, ?>>>flatten( constraintDeclarations( mixinModel.mixinClass() ),
                                                                              constraintClasses )
                    );
                    CompositeMethodModel methodComposite = new CompositeMethodModel(
                        method,
                        constraints,
                        concernsModel,
                        sideEffectsModel,
                        mixinsModel
                    );

                    // Implement @This references
                    Iterable<Class<?>> map = map( new DependencyModel.InjectionTypeFunction(),
                                                  filter( new DependencyModel.ScopeSpecification( This.class ),
                                                          methodComposite.dependencies() ) );
                    Iterable<Class<?>> map1 = map( new DependencyModel.InjectionTypeFunction(),
                                                   filter( new DependencyModel.ScopeSpecification( This.class ),
                                                           mixinModel.dependencies() ) );
                    @SuppressWarnings( "unchecked" )
                    Iterable<Class<?>> filter = filter(
                        not( in( Initializable.class, Lifecycle.class, InvocationHandler.class ) ),
                        map( Classes.RAW_CLASS, interfacesOf( mixinModel.mixinClass() ) )
                    );
                    Iterable<? extends Class<?>> flatten = flatten( map, map1, filter );
                    addAll( thisDependencies, flatten );

                    compositeMethodsModel.addMethod( methodComposite );
                }
            }
            // Add type to set of mixin types
            mixinsModel.addMixinType( mixinType );
        }

        // Implement all @This dependencies that were found
        for( Class<?> thisDependency : thisDependencies )
        {
            // Add additional declarations from the @This type
            Iterable<Class<? extends Constraint<?, ?>>> typeConstraintClasses = flatten(
                constraintClasses,
                constraintDeclarations( thisDependency ) );
            Iterable<Class<?>> typeConcernClasses = flatten(
                concernClasses,
                concernDeclarations( thisDependency ) );
            Iterable<Class<?>> typeSideEffectClasses = flatten(
                sideEffectClasses,
                sideEffectDeclarations( thisDependency ) );
            Iterable<Class<?>> typeMixinClasses = flatten(
                mixinClasses,
                mixinDeclarations( thisDependency ) );

            @SuppressWarnings( "unchecked" )
            Iterable<? extends Class<?>> singleton = iterable( thisDependency );
            implementMixinType( singleton, typeConstraintClasses, typeConcernClasses, typeSideEffectClasses, typeMixinClasses );
        }
    }

    @SuppressWarnings( "raw" )
    protected MixinModel implementMethod( Method method, Iterable<Class<?>> mixinDeclarations )
    {
        MixinModel implementationModel = mixinsModel.mixinFor( method );
        if( implementationModel != null )
        {
            return implementationModel;
        }
        Class mixinClass = findTypedImplementation( method, mixinDeclarations );
        if( mixinClass != null )
        {
            return implementMethodWithClass( method, mixinClass );
        }

        // Check generic implementations
        mixinClass = findGenericImplementation( method, mixinDeclarations );
        if( mixinClass != null )
        {
            return implementMethodWithClass( method, mixinClass );
        }

        throw new InvalidCompositeException( "No implementation found for method \n    " + method.toGenericString()
                                             + "\nin\n    " + types );
    }

    @SuppressWarnings( {"raw", "unchecked"} )
    private Class findTypedImplementation( final Method method, Iterable<Class<?>> mixins )
    {
        // Check if mixinClass implements the method. If so, check if the mixinClass is generic or if the filter passes.
        // If a mixinClass is both generic AND non-generic at the same time, then the filter applies to the non-generic
        // side only.
        Predicate<Class<?>> appliesToSpec = new Predicate<Class<?>>()
        {
            @Override
            public boolean test( Class<?> item )
            {
                return helper.appliesTo( item, method, types, item );
            }
        };
        return first( filter( and( isAssignableFrom( method.getDeclaringClass() ),
                                   or( GenericSpecification.INSTANCE, appliesToSpec ) ),
                              mixins ) );
    }

    @SuppressWarnings( "unchecked" )
    private Class<?> findGenericImplementation( final Method method, Iterable<Class<?>> mixins )
    {
        // Check if mixinClass is generic and the applies-to filter passes
        return first( filter( and( GenericSpecification.INSTANCE, new Predicate<Class<?>>()
        {
            @Override
            public boolean test( Class<?> item )
            {
                return helper.appliesTo( item, method, types, item );
            }
        } ), mixins ) );
    }

    private MixinModel implementMethodWithClass( Method method, Class mixinClass )
    {
        MixinModel mixinModel = mixinsModel.getMixinModel( mixinClass );
        if( mixinModel == null )
        {
            mixinModel = helper.getMixinModel( mixinClass );
            mixinsModel.addMixinModel( mixinModel );
        }

        mixinsModel.addMethodMixin( method, mixinModel );

        return mixinModel;
    }

    protected void addState( final Iterable<Class<? extends Constraint<?, ?>>> constraintClasses )
    {
        // Add method state
        compositeMethodsModel.accept( new HierarchicalVisitorAdapter<Object, Object, RuntimeException>()
        {
            @Override
            public boolean visitEnter( Object visited )
                throws RuntimeException
            {
                if( visited instanceof CompositeMethodModel )
                {
                    CompositeMethodModel methodModel = (CompositeMethodModel) visited;
                    if( methodModel.method().getParameterTypes().length == 0 )
                    {
                        addStateFor( methodModel.method(), constraintClasses );
                    }

                    return false;
                }

                return super.visitEnter( visited );
            }
        } );

        // Add field state
        mixinsModel.accept( new HierarchicalVisitorAdapter<Object, Object, RuntimeException>()
        {
            @Override
            public boolean visitEnter( Object visited )
                throws RuntimeException
            {
                if( visited instanceof MixinModel )
                {
                    MixinModel model = (MixinModel) visited;
                    Visitor<Field, RuntimeException> addState = new Visitor<Field, RuntimeException>()
                    {
                        @Override
                        public boolean visit( Field visited )
                            throws RuntimeException
                        {
                            addStateFor( visited, constraintClasses );
                            return true;
                        }
                    };
                    ForEach.forEach( Fields.FIELDS_OF.apply( model.mixinClass() ) ).
                        filter( Annotations.hasAnnotation( State.class ) ).
                        visit( addState );
                    return false;
                }
                return super.visitEnter( visited );
            }
        } );
    }

    protected void addStateFor( AccessibleObject accessor,
                                Iterable<Class<? extends Constraint<?, ?>>> constraintClasses
    )
    {
        String stateName = QualifiedName.fromAccessor( accessor ).name();

        if( registeredStateNames.contains( stateName ) )
        {
            return; // Skip already registered names
        }

        if( Property.class.isAssignableFrom( Classes.RAW_CLASS.apply( typeOf( accessor ) ) ) )
        {
            propertiesModel.addProperty( newPropertyModel( accessor, constraintClasses ) );
            registeredStateNames.add( stateName );
        }
    }

    protected PropertyModel newPropertyModel( AccessibleObject accessor,
                                              Iterable<Class<? extends Constraint<?, ?>>> constraintClasses
    )
    {
        Iterable<Annotation> annotations = Annotations.findAccessorAndTypeAnnotationsIn( accessor );
        boolean optional = first( filter( isType( Optional.class ), annotations ) ) != null;
        ValueConstraintsModel valueConstraintsModel = constraintsFor(
            annotations,
            GenericPropertyInfo.propertyTypeOf( accessor ),
            ( (Member) accessor ).getName(),
            optional,
            constraintClasses,
            accessor );
        ValueConstraintsInstance valueConstraintsInstance = null;
        if( valueConstraintsModel.isConstrained() )
        {
            valueConstraintsInstance = valueConstraintsModel.newInstance();
        }
        MetaInfo metaInfo = stateDeclarations.metaInfoFor( accessor );
        Object initialValue = stateDeclarations.initialValueOf( accessor );
        boolean useDefaults = metaInfo.get( UseDefaults.class ) != null || stateDeclarations.useDefaults( accessor );
        boolean immutable = this.immutable || metaInfo.get( Immutable.class ) != null;
        PropertyModel propertyModel = new PropertyModel(
            accessor,
            immutable,
            useDefaults,
            valueConstraintsInstance,
            metaInfo,
            initialValue );
        return propertyModel;
    }

    // Model
    private ConstraintsModel constraintsFor( Method method,
                                             Iterable<Class<? extends Constraint<?, ?>>> constraintClasses
    )
    {
        List<ValueConstraintsModel> parameterConstraintModels = Collections.emptyList();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Type[] parameterTypes = method.getGenericParameterTypes();
        boolean constrained = false;
        for( int i = 0; i < parameterAnnotations.length; i++ )
        {
            Annotation[] parameterAnnotation = parameterAnnotations[i];

            Name nameAnnotation = (Name) first( filter( isType( Name.class ), iterable( parameterAnnotation ) ) );
            String name = nameAnnotation == null ? "param" + ( i + 1 ) : nameAnnotation.value();

            boolean optional = first( filter( isType( Optional.class ), iterable( parameterAnnotation ) ) ) != null;
            ValueConstraintsModel parameterConstraintsModel = constraintsFor(
                asList( parameterAnnotation ),
                parameterTypes[i],
                name,
                optional,
                constraintClasses,
                method );
            if( parameterConstraintsModel.isConstrained() )
            {
                constrained = true;
            }

            if( parameterConstraintModels.isEmpty() )
            {
                parameterConstraintModels = new ArrayList<>();
            }
            parameterConstraintModels.add( parameterConstraintsModel );
        }

        if( !constrained )
        {
            return new ConstraintsModel( Collections.<ValueConstraintsModel>emptyList() );
        }
        else
        {
            return new ConstraintsModel( parameterConstraintModels );
        }
    }

    protected ValueConstraintsModel constraintsFor(
        Iterable<Annotation> constraintAnnotations,
        Type valueType,
        String name,
        boolean optional,
        Iterable<Class<? extends Constraint<?, ?>>> constraintClasses,
        AccessibleObject accessor
    )
    {
        valueType = wrapperClass( valueType );

        List<AbstractConstraintModel> constraintModels = new ArrayList<>();
        nextConstraint:
        for( Annotation constraintAnnotation : filter( translate( type(), hasAnnotation( ConstraintDeclaration.class ) ),
                                                       constraintAnnotations ) )
        {
            // Check composite declarations first
            Class<? extends Annotation> annotationType = constraintAnnotation.annotationType();
            for( Class<? extends Constraint<?, ?>> constraint : constraintClasses )
            {
                if( helper.appliesTo( constraint, annotationType, valueType ) )
                {
                    constraintModels.add( new ConstraintModel( constraintAnnotation, constraint ) );
                    continue nextConstraint;
                }
            }

            // Check the annotation itself
            Constraints constraints = annotationType.getAnnotation( Constraints.class );
            if( constraints != null )
            {
                for( Class<? extends Constraint<?, ?>> constraintClass : constraints.value() )
                {
                    if( helper.appliesTo( constraintClass, annotationType, valueType ) )
                    {
                        constraintModels.add( new ConstraintModel( constraintAnnotation, constraintClass ) );
                        continue nextConstraint;
                    }
                }
            }

            // No implementation found!
            // Check if if it's a composite constraints
            Iterable<Annotation> annotations = iterable( annotationType.getAnnotations() );
            if( matchesAny( translate( type(), hasAnnotation( ConstraintDeclaration.class ) ), annotations ) )
            {
                ValueConstraintsModel valueConstraintsModel = constraintsFor(
                    annotations,
                    valueType,
                    name,
                    optional,
                    constraintClasses,
                    accessor );
                CompositeConstraintModel compositeConstraintModel = new CompositeConstraintModel(
                    constraintAnnotation,
                    valueConstraintsModel );
                constraintModels.add( compositeConstraintModel );
                continue nextConstraint;
            }

            throw new InvalidCompositeException(
                "Cannot find implementation of constraint @"
                + annotationType.getSimpleName()
                + " for "
                + valueType
                + " in method "
                + ( (Member) accessor ).getName()
                + " of composite " + types );
        }

        return new ValueConstraintsModel( constraintModels, name, optional );
    }

    private ConcernsModel concernsFor( Method method,
                                       Class<?> mixinClass,
                                       Iterable<Class<?>> concernClasses
    )
    {
        List<ConcernModel> concernsFor = new ArrayList<>();
        for( Class<?> concern : concernClasses )
        {
            if( helper.appliesTo( concern, method, types, mixinClass ) )
            {
                concernsFor.add( helper.getConcernModel( concern ) );
            }
            else
            {
                // Lookup method in mixin
                if( !InvocationHandler.class.isAssignableFrom( mixinClass ) )
                {
                    try
                    {
                        Method mixinMethod = mixinClass.getMethod( method.getName(), method.getParameterTypes() );
                        if( helper.appliesTo( concern, mixinMethod, types, mixinClass ) )
                        {
                            concernsFor.add( helper.getConcernModel( concern ) );
                        }
                    }
                    catch( NoSuchMethodException e )
                    {
                        // Ignore
                    }
                }
            }
        }

        // Check annotations on method that have @Concerns annotations themselves
        for( Annotation annotation : method.getAnnotations() )
        {
            @SuppressWarnings( "raw" )
            Concerns concerns = annotation.annotationType().getAnnotation( Concerns.class );
            if( concerns != null )
            {
                for( Class<?> concern : concerns.value() )
                {
                    if( helper.appliesTo( concern, method, types, mixinClass ) )
                    {
                        concernsFor.add( helper.getConcernModel( concern ) );
                    }
                }
            }
        }

        if( concernsFor.isEmpty() )
        {
            return ConcernsModel.EMPTY_CONCERNS;
        }
        else
        {
            return new ConcernsModel( concernsFor );
        }
    }

    private SideEffectsModel sideEffectsFor( Method method,
                                             Class<?> mixinClass,
                                             Iterable<Class<?>> sideEffectClasses
    )
    {
        List<SideEffectModel> sideEffectsFor = new ArrayList<>();
        for( Class<?> sideEffect : sideEffectClasses )
        {
            if( helper.appliesTo( sideEffect, method, types, mixinClass ) )
            {
                sideEffectsFor.add( helper.getSideEffectModel( sideEffect ) );
            }
            else
            {
                // Lookup method in mixin
                if( !InvocationHandler.class.isAssignableFrom( mixinClass ) )
                {
                    try
                    {
                        Method mixinMethod = mixinClass.getMethod( method.getName(), method.getParameterTypes() );
                        if( helper.appliesTo( sideEffect, mixinMethod, types, mixinClass ) )
                        {
                            sideEffectsFor.add( helper.getSideEffectModel( sideEffect ) );
                        }
                    }
                    catch( NoSuchMethodException e )
                    {
                        // Ignore
                    }
                }
            }
        }

        if( sideEffectsFor.isEmpty() )
        {
            return SideEffectsModel.EMPTY_SIDEEFFECTS;
        }
        else
        {
            return new SideEffectsModel( sideEffectsFor );
        }
    }

    @SuppressWarnings( "unchecked" )
    private Iterable<Class<? extends Constraint<?, ?>>> constraintDeclarations( Class<?> type )
    {
        ArrayList<Type> allTypes = getTypes( type );
        return constraintDeclarations( allTypes );
    }

    private Iterable<Class<? extends Constraint<?, ?>>> constraintDeclarations( ArrayList<Type> allTypes )
    {
        // Find all constraints and flatten them into an iterable
        Function<Type, Iterable<Class<? extends Constraint<?, ?>>>> function = new Function<Type, Iterable<Class<? extends Constraint<?, ?>>>>()
        {
            @Override
            public Iterable<Class<? extends Constraint<?, ?>>> apply( Type type )
            {
                Constraints constraints = Annotations.annotationOn( type, Constraints.class );
                if( constraints == null )
                {
                    return empty();
                }
                else
                {
                    return iterable( constraints.value() );
                }
            }
        };
        Iterable<Class<? extends Constraint<?, ?>>> flatten = flattenIterables( map( function, allTypes ) );
        return toList( flatten );
    }

    @SuppressWarnings( "unchecked" )
    private Iterable<Class<?>> concernDeclarations( Class<?> type )
    {
        Iterable<? extends Class<?>> iterable = iterable( type );
        return concernDeclarations( getTypes( iterable ) );
    }

    private Iterable<Class<?>> concernDeclarations( ArrayList<Type> allTypes )
    {
        // Find all concerns and flattern them into an iterable
        Function<Type, Iterable<Class<?>>> function = new Function<Type, Iterable<Class<?>>>()
        {
            @Override
            public Iterable<Class<?>> apply( Type type )
            {
                Concerns concerns = Annotations.annotationOn( type, Concerns.class );
                if( concerns == null )
                {
                    return empty();
                }
                else
                {
                    return iterable( concerns.value() );
                }
            }
        };
        Iterable<Class<?>> flatten = flattenIterables( map( function, allTypes ) );
        return toList( flatten );
    }

    @SuppressWarnings( "unchecked" )
    protected Iterable<Class<?>> sideEffectDeclarations( Class<?> type )
    {
        Iterable<? extends Class<?>> iterable = iterable( type );
        return sideEffectDeclarations( getTypes( iterable ) );
    }

    protected Iterable<Class<?>> sideEffectDeclarations( ArrayList<Type> allTypes )
    {
        // Find all side-effects and flattern them into an iterable
        Function<Type, Iterable<Class<?>>> function = new Function<Type, Iterable<Class<?>>>()
        {
            @Override
            public Iterable<Class<?>> apply( Type type )
            {
                SideEffects sideEffects = Annotations.annotationOn( type, SideEffects.class );
                if( sideEffects == null )
                {
                    return empty();
                }
                else
                {
                    return iterable( sideEffects.value() );
                }
            }
        };
        Iterable<Class<?>> flatten = flattenIterables( map( function, allTypes ) );
        return toList( flatten );
    }

    private ArrayList<Type> getTypes( Class<?> type )
    {
        Iterable<? extends Class<?>> iterable = iterable( type );
        return getTypes( iterable );
    }

    private ArrayList<Type> getTypes( Iterable<? extends Class<?>> typess )
    {
        // Find side-effect declarations
        ArrayList<Type> allTypes = new ArrayList<>();
        for( Class<?> type : typess )
        {
            Iterable<Type> types;
            if( type.isInterface() )
            {
                types = typesOf( type );
            }
            else
            {
                types = cast( classHierarchy( type ) );
            }
            addAll( allTypes, types );
        }
        return allTypes;
    }

    @SuppressWarnings( "unchecked" )
    protected Iterable<Class<?>> mixinDeclarations( Class<?> type )
    {
        Iterable<? extends Class<?>> iterable = iterable( type );
        return mixinDeclarations( iterable );
    }

    protected Iterable<Class<?>> mixinDeclarations( Iterable<? extends Class<?>> typess )
    {
        // Find mixin declarations
        ArrayList<Type> allTypes = new ArrayList<>();
        for( Class<?> type : typess )
        {
            Iterable<Type> types = typesOf( type );
            addAll( allTypes, types );
        }

        // Find all mixins and flattern them into an iterable
        Function<Type, Iterable<Class<?>>> function = new Function<Type, Iterable<Class<?>>>()
        {
            @Override
            public Iterable<Class<?>> apply( Type type )
            {
                Mixins mixins = Annotations.annotationOn( type, Mixins.class );
                if( mixins == null )
                {
                    return empty();
                }
                else
                {
                    return iterable( mixins.value() );
                }
            }
        };
        Iterable<Class<?>> flatten = flattenIterables( map( function, allTypes ) );
        return toList( flatten );
    }
}
