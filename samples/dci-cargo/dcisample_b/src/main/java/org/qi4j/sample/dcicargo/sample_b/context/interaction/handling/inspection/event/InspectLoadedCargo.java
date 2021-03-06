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

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Random;
import org.qi4j.api.injection.scope.This;
import org.qi4j.api.mixin.Mixins;
import org.qi4j.api.value.ValueBuilder;
import org.qi4j.sample.dcicargo.sample_b.context.interaction.handling.inspection.exception.CargoHijackedException;
import org.qi4j.sample.dcicargo.sample_b.context.interaction.handling.inspection.exception.CargoMisdirectedException;
import org.qi4j.sample.dcicargo.sample_b.context.interaction.handling.inspection.exception.CargoMisroutedException;
import org.qi4j.sample.dcicargo.sample_b.context.interaction.handling.inspection.exception.CargoNotRoutedException;
import org.qi4j.sample.dcicargo.sample_b.context.interaction.handling.inspection.exception.InspectionException;
import org.qi4j.sample.dcicargo.sample_b.context.interaction.handling.inspection.exception.InspectionFailedException;
import org.qi4j.sample.dcicargo.sample_b.context.interaction.handling.inspection.exception.UnexpectedCarrierException;
import org.qi4j.sample.dcicargo.sample_b.data.structure.cargo.Cargo;
import org.qi4j.sample.dcicargo.sample_b.data.structure.cargo.RouteSpecification;
import org.qi4j.sample.dcicargo.sample_b.data.structure.delivery.Delivery;
import org.qi4j.sample.dcicargo.sample_b.data.structure.delivery.NextHandlingEvent;
import org.qi4j.sample.dcicargo.sample_b.data.structure.delivery.RoutingStatus;
import org.qi4j.sample.dcicargo.sample_b.data.structure.handling.HandlingEvent;
import org.qi4j.sample.dcicargo.sample_b.data.structure.itinerary.Itinerary;
import org.qi4j.sample.dcicargo.sample_b.data.structure.itinerary.Leg;
import org.qi4j.sample.dcicargo.sample_b.data.structure.location.Location;
import org.qi4j.sample.dcicargo.sample_b.data.structure.voyage.CarrierMovement;
import org.qi4j.sample.dcicargo.sample_b.data.structure.voyage.Voyage;
import org.qi4j.sample.dcicargo.sample_b.infrastructure.dci.Context;
import org.qi4j.sample.dcicargo.sample_b.infrastructure.dci.RoleMixin;

import static org.qi4j.sample.dcicargo.sample_b.data.structure.delivery.RoutingStatus.MISROUTED;
import static org.qi4j.sample.dcicargo.sample_b.data.structure.delivery.RoutingStatus.NOT_ROUTED;
import static org.qi4j.sample.dcicargo.sample_b.data.structure.delivery.TransportStatus.ONBOARD_CARRIER;
import static org.qi4j.sample.dcicargo.sample_b.data.structure.delivery.TransportStatus.UNKNOWN;
import static org.qi4j.sample.dcicargo.sample_b.data.structure.handling.HandlingEventType.LOAD;
import static org.qi4j.sample.dcicargo.sample_b.data.structure.handling.HandlingEventType.UNLOAD;

/**
 * Inspect Loaded Cargo (subfunction use case)
 *
 * This is one the variations of the
 * {@link org.qi4j.sample.dcicargo.sample_b.context.interaction.handling.inspection.InspectCargoDeliveryStatus} use
 * case.
 *
 * When the cargo is loaded onto some unexpected carrier we need to find out where that carrier
 * is going so that we can make a new route specification starting from that location. The Cargo Owner
 * is then requested to re-route the cargo in order to get the cargo back on track.
 */
public class InspectLoadedCargo extends Context
{
    DeliveryInspectorRole deliveryInspector;

    HandlingEvent loadEvent;
    Location loadLocation;
    Voyage voyage;

    RouteSpecification routeSpecification;
    Itinerary itinerary;
    Integer itineraryProgressIndex;
    RoutingStatus oldRoutingStatus;

    public InspectLoadedCargo( Cargo cargo, HandlingEvent handlingEvent )
    {
        deliveryInspector = rolePlayer( DeliveryInspectorRole.class, cargo );

        loadEvent = handlingEvent;
        loadLocation = loadEvent.location().get();
        voyage = loadEvent.voyage().get();

        routeSpecification = cargo.routeSpecification().get();
        itinerary = cargo.itinerary().get();

        // Before handling
        itineraryProgressIndex = cargo.delivery().get().itineraryProgressIndex().get();
        oldRoutingStatus = cargo.delivery().get().routingStatus().get();
    }

    public void inspect()
        throws InspectionException
    {
        // Pre-conditions
        if( loadEvent == null || !loadEvent.handlingEventType().get().equals( LOAD ) )
        {
            throw new InspectionFailedException( "Can only inspect loaded cargo." );
        }

        deliveryInspector.inspectLoadedCargo();
    }

    @Mixins(DeliveryInspectorRole.Mixin.class)
    public interface DeliveryInspectorRole
    {
        void setContext( InspectLoadedCargo context );

        void inspectLoadedCargo()
            throws InspectionException;

        abstract class Mixin extends RoleMixin<InspectLoadedCargo> implements DeliveryInspectorRole
        {
            @This
            Cargo cargo;

            Delivery newDelivery;

            public void inspectLoadedCargo()
                throws InspectionException
            {
                // Step 1 - Collect known delivery data

                ValueBuilder<Delivery> newDeliveryBuilder = vbf.newValueBuilder( Delivery.class );
                newDelivery = newDeliveryBuilder.prototype();
                newDelivery.timestamp().set( Instant.now() );
                newDelivery.lastHandlingEvent().set( c.loadEvent );
                newDelivery.transportStatus().set( ONBOARD_CARRIER );
                newDelivery.isUnloadedAtDestination().set( false );

                // Step 2 - Determine next unload from carrier

                CarrierMovement carrierMovement = c.voyage.carrierMovementDepartingFrom( c.loadLocation );
                if( carrierMovement == null )
                {
                    // Unexpected carrier movement
                    newDelivery.routingStatus().set( c.oldRoutingStatus );
                    newDelivery.eta().set( null );
                    newDelivery.itineraryProgressIndex().set( 0 );
                    newDelivery.isMisdirected().set( true );
                    cargo.delivery().set( newDeliveryBuilder.newInstance() );
                    throw new UnexpectedCarrierException( c.loadEvent );
                }

                // Estimate carrier arrival time
                ZonedDateTime estimatedArrivalDate = carrierMovement.arrivalTime().get();
                if( c.loadEvent.completionTime().get().isAfter( carrierMovement.departureTime().get() ) )
                {
                    Duration duration = Duration.between( carrierMovement.departureTime().get(),
                                                          carrierMovement.arrivalTime().get() );
                    estimatedArrivalDate = c.loadEvent.completionTime().get().plus(duration);
                    // ... We could notify cargo owner if we already now know that we will miss the next ship
                }

                ValueBuilder<NextHandlingEvent> nextHandlingEvent = vbf.newValueBuilder( NextHandlingEvent.class );
                nextHandlingEvent.prototype().handlingEventType().set( UNLOAD );
                nextHandlingEvent.prototype().location().set( carrierMovement.arrivalLocation().get() );
                nextHandlingEvent.prototype().time().set( estimatedArrivalDate );
                nextHandlingEvent.prototype().voyage().set( c.voyage );
                newDelivery.nextHandlingEvent().set( nextHandlingEvent.newInstance() );

                // Step 3 - Verify cargo is routed

                if( c.itinerary == null )
                {
                    newDelivery.routingStatus().set( NOT_ROUTED );
                    newDelivery.eta().set( null );
                    newDelivery.itineraryProgressIndex().set( 0 );
                    cargo.delivery().set( newDeliveryBuilder.newInstance() );
                    throw new CargoNotRoutedException( c.loadEvent );
                }
                if( !c.routeSpecification.isSatisfiedBy( c.itinerary ) )
                {
                    newDelivery.routingStatus().set( MISROUTED );
                    newDelivery.eta().set( null );
                    newDelivery.itineraryProgressIndex().set( 0 );
                    cargo.delivery().set( newDeliveryBuilder.newInstance() );
                    throw new CargoMisroutedException( c.loadEvent, c.routeSpecification, c.itinerary );
                }
                newDelivery.routingStatus().set( RoutingStatus.ROUTED );
                newDelivery.eta().set( c.itinerary.eta() );
                newDelivery.itineraryProgressIndex().set( c.itineraryProgressIndex );

                // Step 4 - Verify cargo is on track

                Leg plannedCarrierMovement = c.itinerary.leg( c.itineraryProgressIndex );

                // Unexpected internal state
                if( plannedCarrierMovement == null )
                {
                    // We should always know the current itinerary leg
                    throw new InspectionFailedException( "Itinerary progress index '" + c.itineraryProgressIndex + "' is invalid!" );
                }

                // Unexpected load location - Cargo can't travel in time!
                // Either previous or current location is wrong - only investigation can clarify...
                if( !plannedCarrierMovement.loadLocation().get().equals( c.loadLocation ) )
                {
                    newDelivery.isMisdirected().set( true );
                    newDelivery.nextHandlingEvent().set( null );
                    cargo.delivery().set( newDeliveryBuilder.newInstance() );
                    throw new CargoMisdirectedException( c.loadEvent, "Itinerary expected load in "
                                                                      + plannedCarrierMovement.loadLocation()
                        .get()
                        .getString() );
                }

                // Unexpected carrier
                if( !plannedCarrierMovement.voyage().get().equals( c.voyage ) )
                {
                    newDelivery.isMisdirected().set( true );
                    cargo.delivery().set( newDeliveryBuilder.newInstance() );

                    // ...Expected arrival location - should we accept this?
                    if( plannedCarrierMovement.unloadLocation()
                        .get()
                        .equals( carrierMovement.arrivalLocation().get() ) )
                    {
                        throw new CargoMisdirectedException( c.loadEvent, c.itinerary, "Cargo is heading to expected arrival location "
                                                                                       + plannedCarrierMovement.unloadLocation()
                            .get() + " but on unexpected voyage "
                                                                                       + c.voyage
                            .toString() + ". Notify shipper to unload unexpected cargo in next port." );
                    }

                    throw new CargoMisdirectedException( c.loadEvent, c.itinerary, "Itinerary expected load onto voyage "
                                                                                   + plannedCarrierMovement.voyage()
                        .get() );
                }

                // Unexpected carrier destination
                if( !plannedCarrierMovement.unloadLocation().get().equals( carrierMovement.arrivalLocation().get() ) )
                {
                    newDelivery.isMisdirected().set( true );
                    cargo.delivery().set( newDeliveryBuilder.newInstance() );
                    throw new CargoMisdirectedException( c.loadEvent, "Itinerary expects voyage " + c.voyage.toString()
                                                                      + " to arrive in " + plannedCarrierMovement.unloadLocation()
                        .get() + " but carrier is now going to "
                                                                      + carrierMovement.arrivalLocation().get() );
                }

                // True exception
                if( ( c.loadLocation.getCode().equals( "SOMGQ" ) && new Random().nextInt( 100 ) < 20 ) ||
                    ( carrierMovement.arrivalLocation()
                          .get()
                          .getCode()
                          .equals( "SOMGQ" ) && new Random().nextInt( 100 ) < 15 ) )
                {
                    newDelivery.transportStatus().set( UNKNOWN );
                    newDelivery.isMisdirected().set( false );
                    cargo.delivery().set( newDeliveryBuilder.newInstance() );
                    throw new CargoHijackedException( c.loadEvent );
                }

                // Cargo is on track
                newDelivery.isMisdirected().set( false );

                // Step 5 - Save cargo delivery snapshot

                cargo.delivery().set( newDeliveryBuilder.newInstance() );
            }
        }
    }
}