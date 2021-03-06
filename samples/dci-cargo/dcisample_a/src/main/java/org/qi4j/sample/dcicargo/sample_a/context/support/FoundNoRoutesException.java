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

import java.time.ZonedDateTime;

/**
 * Custom messages when the deadline is too close and we can't find a route.
 */
public class FoundNoRoutesException extends Exception
{
    private final String city;
    private final ZonedDateTime deadline;

    public FoundNoRoutesException( String city, ZonedDateTime deadline )
    {
        this.city = city;
        this.deadline = deadline;
    }

    @Override
    public String getMessage()
    {
        if( deadline.isBefore( ZonedDateTime.now().plusDays( 2 ) ) )
        {
            return "Impossible to get the cargo to " + city + " before " + deadline
                   + "! Make a new booking with a deadline 2-3 weeks ahead in time.";
        }
        else if( deadline.isBefore( ZonedDateTime.now().plusDays( 4 ) ) )
        {
            return "Couldn't find any routes arriving in " + city + " before " + deadline
                   + ". Please try again or make a new booking with a deadline 2-3 weeks ahead in time.";
        }
        else if( deadline.isBefore( ZonedDateTime.now().plusDays( 6 ) ) )
        {
            return "Sorry, our system couldn't immediately find a route arriving in " + city + " before " + deadline
                   + ". Please try again, and we should hopefully be able to find a new route for you.";
        }

        return "Couldn't find any route to " + city + " arriving before " + deadline
               + ". We don't know why. Have a nice day.";
    }
}