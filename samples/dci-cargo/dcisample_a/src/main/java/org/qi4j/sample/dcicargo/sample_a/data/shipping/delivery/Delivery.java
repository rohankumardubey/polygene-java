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
package org.qi4j.sample.dcicargo.sample_a.data.shipping.delivery;

import java.time.Instant;
import java.time.ZonedDateTime;
import org.qi4j.api.association.Association;
import org.qi4j.api.common.Optional;
import org.qi4j.api.common.UseDefaults;
import org.qi4j.api.property.Property;
import org.qi4j.api.value.ValueComposite;
import org.qi4j.sample.dcicargo.sample_a.context.support.ApplicationEvents;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.cargo.RouteSpecification;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.handling.HandlingEvent;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.itinerary.Itinerary;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.location.Location;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.voyage.Voyage;

/**
 * The Delivery describes the actual transportation of the cargo, as opposed to
 * the customer requirement {@link RouteSpecification} and the plan {@link Itinerary}.
 *
 * Complex data of the shipping domain is captured here in a value object that is
 * re-created each time some delivery status changes.
 *
 * Booking
 * The life cycle of a cargo begins with the booking procedure. During a (short) period
 * of time, between booking and initial routing, the cargo has no itinerary and is therefore
 * not_routed.
 *
 * Routing
 * The booking clerk requests a list of possible routes, matching the route specification,
 * and assigns the cargo to one route. The route to which a cargo is assigned is described
 * by an itinerary. The cargo is now routed.
 *
 * Handling
 * Receipt of the cargo in the origin location marks the beginning of a series of handling
 * events that will eventually deliver the cargo at the destination. Handling events are
 * supposed to be registered by local authorities and sent asynchronously to the cargo system
 * which then re-calculate and replaces the delivery value object of the cargo.
 * (In this implementation the trigger is synchronous in {@link ApplicationEvents}
 * and the registration-ui is close to the booking and tracking interfaces for simplicity).
 *
 * Change of destination
 * It may also happen that a cargo is accidentally misrouted when a new destination is
 * selected in the middle of the voyage of the cargo. The cargo then becomes misrouted,
 * which should notify the proper personnel and also trigger a re-routing procedure.
 * In this implementation a synchronous message is sent to system output.
 *
 * Re-routing
 * A cargo can be re-routed during transport, on demand of the customer, in which case
 * a new route is specified for the cargo and a new route is requested. The old itinerary,
 * being a value object, is discarded and a new one is attached to the cargo.
 *
 * Customs
 * The cargo can be checked by the custom authorities anytime during the delivery. In that
 * case it's unknown what happens next so the cargo is then considered misdirected.
 *
 * Claim
 * The life cycle of a cargo ends when the cargo is claimed by the customer.
 */
public interface Delivery
    extends ValueComposite
{
    Property<Instant> timestamp();

    /*
   * NOT_ROUTED
   * ROUTED
   * MISROUTED
   * */
    Property<RoutingStatus> routingStatus();

    /*
   * NOT_RECEIVED
   * IN_PORT
   * ONBOARD_CARRIER
   * CLAIMED
   * UNKNOWN
   * */
    Property<TransportStatus> transportStatus();

    // Unexpected location of cargo according to itinerary
    @UseDefaults
    Property<Boolean> isMisdirected();

    /**
     * RECEIVE
     * LOAD
     * UNLOAD
     * CUSTOMS
     * CLAIM
     *
     * ("HandlingActivity" in DDD sample)
     */
    @Optional
    Property<ExpectedHandlingEvent> nextExpectedHandlingEvent();

    @Optional
    Association<HandlingEvent> lastHandlingEvent();

    @Optional
    Association<Location> lastKnownLocation();

    @Optional
    Association<Voyage> currentVoyage();

    @Optional
    Property<ZonedDateTime> eta();

    @UseDefaults
    Property<Boolean> isUnloadedAtDestination();
}