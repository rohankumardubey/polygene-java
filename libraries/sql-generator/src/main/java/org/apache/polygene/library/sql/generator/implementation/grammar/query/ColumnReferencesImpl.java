/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package org.apache.polygene.library.sql.generator.implementation.grammar.query;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.polygene.library.sql.generator.grammar.common.SetQuantifier;
import org.apache.polygene.library.sql.generator.grammar.query.ColumnReferences;
import org.apache.polygene.library.sql.generator.implementation.transformation.spi.SQLProcessorAggregator;

/**
 *
 */
public class ColumnReferencesImpl extends SelectColumnClauseImpl<ColumnReferences>
    implements ColumnReferences
{

    private final List<ColumnReferenceInfo> _columns;

    public ColumnReferencesImpl( SQLProcessorAggregator processor, SetQuantifier quantifier,
                                 ColumnReferenceInfo... columns )
    {
        this( processor, quantifier, Arrays.asList( columns ) );
    }

    public ColumnReferencesImpl( SQLProcessorAggregator processor, SetQuantifier quantifier,
                                 List<ColumnReferenceInfo> columns )
    {
        this( processor, ColumnReferences.class, quantifier, columns );
    }

    public ColumnReferencesImpl( SQLProcessorAggregator processor, Class<? extends ColumnReferences> type,
                                 SetQuantifier quantifier, List<ColumnReferenceInfo> columns )
    {
        super( processor, type, quantifier );
        Objects.requireNonNull( columns, "columns" );
        if( columns.isEmpty() )
        {
            throw new IllegalArgumentException( "Must have at least one column in column list." );
        }

        for( ColumnReferenceInfo column : columns )
        {
            Objects.requireNonNull( column, "column" );
        }

        this._columns = Collections.unmodifiableList( columns );
    }

    public List<ColumnReferenceInfo> getColumns()
    {
        return this._columns;
    }

    @Override
    protected boolean doesEqual( ColumnReferences another )
    {
        return super.doesEqual( another ) && this._columns.equals( another.getColumns() );
    }
}
