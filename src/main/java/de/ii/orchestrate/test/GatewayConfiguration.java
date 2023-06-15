package de.ii.orchestrate.test;

import de.ii.orchestrate.ogcapi.OgcApiFeaturesConfiguration;
import de.ii.orchestrate.ogcapi.OgcApiFeaturesSource;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import java.util.Set;
import org.dotwebstack.orchestrate.engine.Orchestration;
import org.dotwebstack.orchestrate.engine.schema.SchemaFactory;
import org.dotwebstack.orchestrate.ext.spatial.GeometryExtension;
import org.dotwebstack.orchestrate.model.ComponentFactory;
import org.dotwebstack.orchestrate.model.loader.ModelLoader;
import org.dotwebstack.orchestrate.model.loader.ModelLoaderRegistry;
import org.dotwebstack.orchestrate.parser.yaml.YamlModelMappingParser;
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
  private ModelLoaderRegistry modelLoaderRegistry;

  // TODO temporary to detect availability of modelloaders;
  private Set<ModelLoader> modelLoaders;

  public GatewayConfiguration(GatewayProperties gatewayProperties, Set<ModelLoader> modelLoaders) {
    this.gatewayProperties = gatewayProperties;
    this.modelLoaderRegistry = ModelLoaderRegistry.getInstance();
    modelLoaders.forEach(modelLoaderRegistry::registerModelLoader);
    this.modelLoaders = modelLoaders;
  }

  @Bean
  public GraphQlSource graphQlSource() {
    var sourceConfigurationBag = OgcApiFeaturesConfiguration.builder()
        .model(TestFixtures.createBagModel())
        //.apiLandingPage("http://localhost:7080/rest/services/bag")
        .apiLandingPage("https://wau.ldproxy.net/bag")
        .limit(10)
        .supportsPropertySelection(true)
        .supportsRelProfiles(true)
        .build();

    var sourceConfigurationBgt = OgcApiFeaturesConfiguration.builder()
        .model(TestFixtures.createBgtModel())
        //.apiLandingPage("http://localhost:7080/rest/services/bgt")
        .apiLandingPage("https://wau.ldproxy.net/bgt")
        .limit(10)
        .supportsPropertySelection(true)
        .supportsRelProfiles(true)
        .build();

    var sourceConfigurationPerceel = OgcApiFeaturesConfiguration.builder()
        .model(TestFixtures.createBrkModel())
        //.apiLandingPage("http://localhost:7080/rest/services/perceel")
        .apiLandingPage("https://wau.ldproxy.net/perceel")
        .limit(10)
        .supportsPropertySelection(true)
        .supportsRelProfiles(false)
        .build();

    var sourceConfigurationBestuurlijkeGebieden = OgcApiFeaturesConfiguration.builder()
        .model(TestFixtures.createBestuurlijkeGebiedenModel())
        //.apiLandingPage("http://localhost:7080/rest/services/bestuurlijke-gebieden")
        .apiLandingPage("https://wau.ldproxy.net/bestuurlijke-gebieden")
        .limit(10)
        .supportsPropertySelection(true)
        .supportsRelProfiles(true)
        .build();

    var sourceConfigurationWaterschap = OgcApiFeaturesConfiguration.builder()
        .model(TestFixtures.createWatershaapModel())
        //.apiLandingPage("http://localhost:7080/rest/services/waterschap")
        .apiLandingPage("https://wau.ldproxy.net/waterschap")
        .limit(10)
        .supportsPropertySelection(true)
        .supportsRelProfiles(false)
        .build();

    var extensions = Set.of(new GeometryExtension());
    var componentFactory = new ComponentFactory();
    extensions.forEach(extension -> extension.registerComponents(componentFactory));
    var yamlModelMappingParser = YamlModelMappingParser.getInstance(componentFactory, modelLoaderRegistry);

    var orchestration = Orchestration.builder()
        .modelMapping(TestFixtures.createModelMapping(
            GatewayConfiguration.class.getResourceAsStream(gatewayProperties.getMapping()), yamlModelMappingParser,
            modelLoaders.isEmpty()))
        .source("bag", new OgcApiFeaturesSource(sourceConfigurationBag))
        .source("bgt", new OgcApiFeaturesSource(sourceConfigurationBgt))
        .source("brk", new OgcApiFeaturesSource(sourceConfigurationPerceel))
        .source("wat", new OgcApiFeaturesSource(sourceConfigurationWaterschap))
        .source("geb", new OgcApiFeaturesSource(sourceConfigurationBestuurlijkeGebieden))
        .extension(new GeometryExtension())
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
