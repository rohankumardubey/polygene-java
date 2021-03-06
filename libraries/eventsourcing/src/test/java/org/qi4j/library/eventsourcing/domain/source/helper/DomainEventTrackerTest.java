/*
 * Copyright (c) 2011, Rickard Öberg. All Rights Reserved.
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

package org.qi4j.library.eventsourcing.domain.source.helper;

import java.io.IOException;
import java.security.Principal;
import java.util.function.Function;
import org.junit.Test;
import org.qi4j.api.activation.ActivatorAdapter;
import org.qi4j.api.activation.Activators;
import org.qi4j.api.common.UseDefaults;
import org.qi4j.api.configuration.Configuration;
import org.qi4j.api.entity.EntityComposite;
import org.qi4j.api.injection.scope.Service;
import org.qi4j.api.injection.scope.This;
import org.qi4j.api.mixin.Mixins;
import org.qi4j.api.property.Property;
import org.qi4j.api.service.ServiceComposite;
import org.qi4j.api.service.ServiceReference;
import org.qi4j.api.unitofwork.UnitOfWork;
import org.qi4j.api.unitofwork.UnitOfWorkCompletionException;
import org.qi4j.api.usecase.UsecaseBuilder;
import org.qi4j.bootstrap.AssemblyException;
import org.qi4j.bootstrap.ImportedServiceDeclaration;
import org.qi4j.bootstrap.ModuleAssembly;
import org.qi4j.io.Output;
import org.qi4j.io.Outputs;
import org.qi4j.io.Transforms;
import org.qi4j.library.eventsourcing.domain.api.DomainEvent;
import org.qi4j.library.eventsourcing.domain.api.DomainEventValue;
import org.qi4j.library.eventsourcing.domain.api.UnitOfWorkDomainEventsValue;
import org.qi4j.library.eventsourcing.domain.factory.CurrentUserUoWPrincipal;
import org.qi4j.library.eventsourcing.domain.factory.DomainEventCreationConcern;
import org.qi4j.library.eventsourcing.domain.factory.DomainEventFactoryService;
import org.qi4j.library.eventsourcing.domain.source.EventSource;
import org.qi4j.library.eventsourcing.domain.source.EventStream;
import org.qi4j.library.eventsourcing.domain.source.memory.MemoryEventStoreService;
import org.qi4j.test.AbstractQi4jTest;
import org.qi4j.test.EntityTestAssembler;

public class DomainEventTrackerTest
    extends AbstractQi4jTest
{
    public void assemble( ModuleAssembly module ) throws AssemblyException
    {
        new EntityTestAssembler(  ).assemble( module );

        module.values( DomainEventValue.class, UnitOfWorkDomainEventsValue.class );
        module.services( MemoryEventStoreService.class );
        module.services( DomainEventFactoryService.class );
        module.importedServices( CurrentUserUoWPrincipal.class ).importedBy( ImportedServiceDeclaration.NEW_OBJECT );
        module.objects( CurrentUserUoWPrincipal.class );

        module.entities( TestEntity.class ).withConcerns( DomainEventCreationConcern.class );

        module.services( EventLoggingService.class ).instantiateOnStartup();
        module.entities( DomainEventTrackerConfiguration.class );
    }

    @Test
    public void testDomainEvent() throws UnitOfWorkCompletionException, IOException
    {
        UnitOfWork uow = module.newUnitOfWork( UsecaseBuilder.newUsecase( "Change description" ));
        uow.setMetaInfo( new Principal()
        {
            public String getName()
            {
                return "administrator";
            }
        });

        TestEntity entity = uow.newEntity( TestEntity.class );
        entity.changeDescription( "New description" );
        uow.complete();

        try
        {
            Thread.sleep( 5000 );
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    @Mixins( TestEntity.Mixin.class )
    public interface TestEntity
        extends EntityComposite
    {
        @UseDefaults
        Property<String> description();

        @DomainEvent
        void changeDescription( String newName );

        abstract class Mixin
            implements TestEntity
        {
            public void changeDescription( String newName )
            {
                description().set( newName );
            }
        }
    }

    @Mixins(EventLoggingService.Mixin.class)
    @Activators( EventLoggingService.Activator.class )
    public interface EventLoggingService
        extends ServiceComposite, Configuration<DomainEventTrackerConfiguration>
    {
        
        void startTracker();
        
        void stopTracker();
        
        public class Activator
                extends ActivatorAdapter<ServiceReference<EventLoggingService>>
        {

            @Override
            public void afterActivation( ServiceReference<EventLoggingService> activated )
                    throws Exception
            {
                activated.get().startTracker();
            }

            @Override
            public void beforePassivation( ServiceReference<EventLoggingService> passivating )
                    throws Exception
            {
                passivating.get().stopTracker();
            }

        }
        
        public abstract class Mixin implements EventLoggingService
        {
            DomainEventTracker tracker;

            @This
            Configuration<DomainEventTrackerConfiguration> config;

            @Service
            EventStream eventStream;

            @Service
            EventSource eventSource;

            public void startTracker()
            {
                config.get().enabled().set( true );

               Output<UnitOfWorkDomainEventsValue,RuntimeException> map = Transforms.map( new Function<UnitOfWorkDomainEventsValue, String>()
                       {
                           public String apply( UnitOfWorkDomainEventsValue unitOfWorkDomainEventsValue )
                           {
                               return unitOfWorkDomainEventsValue.toString();
                           }
                       }, Outputs.systemOut() );
               tracker = new DomainEventTracker(eventStream, eventSource, config, map);

                tracker.start();
            }

            public void stopTracker()
            {
                tracker.stop();
            }
        }
    }
}
