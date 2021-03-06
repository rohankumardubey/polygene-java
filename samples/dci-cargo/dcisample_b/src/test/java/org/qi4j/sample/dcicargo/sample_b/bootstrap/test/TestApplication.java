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
package org.qi4j.sample.dcicargo.sample_b.bootstrap.test;

import java.time.ZonedDateTime;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.qi4j.api.entity.EntityBuilder;
import org.qi4j.api.service.ServiceReference;
import org.qi4j.api.structure.Application;
import org.qi4j.api.structure.Module;
import org.qi4j.api.unitofwork.UnitOfWork;
import org.qi4j.api.usecase.Usecase;
import org.qi4j.api.usecase.UsecaseBuilder;
import org.qi4j.api.value.ValueBuilder;
import org.qi4j.bootstrap.Energy4Java;
import org.qi4j.sample.dcicargo.sample_b.bootstrap.sampledata.BaseData;
import org.qi4j.sample.dcicargo.sample_b.data.factory.RouteSpecificationFactoryService;
import org.qi4j.sample.dcicargo.sample_b.data.structure.cargo.Cargo;
import org.qi4j.sample.dcicargo.sample_b.data.structure.cargo.RouteSpecification;
import org.qi4j.sample.dcicargo.sample_b.data.structure.delivery.Delivery;
import org.qi4j.sample.dcicargo.sample_b.data.structure.delivery.NextHandlingEvent;
import org.qi4j.sample.dcicargo.sample_b.data.structure.delivery.RoutingStatus;
import org.qi4j.sample.dcicargo.sample_b.data.structure.delivery.TransportStatus;
import org.qi4j.sample.dcicargo.sample_b.data.structure.handling.HandlingEvent;
import org.qi4j.sample.dcicargo.sample_b.data.structure.handling.HandlingEventType;
import org.qi4j.sample.dcicargo.sample_b.data.structure.itinerary.Itinerary;
import org.qi4j.sample.dcicargo.sample_b.data.structure.location.Location;
import org.qi4j.sample.dcicargo.sample_b.data.structure.tracking.TrackingId;
import org.qi4j.sample.dcicargo.sample_b.data.structure.voyage.Schedule;
import org.qi4j.sample.dcicargo.sample_b.data.structure.voyage.Voyage;
import org.qi4j.sample.dcicargo.sample_b.data.structure.voyage.VoyageNumber;
import org.qi4j.sample.dcicargo.sample_b.infrastructure.dci.Context;
import org.qi4j.sample.dcicargo.sample_b.infrastructure.testing.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Base class for testing Context Interactions
 */
public class TestApplication
    extends BaseData
{
    // Logger for sub classes
    protected Logger logger = LoggerFactory.getLogger( getClass() );

    protected static Application app;

    protected static RouteSpecificationFactoryService routeSpecFactory;

    final protected static ZonedDateTime TODAY = ZonedDateTime.now();
    final protected static ZonedDateTime DAY1 = TODAY.plusDays( 1 );
    final protected static ZonedDateTime DAY2 = TODAY.plusDays( 2 );
    final protected static ZonedDateTime DAY3 = TODAY.plusDays( 3 );
    final protected static ZonedDateTime DAY4 = TODAY.plusDays( 4 );
    final protected static ZonedDateTime DAY5 = TODAY.plusDays( 5 );
    final protected static ZonedDateTime DAY6 = TODAY.plusDays( 6 );
    final protected static ZonedDateTime DAY7 = TODAY.plusDays( 7 );
    final protected static ZonedDateTime DAY8 = TODAY.plusDays( 8 );
    final protected static ZonedDateTime DAY9 = TODAY.plusDays( 9 );
    final protected static ZonedDateTime DAY10 = TODAY.plusDays( 10 );
    final protected static ZonedDateTime DAY11 = TODAY.plusDays( 11 );
    final protected static ZonedDateTime DAY12 = TODAY.plusDays( 12 );
    final protected static ZonedDateTime DAY13 = TODAY.plusDays( 13 );
    final protected static ZonedDateTime DAY14 = TODAY.plusDays( 14 );
    final protected static ZonedDateTime DAY15 = TODAY.plusDays( 15 );
    final protected static ZonedDateTime DAY16 = TODAY.plusDays( 16 );
    final protected static ZonedDateTime DAY17 = TODAY.plusDays( 17 );
    final protected static ZonedDateTime DAY18 = TODAY.plusDays( 18 );
    final protected static ZonedDateTime DAY19 = TODAY.plusDays( 19 );
    final protected static ZonedDateTime DAY20 = TODAY.plusDays( 20 );
    final protected static ZonedDateTime DAY21 = TODAY.plusDays( 21 );
    final protected static ZonedDateTime DAY22 = TODAY.plusDays( 22 );
    final protected static ZonedDateTime DAY23 = TODAY.plusDays( 23 );
    final protected static ZonedDateTime DAY24 = TODAY.plusDays( 24 );
    final protected static ZonedDateTime DAY25 = TODAY.plusDays( 25 );

    protected static Voyage V201;
    protected static Voyage V202;
    protected static Voyage V203;
    protected static Voyage V204;
    protected static Voyage V205;

    final protected static Voyage noVoyage = null;
    final protected static boolean notArrived = false;
    final protected static boolean arrived = true;
    final protected static boolean directed = false;
    final protected static boolean misdirected = true;
    final protected static ZonedDateTime unknownETA = null;
    final protected static ZonedDateTime noSpecificDate = null;
    final protected static Integer leg1 = 0;
    final protected static Integer leg2 = 1;
    final protected static Integer leg3 = 2;
    final protected static Integer leg4 = 3;
    final protected static Integer leg5 = 4;
    final protected static Integer unknownLeg = 0;
    final protected static NextHandlingEvent unknownNextHandlingEvent = null;

    protected ZonedDateTime deadline;
    protected ZonedDateTime arrival;
    protected RouteSpecification routeSpec;
    protected RouteSpecification newRouteSpec;
    protected Delivery delivery;
    protected Cargo cargo;
    protected TrackingId trackingId;
    protected String trackingIdString;
    protected Itinerary itinerary;
    protected Itinerary wrongItinerary;
    protected HandlingEvent handlingEvent;
    protected Location MELBOURNE;
    protected Location HANGZHOU;
    protected Location HONGKONG;
    protected Location SHANGHAI;
    protected Location HAMBURG;
    protected Location STOCKHOLM;
    protected Location DALLAS;
    protected Location NEWYORK;
    protected Location MOGADISHU;
    protected Location ROTTERDAM;
    protected Location HELSINKI;
    protected Location GOTHENBURG;
    protected Location TOKYO;
    protected Location CHICAGO;

    protected TestApplication()
    {
        super( findHostingModule() );
    }

    @BeforeClass
    public static void setup()
        throws Exception
    {
        System.out.println( "\n@@@@@@@@@@@  TEST SUITE  @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@" );
        app = new Energy4Java().newApplication( new TestAssembler() );
        app.activate();
        Context.prepareContextBaseClass( findHostingModule() );
    }

    // Allow to test message output from exceptions after they have been thrown
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    // Print current test method name to console
    @Rule
    public TestName name = new TestName();

    @Before
    public void prepareTest()
        throws Exception
    {
        logger.info( name.getMethodName() );
        Usecase usecase = UsecaseBuilder.newUsecase( "Usecase:" + name );
        UnitOfWork uow = module.newUnitOfWork( usecase );
        populateTestData();

        ServiceReference<RouteSpecificationFactoryService> routeSpecFactoryServiceRef =
            module.findService( RouteSpecificationFactoryService.class );
        routeSpecFactory = routeSpecFactoryServiceRef.get();

        // Separate test suites in console output
        System.out.println();
    }

    @After
    public void concludeTests()
    {
        UnitOfWork uow = module.currentUnitOfWork();
        if( uow != null )
        {
            uow.discard();
        }
        if( module.isUnitOfWorkActive() )
        {
            while( module.isUnitOfWorkActive() )
            {
                uow = module.currentUnitOfWork();
                if( uow.isOpen() )
                {
                    System.err.println( "UnitOfWork not cleaned up:" + uow.usecase().name() );
                    uow.discard();
                }
                else
                {
                    throw new InternalError( "I have seen a case where a UoW is on the stack, but not opened. First is: " + uow
                        .usecase()
                        .name() );
                }
            }
            new Exception( "UnitOfWork not properly cleaned up" ).printStackTrace();
        }
    }

    @AfterClass
    public static void terminateApplication()
        throws Exception
    {
        if( app != null )
        {
            app.passivate();
        }
    }

    public void assertMessage( Exception e, String msg )
    {
        String message = "\nEXPECTED: " + msg + "\nGOT: " + e.getMessage();
        assertTrue( message, e.getMessage().contains( msg ) );
    }

    private void populateTestData()
        throws Exception
    {
        // UnLocode value objects
        AUMEL = unlocode( "AUMEL" ); // Melbourne
        CNHGH = unlocode( "CNHGH" ); // Hangzou
        CNHKG = unlocode( "CNHKG" ); // Hong Kong
        CNSHA = unlocode( "CNSHA" ); // Shanghai
        DEHAM = unlocode( "DEHAM" ); // Hamburg
        FIHEL = unlocode( "FIHEL" ); // Helsinki
        JNTKO = unlocode( "JNTKO" ); // Tokyo
        NLRTM = unlocode( "NLRTM" ); // Rotterdam
        SEGOT = unlocode( "SEGOT" ); // Gothenburg
        SESTO = unlocode( "SESTO" ); // Stockholm
        SOMGQ = unlocode( "SOMGQ" ); // Mogadishu
        USCHI = unlocode( "USCHI" ); // Chicago
        USDAL = unlocode( "USDAL" ); // Dallas
        USNYC = unlocode( "USNYC" ); // New York

        UnitOfWork uow = module.currentUnitOfWork();

        // Get locations created in BaseDataService on startup
        MELBOURNE = uow.get( Location.class, "AUMEL" );
        HANGZHOU = uow.get( Location.class, "CNHGH" );
        HONGKONG = uow.get( Location.class, "CNHKG" );
        SHANGHAI = uow.get( Location.class, "CNSHA" );
        HAMBURG = uow.get( Location.class, "DEHAM" );
        HELSINKI = uow.get( Location.class, "FIHEL" );
        TOKYO = uow.get( Location.class, "JNTKO" );
        ROTTERDAM = uow.get( Location.class, "NLRTM" );
        GOTHENBURG = uow.get( Location.class, "SEGOT" );
        STOCKHOLM = uow.get( Location.class, "SESTO" );
        MOGADISHU = uow.get( Location.class, "SOMGQ" );
        CHICAGO = uow.get( Location.class, "USCHI" );
        DALLAS = uow.get( Location.class, "USDAL" );
        NEWYORK = uow.get( Location.class, "USNYC" );

        // Voyage entity objects for testing
        V201 = voyage( "V201", schedule(
            carrierMovement( HONGKONG, CHICAGO, DAY1, DAY5 ),
            carrierMovement( CHICAGO, NEWYORK, DAY5, DAY6 ),
            carrierMovement( NEWYORK, GOTHENBURG, DAY6, DAY12 )
        ) );
        V202 = voyage( "V202", schedule(
            carrierMovement( CHICAGO, NEWYORK, DAY3, DAY5 ),
            carrierMovement( NEWYORK, DALLAS, DAY7, DAY8 ),
            carrierMovement( DALLAS, ROTTERDAM, DAY10, DAY17 ),
            carrierMovement( ROTTERDAM, HAMBURG, DAY17, DAY19 ),
            carrierMovement( HAMBURG, GOTHENBURG, DAY20, DAY24 )
        ) );
        V203 = voyage( "V203", schedule(
            carrierMovement( NEWYORK, HAMBURG, DAY3, DAY12 ),
            carrierMovement( HAMBURG, ROTTERDAM, DAY13, DAY18 ),
            carrierMovement( ROTTERDAM, STOCKHOLM, DAY20, DAY23 ),
            carrierMovement( STOCKHOLM, HELSINKI, DAY24, DAY25 )
        ) );
        V204 = voyage( "V204", schedule(
            carrierMovement( TOKYO, HANGZHOU, DAY3, DAY6 ),
            carrierMovement( HANGZHOU, HONGKONG, DAY7, DAY8 ),
            carrierMovement( HONGKONG, NEWYORK, DAY9, DAY12 ),
            carrierMovement( NEWYORK, MELBOURNE, DAY13, DAY19 )
        ) );
        V205 = voyage( "V205", schedule(
            carrierMovement( HANGZHOU, MOGADISHU, DAY1, DAY2 ),
            carrierMovement( MOGADISHU, ROTTERDAM, DAY2, DAY4 ),
            carrierMovement( ROTTERDAM, NEWYORK, DAY4, DAY7 ),
            carrierMovement( NEWYORK, DALLAS, DAY9, DAY10 )
        ) );

        itinerary = itinerary(
            leg( V201, HONGKONG, CHICAGO, DAY1, DAY5 ),
            leg( V201, CHICAGO, NEWYORK, DAY5, DAY6 ),
            leg( V202, NEWYORK, DALLAS, DAY7, DAY8 ),
            leg( V202, DALLAS, ROTTERDAM, DAY10, DAY17 ),
            leg( V203, ROTTERDAM, STOCKHOLM, DAY20, DAY23 )
        );

        wrongItinerary = itinerary(
            leg( V201, HONGKONG, CHICAGO, DAY1, DAY5 ),
            leg( V201, CHICAGO, NEWYORK, DAY5, DAY6 ),
            leg( V204, NEWYORK, MELBOURNE, DAY13, DAY19 )
        );
    }

    public void assertDelivery( HandlingEventType handlingEventType,
                                Location location,
                                ZonedDateTime completion,
                                Voyage voyage,

                                TransportStatus transportStatus,
                                Boolean isUnloadedAtDestination,

                                RoutingStatus routingStatus,
                                Boolean isMisdirected,
                                ZonedDateTime eta,
                                Integer itineraryProgress,

                                HandlingEventType nextHandlingEventType,
                                Location nextLocation,
                                ZonedDateTime nextTime,
                                Voyage nextVoyage
    )
        throws Exception
    {
        delivery = cargo.delivery().get();

        // Last handling event
        if( delivery.lastHandlingEvent().get() != null
            || handlingEventType != null || location != null || completion != null || voyage != null )
        {
            assertThat( "lastHandlingEvent - handlingEventType",
                        delivery.lastHandlingEvent()
                            .get()
                            .handlingEventType()
                            .get(), is( equalTo( handlingEventType ) ) );
            assertThat( "lastHandlingEvent - location",
                        delivery.lastHandlingEvent().get().location().get(), is( equalTo( location ) ) );
            assertThat( "lastHandlingEvent - completionTime",
                        delivery.lastHandlingEvent().get().completionTime().get(), is( equalTo( completion ) ) );
            assertThat( "lastHandlingEvent - voyage",
                        delivery.lastHandlingEvent().get().voyage().get(), is( equalTo( voyage ) ) );
        }

        // Other transport status
        assertThat( "transportStatus",
                    delivery.transportStatus().get(), is( equalTo( transportStatus ) ) );
        assertThat( "isUnloadedAtDestination",
                    delivery.isUnloadedAtDestination().get(), is( equalTo( isUnloadedAtDestination ) ) );

        // Routing and direction
        assertThat( "routingStatus",
                    delivery.routingStatus().get(), is( equalTo( routingStatus ) ) );
        assertThat( "isMisdirected",
                    delivery.isMisdirected().get(), is( equalTo( isMisdirected ) ) );
        assertThat( "eta",
                    delivery.eta().get(), is( equalTo( eta ) ) );
        assertThat( "itineraryProgressIndex",
                    delivery.itineraryProgressIndex().get(), is( equalTo( itineraryProgress ) ) );

        // Next handling event
        if( nextHandlingEventType == null )
        {
            assertThat( "nextHandlingEvent - handlingEventType",
                        delivery.nextHandlingEvent().get(), nullValue() );
        }
        else
        {
            assertThat( "nextHandlingEvent - handlingEventType",
                        delivery.nextHandlingEvent()
                            .get()
                            .handlingEventType()
                            .get(), is( equalTo( nextHandlingEventType ) ) );
            assertThat( "nextHandlingEvent - location",
                        delivery.nextHandlingEvent().get().location().get(), is( equalTo( nextLocation ) ) );

            if( delivery.nextHandlingEvent().get().time().get() != null )
            {
                // Estimating a new carrier arrival time might be calculated a second
                // after initial dates are set, so we skip the seconds
                ZonedDateTime calculatedTime = delivery.nextHandlingEvent().get().time().get();
                assertThat( "nextHandlingEvent - time", calculatedTime, equalTo( nextTime ) );
            }
            else
            {
                assertThat( "nextHandlingEvent - time", delivery.nextHandlingEvent()
                    .get()
                    .time()
                    .get(), is( equalTo( nextTime ) ) );
            }

            assertThat( "nextHandlingEvent - voyage",
                        delivery.nextHandlingEvent().get().voyage().get(), is( equalTo( nextVoyage ) ) );
        }
    }

    public void assertDelivery( HandlingEventType handlingEventType,
                                Location location,
                                ZonedDateTime completion,
                                Voyage voyage,

                                TransportStatus transportStatus,
                                Boolean isUnloadedAtDestination,

                                RoutingStatus routingStatus,
                                Boolean isMisdirected,
                                ZonedDateTime eta,
                                Integer itineraryProgress,

                                NextHandlingEvent noNextHandlingEvent
    )
        throws Exception
    {
        assertDelivery( handlingEventType, location, completion, voyage,
                        transportStatus, isUnloadedAtDestination, routingStatus, isMisdirected, eta,
                        itineraryProgress, null, null, null, null );
    }

    public void assertRouteSpec( Location origin, Location destination, ZonedDateTime earliestDeparture, ZonedDateTime deadline )
    {
        newRouteSpec = cargo.routeSpecification().get();

        assertThat( newRouteSpec.origin().get(), is( equalTo( origin ) ) );
        assertThat( newRouteSpec.destination().get(), is( equalTo( destination ) ) );
        assertThat( newRouteSpec.earliestDeparture().get(), is( equalTo( earliestDeparture ) ) );
        assertThat( newRouteSpec.arrivalDeadline().get(), is( equalTo( deadline ) ) );
    }

    protected Voyage voyage( String voyageNumberStr, Schedule schedule )
    {
        UnitOfWork uow = module.currentUnitOfWork();
        EntityBuilder<Voyage> voyage = uow.newEntityBuilder( Voyage.class, voyageNumberStr );

        // VoyageNumber
        ValueBuilder<VoyageNumber> voyageNumber = module.newValueBuilder( VoyageNumber.class );
        voyageNumber.prototype().number().set( voyageNumberStr );
        voyage.instance().voyageNumber().set( voyageNumber.newInstance() );

        // Schedule
        voyage.instance().schedule().set( schedule );
        return voyage.newInstance();
    }

    private static Module findHostingModule()
    {
        return app.findModule( "BOOTSTRAP", "BOOTSTRAP-Bootstrap" );
    }
}
