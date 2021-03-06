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
package org.qi4j.sample.dcicargo.sample_a.communication.query.dto;

import java.time.ZonedDateTime;
import org.qi4j.api.common.Optional;
import org.qi4j.api.property.Property;
import org.qi4j.library.conversion.values.Unqualified;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.cargo.TrackingId;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.handling.HandlingEvent;
import org.qi4j.sample.dcicargo.sample_a.data.shipping.handling.HandlingEventType;
import org.qi4j.sample.dcicargo.sample_a.infrastructure.conversion.DTO;

/**
 * HandlingEvent DTO
 *
 * We need the @Unqualified annotation since the HandlingEventDTO interface has other properties than
 * {@link HandlingEvent} so that properties can not be directly mapped when we convert from entity to
 * immutable value DTO. With the annotation, property access methods are compared by name instead.
 */
@Unqualified
public interface HandlingEventDTO extends DTO
{
    Property<ZonedDateTime> completionTime();

    Property<TrackingId> trackingId();

    Property<HandlingEventType> handlingEventType();

    Property<LocationDTO> location();

    @Optional
    Property<VoyageDTO> voyage();
}
