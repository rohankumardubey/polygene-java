/*
 * Copyright 2009-2010 Rickard Öberg AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.qi4j.library.eventsourcing.application.source.helper;

import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.qi4j.api.util.Methods;
import org.qi4j.functional.Iterables;
import org.qi4j.io.Output;
import org.qi4j.io.Receiver;
import org.qi4j.io.Sender;
import org.qi4j.library.eventsourcing.application.api.ApplicationEvent;
import org.qi4j.library.eventsourcing.application.api.TransactionApplicationEvents;
import org.qi4j.library.eventsourcing.application.replay.ApplicationEventPlayer;
import org.qi4j.library.eventsourcing.application.replay.ApplicationEventReplayException;

/**
 * Helper methods for working with Iterables of DomainEvents and UnitOfWorkDomainEventsValue.
 */
public class ApplicationEvents
{
    public static Iterable<ApplicationEvent> events( Iterable<TransactionApplicationEvents> transactions )
    {
        List<Iterable<ApplicationEvent>> events = new ArrayList<Iterable<ApplicationEvent>>();
        for (TransactionApplicationEvents transactionDomain : transactions)
        {
            events.add( transactionDomain.events().get() );
        }

        Iterable<ApplicationEvent>[] iterables = (Iterable<ApplicationEvent>[]) new Iterable[events.size()];
        return Iterables.flatten( events.<Iterable<ApplicationEvent>>toArray( iterables ) );
    }

    public static Iterable<ApplicationEvent> events( TransactionApplicationEvents... transactionDomains )
    {
        List<Iterable<ApplicationEvent>> events = new ArrayList<Iterable<ApplicationEvent>>();
        for (TransactionApplicationEvents transactionDomain : transactionDomains)
        {
            events.add( transactionDomain.events().get() );
        }

        Iterable<ApplicationEvent>[] iterables = (Iterable<ApplicationEvent>[]) new Iterable[events.size()];
        return Iterables.flatten( events.<Iterable<ApplicationEvent>>toArray( iterables ) );
    }

    public static boolean matches( Predicate<ApplicationEvent> specification, Iterable<TransactionApplicationEvents> transactions )
    {
        return Iterables.filter( specification, events( transactions ) ).iterator().hasNext();
    }

    // Common specifications

    public static Predicate<ApplicationEvent> withNames( final Iterable<String> names )
    {
        return new Predicate<ApplicationEvent>()
        {
            @Override
            public boolean test( ApplicationEvent event )
            {
                for (String name : names)
                {
                    if (event.name().get().equals( name ))
                        return true;
                }
                return false;
            }
        };
    }

    public static Predicate<ApplicationEvent> withNames( final String... names )
    {
        return new Predicate<ApplicationEvent>()
        {
            @Override
            public boolean test( ApplicationEvent event )
            {
                for (String name : names)
                {
                    if (event.name().get().equals( name ))
                        return true;
                }
                return false;
            }
        };
    }

    public static Predicate<ApplicationEvent> withNames( final Class eventClass )
    {
        return ApplicationEvents.withNames( Iterables.map( new Function<Method, String>()
        {
            @Override
            public String apply( Method method )
            {
                return method.getName();
            }
        }, Iterables.toList( Methods.METHODS_OF.apply( eventClass ) ) ));
    }

    public static Predicate<ApplicationEvent> afterDate( final ZonedDateTime afterDate )
    {
        return new Predicate<ApplicationEvent>()
        {
            @Override
            public boolean test( ApplicationEvent event )
            {
                return event.on().get().isAfter( afterDate );
            }
        };
    }

    public static Predicate<ApplicationEvent> beforeDate( final ZonedDateTime beforeDate )
    {
        return new Predicate<ApplicationEvent>()
        {
            @Override
            public boolean test( ApplicationEvent event )
            {
                return event.on().get().isBefore( beforeDate );
            }
        };
    }

    public static Predicate<ApplicationEvent> withUsecases( final String... names )
    {
        return new Predicate<ApplicationEvent>()
        {
            @Override
            public boolean test( ApplicationEvent event )
            {
                for (String name : names)
                {
                    if (event.usecase().get().equals( name ))
                        return true;
                }
                return false;
            }
        };
    }

    public static Predicate<ApplicationEvent> paramIs( final String name, final String value )
    {
        return new Predicate<ApplicationEvent>()
        {
            @Override
            public boolean test( ApplicationEvent event )
            {
                return ApplicationEventParameters.getParameter( event, name ).equals( value );
            }
        };
    }

    public static Output<TransactionApplicationEvents, ApplicationEventReplayException> playEvents( final ApplicationEventPlayer player, final Object eventHandler )
    {
        final Predicate<ApplicationEvent> specification = ApplicationEvents.withNames( eventHandler.getClass() );

        return new Output<TransactionApplicationEvents, ApplicationEventReplayException>()
        {
           @Override
           public <SenderThrowableType extends Throwable> void receiveFrom(Sender<? extends TransactionApplicationEvents, SenderThrowableType> sender) throws ApplicationEventReplayException, SenderThrowableType
           {
                sender.sendTo( new Receiver<TransactionApplicationEvents, ApplicationEventReplayException>()
                {
                    @Override
                    public void receive( TransactionApplicationEvents item ) throws ApplicationEventReplayException
                    {
                        for (ApplicationEvent applicationEvent : events( item ))
                        {
                            if (specification.test( applicationEvent ))
                                player.playEvent( applicationEvent, eventHandler );
                        }
                    }
                } );
            }
        };
    }
}
