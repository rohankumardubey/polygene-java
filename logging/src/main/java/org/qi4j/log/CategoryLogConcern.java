/*
 * Copyright 2006 Niclas Hedhman.
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
package org.qi4j.log;

import org.qi4j.Qi4j;
import org.qi4j.composite.Composite;
import org.qi4j.injection.scope.Service;
import org.qi4j.injection.scope.Structure;
import org.qi4j.injection.scope.This;
import org.qi4j.log.logtypes.ErrorType;
import org.qi4j.log.logtypes.InfoType;
import org.qi4j.log.logtypes.WarningType;
import org.qi4j.log.service.LoggingService;

public final class CategoryLogConcern
    implements CategoryLog
{
    @Structure Qi4j api;
    @Service private LoggingService loggingService;
    @This Composite composite;

    public void info( String category, String message )
    {
        loggingService.log( InfoType.INSTANCE, api.dereference( composite ), category, message );
    }

    public void info( String category, String message, Object param1 )
    {
        loggingService.log( InfoType.INSTANCE, api.dereference( composite ), category, message, param1 );
    }

    public void info( String category, String message, Object param1, Object param2 )
    {
        loggingService.log( InfoType.INSTANCE, api.dereference( composite ), category, message, param1, param2 );
    }

    public void info( String category, String message, Object... params )
    {
        loggingService.log( InfoType.INSTANCE, api.dereference( composite ), category, message, params );
    }

    public void warning( String category, String message )
    {
        loggingService.log( WarningType.INSTANCE, api.dereference( composite ), category, message );
    }

    public void warning( String category, String message, Object param1 )
    {
        loggingService.log( WarningType.INSTANCE, api.dereference( composite ), category, message, param1 );
    }

    public void warning( String category, String message, Object param1, Object param2 )
    {
        loggingService.log( WarningType.INSTANCE, api.dereference( composite ), category, message, param1, param2 );
    }

    public void warning( String category, String message, Object... params )
    {
        loggingService.log( WarningType.INSTANCE, api.dereference( composite ), category, message, params );
    }

    public void error( String category, String message )
    {
        loggingService.log( ErrorType.INSTANCE, api.dereference( composite ), category, message );
    }

    public void error( String category, String message, Object param1 )
    {
        loggingService.log( ErrorType.INSTANCE, api.dereference( composite ), category, message, param1 );
    }

    public void error( String category, String message, Object param1, Object param2 )
    {
        loggingService.log( ErrorType.INSTANCE, api.dereference( composite ), category, message, param1, param2 );
    }

    public void error( String category, String message, Object... params )
    {
        loggingService.log( ErrorType.INSTANCE, api.dereference( composite ), category, message, params );
    }
}
