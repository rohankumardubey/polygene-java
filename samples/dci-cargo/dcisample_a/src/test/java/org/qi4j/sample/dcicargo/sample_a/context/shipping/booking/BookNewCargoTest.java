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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.qi4j.api.unitofwork.UnitOfWork;
import org.qi4j.sample.dcicargo.sample_a.bootstrap.test.TestApplication;
import org.qi4j.sample.dcicargo.sample_a.context.support.FoundNoRoutesException;
import org.qi4j.sample.dcicargo.sample_a.data.entity.CargosEntity;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.cargo.Cargo;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.cargo.Cargos;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.cargo.TrackingId;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.delivery.Delivery;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.delivery.RoutingStatus;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.delivery.TransportStatus;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.handling.HandlingEventType;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.itinerary.Itinerary;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.location.Location;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

/**
 * Test of Book New Cargo use case.
 *
 * This is a test suite where all steps and deviations in the use case are tested.
 * Some data will carry over from one test to another (all tests run within the same UnitOfWork).
 *
 * Test method names describe the test purpose. The prefix refers to the step in the use case.
 */
public class BookNewCargoTest
      extends TestApplication
{

    private static final Instant TODAY = Instant.now();

    @Before
    public void prepareTest()
        throws Exception
    {
        super.prepareTest();

    }

    @Test( expected = RouteException.class )
    public void deviation2a_OriginAndDestinationSame() throws Exception
    {
        UnitOfWork uow = module.currentUnitOfWork();
        Location HONGKONG = uow.get( Location.class, CNHKG.code().get() );
        Cargos CARGOS = uow.get( Cargos.class, CargosEntity.CARGOS_ID );
        new BookNewCargo( CARGOS, HONGKONG, HONGKONG, day( 17 ) ).book();
    }

    @Test( expected = RouteException.class )
    public void deviation_2b_1_DeadlineInThePastNotAccepted() throws Exception
    {
        UnitOfWork uow = module.currentUnitOfWork();
        Location HONGKONG = uow.get( Location.class, CNHKG.code().get() );
        Location STOCKHOLM = uow.get( Location.class, SESTO.code().get() );
        Cargos CARGOS = uow.get( Cargos.class, CargosEntity.CARGOS_ID );
        new BookNewCargo( CARGOS, HONGKONG, STOCKHOLM, day( -1 ) ).book();
    }

    @Test( expected = RouteException.class )
    public void deviation_2b_2_DeadlineTodayIsTooEarly() throws Exception
    {
        UnitOfWork uow = module.currentUnitOfWork();
        Location HONGKONG = uow.get( Location.class, CNHKG.code().get() );
        Location STOCKHOLM = uow.get( Location.class, SESTO.code().get() );
        Cargos CARGOS = uow.get( Cargos.class, CargosEntity.CARGOS_ID );
        new BookNewCargo( CARGOS, HONGKONG, STOCKHOLM, day( 0 ) ).book();
    }

    @Test
    public void deviation_2b_3_DeadlineTomorrowIsOkay() throws Exception
    {
        UnitOfWork uow = module.currentUnitOfWork();
        Location HONGKONG = uow.get( Location.class, CNHKG.code().get() );
        Location STOCKHOLM = uow.get( Location.class, SESTO.code().get() );
        Cargos CARGOS = uow.get( Cargos.class, CargosEntity.CARGOS_ID );
        new BookNewCargo( CARGOS, HONGKONG, STOCKHOLM, day( 1 ) ).book();
    }

    @Test
    public void step_2_CreateNewCargo() throws Exception
    {
        UnitOfWork uow = module.currentUnitOfWork();
        Location HONGKONG = uow.get( Location.class, CNHKG.code().get() );
        Location STOCKHOLM = uow.get( Location.class, SESTO.code().get() );
        Cargos CARGOS = uow.get( Cargos.class, CargosEntity.CARGOS_ID );
        // Create cargo with valid input from customer
        TrackingId trackingId = new BookNewCargo( CARGOS, HONGKONG, STOCKHOLM, day( 17 ) ).book();

        // Retrieve created cargo from store
        Cargo cargo = uow.get( Cargo.class, trackingId.id().get() );

        // Test cargo data
        assertThat( cargo.trackingId().get(), is( equalTo( trackingId ) ) );
        assertThat( cargo.origin().get(), is( equalTo( HONGKONG ) ) );

        // Test route specification
        assertThat( cargo.routeSpecification().get().destination().get(), is( equalTo( STOCKHOLM ) ) );
        // day(17) here is calculated a few milliseconds after initial day(17), so it will be later...
        assertThat( cargo.routeSpecification().get().arrivalDeadline().get(),  equalTo( day( 17 )  ));

        // (Itinerary is not assigned yet)

        // Test derived delivery snapshot
        Delivery delivery = cargo.delivery().get();
        assertThat( delivery.timestamp().get().isAfter( TODAY ), is( equalTo( true ) ) ); // TODAY is set first
        assertThat( delivery.routingStatus().get(), is( equalTo( RoutingStatus.NOT_ROUTED ) ) );
        assertThat( delivery.transportStatus().get(), is( equalTo( TransportStatus.NOT_RECEIVED ) ) );
        assertThat( delivery.nextExpectedHandlingEvent().get().handlingEventType().get(), is( equalTo( HandlingEventType.RECEIVE ) ) );
        assertThat( delivery.nextExpectedHandlingEvent().get().location().get(), is( equalTo( HONGKONG ) ) );
        assertThat( delivery.nextExpectedHandlingEvent().get().voyage().get(), nullValue() );
        assertThat( delivery.lastHandlingEvent().get(), nullValue() );
        assertThat( delivery.lastKnownLocation().get(), nullValue() );
        assertThat( delivery.currentVoyage().get(), nullValue() );
        assertThat( delivery.eta().get(), nullValue() ); // Is set when itinerary is assigned
        assertThat( delivery.isMisdirected().get(), equalTo( false ) );
        assertThat( delivery.isUnloadedAtDestination().get(), equalTo( false ) );
    }

    @Test( expected = FoundNoRoutesException.class )
    public void deviation_3a_NoRoutesCanBeThatFast() throws Exception
    {
        UnitOfWork uow = module.currentUnitOfWork();
        Location HONGKONG = uow.get( Location.class, CNHKG.code().get() );
        Location STOCKHOLM = uow.get( Location.class, SESTO.code().get() );
        Cargos CARGOS = uow.get( Cargos.class, CargosEntity.CARGOS_ID );
        TrackingId trackingId = new BookNewCargo( CARGOS, HONGKONG, STOCKHOLM, day( 1 ) ).book();
        Cargo cargo = uow.get( Cargo.class, trackingId.id().get() );

        // No routes will be found
        new BookNewCargo( cargo ).routeCandidates();
    }

    @Test
    public void step_3_CalculatePossibleRoutes() throws Exception
    {
        UnitOfWork uow = module.currentUnitOfWork();
        Location HONGKONG = uow.get( Location.class, CNHKG.code().get() );
        Location STOCKHOLM = uow.get( Location.class, SESTO.code().get() );
        Cargos CARGOS = uow.get( Cargos.class, CargosEntity.CARGOS_ID );

        // Create valid cargo
        TrackingId trackingId = new BookNewCargo( CARGOS, HONGKONG, STOCKHOLM, day( 30 ) ).book();
        Cargo cargo = uow.get( Cargo.class, trackingId.id().get() );

        // Step 3 - Find possible routes
        List<Itinerary> routeCandidates = new BookNewCargo( cargo ).routeCandidates();

        // Check possible routes
        for (Itinerary itinerary : routeCandidates)
        {
            assertThat( "First load location equals origin location.",
                        itinerary.firstLeg().loadLocation().get(),
                        is( equalTo( cargo.routeSpecification().get().origin().get() ) ) );
            assertThat( "Last unload location equals destination location.",
                        itinerary.lastLeg().unloadLocation().get(),
                        is( equalTo( cargo.routeSpecification().get().destination().get() ) ) );
            assertThat( "Cargo will be delivered in time.",
                        itinerary.finalArrivalDate().isBefore( cargo.routeSpecification()
                                                                   .get()
                                                                   .arrivalDeadline()
                                                                   .get() ),
                        is( equalTo( true ) ) );
        }
    }

    @Test
    public void step_5_AssignCargoToRoute() throws Exception
    {
        UnitOfWork uow = module.currentUnitOfWork();
        Location HONGKONG = uow.get( Location.class, CNHKG.code().get() );
        Location STOCKHOLM = uow.get( Location.class, SESTO.code().get() );
        Cargos CARGOS = uow.get( Cargos.class, CargosEntity.CARGOS_ID );

        // Create valid cargo
        ZonedDateTime deadline = day( 60 );
        TrackingId trackingId = new BookNewCargo( CARGOS, HONGKONG, STOCKHOLM, deadline ).book();
        Cargo cargo = uow.get( Cargo.class, trackingId.id().get() );

        List<Itinerary> routeCandidates = new BookNewCargo( cargo ).routeCandidates();

        // Get first route found
        // Would normally be found with an Itinerary id from customer selection
        Itinerary itinerary = routeCandidates.get( 0 );

        // Use case step 5 - System assigns cargo to route
        new BookNewCargo( cargo, itinerary ).assignCargoToRoute();

        assertThat( "Itinerary has been assigned to cargo.", itinerary, is( equalTo( cargo.itinerary().get() ) ) );

        // BuildDeliverySnapshot will check if itinerary is valid. No need to check it here.

        // Check values set in new delivery snapshot
        Delivery delivery = cargo.delivery().get();
        assertThat( delivery.routingStatus().get(), is( equalTo( RoutingStatus.ROUTED ) ) );

        // ETA (= Unload time of last Leg) is before Deadline (set in previous test)
        assertTrue( delivery.eta().get().isBefore( deadline ) );
    }
}
