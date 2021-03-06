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
package org.qi4j.sample.dcicargo.sample_a.bootstrap.sampledata;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.qi4j.api.activation.ActivatorAdapter;
import org.qi4j.api.activation.Activators;
import org.qi4j.api.injection.scope.Service;
import org.qi4j.api.injection.scope.Structure;
import org.qi4j.api.mixin.Mixins;
import org.qi4j.api.query.Query;
import org.qi4j.api.query.QueryBuilder;
import org.qi4j.api.query.QueryBuilderFactory;
import org.qi4j.api.service.ServiceComposite;
import org.qi4j.api.service.ServiceReference;
import org.qi4j.api.unitofwork.UnitOfWork;
import org.qi4j.api.unitofwork.UnitOfWorkFactory;
import org.qi4j.api.usecase.Usecase;
import org.qi4j.api.usecase.UsecaseBuilder;
import org.qi4j.sample.dcicargo.sample_a.context.shipping.booking.BookNewCargo;
import org.qi4j.sample.dcicargo.sample_a.context.shipping.handling.RegisterHandlingEvent;
import org.qi4j.sample.dcicargo.sample_a.data.entity.CargosEntity;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.cargo.Cargo;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.delivery.ExpectedHandlingEvent;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.handling.HandlingEventType;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.itinerary.Itinerary;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.location.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.qi4j.api.usecase.UsecaseBuilder.newUsecase;
import static org.qi4j.sample.dcicargo.sample_a.infrastructure.dci.Context.prepareContextBaseClass;

/**
 * Create sample Cargos in different delivery stages
 */
@Mixins(SampleDataService.Mixin.class)
@Activators(SampleDataService.Activator.class)
public interface SampleDataService
    extends ServiceComposite
{

    void insertSampleData()
        throws Exception;

    class Activator
        extends ActivatorAdapter<ServiceReference<SampleDataService>>
    {

        @Override
        public void afterActivation( ServiceReference<SampleDataService> activated )
            throws Exception
        {
            activated.get().insertSampleData();
        }
    }

    public abstract class Mixin
        implements SampleDataService
    {
        @Structure
        QueryBuilderFactory qbf;

        @Structure
        UnitOfWorkFactory uowf;

        @Service // We depend on BaseData to be inserted
            BaseDataService baseDataService;

        private static final Logger logger = LoggerFactory.getLogger( SampleDataService.class );

        @Override
        public void insertSampleData()
            throws Exception
        {
            prepareContextBaseClass( uowf );

            logger.info( "######  CREATING SAMPLE DATA...  ##########################################" );

            // Create cargos
            populateRandomCargos( 15 );

            // Handle cargos
            UnitOfWork uow = uowf.newUnitOfWork( newUsecase( "### Create sample data" ) );
            try
            {
                int i = 11; // starting at 11 for sortable tracking id prefix in lists
                QueryBuilder<Cargo> qb = qbf.newQueryBuilder( Cargo.class );
                for( Cargo cargo : uow.newQuery( qb ) )
                {
                    String trackingId = cargo.trackingId().get().id().get();
                    ExpectedHandlingEvent nextEvent;
                    ZonedDateTime time;
                    String port;
                    String voyage;
                    HandlingEventType type;

                    // BOOK cargo with no handling (i == 11)

                    // ROUTE
                    if( i > 11 )
                    {
                        Itinerary itinerary = new BookNewCargo( cargo ).routeCandidates().get( 0 );
                        new BookNewCargo( cargo, itinerary ).assignCargoToRoute();
                    }

                    // RECEIVE
                    if( i > 12 )
                    {
                        nextEvent = cargo.delivery().get().nextExpectedHandlingEvent().get();
                        port = nextEvent.location().get().getCode();
                        ZonedDateTime mockTime = ZonedDateTime.now();
                        new RegisterHandlingEvent( mockTime, mockTime, trackingId, "RECEIVE", port, null ).register();
                    }

                    // LOAD
                    if( i > 13 )
                    {
                        nextEvent = cargo.delivery().get().nextExpectedHandlingEvent().get();
                        time = nextEvent.time().get();
                        port = nextEvent.location().get().getCode();
                        voyage = nextEvent.voyage().get().voyageNumber().get().number().get();
                        new RegisterHandlingEvent( time, time, trackingId, "LOAD", port, voyage ).register();
                    }

                    // UNLOAD
                    if( i > 14 )
                    {
                        nextEvent = cargo.delivery().get().nextExpectedHandlingEvent().get();
                        time = nextEvent.time().get();
                        port = nextEvent.location().get().getCode();
                        voyage = nextEvent.voyage().get().voyageNumber().get().number().get();
                        new RegisterHandlingEvent( time, time, trackingId, "UNLOAD", port, voyage ).register();
                    }

                    // Cargo is now in port
                    nextEvent = cargo.delivery().get().nextExpectedHandlingEvent().get();
                    time = nextEvent.time().get();
                    port = nextEvent.location().get().getCode();
                    type = nextEvent.handlingEventType().get();

                    // MISDIRECTED: Unexpected customs handling before reaching destination
                    if( i == 16 )
                    {
                        new RegisterHandlingEvent( time, time, trackingId, "CUSTOMS", port, null ).register();
                    }

                    // MISDIRECTED: Unexpected claim before reaching destination
                    if( i == 17 )
                    {
                        new RegisterHandlingEvent( time, time, trackingId, "CLAIM", port, null ).register();
                    }

                    // MISDIRECTED: LOAD in wrong port
                    if( i == 18 )
                    {
                        String wrongPort = port.equals( "USDAL" ) ? "USCHI" : "USDAL";
                        voyage = nextEvent.voyage().get().voyageNumber().get().number().get();
                        new RegisterHandlingEvent( time, time, trackingId, "LOAD", wrongPort, voyage ).register();
                    }

                    // MISDIRECTED: LOAD onto wrong carrier
                    if( i == 19 )
                    {
                        voyage = nextEvent.voyage().get().voyageNumber().get().number().get();
                        String wrongVoyage = voyage.equals( "V100S" ) ? "V200T" : "V100S";
                        new RegisterHandlingEvent( time, time, trackingId, "LOAD", port, wrongVoyage ).register();
                    }

                    // MISDIRECTED: LOAD onto wrong carrier in wrong port
                    if( i == 20 )
                    {
                        String wrongPort = port.equals( "USDAL" ) ? "USCHI" : "USDAL";
                        voyage = nextEvent.voyage().get().voyageNumber().get().number().get();
                        String wrongVoyage = voyage.equals( "V100S" ) ? "V200T" : "V100S";
                        new RegisterHandlingEvent( time, time, trackingId, "LOAD", wrongPort, wrongVoyage ).register();
                    }

                    // MISDIRECTED: UNLOAD in wrong port
                    if( i == 21 )
                    {
                        String wrongPort = port.equals( "USDAL" ) ? "USCHI" : "USDAL";
                        voyage = nextEvent.voyage().get().voyageNumber().get().number().get();
                        new RegisterHandlingEvent( time, time, trackingId, "UNLOAD", wrongPort, voyage ).register();
                    }

                    // MISDIRECTED: UNLOAD from wrong carrier
                    if( i == 22 )
                    {
                        voyage = nextEvent.voyage().get().voyageNumber().get().number().get();
                        String wrongVoyage = voyage.equals( "V100S" ) ? "V200T" : "V100S";
                        new RegisterHandlingEvent( time, time, trackingId, "UNLOAD", port, wrongVoyage ).register();
                    }

                    // MISDIRECTED: UNLOAD from wrong carrier in wrong port
                    if( i == 23 )
                    {
                        String wrongPort = port.equals( "USDAL" ) ? "USCHI" : "USDAL";
                        voyage = nextEvent.voyage().get().voyageNumber().get().number().get();
                        String wrongVoyage = voyage.equals( "V100S" ) ? "V200T" : "V100S";
                        new RegisterHandlingEvent( time, time, trackingId, "UNLOAD", wrongPort, wrongVoyage ).register();
                    }

                    // Complete all LOAD/UNLOADS
                    if( i > 23 )
                    {
                        do
                        {
                            voyage = nextEvent.voyage().get().voyageNumber().get().number().get();
                            new RegisterHandlingEvent( time, time, trackingId, type.name(), port, voyage ).register();

                            nextEvent = cargo.delivery().get().nextExpectedHandlingEvent().get();
                            time = nextEvent.time().get();
                            port = nextEvent.location().get().getCode();
                            type = nextEvent.handlingEventType().get();
                        }
                        while( type != HandlingEventType.CLAIM );
                    }

                    // CLAIM at destination - this ends the life cycle of the cargo delivery
                    if( i == 25 )
                    {
                        new RegisterHandlingEvent( time, time, trackingId, "CLAIM", port, null ).register();
                    }

                    // Add more cases if needed...

                    i++;
                }

                uow.complete();
            }
            catch( Exception e )
            {
                uow.discard();
                logger.error( "Problem handling cargos: " + e.getMessage() );
                throw e;
            }

            logger.info( "######  SAMPLE DATA CREATED  ##############################################" );
        }

        private void populateRandomCargos( int numberOfCargos )
        {
            Usecase usecase = UsecaseBuilder.newUsecase( "### Populate Random Cargos ###" );
            UnitOfWork uow = uowf.newUnitOfWork( usecase );

            CargosEntity cargos = uow.get( CargosEntity.class, CargosEntity.CARGOS_ID );

            QueryBuilder<Location> qb = qbf.newQueryBuilder( Location.class );
            Query<Location> allLocations = uow.newQuery( qb );
            int locationSize = (int) allLocations.count();

            // Make array for selection of location with random index
            final List<Location> locationList = new ArrayList<Location>();
            for( Location location : allLocations )
            {
                locationList.add( location );
            }

            Location origin;
            Location destination;
            Random random = new Random();
            ZonedDateTime deadline;
            String uuid;
            String id;
            try
            {
                for( int i = 0; i < numberOfCargos; i++ )
                {
                    origin = locationList.get( random.nextInt( locationSize ) );

                    // Find destination different from origin
                    do
                    {
                        destination = locationList.get( random.nextInt( locationSize ) );
                    }
                    while( destination.equals( origin ) );

                    deadline = ZonedDateTime.now().plusDays( 15 + random.nextInt( 10 ) );

                    // Build sortable random tracking ids
                    uuid = UUID.randomUUID().toString().toUpperCase();
                    id = ( i + 11 ) + "-" + uuid.substring( 0, uuid.indexOf( "-" ) );

                    new BookNewCargo( cargos, origin, destination, deadline ).createCargo( id );
                }
                uow.complete();
            }
            catch( Exception e )
            {
                uow.discard();
                logger.error( "Problem booking a new cargo: " + e.getMessage() );
            }
        }
    }
}
