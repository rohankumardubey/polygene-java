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
package org.qi4j.sample.dcicargo.sample_b.context.interaction.handling.inspection.event;

import java.time.Instant;
import org.qi4j.api.injection.scope.This;
import org.qi4j.api.mixin.Mixins;
import org.qi4j.api.value.ValueBuilder;
import org.qi4j.sample.dcicargo.sample_b.context.interaction.handling.inspection.exception.InspectionException;
import org.qi4j.sample.dcicargo.sample_b.context.interaction.handling.inspection.exception.InspectionFailedException;
import org.qi4j.sample.dcicargo.sample_b.data.structure.cargo.Cargo;
import org.qi4j.sample.dcicargo.sample_b.data.structure.cargo.RouteSpecification;
import org.qi4j.sample.dcicargo.sample_b.data.structure.delivery.Delivery;
import org.qi4j.sample.dcicargo.sample_b.data.structure.handling.HandlingEvent;
import org.qi4j.sample.dcicargo.sample_b.data.structure.itinerary.Itinerary;
import org.qi4j.sample.dcicargo.sample_b.data.structure.location.Location;
import org.qi4j.sample.dcicargo.sample_b.infrastructure.dci.Context;
import org.qi4j.sample.dcicargo.sample_b.infrastructure.dci.RoleMixin;

import static org.qi4j.sample.dcicargo.sample_b.data.structure.delivery.RoutingStatus.*;
import static org.qi4j.sample.dcicargo.sample_b.data.structure.delivery.TransportStatus.CLAIMED;
import static org.qi4j.sample.dcicargo.sample_b.data.structure.handling.HandlingEventType.CLAIM;

/**
 * Inspect Claimed Cargo (subfunction use case)
 *
 * NOTE: We don't throw any misrouted/misdirected exceptions even though the cargo might not have
 * followed the original itinerary. The cargo has been delivered at destination and claimed by
 * Cargo Owner, so we're happy no matter how it got there.
 */
public class InspectClaimedCargo extends Context
{
    DeliveryInspectorRole deliveryInspector;

    HandlingEvent claimEvent;
    Location claimLocation;

    RouteSpecification routeSpecification;
    Itinerary itinerary;
    Integer itineraryProgressIndex;

    public InspectClaimedCargo( Cargo cargo, HandlingEvent handlingEvent )
    {
        deliveryInspector = rolePlayer( DeliveryInspectorRole.class, cargo );

        claimEvent = handlingEvent;
        claimLocation = claimEvent.location().get();

        routeSpecification = cargo.routeSpecification().get();
        itinerary = cargo.itinerary().get();

        // Before handling
        itineraryProgressIndex = cargo.delivery().get().itineraryProgressIndex().get();
    }

    public void inspect()
        throws InspectionException
    {
        // Pre-conditions
        if( claimEvent == null || !claimEvent.handlingEventType().get().equals( CLAIM ) )
        {
            throw new InspectionFailedException( "Can only inspect claimed cargo." );
        }

        deliveryInspector.inspectClaimedCargo();
    }

    @Mixins( DeliveryInspectorRole.Mixin.class )
    public interface DeliveryInspectorRole
    {
        void setContext( InspectClaimedCargo context );

        void inspectClaimedCargo()
            throws InspectionException;

        class Mixin
            extends RoleMixin<InspectClaimedCargo>
            implements DeliveryInspectorRole
        {
            @This
            Cargo cargo;

            Delivery newDelivery;

            public void inspectClaimedCargo()
                throws InspectionException
            {
                // Step 1 - Collect known delivery data

                ValueBuilder<Delivery> newDeliveryBuilder = vbf.newValueBuilder( Delivery.class );
                newDelivery = newDeliveryBuilder.prototype();
                newDelivery.timestamp().set( Instant.now() );
                newDelivery.lastHandlingEvent().set( c.claimEvent );
                newDelivery.transportStatus().set( CLAIMED );
                newDelivery.isUnloadedAtDestination().set( false ); // Why not true if claimed in final destination?

                // Claim is end of delivery cycle
                newDelivery.nextHandlingEvent().set( null );

                // Step 2 - Determine that cargo was routed (for internal reference)

                if( c.itinerary == null )
                {
                    newDelivery.routingStatus().set( NOT_ROUTED );
                    newDelivery.eta().set( null );
                    newDelivery.itineraryProgressIndex().set( 0 );
                }
                else if( !c.routeSpecification.isSatisfiedBy( c.itinerary ) )
                {
                    newDelivery.routingStatus().set( MISROUTED );
                    newDelivery.eta().set( null );
                    newDelivery.itineraryProgressIndex().set( 0 );
                }
                else
                {
                    newDelivery.routingStatus().set( ROUTED );
                    newDelivery.eta().set( c.itinerary.eta() );
                    newDelivery.itineraryProgressIndex().set( c.itineraryProgressIndex );
                }

                // Step 3 - Determine that cargo was on track according to itinerary (for internal reference)

                if( newDelivery.routingStatus().get().equals( ROUTED ) )
                {
                    newDelivery.isMisdirected()
                        .set( !c.claimLocation.equals( c.itinerary.lastLeg().unloadLocation().get() ) );
                }
                else
                {
                    newDelivery.isMisdirected().set( false );
                }

                // Step 4 - Save cargo delivery snapshot

                cargo.delivery().set( newDeliveryBuilder.newInstance() );
            }
        }
    }
}