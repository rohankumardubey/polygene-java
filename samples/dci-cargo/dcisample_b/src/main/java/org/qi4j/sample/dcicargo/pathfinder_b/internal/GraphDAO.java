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
package org.qi4j.sample.dcicargo.pathfinder_b.internal;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.qi4j.sample.dcicargo.pathfinder_b.api.TransitEdge;
import org.qi4j.sample.dcicargo.pathfinder_b.api.TransitPath;

public class GraphDAO
{

    private static final Random random = new Random();
    private static final long ONE_MIN_MS = 1000 * 60;
    private static final long ONE_HOUR_MS = ONE_MIN_MS * 60;
    private static final long ONE_DAY_MS = ONE_HOUR_MS * 24;

    private List<TransitPath> voyages = new ArrayList<TransitPath>();

    public List<String> listLocations()
    {
        return new ArrayList<String>( Arrays.asList(
            "CNHKG", "AUMEL", "SESTO", "FIHEL", "USCHI", "JNTKO", "DEHAM", "CNSHA", "NLRTM", "SEGOT", "CNHGH", "SOMGQ", "USNYC", "USDAL"
        ) );
    }

    public List<TransitPath> voyages()
    {
        if( voyages.size() > 0 )
        {
            return voyages;
        }

        ZonedDateTime departureDate = ZonedDateTime.now();
        for( int i = 0; i < 50; i++ )
        {
            List<String> locations = getRandomChunkOfLocations( listLocations() );
            final List<TransitEdge> transitEdges = new ArrayList<TransitEdge>( locations.size() - 1 );
            final String voyageNumber = "V" + ( 101 + i );

            // Origin and destination of voyage schedule
            String from = locations.remove( 0 );
            String destination = locations.remove( 0 );

            ZonedDateTime date = nextDate( departureDate );
            ZonedDateTime fromDate;
            ZonedDateTime toDate;

            // Carrier movements
            for( final String to : locations )
            {
                fromDate = nextDate( date );
                toDate = nextDate( fromDate );
                date = nextDate( toDate );
                transitEdges.add( new TransitEdge( voyageNumber, from, to, fromDate, toDate ) );

                // Arrival location of last carrier movement becomes departure location of next
                from = to;
            }

            // Final carrier movement
            fromDate = nextDate( date );
            toDate = nextDate( fromDate );
            transitEdges.add( new TransitEdge( voyageNumber, from, destination, fromDate, toDate ) );

            voyages.add( new TransitPath( transitEdges ) );
        }

        // Output for testing...
//        for (int i = 0; i < voyages.size(); i++)
//            System.out.println( i + " " + voyages.get( i ).print() );

        return voyages;
    }

    private ZonedDateTime nextDate( ZonedDateTime date )
    {
        return date.plusDays( 1 + random.nextInt( 30 ) );
    }

    private List<String> getRandomChunkOfLocations( List<String> allLocations )
    {
        Collections.shuffle( allLocations );
        final int total = allLocations.size();
        // Including origin and destination
        final int chunk = total > 6 ? 3 + new Random().nextInt( 5 ) : total;
        return allLocations.subList( 0, chunk );
    }
}
