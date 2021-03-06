/*
 * Copyright (c) 2013-2014, Paul Merlin. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *     You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.qi4j.spi.entitystore.helpers;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.qi4j.api.entity.EntityReference;
import org.qi4j.spi.entity.EntityStatus;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static org.qi4j.functional.Iterables.map;
import static org.qi4j.functional.Iterables.toList;

public class JSONManyAssociationStateTest
{

    @Test
    public void givenEmptyJSONManyAssociationStateWhenAddingTwoRefsAtZeroIndexExpectCorrectOrder()
        throws JSONException
    {
        // Fake JSONManyAssociationState
        JSONObject state = new JSONObject();
        state.put( JSONKeys.PROPERTIES, new JSONObject() );
        state.put( JSONKeys.ASSOCIATIONS, new JSONObject() );
        state.put( JSONKeys.MANY_ASSOCIATIONS, new JSONObject() );
        state.put( JSONKeys.NAMED_ASSOCIATIONS, new JSONObject() );
        JSONEntityState entityState = new JSONEntityState( null,
                                                           "0",
                                                           Instant.now(),
                                                           EntityReference.parseEntityReference( "123" ),
                                                           EntityStatus.NEW,
                                                           null,
                                                           state );
        JSONManyAssociationState jsonState = new JSONManyAssociationState( entityState, new JSONArray() );

        jsonState.add( 0, EntityReference.parseEntityReference( "first" ) );
        jsonState.add( 0, EntityReference.parseEntityReference( "second" ) );

        assertThat( jsonState.count(), equalTo( 2 ) );
    }

    @Test
    public void givenJSONManyAssociationStateWhenChangingReferencesExpectCorrectBehavior()
        throws JSONException
    {
        // Fake JSONManyAssociationState
        JSONObject state = new JSONObject();
        state.put( JSONKeys.PROPERTIES, new JSONObject() );
        state.put( JSONKeys.ASSOCIATIONS, new JSONObject() );
        state.put( JSONKeys.MANY_ASSOCIATIONS, new JSONObject() );
        state.put( JSONKeys.NAMED_ASSOCIATIONS, new JSONObject() );
        JSONEntityState entityState = new JSONEntityState( null,
                                                           "0",
                                                           Instant.now(),
                                                           EntityReference.parseEntityReference( "123" ),
                                                           EntityStatus.NEW,
                                                           null,
                                                           state );
        JSONManyAssociationState jsonState = new JSONManyAssociationState( entityState, new JSONArray() );

        assertThat( jsonState.contains( EntityReference.parseEntityReference( "NOT_PRESENT" ) ), is( false ) );

        jsonState.add( 0, EntityReference.parseEntityReference( "0" ) );
        jsonState.add( 1, EntityReference.parseEntityReference( "1" ) );
        jsonState.add( 2, EntityReference.parseEntityReference( "2" ) );

        assertThat( jsonState.contains( EntityReference.parseEntityReference( "1" ) ), is( true ) );

        assertThat( jsonState.get( 0 ).identity(), equalTo( "0" ) );
        assertThat( jsonState.get( 1 ).identity(), equalTo( "1" ) );
        assertThat( jsonState.get( 2 ).identity(), equalTo( "2" ) );

        assertThat( jsonState.count(), equalTo( 3 ) );

        jsonState.remove( EntityReference.parseEntityReference( "1" ) );

        assertThat( jsonState.count(), equalTo( 2 ) );
        assertThat( jsonState.contains( EntityReference.parseEntityReference( "1" ) ), is( false ) );
        assertThat( jsonState.get( 0 ).identity(), equalTo( "0" ) );
        assertThat( jsonState.get( 1 ).identity(), equalTo( "2" ) );

        jsonState.add( 2, EntityReference.parseEntityReference( "1" ) );

        assertThat( jsonState.count(), equalTo( 3 ) );

        jsonState.add( 0, EntityReference.parseEntityReference( "A" ) );
        jsonState.add( 0, EntityReference.parseEntityReference( "B" ) );
        jsonState.add( 0, EntityReference.parseEntityReference( "C" ) );

        assertThat( jsonState.count(), equalTo( 6 ) );

        assertThat( jsonState.get( 0 ).identity(), equalTo( "C" ) );
        assertThat( jsonState.get( 1 ).identity(), equalTo( "B" ) );
        assertThat( jsonState.get( 2 ).identity(), equalTo( "A" ) );

        assertThat( jsonState.contains( EntityReference.parseEntityReference( "C" ) ), is( true ) );
        assertThat( jsonState.contains( EntityReference.parseEntityReference( "B" ) ), is( true ) );
        assertThat( jsonState.contains( EntityReference.parseEntityReference( "A" ) ), is( true ) );
        assertThat( jsonState.contains( EntityReference.parseEntityReference( "0" ) ), is( true ) );
        assertThat( jsonState.contains( EntityReference.parseEntityReference( "2" ) ), is( true ) );
        assertThat( jsonState.contains( EntityReference.parseEntityReference( "1" ) ), is( true ) );

        List<String> refList = toList( map( new Function<EntityReference, String>()
        {
            @Override
            public String apply( EntityReference from )
            {
                return from.identity();
            }
        }, jsonState ) );
        assertThat( refList.isEmpty(), is( false ) );
        assertArrayEquals( new String[]
        {
            "C", "B", "A", "0", "2", "1"
        }, refList.toArray() );
    }
}
