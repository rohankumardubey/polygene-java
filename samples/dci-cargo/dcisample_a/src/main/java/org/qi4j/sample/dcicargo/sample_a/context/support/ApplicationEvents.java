/*
 * Copyright 2011 Marc Grue.
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
package org.qi4j.sample.dcicargo.sample_a.context.support;

import java.text.SimpleDateFormat;
import org.qi4j.api.mixin.Mixins;
import org.qi4j.api.service.ServiceComposite;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.cargo.Cargo;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.handling.HandlingEvent;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.voyage.Voyage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * "Messaging"
 *
 * This interface provides a way to let other parts of the system know about events that have occurred.
 * For now, no notifications are passed on but could be implemented here using for example JMS.
 */
@Mixins( ApplicationEvents.SynchronousApplicationEventsStub.class )
public interface ApplicationEvents
    extends ServiceComposite
{
    /**
     * A cargo has been handled.
     *
     * @param registeredHandlingEvent handling event
     */
    void cargoWasHandled( HandlingEvent registeredHandlingEvent );

    /**
     * A cargo has been misdirected.
     *
     * @param cargo cargo
     */
    void cargoWasMisdirected( Cargo cargo );

    /**
     * A cargo has arrived at its final destination.
     *
     * @param cargo cargo
     */
    void cargoHasArrived( Cargo cargo );

    /**
     * A handling event registration attempt is received.
     *
     * @param attempt Handling event registration attempt
     */
    void receivedHandlingEventRegistrationAttempt( RegisterHandlingEventAttemptDTO attempt );

    void unsuccessfulHandlingEventRegistration( RegisterHandlingEventAttemptDTO attempt );

    // Default implementation (could be substituted in assembly)
    abstract class SynchronousApplicationEventsStub
        implements ApplicationEvents
    {
        String id;
        String time;
        String type;
        String unloc;
        String loc;
        String voyage;

        Logger logger = LoggerFactory.getLogger( ApplicationEvents.class );

        public void cargoWasHandled( HandlingEvent registeredHandlingEvent )
        {
            id = registeredHandlingEvent.trackingId().get().id().get();
            time = registeredHandlingEvent.completionTime().get().toString();
            type = registeredHandlingEvent.handlingEventType().get().name();
            unloc = registeredHandlingEvent.location().get().getCode();
            loc = registeredHandlingEvent.location().get().name().get();
            Voyage voy = registeredHandlingEvent.voyage().get();
            voyage = voy != null ? ", voyage " + voy.voyageNumber().get().number().get() : "";

            logger.info( "  Cargo '" + id + "' was handled " + time
                         + " (" + type + " in " + loc + "/" + unloc + voyage + ")." );
        }

        public void cargoWasMisdirected( Cargo cargo )
        {
            id = cargo.trackingId().get().id().get();
            loc = cargo.delivery().get().lastKnownLocation().get().name().get();
            unloc = cargo.delivery().get().lastKnownLocation().get().toString();
            type = cargo.delivery().get().lastHandlingEvent().get().handlingEventType().get().name();

            logger.info( "  Unexpected " + type + " of cargo '" + id
                         + "' in " + loc + "/" + unloc + "." );
            logger.info( "  NOTIFICATION TO CUSTOMER: Please re-route misdirected cargo '"
                         + id + "' (now in " + loc + ")." );
        }

        public void cargoHasArrived( Cargo cargo )
        {
            id = cargo.trackingId().get().id().get();
            loc = cargo.delivery().get().lastKnownLocation().get().name().get();
            unloc = cargo.delivery().get().lastKnownLocation().get().toString();

            logger.info( "  Cargo '" + id + "' has arrived in " + loc + "/" + unloc + "." );
            logger.info( "  NOTIFICATION TO CUSTOMER: Please claim cargo '" + id + "' in " + loc + "." );
        }

        public void receivedHandlingEventRegistrationAttempt( RegisterHandlingEventAttemptDTO attempt )
        {
            time = attempt.completionTime().get().toString();
            id = parse( attempt.trackingIdString().get() );
            type = parse( attempt.eventTypeString().get() );
            unloc = parse( attempt.unLocodeString().get() );
            voyage = parse( attempt.voyageNumberString().get() );

            logger.info( "  Handling registration attempt received ("
                         + time + ", " + id + ", " + type + ", " + unloc + ", " + voyage + ")." );
        }

        public void unsuccessfulHandlingEventRegistration( RegisterHandlingEventAttemptDTO attempt )
        {
            id = attempt.trackingIdString().get();
            type = attempt.eventTypeString().get();
            unloc = attempt.unLocodeString().get();

            logger.info( "  Unsuccessful handling event registration for cargo '"
                         + id + "' (handling event '" + type + "' in '" + unloc + "')." );
        }

        private String parse( String str )
        {
            return str == null ? "null" : str;
        }

    }
}
