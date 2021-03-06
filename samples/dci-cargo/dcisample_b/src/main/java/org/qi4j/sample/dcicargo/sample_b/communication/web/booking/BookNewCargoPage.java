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

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.ComponentFeedbackPanel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.qi4j.sample.dcicargo.sample_b.communication.query.CommonQueries;
import org.qi4j.sample.dcicargo.sample_b.context.interaction.booking.BookNewCargo;
import org.qi4j.sample.dcicargo.sample_b.data.structure.tracking.TrackingId;
import org.qi4j.sample.dcicargo.sample_b.infrastructure.wicket.form.AbstractForm;
import org.qi4j.sample.dcicargo.sample_b.infrastructure.wicket.form.DateTextFieldWithPicker;
import org.qi4j.sample.dcicargo.sample_b.infrastructure.wicket.form.SelectorInForm;
import org.qi4j.sample.dcicargo.sample_b.infrastructure.wicket.prevnext.PrevNext;

/**
 * Book new cargo
 *
 * An example of UI validation:
 * - you can't choose equal locations
 * - earliest deadline you can choose from the date picker is tomorrow (we could set any date that the domain wants).
 *
 * More elaborate validation rules could be enforced in a real domain to avoid even touching
 * the backend with invalid data (of course we would still need backend validation).
 */
public class BookNewCargoPage extends BookingBasePage
{
    public BookNewCargoPage()
    {
        add( new BookNewCargoForm() );
    }

    private final class BookNewCargoForm extends AbstractForm<Void>
    {
        // Set by Wicket property resolvers:
        private String origin, destination;
        private ZonedDateTime deadline;

        public BookNewCargoForm()
        {
            List<String> locations = new CommonQueries().unLocodes();

            final FeedbackPanel feedback = new FeedbackPanel( "feedback" );
            add( feedback.setOutputMarkupId( true ) );

            final SelectorInForm originSelector = new SelectorInForm(
                "origin", "Origin", locations, this, "destination" );
            originSelector.setRequired( true );

            final ComponentFeedbackPanel originFeedback = new ComponentFeedbackPanel(
                "originFeedback", originSelector );
            add( originFeedback.setOutputMarkupId( true ) );

            final SelectorInForm destinationSelector = new SelectorInForm(
                "destination", "Destinatin", locations, this, "origin" );
            destinationSelector.setRequired( true );

            final ComponentFeedbackPanel destinationFeedback = new ComponentFeedbackPanel(
                "destinationFeedback", destinationSelector );
            add( destinationFeedback.setOutputMarkupId( true ) );

            // Disable equal locations
            originSelector.add( new AjaxFormComponentUpdatingBehavior( "onchange" )
            {
                @Override
                protected void onUpdate( AjaxRequestTarget target )
                {
                    // Exclude origin in destination drop down
                    target.add( originSelector, originFeedback, destinationSelector );
                    focusFirstError( target );
                }
            } );

            destinationSelector.add( new AjaxFormComponentUpdatingBehavior( "onchange" )
            {
                @Override
                protected void onUpdate( AjaxRequestTarget target )
                {
                    // Exclude destination in origin drop down
                    target.add( destinationSelector, destinationFeedback, originSelector );
                    focusFirstError( target );
                }
            } );

            // Deadline
            final DateTextFieldWithPicker deadlineField = new DateTextFieldWithPicker( "deadline", "Arrival deadline", this );
            deadlineField.earliestDate( LocalDate.now().plusDays( 1 ) );

            final ComponentFeedbackPanel deadlineFeedback = new ComponentFeedbackPanel(
                "deadlineFeedback", deadlineField );
            add( deadlineFeedback.setOutputMarkupId( true ) );

            add( originSelector, destinationSelector, deadlineField );

            add( new AjaxFallbackButton( "book", this )
            {
                @Override
                protected void onSubmit( AjaxRequestTarget target, Form<?> form )
                {
                    try
                    {
                        // Perform use case
                        TrackingId trackingId = new BookNewCargo( origin, destination, deadline ).getTrackingId();

                        // Add new tracking id to list in session
                        PrevNext.addId( Session.get(), trackingId.id().get() );

                        // Show created cargo
                        setResponsePage( CargoDetailsPage.class, new PageParameters().set( 0, trackingId.id().get() ) );
                    }
                    catch( Exception e )
                    {
                        logger.warn( "Problem booking a new cargo: " + e.getMessage() );
                        feedback.error( e.getMessage() );
                        target.add( feedback );
                    }
                }

                @Override
                protected void onError( final AjaxRequestTarget target, Form<?> form )
                {
                    target.add( originFeedback, destinationFeedback, deadlineFeedback );
                    focusFirstError( target );
                }
            } );
        }
    }
}