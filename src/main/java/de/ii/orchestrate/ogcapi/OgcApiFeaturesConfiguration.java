package de.ii.orchestrate.ogcapi;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import nl.geostandaarden.imx.orchestrate.model.Model;
import nl.geostandaarden.imx.orchestrate.source.SourceException;
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
  private final boolean supportsQueryablesAsQueryParameters;
  private final boolean supportsBatchLoading;
  private final boolean supportsAdHocQuery;
  private final boolean supportsCql2InOperator;
  private final boolean supportsRelProfiles;
  private final boolean supportsIntersects;

  @Builder(toBuilder = true)
  public OgcApiFeaturesConfiguration(@NonNull Model model, @NonNull String apiLandingPage, int limit,
                                     boolean supportsPropertySelection, boolean supportsRelProfiles) {
    this.model = model;
    this.apiLandingPage = apiLandingPage;
    this.limit = limit;
    this.srid = 28992; // TODO temporary fix
    this.supportsPropertySelection = supportsPropertySelection;
    this.supportsRelProfiles = supportsRelProfiles;

    var conformsTo = getConformanceDeclaration();
    validateCapabilities(model, conformsTo);
    this.supportsQueryablesAsQueryParameters = conformsTo.stream()
        .anyMatch(uri -> uri.startsWith("http://www.opengis.net/spec/ogcapi-features-3/") &&
            uri.endsWith("/conf/queryables-query-parameters"));
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


  private static void validateCapabilities(@NonNull Model model, @NonNull List<String> conformsTo) {
    if (conformsTo.stream().noneMatch(
        uri -> uri.equals("http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/core"))) {
      throw new SourceException("APIs must support the OGC API Feature 'Core' conformance class.");
    }
    if (conformsTo.stream().noneMatch(
        uri -> uri.equals("http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/geojson"))) {
      throw new SourceException("APIs must support the OGC API Feature 'GeoJSON' conformance class.");
    }
    if (conformsTo.stream().noneMatch(
        uri -> uri.equals("http://www.opengis.net/spec/ogcapi-features-2/1.0/conf/crs"))) {
      throw new SourceException(
          "APIs must support the OGC API Feature 'Coordinate Reference Systems by Reference' conformance class.");
    }
    // The following are drafts, so we accept any version number (and hope that the implementation is up-to-date).
    if (conformsTo.stream().noneMatch(
        uri -> uri.startsWith("http://www.opengis.net/spec/cql2/") && uri.endsWith("/conf/cql2-text"))) {
      throw new SourceException("APIs must support the OGC API Feature 'CQL2 Text' conformance class.");
    }
    if (conformsTo.stream().noneMatch(
        uri -> uri.startsWith("http://www.opengis.net/spec/ogcapi-features-3/") &&
            uri.endsWith("/conf/features-filter"))) {
      throw new SourceException("APIs must support the OGC API Feature 'Features Filter' conformance class.");
    }
  }

  private List<String> getConformanceDeclaration() {
    var uri = CONFORMANCE_DECLARATION_TEMPLATE.replace("{apiLandingPage}", apiLandingPage);
    return CLIENT.get()
        .uri(uri)
        .responseSingle((response, content) -> {
          if (response.status() != HttpResponseStatus.OK) {
            throw new SourceException(
                String.format("Conformance Declaration request returned a status different than 200: %d. URI: %s",
                    response.status().code(), uri));
          }
          return content.asByteArray();
        })
        .map(conformanceDeclarationAsByteArray -> {
          ConformanceDeclaration conformanceDeclaration;
          try {
            conformanceDeclaration = MAPPER.readValue(conformanceDeclarationAsByteArray, ConformanceDeclaration.class);
          } catch (IOException e) {
            throw new SourceException(
                "APIs must support the OGC API Conformance Declaration resource. Problem found: " + e.getMessage());
          }
          return conformanceDeclaration.getConformsTo();
        })
        .block();
  }
}
