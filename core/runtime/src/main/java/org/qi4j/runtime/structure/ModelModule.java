package org.qi4j.runtime.structure;

import java.util.function.Function;
import org.qi4j.api.composite.ModelDescriptor;

/**
 * TODO
 */
public class ModelModule<T extends ModelDescriptor>
{
    public static <T extends ModelDescriptor> Function<T, ModelModule<T>> modelModuleFunction( final ModuleInstance module )
    {
        return new Function<T, ModelModule<T>>()
        {
            @Override
            public ModelModule<T> apply( T model )
            {
                return new ModelModule<>( module, model );
            }
        };
    }

    public static <T extends ModelDescriptor> Function<ModelModule<T>, T> modelFunction()
    {
        return new Function<ModelModule<T>, T>()
        {
            @Override
            public T apply( ModelModule<T> modelModule )
            {
                return modelModule.model();
            }
        };
    }

    private final ModuleInstance module;
    private final T model;

    public ModelModule( ModuleInstance module, T model )
    {
        this.module = module;
        this.model = model;
    }

    public ModuleInstance module()
    {
        return module;
    }

    public T model()
    {
        return model;
    }

    @Override
    public boolean equals( Object o )
    {
        if( this == o )
        {
            return true;
        }
        if( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        ModelModule that = (ModelModule) o;

        if( model != null ? !model.equals( that.model ) : that.model != null )
        {
            return false;
        }

        return !( module != null ? !module.equals( that.module ) : that.module != null );
    }

    @Override
    public int hashCode()
    {
        int result = module != null ? module.hashCode() : 0;
        result = 31 * result + ( model != null ? model.hashCode() : 0 );
        return result;
    }

    @Override
    public String toString()
    {
        return module.name() + ":" + model;
    }
}
