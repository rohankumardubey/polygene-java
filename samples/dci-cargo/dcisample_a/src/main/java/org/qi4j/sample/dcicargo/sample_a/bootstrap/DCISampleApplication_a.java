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
package org.qi4j.sample.dcicargo.sample_a.bootstrap;

import org.apache.wicket.ConverterLocator;
import org.apache.wicket.Page;
import org.apache.wicket.datetime.PatternDateConverter;
import org.apache.wicket.devutils.stateless.StatelessChecker;
import org.qi4j.sample.dcicargo.sample_a.communication.web.booking.BookNewCargoPage;
import org.qi4j.sample.dcicargo.sample_a.communication.web.booking.CargoDetailsPage;
import org.qi4j.sample.dcicargo.sample_a.communication.web.booking.CargoListPage;
import org.qi4j.sample.dcicargo.sample_a.communication.web.booking.ChangeDestinationPage;
import org.qi4j.sample.dcicargo.sample_a.communication.web.booking.RouteCargoPage;
import org.qi4j.sample.dcicargo.sample_a.communication.web.handling.RegisterHandlingEventPage;
import org.qi4j.sample.dcicargo.sample_a.communication.web.tracking.TrackCargoPage;
import org.qi4j.sample.dcicargo.sample_a.infrastructure.WicketQi4jApplication;
import org.qi4j.sample.dcicargo.sample_a.infrastructure.wicket.tabs.TabsPanel;

/**
 * DCI Sample application instance
 *
 * A Wicket application backed by Qi4j.
 */
public class DCISampleApplication_a
    extends WicketQi4jApplication
{
    public void wicketInit()
    {
        // Tabs and SEO urls
        mountPages();

        // Show/hide Ajax debugging
        getDebugSettings().setDevelopmentUtilitiesEnabled( true );

        // Check that components are stateless when required
        getComponentPostOnBeforeRenderListeners().add( new StatelessChecker() );

        // Show/hide wicket tags in html code
        getMarkupSettings().setStripWicketTags( true );
    }

    private void mountPages()
    {
        TabsPanel.registerTab( this, CargoListPage.class, "booking", "Booking and Routing" );
        TabsPanel.registerTab( this, TrackCargoPage.class, "tracking", "Tracking" );
        TabsPanel.registerTab( this, RegisterHandlingEventPage.class, "handling", "Handling" );

        mountPage( "/booking", CargoListPage.class );
        mountPage( "/booking/book-new-cargo", BookNewCargoPage.class );
        mountPage( "/booking/cargo", CargoDetailsPage.class );
        mountPage( "/booking/change-destination", ChangeDestinationPage.class );
        mountPage( "/booking/route-cargo", RouteCargoPage.class );

        mountPage( "/handling", RegisterHandlingEventPage.class );
        mountPage( "/tracking", TrackCargoPage.class );
    }

    public Class<? extends Page> getHomePage()
    {
        return CargoListPage.class;
    }
}