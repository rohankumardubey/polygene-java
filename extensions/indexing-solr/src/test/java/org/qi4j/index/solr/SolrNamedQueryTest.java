/*
 * Copyright 2012 Paul Merlin.
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
package org.qi4j.index.solr;

import java.io.File;
import java.util.function.Predicate;
import org.junit.Ignore;
import org.junit.Rule;
import org.qi4j.api.composite.Composite;
import org.qi4j.bootstrap.AssemblyException;
import org.qi4j.bootstrap.ModuleAssembly;
import org.qi4j.library.fileconfig.FileConfigurationOverride;
import org.qi4j.library.fileconfig.FileConfigurationService;
import org.qi4j.test.indexing.AbstractNamedQueryTest;
import org.qi4j.test.util.DelTreeAfter;

@Ignore( "SOLR Index/Query is not working at all" )
public class SolrNamedQueryTest
    extends AbstractNamedQueryTest
{

    private static final File DATA_DIR = new File( "build/tmp/solr-named-query-test" );
    @Rule
    public final DelTreeAfter delTreeAfter = new DelTreeAfter( DATA_DIR );

    @Override
    public void assemble( ModuleAssembly module )
        throws AssemblyException
    {
        super.assemble( module );
        FileConfigurationOverride override = new FileConfigurationOverride().withData( new File( DATA_DIR, "qi4j-data" ) ).
            withLog( new File( DATA_DIR, "qi4j-logs" ) ).withTemporary( new File( DATA_DIR, "qi4j-temp" ) );
        module.services( FileConfigurationService.class ).
            setMetaInfo( override );
        new SolrAssembler().assemble( module );
    }

    @Override
    protected String[] queryStrings()
    {
        return new String[]
        {
        }; // TODO Write example Solr named queries
    }

    @Override
    protected Predicate<Composite> createNamedQueryDescriptor( String queryName, String queryString )
    {
        return SolrExpressions.search( queryString );
    }

}
