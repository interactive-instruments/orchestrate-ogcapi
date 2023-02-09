package de.ii.orchestrate.test;

import de.ii.orchestrate.ogcapi.OgcApiFeaturesConfiguration;
import de.ii.orchestrate.ogcapi.OgcApiFeaturesSource;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import org.dotwebstack.orchestrate.engine.Orchestration;
import org.dotwebstack.orchestrate.engine.schema.SchemaFactory;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.DefaultExecutionGraphQlService;
import org.springframework.graphql.execution.GraphQlSource;

@Configuration
@EnableConfigurationProperties(GraphQlProperties.class)
public class GatewayConfiguration {

  @Bean
  public GraphQlSource graphQlSource() {
    var sourceConfiguration = OgcApiFeaturesConfiguration.builder()
        .model(TestFixtures.SOURCE_MODEL)
        // .apiLandingPage("http://localhost:8080/rest/services/bag")
        .apiLandingPage("https://wau.ldproxy.net/bag")
        .limit(10)
        .supportsPropertySelection(true)
        .build();

    var orchestration = Orchestration.builder()
        .modelMapping(TestFixtures.createModelMapping())
        .source("bag", new OgcApiFeaturesSource(sourceConfiguration))
        .build();

    var graphQL = GraphQL.newGraphQL(SchemaFactory.create(orchestration)).build();

    return new GraphQlSource() {
      @Override
      public GraphQL graphQl() {
        return graphQL;
      }

      @Override
      public GraphQLSchema schema() {
        return graphQL.getGraphQLSchema();
      }
    };
  }

  @Bean
  public DefaultExecutionGraphQlService graphQlService(GraphQlSource graphQlSource) {
    return new DefaultExecutionGraphQlService(graphQlSource);
  }
}
