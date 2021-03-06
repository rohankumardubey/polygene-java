/**
 *
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

package org.qi4j.library.eventsourcing.domain.source.helper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.qi4j.io.Output;
import org.qi4j.io.Receiver;
import org.qi4j.io.Sender;
import org.qi4j.library.eventsourcing.domain.api.UnitOfWorkDomainEventsValue;

/**
 * UnitOfWork handling router. Add specification-&gt;receiver routes. When a UnitOfWorkEDomainEventsValue comes in
 * the router will ask each specification if it matches, and if so, delegate to the
 * receiver. If no routes match, delegate to the default receiver.
 */
public class UnitOfWorkRouter<T extends Throwable>
    implements Output<UnitOfWorkDomainEventsValue, T>
{
    private Map<Predicate<UnitOfWorkDomainEventsValue>, Receiver<UnitOfWorkDomainEventsValue, T>> routes = new LinkedHashMap<Predicate<UnitOfWorkDomainEventsValue>, Receiver<UnitOfWorkDomainEventsValue, T>>(  );

    private Receiver<UnitOfWorkDomainEventsValue, T> defaultReceiver = new Receiver<UnitOfWorkDomainEventsValue, T>()
    {
        @Override
        public void receive( UnitOfWorkDomainEventsValue item ) throws T
        {
            // Do nothing;
        }
    };

    public UnitOfWorkRouter route( Predicate<UnitOfWorkDomainEventsValue> specification, Receiver<UnitOfWorkDomainEventsValue, T> receiver)
    {
        routes.put(specification, receiver);

        return this;
    }

    public UnitOfWorkRouter defaultReceiver(Receiver<UnitOfWorkDomainEventsValue, T> defaultReceiver)
    {
        this.defaultReceiver = defaultReceiver;
        return this;
    }

    @Override
    public <SenderThrowableType extends Throwable> void receiveFrom( Sender<? extends UnitOfWorkDomainEventsValue, SenderThrowableType> sender ) throws T, SenderThrowableType
    {
        sender.sendTo( new Receiver<UnitOfWorkDomainEventsValue, T>()
        {
            @Override
            public void receive( UnitOfWorkDomainEventsValue item ) throws T
            {
                for( Map.Entry<Predicate<UnitOfWorkDomainEventsValue>, Receiver<UnitOfWorkDomainEventsValue, T>> specificationReceiverEntry : routes.entrySet() )
                {
                    if (specificationReceiverEntry.getKey().test( item ))
                    {
                        specificationReceiverEntry.getValue().receive( item );
                        return;
                    }
                }

                // No match, use default
                defaultReceiver.receive( item );
            }
        });
    }
}
