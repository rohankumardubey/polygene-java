/*
 * Copyright (c) 2010, Rickard Öberg. All Rights Reserved.
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

package org.apache.zest.index.rdf;

/**
 * JAVADOC
 */

import java.io.File;
import org.junit.Ignore;
import org.junit.Test;
import org.apache.zest.api.common.UseDefaults;
import org.apache.zest.api.common.Visibility;
import org.apache.zest.api.entity.EntityComposite;
import org.apache.zest.api.association.ManyAssociation;
import org.apache.zest.api.property.Property;
import org.apache.zest.api.query.Query;
import org.apache.zest.api.query.QueryExpressions;
import org.apache.zest.api.unitofwork.UnitOfWork;
import org.apache.zest.bootstrap.Assembler;
import org.apache.zest.bootstrap.AssemblyException;
import org.apache.zest.bootstrap.ModuleAssembly;
import org.apache.zest.index.rdf.assembly.RdfNativeSesameStoreAssembler;
import org.apache.zest.library.fileconfig.FileConfigurationService;
import org.apache.zest.library.rdf.repository.NativeConfiguration;
import org.apache.zest.spi.query.IndexExporter;
import org.apache.zest.test.AbstractZestTest;
import org.apache.zest.test.EntityTestAssembler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.Rule;
import org.apache.zest.test.util.DelTreeAfter;

public class RDFPerformanceTest extends AbstractZestTest
{
    private static final Logger LOG = LoggerFactory.getLogger( RDFPerformanceTest.class );
    private static final File DATA_DIR = new File( "build/tmp/rdf-performance-test" );
    @Rule
    public final DelTreeAfter delTreeAfter = new DelTreeAfter( DATA_DIR );

    public interface ExampleEntity extends EntityComposite
    {
        @UseDefaults
        Property<String> someProperty();

        ManyAssociation<ExampleEntity> manyAssoc();
    }

    @Override
    public void assemble( ModuleAssembly module ) throws AssemblyException
    {
        module.services( FileConfigurationService.class );
        ModuleAssembly prefModule = module.layer().module( "PrefModule" );
        prefModule.entities( NativeConfiguration.class ).visibleIn( Visibility.application );
        prefModule.forMixin( NativeConfiguration.class ).declareDefaults().tripleIndexes().set( "spoc,cspo" );
        prefModule.forMixin( NativeConfiguration.class ).declareDefaults().dataDirectory().set( DATA_DIR.getAbsolutePath() );
        new EntityTestAssembler().assemble( prefModule );

        module.entities( ExampleEntity.class );

        EntityTestAssembler testAss = new EntityTestAssembler();
        testAss.assemble( module );

        Assembler rdfAssembler = new RdfNativeSesameStoreAssembler();
        rdfAssembler.assemble( module );
    }


    private List<ExampleEntity> doCreate( int howMany )
    {
        List<ExampleEntity> result = new ArrayList<ExampleEntity>( howMany );

        List<ExampleEntity> entities = new ArrayList<ExampleEntity>();
        for (Integer x = 0; x < howMany; ++x)
        {
            ExampleEntity exampleEntity = this.uowf.currentUnitOfWork().newEntity( ExampleEntity.class, "entity" + x );

            for (ExampleEntity entity : entities)
            {
                exampleEntity.manyAssoc().add( entity );
            }

            entities.add( exampleEntity );
            if (entities.size() > 10)
                entities.remove( 0 );

            result.add( exampleEntity );
        }

        return result;
    }

    private void doRemoveAll( List<ExampleEntity> entities )
    {
        for (ExampleEntity entity : entities)
        {
            this.uowf.currentUnitOfWork().remove( this.uowf.currentUnitOfWork().get( entity ) );
        }
    }

    private List<ExampleEntity> doList( int howMany )
    {
        List<ExampleEntity> list = new ArrayList<ExampleEntity>();
        UnitOfWork uow = this.uowf.newUnitOfWork();
        Iterator<ExampleEntity> iter = uow.newQuery( this.module.newQueryBuilder( ExampleEntity.class ) ).iterator();
        int found = 0;
        while (iter.hasNext())
        {
            found++;
            ExampleEntity exampleEntity = iter.next();
            if (exampleEntity != null)
                list.add( exampleEntity );
        }

        uow.discard();

        if (found != howMany)
        {
            LOG.warn( "Found " + found + " entities instead of " + howMany + "." );
        }

        return list;
    }


    private void doRemove( int howMany )
    {
        Iterator<ExampleEntity> iter = this.uowf.currentUnitOfWork().newQuery( this.module.newQueryBuilder( ExampleEntity.class )).maxResults( howMany ).iterator();
        Integer removed = 0;
        while (iter.hasNext())
        {
            this.uowf.currentUnitOfWork().remove( iter.next() );
            ++removed;
        }

        if (removed != howMany)
        {
            LOG.warn( "Removed " + removed + " entities instead of " + howMany + "." );
        }
    }

    private void performTest( int howMany ) throws Exception
    {
        long startTest = System.currentTimeMillis();

        UnitOfWork creatingUOW = this.uowf.newUnitOfWork();
        Long startingTime = System.currentTimeMillis();
        List<ExampleEntity> entities = this.doCreate( howMany );
        LOG.info( "Time to create " + howMany + " entities (ms): " + (System.currentTimeMillis() - startingTime) );

        startingTime = System.currentTimeMillis();
        creatingUOW.complete();
        LOG.info( "Time to complete creation uow (ms): " + (System.currentTimeMillis() - startingTime) );


        List<ExampleEntity> entityList = this.doList( howMany );

        startingTime = System.currentTimeMillis();
        UnitOfWork uow = this.uowf.newUnitOfWork();
        for (int i = 0; i < 1000; i++)
        {
            Query<ExampleEntity> query = uow.newQuery( this.module.newQueryBuilder( ExampleEntity.class ).
                    where( QueryExpressions.contains( QueryExpressions.templateFor( ExampleEntity.class ).manyAssoc(), uow.get( ExampleEntity.class, "entity50" ) ) ));
            System.out.println(query.count());
        }

        long endTest = System.currentTimeMillis();
        LOG.info( "Time to query " + howMany + " entities (ms): " + (endTest - startingTime) );

        UnitOfWork deletingUOW = this.uowf.newUnitOfWork();
        startingTime = System.currentTimeMillis();
        this.doRemoveAll( entityList );
//      this.doRemove(200);
        LOG.info( "Time to delete " + howMany + " entities (ms): " + (System.currentTimeMillis() - startingTime) );

        startingTime = System.currentTimeMillis();
        deletingUOW.complete();

        endTest = System.currentTimeMillis();

        LOG.info( "time to complete deletion uow (ms): " + (endTest - startingTime) );

        LOG.info( "time to complete test (ms): " + (endTest - startTest) );

    }

    @Test
    public void dummy()
    {
        // Dummy test to make Maven happy
    }

    // @Test
    public void performanceTest200() throws Exception
    {
        this.performTest( 200 );
        this.performTest( 200 );
        this.performTest( 200 );
        this.performTest( 200 );
        this.performTest( 200 );
        this.performTest( 200 );
        this.performTest( 200 );
        this.performTest( 200 );

        IndexExporter indexerExporter =
                module.findService( IndexExporter.class ).get();
        indexerExporter.exportReadableToStream( System.out );
    }

    @Ignore
    @Test
    public void performanceTest1000() throws Exception
    {
        this.performTest( 1000 );
    }

    @Ignore
    @Test
    public void performanceTest5000() throws Exception
    {
        this.performTest( 5000 );
    }

    @Ignore
    @Test
    public void performanceTest10000() throws Exception
    {
        this.performTest( 10000 );
    }

    @Ignore
    @Test
    public void performanceTest100000() throws Exception
    {
        this.performTest( 100000 );
    }
}
