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
package org.qi4j.sample.dcicargo.sample_a.data.shipping.itinerary;

import java.time.ZonedDateTime;
import org.qi4j.api.association.Association;
import org.qi4j.api.property.Property;
import org.qi4j.api.value.ValueComposite;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.location.Location;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.voyage.Voyage;

/**
 * A leg describes an expected segment of a route:
 * - loading onto a voyage at a load location
 * - unloading from the voyage at a unload location
 *
 * All properties are mandatory and immutable.
 */
public interface Leg
    extends ValueComposite
{
    Association<Location> loadLocation();

    Property<ZonedDateTime> loadTime();

    Association<Voyage> voyage();

    Property<ZonedDateTime> unloadTime();

    Association<Location> unloadLocation();
}
