package de.ii.orchestrate.ogcapi;

import static nl.geostandaarden.imx.orchestrate.model.Cardinality.INFINITE;

import graphql.com.google.common.collect.ImmutableMap;
import graphql.com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import nl.geostandaarden.imx.orchestrate.ext.spatial.GeometryType;
import nl.geostandaarden.imx.orchestrate.model.Attribute;
import nl.geostandaarden.imx.orchestrate.model.Cardinality;
import nl.geostandaarden.imx.orchestrate.model.Model;
import nl.geostandaarden.imx.orchestrate.model.ObjectType;
import nl.geostandaarden.imx.orchestrate.model.ObjectTypeRef;
import nl.geostandaarden.imx.orchestrate.model.Path;
import nl.geostandaarden.imx.orchestrate.model.Relation;
import nl.geostandaarden.imx.orchestrate.model.filters.EqualsOperatorType;
import nl.geostandaarden.imx.orchestrate.model.filters.FilterExpression;
import nl.geostandaarden.imx.orchestrate.model.types.ScalarTypes;
import nl.geostandaarden.imx.orchestrate.source.BatchRequest;
import nl.geostandaarden.imx.orchestrate.source.CollectionRequest;
import nl.geostandaarden.imx.orchestrate.source.ObjectRequest;
import nl.geostandaarden.imx.orchestrate.source.SelectedProperty;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

public class OgcApiFeaturesDataRepositoryTest {

  private static final Model BAG = Model.builder()
      .alias("bag")
      .objectType(ObjectType.builder()
          .name("Nummeraanduiding")
          .property(Attribute.builder()
              .name("identificatie")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.REQUIRED)
              .identifier(true)
              .build())
          .property(Attribute.builder()
              .name("huisnummer")
              .type(ScalarTypes.INTEGER)
              .cardinality(Cardinality.REQUIRED)
              .build())
          .property(Attribute.builder()
              .name("huisnummertoevoeging")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.OPTIONAL)
              .build())
          .property(Attribute.builder()
              .name("huisletter")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.OPTIONAL)
              .build())
          .property(Attribute.builder()
              .name("postcode")
              .type(ScalarTypes.STRING)
              .build())
          .property(Relation.builder()
              .name("ligtAan")
              .target(ObjectTypeRef.forType("OpenbareRuimte"))
              .cardinality(Cardinality.REQUIRED)
              .build())
          .property(Relation.builder()
              .name("ligtIn")
              .target(ObjectTypeRef.forType("Woonplaats"))
              .cardinality(Cardinality.OPTIONAL)
              .build())
          .build())
      .objectType(ObjectType.builder()
          .name("OpenbareRuimte")
          .property(Attribute.builder()
              .name("identificatie")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.REQUIRED)
              .identifier(true)
              .build())
          .property(Attribute.builder()
              .name("naam")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.REQUIRED)
              .build())
          .property(Relation.builder()
              .name("ligtIn")
              .target(ObjectTypeRef.forType("Woonplaats"))
              .cardinality(Cardinality.REQUIRED)
              .build())
          .build())
      .objectType(ObjectType.builder()
          .name("Woonplaats")
          .property(Attribute.builder()
              .name("identificatie")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.REQUIRED)
              .identifier(true)
              .build())
          .property(Attribute.builder()
              .name("naam")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.REQUIRED)
              .build())
          .build())
      .objectType(ObjectType.builder()
          .name("Pand")
          .property(Attribute.builder()
              .name("identificatie")
              .type(ScalarTypes.INTEGER)
              .cardinality(Cardinality.REQUIRED)
              .identifier(true)
              .build())
          .property(Attribute.builder()
              .name("oorspronkelijkBouwjaar")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.REQUIRED)
              .build())
          .property(Attribute.builder()
              .name("status")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.REQUIRED)
              .build())
          .property(Attribute.builder()
              .name("geometrie")
              .type(new GeometryType())
              .cardinality(Cardinality.REQUIRED)
              .build())
          .build())
      .objectType(ObjectType.builder()
          .name("Verblijfsobject")
          .property(Attribute.builder()
              .name("identificatie")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.REQUIRED)
              .identifier(true)
              .build())
          .property(Attribute.builder()
              .name("status")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.REQUIRED)
              .build())
          .property(Relation.builder()
              .name("heeftAlsHoofdadres")
              .target(ObjectTypeRef.forType("Nummeraanduiding"))
              .cardinality(Cardinality.REQUIRED)
              .inverseName("isHoofdadresVan")
              .inverseCardinality(Cardinality.REQUIRED)
              .build())
          .property(Relation.builder()
              .name("heeftAlsNevenadres")
              .target(ObjectTypeRef.forType("Nummeraanduiding"))
              .cardinality(Cardinality.MULTI)
              .inverseName("isNevenadresVan")
              .inverseCardinality(Cardinality.OPTIONAL)
              .build())
          .property(Relation.builder()
              .name("maaktDeelUitVan")
              .target(ObjectTypeRef.forType("Pand"))
              .cardinality(Cardinality.of(1, INFINITE))
              .inverseName("bevat")
              .inverseCardinality(Cardinality.MULTI)
              .build())
          .build())
      .build();

  private static final Map<String, Object> PAND_0034100000000360 =
      ImmutableMap.of("identificatie", "0034100000000360", "status", "Pand in gebruik", "oorspronkelijkBouwjaar", 2005);

  private static final Map<String, Object> PAND_0313100000183898 =
      ImmutableMap.of("identificatie", "0313100000183898", "oorspronkelijkBouwjaar", 1967);
  private static final Map<String, Object> PAND_0313100000184339 =
      ImmutableMap.of("identificatie", "0313100000184339", "oorspronkelijkBouwjaar", 1967);
  private static final Map<String, Object> PAND_0313100000184467 =
      ImmutableMap.of("identificatie", "0313100000184467", "oorspronkelijkBouwjaar", 1967);

  private static final Map<String, Object> VERBLIJFSOBJECT_0034010000006016 =
      ImmutableMap.of("identificatie", "0034010000006016", "status", "Verblijfsobject in gebruik", "maaktDeelUitVan",
          ImmutableMap.of("identificatie", "0034100000000360"));
  private static final Map<String, Object> VERBLIJFSOBJECT_0313010000193752 =
      ImmutableMap.of("identificatie", "0313010000193752", "status", "Verblijfsobject in gebruik", "maaktDeelUitVan",
          ImmutableMap.of("identificatie", "0313100000184377"));
  private static final Map<String, Object> VERBLIJFSOBJECT_0313010000195924 =
      ImmutableMap.of("identificatie", "0313010000195924", "status", "Verblijfsobject in gebruik", "maaktDeelUitVan",
          ImmutableMap.of("identificatie", "0313100000181405"));
  private static final Map<String, Object> VERBLIJFSOBJECT_0313010000202088 =
      ImmutableMap.of("identificatie", "0313010000202088", "status", "Verblijfsobject in gebruik", "maaktDeelUitVan",
          ImmutableMap.of("identificatie", "0313100000187444"));

  @Test
  void test_bag_findOne() {
    var bag = new OgcApiFeaturesSource(
        new OgcApiFeaturesConfiguration(BAG, "https://wau.ldproxy.net/bag", 50, true, true)).getDataRepository();
    var pand = BAG.getObjectType("Pand");
    var objectRequest = ObjectRequest.builder()
        .objectType(pand)
        .objectKey(Map.of("identificatie", "0034100000000360"))
        .selectedProperties(List.of(
            new SelectedProperty(pand.getProperty("identificatie")),
            new SelectedProperty(pand.getProperty("status")),
            new SelectedProperty(pand.getProperty("oorspronkelijkBouwjaar"))))
        .build();
    var result = bag.findOne(objectRequest);

    StepVerifier.create(result).expectNext(PAND_0034100000000360).verifyComplete();

    var verblijfsobject = BAG.getObjectType("Verblijfsobject");
    objectRequest = ObjectRequest.builder()
        .objectType(verblijfsobject)
        .objectKey(Map.of("identificatie", "0034010000006016"))
        .selectedProperties(List.of(
            new SelectedProperty(verblijfsobject.getProperty("identificatie")),
            new SelectedProperty(verblijfsobject.getProperty("status")),
            new SelectedProperty(verblijfsobject.getProperty("maaktDeelUitVan"),
                ImmutableSet.of(new SelectedProperty(pand.getIdentityProperties().get(0))))))
        .build();
    result = bag.findOne(objectRequest);
    StepVerifier.create(result).expectNext(VERBLIJFSOBJECT_0034010000006016).verifyComplete();
  }

  @Test
  void test_bag_findOne_404() {
    var bag = new OgcApiFeaturesSource(
        new OgcApiFeaturesConfiguration(BAG, "https://wau.ldproxy.net/bag", 50, true, true)).getDataRepository();
    var pand = BAG.getObjectType("Pand");
    var objectRequest = ObjectRequest.builder()
        .objectType(pand)
        .objectKey(Map.of("identificatie", "does_not_exist"))
        .selectedProperties(List.of(
            new SelectedProperty(pand.getProperty("identificatie")),
            new SelectedProperty(pand.getProperty("status")),
            new SelectedProperty(pand.getProperty("oorspronkelijkBouwjaar"))))
        .build();
    var result = bag.findOne(objectRequest);

    StepVerifier.create(result).verifyComplete();

    var verblijfsobject = BAG.getObjectType("Verblijfsobject");
    objectRequest = ObjectRequest.builder()
        .objectType(verblijfsobject)
        .objectKey(Map.of("identificatie", "does_not_exist"))
        .selectedProperties(List.of(
            new SelectedProperty(verblijfsobject.getProperty("identificatie")),
            new SelectedProperty(verblijfsobject.getProperty("status")),
            new SelectedProperty(verblijfsobject.getProperty("maaktDeelUitVan"),
                ImmutableSet.of(new SelectedProperty(pand.getIdentityProperties().get(0))))))
        .build();
    result = bag.findOne(objectRequest);
    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void test_bag_find() {
    var bag = new OgcApiFeaturesSource(
        new OgcApiFeaturesConfiguration(BAG, "https://wau.ldproxy.net/bag", 3, true, true)).getDataRepository();
    var pand = BAG.getObjectType("Pand");
    var collectionRequest = CollectionRequest.builder()
        .objectType(pand)
        .selectedProperties(List.of(
            new SelectedProperty(pand.getProperty("identificatie")),
            new SelectedProperty(pand.getProperty("oorspronkelijkBouwjaar"))))
        .filter(FilterExpression.builder().operator(new EqualsOperatorType().create(Map.of()))
            .path(Path.fromString("oorspronkelijkBouwjaar")).value(Map.of("oorspronkelijkBouwjaar", "1967")).build())
        .build();
    var result = bag.find(collectionRequest);

    StepVerifier.create(result).expectNext(PAND_0313100000183898).expectNext(PAND_0313100000184339)
        .expectNext(PAND_0313100000184467).verifyComplete();

    var verblijfsobject = BAG.getObjectType("Verblijfsobject");
    collectionRequest = CollectionRequest.builder()
        .objectType(verblijfsobject)
        .selectedProperties(List.of(
            new SelectedProperty(verblijfsobject.getProperty("identificatie")),
            new SelectedProperty(verblijfsobject.getProperty("status")),
            new SelectedProperty(verblijfsobject.getProperty("maaktDeelUitVan"),
                ImmutableSet.of(new SelectedProperty(pand.getIdentityProperties().get(0))))))
        .build();
    result = bag.find(collectionRequest);
    StepVerifier.create(result).expectNext(VERBLIJFSOBJECT_0313010000193752)
        .expectNext(VERBLIJFSOBJECT_0313010000195924)
        .expectNext(VERBLIJFSOBJECT_0313010000202088).verifyComplete();
  }

  @Test
  void test_bag_findBatch() {
    var bag = new OgcApiFeaturesSource(
        new OgcApiFeaturesConfiguration(BAG, "https://wau.ldproxy.net/bag", 3, true, true)).getDataRepository();
    var pand = BAG.getObjectType("Pand");
    var batchRequest = BatchRequest.builder()
        .objectType(pand)
        .selectedProperties(List.of(
            new SelectedProperty(pand.getProperty("identificatie")),
            new SelectedProperty(pand.getProperty("oorspronkelijkBouwjaar"))))
        .objectKeys(List.of(Map.of("identificatie", "0313100000183898"), Map.of("identificatie", "0313100000184339"),
            Map.of("identificatie", "0313100000184467")))
        .build();
    var result = bag.findBatch(batchRequest);

    StepVerifier.create(result).expectNext(PAND_0313100000183898).expectNext(PAND_0313100000184339)
        .expectNext(PAND_0313100000184467).verifyComplete();

    var verblijfsobject = BAG.getObjectType("Verblijfsobject");
    batchRequest = BatchRequest.builder()
        .objectType(verblijfsobject)
        .objectKeys(List.of(Map.of("identificatie", "0313010000193752"), Map.of("identificatie", "0313010000195924"),
            Map.of("identificatie", "0313010000202088")))
        .selectedProperties(List.of(
            new SelectedProperty(verblijfsobject.getProperty("identificatie")),
            new SelectedProperty(verblijfsobject.getProperty("status")),
            new SelectedProperty(verblijfsobject.getProperty("maaktDeelUitVan"),
                ImmutableSet.of(new SelectedProperty(pand.getIdentityProperties().get(0))))))
        .build();
    result = bag.findBatch(batchRequest);
    StepVerifier.create(result).expectNext(VERBLIJFSOBJECT_0313010000193752)
        .expectNext(VERBLIJFSOBJECT_0313010000195924)
        .expectNext(VERBLIJFSOBJECT_0313010000202088).verifyComplete();
  }
}
