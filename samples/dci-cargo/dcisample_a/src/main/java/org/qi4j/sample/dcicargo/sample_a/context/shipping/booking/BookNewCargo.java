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
package org.qi4j.sample.dcicargo.sample_a.context.shipping.booking;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import org.qi4j.api.common.Optional;
import org.qi4j.api.injection.scope.Service;
import org.qi4j.api.injection.scope.This;
import org.qi4j.api.mixin.Mixins;
import org.qi4j.api.value.ValueBuilder;
import org.qi4j.api.value.ValueBuilderFactory;
import org.qi4j.sample.dcicargo.sample_a.context.support.FoundNoRoutesException;
import org.qi4j.sample.dcicargo.sample_a.context.support.RoutingService;
import org.qi4j.sample.dcicargo.sample_a.data.entity.CargoEntity;
import org.qi4j.sample.dcicargo.sample_a.data.entity.CargosEntity;
import org.qi4j.sample.dcicargo.sample_a.data.entity.LocationEntity;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.cargo.Cargo;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.cargo.Cargos;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.cargo.RouteSpecification;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.cargo.TrackingId;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.delivery.Delivery;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.itinerary.Itinerary;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.location.Location;
import org.qi4j.sample.dcicargo.sample_a.infrastructure.dci.Context;
import org.qi4j.sample.dcicargo.sample_a.infrastructure.dci.RoleMixin;

/**
 * Book New Cargo use case
 */
public class BookNewCargo extends Context
{
    // ROLES ---------------------------------------------------------------------

    // Methodful Roles
    private CargoFactoryRole cargoFactory;
    private RoutingFacadeRole routingFacade;

    // Methodless Roles
    private Location origin;
    private Location destination;
    private ZonedDateTime arrivalDeadline;
    private Itinerary itinerary;

    // CONTEXT CONSTRUCTORS ------------------------------------------------------

    public BookNewCargo( Cargos cargos,
                         Location origin,
                         Location destination,
                         ZonedDateTime arrivalDeadline
    )
        throws Exception
    {
        cargoFactory = rolePlayer( CargoFactoryRole.class, cargos );
        this.origin = origin;
        this.destination = destination;
        this.arrivalDeadline = arrivalDeadline;
    }

    public BookNewCargo( Cargo cargo )
    {
        routingFacade = rolePlayer( RoutingFacadeRole.class, cargo );
    }

    public BookNewCargo( Cargo cargo, Itinerary itinerary )
    {
        routingFacade = rolePlayer( RoutingFacadeRole.class, cargo );
        this.itinerary = itinerary;
    }

    // Constructor proxies for communication layer

    public BookNewCargo( String originId, String destinationId, ZonedDateTime deadline )
        throws Exception
    {
        this( loadEntity( CargosEntity.class, CargosEntity.CARGOS_ID ),
              loadEntity( Location.class, originId ),
              loadEntity( Location.class, destinationId ),
              deadline );
    }

    public BookNewCargo( String trackingIdString )
    {
        this( loadEntity( CargoEntity.class, trackingIdString ) );
    }

    public BookNewCargo( String trackingIdString, Itinerary itinerary )
    {
        this( loadEntity( Cargo.class, trackingIdString ), itinerary );
    }

    // INTERACTIONS --------------------------------------------------------------

    public TrackingId book()
    {
        return cargoFactory.createCargo( null );
    }

    public TrackingId createCargo( String trackingIdString )
    {
        return cargoFactory.createCargo( trackingIdString );
    }

    public void changeDestination( String destination )
    {
        routingFacade.changeDestination( loadEntity( LocationEntity.class, destination ) );
    }

    public List<Itinerary> routeCandidates()
        throws FoundNoRoutesException
    {
        return routingFacade.routeCandidates();
    }

    public void assignCargoToRoute()
    {
        routingFacade.assignCargoToRoute();
    }

    // METHODFUL ROLE IMPLEMENTATIONS --------------------------------------------

    @Mixins(CargoFactoryRole.Mixin.class)
    public interface CargoFactoryRole
    {
        void setContext( BookNewCargo context );

        TrackingId createCargo( @Optional String trackingIdString );

        class Mixin
            extends RoleMixin<BookNewCargo>
            implements CargoFactoryRole
        {
            @This
            Cargos cargos;

            public TrackingId createCargo( String trackingIdString )
            {
                // New route specification
                RouteSpecification routeSpec = context.buildRouteSpecification(
                    vbf, context.origin, context.destination, context.arrivalDeadline );

                // Build delivery snapshot from route specification
                Delivery delivery = new BuildDeliverySnapshot( routeSpec ).get();

                // Create cargo
                Cargo cargo = cargos.createCargo( routeSpec, delivery, trackingIdString );

                return cargo.trackingId().get();
            }
        }
    }

    @Mixins(RoutingFacadeRole.Mixin.class)
    public interface RoutingFacadeRole
    {
        void setContext( BookNewCargo context );

        List<Itinerary> routeCandidates()
            throws FoundNoRoutesException;

        void assignCargoToRoute();

        void changeDestination( Location destination );

        class Mixin
            extends RoleMixin<BookNewCargo>
            implements RoutingFacadeRole
        {
            @This
            Cargo cargo;

            @Service
            RoutingService routingService;

            // Use case step 3 - system calculates possible routes
            public List<Itinerary> routeCandidates()
                throws FoundNoRoutesException  // Deviation 3a
            {
                return routingService.fetchRoutesForSpecification( cargo.routeSpecification().get() );
            }

            public void assignCargoToRoute()
            {
                cargo.itinerary().set( context.itinerary );

                if( cargo.delivery().get().lastHandlingEvent().get() != null )
                {
                    // We treat subsequent route assignments as reroutes of misdirected cargo
                    cargo.delivery().get().lastHandlingEvent().get().wasUnexpected().set( true );
                }

                // Build delivery snapshot with updated itinerary
                cargo.delivery().set( new BuildDeliverySnapshot( cargo ).get() );
            }

            public void changeDestination( Location newDestination )
            {
                Location currentOrigin = cargo.routeSpecification().get().origin().get();
                ZonedDateTime currentDeadline = cargo.routeSpecification().get().arrivalDeadline().get();

                RouteSpecification newRouteSpecification =
                    context.buildRouteSpecification( vbf, currentOrigin, newDestination, currentDeadline );

                cargo.routeSpecification().set( newRouteSpecification );

                // Build new delivery snapshot with updated route specification
                cargo.delivery().set( new BuildDeliverySnapshot( cargo ).get() );
            }
        }
    }

    public RouteSpecification buildRouteSpecification(
        ValueBuilderFactory vbf, Location origin, Location destination, ZonedDateTime deadline
    )
    {
        if( origin == destination )
        {
            throw new RouteException( "Origin location can't be same as destination location." );
        }

        if( deadline == null )
        {
            throw new RouteException( "Arrival deadline cannot be null." );
        }

        if( deadline.isBefore( ZonedDateTime.now().plusDays( 1 ).with( LocalTime.MIDNIGHT ) ) )
        {
            throw new RouteException( "Arrival deadline is in the past or Today: " + deadline );
        }

        ValueBuilder<RouteSpecification> routeSpec = vbf.newValueBuilder( RouteSpecification.class );
        routeSpec.prototype().origin().set( origin );
        routeSpec.prototype().destination().set( destination );
        routeSpec.prototype().arrivalDeadline().set( deadline );
        return routeSpec.newInstance();
    }
}