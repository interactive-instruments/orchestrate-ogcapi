package de.ii.orchestrate.ogcapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.com.google.common.collect.ImmutableList;
import graphql.com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.HttpHeaderNames;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.dotwebstack.orchestrate.model.Attribute;
import org.dotwebstack.orchestrate.model.Model;
import org.dotwebstack.orchestrate.model.ObjectType;
import org.dotwebstack.orchestrate.model.Relation;
import org.dotwebstack.orchestrate.source.BatchRequest;
import org.dotwebstack.orchestrate.source.CollectionRequest;
import org.dotwebstack.orchestrate.source.DataRepository;
import org.dotwebstack.orchestrate.source.ObjectRequest;
import org.dotwebstack.orchestrate.source.SelectedProperty;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

class OgcApiFeaturesDataRepository implements DataRepository {

  final static HttpClient CLIENT = HttpClient.create()
      .headers(h -> h.set(HttpHeaderNames.ACCEPT,
          "application/geo+json,application/problem+json;q=0.8,application/json;q=0.7"));

  final static String ONE_TEMPLATE = "{apiLandingPage}/collections/{collectionId}/items/{featureId}";

  // limit to 10 features for now until there is paging support
  final static String COLLECTION_TEMPLATE = "{apiLandingPage}/collections/{collectionId}/items?limit={limit}";

  final static ObjectMapper MAPPER = new ObjectMapper();
  public static final String PATH_SEPARATOR = ".";

  private final String apiLandingPage;
  private final int limit;
  private final boolean supportsPropertySelection;
  private final Model model;

  public OgcApiFeaturesDataRepository(OgcApiFeaturesConfiguration configuration) {
    this.apiLandingPage = configuration.getApiLandingPage();
    this.limit = configuration.getLimit();
    this.supportsPropertySelection = configuration.isSupportsPropertySelection();
    this.model = configuration.getModel();
  }

  @Override
  public boolean supportsBatchLoading(ObjectType objectType) {
    return true;
  }

  @Override
  public Mono<Map<String, Object>> findOne(ObjectRequest objectRequest) {
    var collectionId = getCollectionId(objectRequest.getObjectType());
    var featureId = (String) objectRequest.getObjectKey().get("identificatie");
    var properties = supportsPropertySelection ?
        "?properties=" + getPropertiesParameter(objectRequest.getSelectedProperties(), ImmutableList.of()) : "";
    return CLIENT
        .get()
        .uri(ONE_TEMPLATE.replace("{apiLandingPage}", apiLandingPage)
            .replace("{collectionId}", collectionId)
            .replace("{featureId}", featureId)
            + properties)
        .responseContent()
        .aggregate()
        .asString()
        .map(geojsonFeatureAsString -> {
          try {
            //noinspection unchecked
            return (Map<String, Object>) getFeature(objectRequest.getSelectedProperties(),
                MAPPER.readValue(geojsonFeatureAsString, Map.class));
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Override
  public Flux<Map<String, Object>> find(CollectionRequest collectionRequest) {
    var collectionId = getCollectionId(collectionRequest.getObjectType());
    var filterExpression = collectionRequest.getFilter();
    var filter = filterExpression != null ?
        String.format("&%s=%s", String.join(PATH_SEPARATOR, filterExpression.getPropertyPath().getSegments()),
            filterExpression.getValue()) : "";
    var properties = supportsPropertySelection ? String.format("&properties=%s",
        getPropertiesParameter(collectionRequest.getSelectedProperties(), ImmutableList.of())) : "";
    return CLIENT
        .get()
        .uri(COLLECTION_TEMPLATE.replace("{apiLandingPage}", apiLandingPage)
            .replace("{collectionId}", collectionId)
            .replace("{limit}", String.valueOf(limit))
            + filter
            + properties)
        .responseContent()
        .aggregate()
        .asString()
        .map(geojsonFeatureCollectionAsString -> {
          Map<String, Object> geojsonFeatureCollection;
          try {
            //noinspection unchecked
            geojsonFeatureCollection = MAPPER.readValue(geojsonFeatureCollectionAsString, Map.class);
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }

          //noinspection unchecked
          return ((List<Map<String, Object>>) geojsonFeatureCollection.get("features")).stream()
              .map(geojsonFeature -> getFeature(collectionRequest.getSelectedProperties(), geojsonFeature))
              .toList();
        })
        .flatMapMany(Flux::fromIterable);
  }

  @Override
  public Flux<Map<String, Object>> findBatch(BatchRequest batchRequest) {
    var collectionId = getCollectionId(batchRequest.getObjectType());
    var properties = supportsPropertySelection ? String.format("&properties=%s",
        getPropertiesParameter(batchRequest.getSelectedProperties(), ImmutableList.of())) : "";
    var objectKeys = batchRequest.getObjectKeys().stream().map(id -> (String) id.get("identificatie")).toList();
    var filter =
        String.format("&filter=%s%%20in%%20['%s']", "identificatie", String.join("', '", objectKeys));

    return CLIENT
        .get()
        .uri(COLLECTION_TEMPLATE.replace("{apiLandingPage}", apiLandingPage)
            .replace("{collectionId}", collectionId)
            .replace("{limit}", String.valueOf(objectKeys.size()))
            + filter
            + properties)
        .responseContent()
        .aggregate()
        .asString()
        .map(geojsonFeatureCollectionAsString -> {
          Map<String, Object> geojsonFeatureCollection;
          try {
            //noinspection unchecked
            geojsonFeatureCollection = MAPPER.readValue(geojsonFeatureCollectionAsString, Map.class);
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }

          //noinspection unchecked
          return ((List<Map<String, Object>>) geojsonFeatureCollection.get("features")).stream()
              .map(geojsonFeature -> getFeature(batchRequest.getSelectedProperties(), geojsonFeature))
              .toList();
        })
        .flatMapMany(Flux::fromIterable);
  }

  private String getCollectionId(ObjectType objectType) {
    return objectType.getName();
  }

  private String getPropertiesParameter(List<SelectedProperty> selectedProperties, List<String> parentPath) {
    return selectedProperties.stream()
        .map(selectedProperty -> {
          var propertyName = selectedProperty.getProperty().getName();
          var subProperties = selectedProperty.getSelectedProperties();
          var path = ImmutableList.<String>builder().addAll(parentPath).add(propertyName).build();
          if (subProperties.isEmpty()) {
            return String.join(PATH_SEPARATOR, path);
          }
          return getPropertiesParameter(subProperties, path);
        })
        .collect(Collectors.joining(","));
  }

  private Map<String, Object> getFeature(List<SelectedProperty> selectedProperties,
                                         Map<String, Object> geojsonFeature) {
    //noinspection unchecked
    var featureProperties = geojsonFeature.containsKey("properties")
        ? (Map<String, Object>) geojsonFeature.get("properties")
        : ImmutableMap.<String, Object>of();

    // ignoring geometry for now
    return getObject(selectedProperties, geojsonFeature.get("id"), featureProperties);
  }

  private Map<String, Object> getObject(List<SelectedProperty> selectedProperties, Object featureId,
                                        Map<String, Object> featureProperties) {
    var builder = ImmutableMap.<String, Object>builder();
    selectedProperties.forEach(
        selectedProperty -> processProperty(builder, selectedProperty, featureId, featureProperties));
    return builder.build();
  }

  private void processProperty(ImmutableMap.Builder<String, Object> builder, SelectedProperty selectedProperty,
                               Object featureId, Map<String, Object> featureProperties) {
    var key = selectedProperty.getProperty().getName();
    if (selectedProperty.getProperty().isIdentifier() && featureId != null) {
      builder.put(key, featureId);
    } else if (selectedProperty.getProperty() instanceof Attribute attribute) {
      processAttribute(builder, selectedProperty, featureProperties, key, attribute);
    } else if (selectedProperty.getProperty() instanceof Relation) {
      processAssociationRole(builder, selectedProperty, featureProperties, key);
    }
  }

  private void processAssociationRole(ImmutableMap.Builder<String, Object> builder, SelectedProperty selectedProperty,
                                      Map<String, Object> featureProperties, String key) {
    var value = featureProperties.get(key);
    if (value != null) {
      if (value instanceof List) {
        //noinspection unchecked
        var valueAsList = (List<Map<String, Object>>) value;
        var listBuilder = ImmutableList.<Map<String, Object>>builder();
        valueAsList.forEach(refProperties -> listBuilder.add(
            getObject(selectedProperty.getSelectedProperties(), null, refProperties)));
        builder.put(key, listBuilder.build());
      } else if (value instanceof Map) {
        //noinspection unchecked
        builder.put(key, getObject(selectedProperty.getSelectedProperties(), null, (Map<String, Object>) value));
      } else {
        throw new RuntimeException(String.format("Unsupported value type: %s", value.getClass().getSimpleName()));
      }
    }
  }

  private void processAttribute(ImmutableMap.Builder<String, Object> builder, SelectedProperty sp,
                                Map<String, Object> featureProperties, String key, Attribute attribute) {
    var value = featureProperties.get(key);
    if (value != null) {
      switch (attribute.getType().getName()) {
        case "Boolean", "Double", "Float", "Integer", "Long", "String" -> builder.put(key, value);
        default -> throw new RuntimeException(
            String.format("Unsupported attribute type: %s", ((Attribute) sp.getProperty()).getType().getName()));
      }
    }
  }
}
