/*
 * Copyright 2008 Alin Dreghiciu.
 * Copyright 2009 Niclas Hedhman.
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
package org.qi4j.index.rdf.internal;

import static java.lang.String.format;
import java.util.logging.Logger;
import org.qi4j.api.query.grammar.AssociationIsNullPredicate;
import org.qi4j.api.query.grammar.AssociationNullPredicate;
import org.qi4j.api.query.grammar.BooleanExpression;
import org.qi4j.api.query.grammar.ComparisonPredicate;
import org.qi4j.api.query.grammar.Conjunction;
import org.qi4j.api.query.grammar.Disjunction;
import org.qi4j.api.query.grammar.MatchesPredicate;
import org.qi4j.api.query.grammar.Negation;
import org.qi4j.api.query.grammar.OrderBy;
import org.qi4j.api.query.grammar.PropertyIsNullPredicate;
import org.qi4j.api.query.grammar.PropertyNullPredicate;
import org.qi4j.api.query.grammar.SingleValueExpression;
import org.qi4j.api.query.grammar.ValueExpression;
import org.qi4j.index.rdf.RdfQueryParser;

/**
 * JAVADOC Add JavaDoc
 */
public class SparqlRdfQueryParser
    implements RdfQueryParser
{
    private Namespaces namespaces = new Namespaces();
    private Triples triples = new Triples( namespaces );

    public SparqlRdfQueryParser()
    {
    }

    public String getQuery( final String resultType,
                            final BooleanExpression whereClause,
                            final OrderBy[] orderBySegments,
                            final Integer firstResult,
                            final Integer maxResults )
    {
        triples.addDefaultTriples( resultType );

        // and collect namespaces
        final String filter = processFilter( whereClause );
        final String orderBy = processOrderBy( orderBySegments );

        StringBuilder query = new StringBuilder();

        for( String namespace : namespaces.getNamespaces() )
        {
            query.append( format( "PREFIX %s: <%s> %n", namespaces.getNamespacePrefix( namespace ), namespace ) );
        }
        query.append( "SELECT DISTINCT ?entityType ?identity\n" );
        if( triples.hasTriples() )
        {
            query.append( "WHERE {\n" );
            for( Triples.Triple triple : triples )
            {
                final String subject = triple.getSubject();
                final String predicate = triple.getPredicate();
                final String value = triple.getValue();

                if( triple.isOptional() )
                {
                    query.append( format( "OPTIONAL {%s %s %s}. ", subject, predicate, value ) );
                }
                else
                {
                    query.append( format( "%s %s %s. ", subject, predicate, value ) );
                }
                query.append( '\n' );
            }

            if( filter.length() > 0 )
            {
                query.append( "FILTER " ).append( filter );
            }
            query.append( "\n}" );
        }
        if( orderBy != null )
        {
            query.append( "\nORDER BY " ).append( orderBy );
        }
        if( firstResult != null )
        {
            query.append( "\nOFFSET " ).append( firstResult );
        }
        if( maxResults != null )
        {
            query.append( "\nLIMIT " ).append( maxResults );
        }

        Logger.getLogger( getClass().getName() ).fine( "Query:\n" + query );
        return query.toString();
    }

    private String processFilter( final BooleanExpression expression )
    {
        if( expression == null )
        {
            return "";
        }
        if( expression instanceof Conjunction )
        {
            final Conjunction conjunction = (Conjunction) expression;
            return format( "(%s && %s)",
                           processFilter( conjunction.leftSideExpression() ),
                           processFilter( conjunction.rightSideExpression() ) );
        }
        if( expression instanceof Disjunction )
        {
            final Disjunction disjunction = (Disjunction) expression;
            return format( "(%s || %s)",
                           processFilter( disjunction.leftSideExpression() ),
                           processFilter( disjunction.rightSideExpression() ) );
        }
        if( expression instanceof Negation )
        {
            return format( "(!%s)", processFilter( ( (Negation) expression ).expression() ) );
        }
        if( expression instanceof MatchesPredicate )
        {
            return processMatchesPredicate( (MatchesPredicate) expression );
        }
        if( expression instanceof ComparisonPredicate )
        {
            return processComparisonPredicate( (ComparisonPredicate) expression );
        }
        if( expression instanceof PropertyNullPredicate )
        {
            return processNullPredicate( (PropertyNullPredicate) expression );
        }
        if( expression instanceof AssociationNullPredicate )
        {
            return processNullPredicate( (AssociationNullPredicate) expression );
        }
        throw new UnsupportedOperationException( "Expression " + expression + " is not supported" );
    }

    private String processMatchesPredicate( final MatchesPredicate predicate )
    {
        ValueExpression valueExpression = predicate.valueExpression();
        if( valueExpression instanceof SingleValueExpression )
        {
            String valueVariable = triples.addTriple( predicate.propertyReference(), false ).getValue();
            final SingleValueExpression singleValueExpression = (SingleValueExpression) valueExpression;
            return format( "regex(%s,\"%s\")", valueVariable, singleValueExpression.value() );
        }
        else
        {
            throw new UnsupportedOperationException( "Value " + valueExpression + " is not supported" );
        }
    }

    private String processComparisonPredicate( final ComparisonPredicate predicate )
    {
        ValueExpression valueExpression = predicate.valueExpression();
        if( valueExpression instanceof SingleValueExpression )
        {
            String valueVariable = triples.addTriple( predicate.propertyReference(), false ).getValue();
            final SingleValueExpression singleValueExpression = (SingleValueExpression) valueExpression;
            return String.format( "(%s %s \"%s\")", valueVariable, Operators.getOperator( predicate.getClass() ),
                                  singleValueExpression.value() );
        }
        else
        {
            throw new UnsupportedOperationException( "Value " + valueExpression + " is not supported" );
        }
    }

    private String processNullPredicate( final PropertyNullPredicate predicate )
    {
        final String value = triples.addTriple( predicate.propertyReference(), true ).getValue();
        if( predicate instanceof PropertyIsNullPredicate )
        {
            return format( "(! bound(%s))", value );
        }
        else
        {
            return format( "(bound(%s))", value );
        }
    }

    private String processNullPredicate( final AssociationNullPredicate predicate )
    {
        final String value = triples.addTriple( predicate.associationReference(), true ).getValue();
        if( predicate instanceof AssociationIsNullPredicate )
        {
            return format( "(! bound(%s))", value );
        }
        else
        {
            return format( "(bound(%s))", value );
        }
    }

    private String processOrderBy( OrderBy[] orderBySegments )
    {
        if( orderBySegments != null && orderBySegments.length > 0 )
        {
            final StringBuilder orderBy = new StringBuilder();
            for( OrderBy orderBySegment : orderBySegments )
            {
                if( orderBySegment != null )
                {
                    final String valueVariable = triples.addTriple( orderBySegment.propertyReference(), false ).getValue();
                    if( orderBySegment.order() == OrderBy.Order.ASCENDING )
                    {
                        orderBy.append( format( "ASC(%s)", valueVariable ) );
                    }
                    else
                    {
                        orderBy.append( format( "DESC(%s)", valueVariable ) );
                    }
                }
            }
            return orderBy.length() > 0 ? orderBy.toString() : null;
        }
        return null;
    }
}