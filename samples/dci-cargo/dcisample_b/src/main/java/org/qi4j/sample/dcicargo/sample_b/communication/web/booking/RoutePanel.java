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
package org.qi4j.sample.dcicargo.sample_b.communication.web.booking;

import java.util.Date;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.qi4j.sample.dcicargo.sample_b.context.interaction.booking.routing.AssignCargoToRoute;
import org.qi4j.sample.dcicargo.sample_b.data.structure.itinerary.Itinerary;
import org.qi4j.sample.dcicargo.sample_b.data.structure.itinerary.Leg;

/**
 * Route Panel
 *
 * Shows a suggested route that a Cargo can take.
 *
 * If the user chooses this candidate route, all route data (a value object) is attached by the
 * RegisterNewDestination context as an Itinerary to the Cargo. To see the result, the user is
 * then redirected to the details page of the cargo.
 */
public class RoutePanel extends Panel
{
    public RoutePanel( String id,
                       final String trackingIdString,
                       final IModel<Itinerary> candidateRouteModel,
                       int index
    )
    {
        super( id, candidateRouteModel );
        Itinerary itinerary = candidateRouteModel.getObject();

        IModel<String> header = Model.of( "Route candidate " + index + " - duration: " + itinerary.days() + " days." );
        add( new Label( "routeHeader", header ) );

        final FeedbackPanel routeFeedback = new FeedbackPanel( "routeFeedback" );
        add( routeFeedback.setOutputMarkupId( true ).setEscapeModelStrings( true ) );

        IModel<List<Leg>> legListModel = new LoadableDetachableModel<List<Leg>>()
        {
            @Override
            protected List<Leg> load()
            {
                return candidateRouteModel.getObject().legs().get();
            }
        };

        add( new ListView<Leg>( "legs", legListModel )
        {
            @Override
            protected void populateItem( ListItem<Leg> item )
            {
                Leg leg = item.getModelObject();
                Date loadTime = new Date( leg.loadTime().get().toInstant().toEpochMilli() );
                Date unloadTime = new Date( leg.unloadTime().get().toInstant().toEpochMilli() );
                item.add( new Label( "voyage", leg.voyage().get().toString() ),
                          new Label( "loadLocation", leg.loadLocation().get().getCode() ),
                          new Label( "loadTime", new Model<Date>( loadTime ) ),
                          new Label( "unloadLocation", leg.unloadLocation().get().getCode() ),
                          new Label( "unloadTime", new Model<Date>( unloadTime ) )
                );
            }
        } );

        StatelessForm form = new StatelessForm<Void>( "form" );
        form.add( new AjaxFallbackButton( "assign", form )
        {
            @Override
            protected void onSubmit( AjaxRequestTarget target, Form<?> form )
            {
                try
                {
                    Itinerary itinerary = candidateRouteModel.getObject();
                    new AssignCargoToRoute( trackingIdString, itinerary ).assign();
                    setResponsePage( CargoDetailsPage.class, new PageParameters().set( 0, trackingIdString ) );
                }
                catch( Exception e )
                {
                    String msg = "Problem assigning this route to cargo: " + e.getMessage();
                    routeFeedback.error( msg );
                    target.add( routeFeedback );
                }
            }

            @Override
            protected void onError( final AjaxRequestTarget target, Form<?> form )
            {
                routeFeedback.error( "Unexpected error - all routes are expected to be valid." );
                target.add( routeFeedback );
            }
        } );
        add( form );
    }
}