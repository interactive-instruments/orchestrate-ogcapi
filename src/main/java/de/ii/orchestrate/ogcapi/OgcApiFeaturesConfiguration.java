package de.ii.orchestrate.ogcapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpHeaderNames;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.dotwebstack.orchestrate.model.Model;
import reactor.netty.http.client.HttpClient;

@Getter
public class OgcApiFeaturesConfiguration {

  final static HttpClient CLIENT = HttpClient.create().headers(
      h -> h.set(HttpHeaderNames.ACCEPT, "application/json,application/problem+json;q=0.8"));

  final static String CONFORMANCE_DECLARATION_TEMPLATE = "{apiLandingPage}/conformance";

  final static ObjectMapper MAPPER =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private final Model model;
  private final String apiLandingPage;
  private final int limit;
  private final Integer srid;
  private final boolean supportsPropertySelection;
  private final boolean supportsBatchLoading;
  private final boolean supportsAdHocQuery;
  private final boolean supportsCql2InOperator;
  private final boolean supportsRelProfiles;
  private final boolean supportsIntersects;

  @Builder(toBuilder = true)
  public OgcApiFeaturesConfiguration(Model model, String apiLandingPage, int limit, boolean supportsPropertySelection, boolean supportsRelProfiles) {
    this.model = model;
    this.apiLandingPage = apiLandingPage;
    this.limit = limit;
    this.srid = 28992; // TODO temporary fix
    this.supportsPropertySelection = supportsPropertySelection;
    this.supportsRelProfiles = supportsRelProfiles;
    var conformsTo = getConformanceDeclaration();
    this.supportsBatchLoading = conformsTo.stream()
        .anyMatch(uri -> uri.startsWith("http://www.opengis.net/spec/cql2/") && uri.endsWith("/conf/cql2-text")) &&
        conformsTo.stream().anyMatch(uri -> uri.startsWith("http://www.opengis.net/spec/ogcapi-features-3/") &&
            uri.endsWith("/conf/features-filter"));
    this.supportsCql2InOperator = supportsBatchLoading && conformsTo.stream().anyMatch(
        uri -> uri.startsWith("http://www.opengis.net/spec/cql2/") &&
            uri.endsWith("/conf/advanced-comparison-operators"));
    this.supportsAdHocQuery = conformsTo.stream()
        .anyMatch(uri -> uri.startsWith("http://www.opengis.net/spec/cql2/") &&
            uri.endsWith("/conf/advanced-comparison-operators")) &&
        conformsTo.stream().anyMatch(uri -> uri.startsWith("http://www.opengis.net/spec/ogcapi-features-") &&
            uri.endsWith("/conf/ad-hoc-queries"));
    this.supportsIntersects = conformsTo.stream()
        .anyMatch(uri -> uri.startsWith("http://www.opengis.net/spec/cql2/") && uri.endsWith("/conf/cql2-text")) &&
        conformsTo.stream().anyMatch(uri -> uri.startsWith("http://www.opengis.net/spec/ogcapi-features-3/") &&
            uri.endsWith("/conf/features-filter")) &&
        conformsTo.stream()
            .anyMatch(
                uri -> uri.startsWith("http://www.opengis.net/spec/cql2/") && uri.endsWith("/conf/spatial-operators"));
  }

  private List<String> getConformanceDeclaration() {
    var uri = CONFORMANCE_DECLARATION_TEMPLATE.replace("{apiLandingPage}", apiLandingPage);
    var conformanceDeclarationAsString = CLIENT.get()
        .uri(uri)
        .responseContent().aggregate().asString().block();
    try {
      var declaration = MAPPER.readValue(conformanceDeclarationAsString, ConformanceDeclaration.class);
      return declaration.getConformsTo();
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
