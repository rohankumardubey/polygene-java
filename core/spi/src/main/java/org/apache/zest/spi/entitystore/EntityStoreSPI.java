/*
 * Copyright (c) 2009, Rickard Öberg. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.zest.spi.entitystore;

import org.apache.zest.api.entity.EntityDescriptor;
import org.apache.zest.api.entity.EntityReference;
import org.apache.zest.spi.entity.EntityState;
import org.apache.zest.spi.module.ModuleSpi;

/**
 * EntityStore SPI.
 */
public interface EntityStoreSPI
{
    EntityState newEntityState( EntityStoreUnitOfWork unitOfWork,
                                ModuleSpi module,
                                EntityReference identity, EntityDescriptor entityDescriptor
    );

    EntityState entityStateOf( EntityStoreUnitOfWork unitOfWork, ModuleSpi module, EntityReference identity );

    StateCommitter applyChanges( EntityStoreUnitOfWork unitOfWork, Iterable<EntityState> state
    );
}