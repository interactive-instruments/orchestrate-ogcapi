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

  private final GatewayProperties gatewayProperties;

  public GatewayConfiguration(GatewayProperties gatewayProperties) {
    this.gatewayProperties = gatewayProperties;
  }

  @Bean
  public GraphQlSource graphQlSource() {
    var sourceConfigurationBag = OgcApiFeaturesConfiguration.builder()
        .model(TestFixtures.SOURCE_MODEL_BAG)
        // .apiLandingPage("http://localhost:8080/rest/services/bag")x^
        .apiLandingPage("https://wau.ldproxy.net/bag")
        .limit(10)
        .supportsPropertySelection(true)
        .build();

    var sourceConfigurationBgt = OgcApiFeaturesConfiguration.builder()
        .model(TestFixtures.SOURCE_MODEL_BGT)
        // .apiLandingPage("http://localhost:8080/rest/services/bgt")x^
        .apiLandingPage("https://wau.ldproxy.net/bgt")
        .limit(10)
        .supportsPropertySelection(true)
        .build();

    var orchestration = Orchestration.builder()
        .modelMapping(TestFixtures.createModelMapping(gatewayProperties.getTargetModel(),
            GatewayConfiguration.class.getResourceAsStream(gatewayProperties.getMapping())))
        .source("bag", new OgcApiFeaturesSource(sourceConfigurationBag))
        .source("bgt", new OgcApiFeaturesSource(sourceConfigurationBgt))
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
