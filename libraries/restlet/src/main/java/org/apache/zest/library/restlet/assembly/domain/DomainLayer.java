/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.apache.zest.library.restlet.assembly.domain;

import org.apache.zest.api.structure.Application;
import org.apache.zest.api.structure.Module;
import org.apache.zest.bootstrap.AssemblyException;
import org.apache.zest.bootstrap.LayerAssembly;
import org.apache.zest.bootstrap.layered.LayerAssembler;
import org.apache.zest.bootstrap.layered.LayeredLayerAssembler;
import org.apache.zest.functional.Function;

public class DomainLayer extends LayeredLayerAssembler
    implements LayerAssembler
{
    public static final String NAME = "Domain Layer";

    @Override
    public LayerAssembly assemble( LayerAssembly layer )
        throws AssemblyException
    {
        return layer;
    }

    public static Function<Application, Module> typeFinder()
    {
        return application -> application.findModule( "Domain Layer", "TypeFinder Module" );
    }
}