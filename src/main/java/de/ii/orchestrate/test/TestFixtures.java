package de.ii.orchestrate.test;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import org.dotwebstack.orchestrate.ext.spatial.GeometryType;
import org.dotwebstack.orchestrate.model.Attribute;
import org.dotwebstack.orchestrate.model.Cardinality;
import org.dotwebstack.orchestrate.model.ComponentRegistry;
import org.dotwebstack.orchestrate.model.Model;
import org.dotwebstack.orchestrate.model.ModelMapping;
import org.dotwebstack.orchestrate.model.ObjectType;
import org.dotwebstack.orchestrate.model.ObjectTypeRef;
import org.dotwebstack.orchestrate.model.Relation;
import org.dotwebstack.orchestrate.model.combiners.Concat;
import org.dotwebstack.orchestrate.model.transforms.FunctionTransform;
import org.dotwebstack.orchestrate.model.transforms.TestPredicate;
import org.dotwebstack.orchestrate.model.types.ScalarTypes;
import org.dotwebstack.orchestrate.parser.yaml.YamlModelMappingParser;

final class TestFixtures {

  public enum TargetModelType {
    ADRES, CORE_LOCATION
  }

  public static Model SOURCE_MODEL_BAG = Model.builder()
      .objectType(ObjectType.builder()
          .name("pand")
          .property(Attribute.builder()
              .name("identificatie")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.REQUIRED)
              .identifier(true)
              .build())
          .property(Attribute.builder()
              .name("geometrie")
              .type(new GeometryType())
              .cardinality(Cardinality.REQUIRED)
              .build())
          .property(Attribute.builder()
              .name("bouwjaar")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.REQUIRED)
              .build())
          .property(Attribute.builder()
              .name("gebruiksdoel")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.REQUIRED)
              .build())
          .property(Attribute.builder()
              .name("status")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.REQUIRED)
              .build())
          .build())
      .objectType(ObjectType.builder()
          .name("nummeraanduidingen")
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
              .target(ObjectTypeRef.forType("openbareruimtes"))
              .cardinality(Cardinality.REQUIRED)
              .build())
          .property(Relation.builder()
              .name("ligtIn")
              .target(ObjectTypeRef.forType("woonplaatsen"))
              .cardinality(Cardinality.OPTIONAL)
              .build())
          .build())
      .objectType(ObjectType.builder()
          .name("openbareruimtes")
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
              .target(ObjectTypeRef.forType("woonplaatsen"))
              .cardinality(Cardinality.REQUIRED)
              .build())
          .build())
      .objectType(ObjectType.builder()
          .name("woonplaatsen")
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
          .name("verblijfsobjecten")
          .property(Attribute.builder()
              .name("identificatie")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.REQUIRED)
              .identifier(true)
              .build())
          .property(Relation.builder()
              .name("heeftAlsHoofdadres")
              .target(ObjectTypeRef.forType("nummeraanduidingen"))
              .cardinality(Cardinality.REQUIRED)
              .inverseName("isHoofdadresVan")
              .inverseCardinality(Cardinality.REQUIRED)
              .build())
          .property(Relation.builder()
              .name("heeftAlsNevenadres")
              .target(ObjectTypeRef.forType("nummeraanduidingen"))
              .cardinality(Cardinality.MULTI)
              .inverseName("isNevenadresVan")
              .inverseCardinality(Cardinality.OPTIONAL)
              .build())
          .build())
      .build();

  public static Model SOURCE_MODEL_BGT = Model.builder()
      .objectType(ObjectType.builder()
          .name("Pand")
          .property(Attribute.builder()
              .name("identificatie")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.REQUIRED)
              .identifier(true)
              .build())
          .property(Relation.builder()
              .name("identificatieBAGPND")
              .target(ObjectTypeRef.forType("bag", "pand"))
              .cardinality(Cardinality.OPTIONAL)
              .build())
          .property(Attribute.builder()
              .name("geometrie2d")
              .type(new GeometryType())
              .cardinality(Cardinality.REQUIRED)
              .build())
          .property(Attribute.builder()
              .name("bronhouder")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.REQUIRED)
              .build())
          .property(Attribute.builder()
              .name("bgt-status")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.REQUIRED)
              .build())
          .property(Attribute.builder()
              .name("plus-status")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.REQUIRED)
              .build())
          .build())
      .objectType(ObjectType.builder()
          .name("OverigBouwwerk")
          .property(Attribute.builder()
              .name("identificatie")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.REQUIRED)
              .identifier(true)
              .build())
          .property(Attribute.builder()
              .name("geometrie2d")
              .type(new GeometryType())
              .cardinality(Cardinality.REQUIRED)
              .build())
          .property(Attribute.builder()
              .name("bronhouder")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.REQUIRED)
              .build())
          .property(Attribute.builder()
              .name("bgt-type")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.OPTIONAL)
              .build())
          .property(Attribute.builder()
              .name("bgt-status")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.OPTIONAL)
              .build())
          .property(Attribute.builder()
              .name("plus-type")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.OPTIONAL)
              .build())
          .property(Attribute.builder()
              .name("plus-status")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.OPTIONAL)
              .build())
          .build())
      .objectType(ObjectType.builder()
          .name("GebouwInstallatie")
          .property(Attribute.builder()
              .name("identificatie")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.REQUIRED)
              .identifier(true)
              .build())
          .property(Attribute.builder()
              .name("geometrie2d")
              .type(new GeometryType())
              .cardinality(Cardinality.REQUIRED)
              .build())
          .property(Attribute.builder()
              .name("bronhouder")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.REQUIRED)
              .build())
          .property(Attribute.builder()
              .name("bgt-type")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.OPTIONAL)
              .build())
          .property(Attribute.builder()
              .name("bgt-status")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.OPTIONAL)
              .build())
          .property(Attribute.builder()
              .name("plus-type")
              .type(ScalarTypes.STRING)
              .cardinality(Cardinality.OPTIONAL)
              .build())
          .build())
      .build();

  public static ModelMapping createModelMapping(TestFixtures.TargetModelType targetModelType, InputStream mappingInputStream) {
    Model targetModel = null;

    if (targetModelType == TestFixtures.TargetModelType.ADRES) {
      targetModel = buildAdresTargetModel();
    } else if (targetModelType == TestFixtures.TargetModelType.CORE_LOCATION) {
      targetModel = buildCoreLocationTargetModel();
    }

    var componentRegistry = new ComponentRegistry()
        .registerTransform(TestPredicate.builder()
            .name("nonNull")
            .predicate(Objects::nonNull)
            .build())
        .registerTransform(FunctionTransform.builder()
            .name("toString")
            .function(Objects::toString)
            .build());

    var yamlMapper = YamlModelMappingParser.getInstance(Map.of("concat", Concat.class, "nonNull", TestPredicate.class),
        componentRegistry);

    var modelMapping = yamlMapper.parse(mappingInputStream);

    return modelMapping.toBuilder()
        .targetModel(targetModel)
        .sourceModel("bag", SOURCE_MODEL_BAG)
        .sourceModel("bgt", SOURCE_MODEL_BGT)
        .build();
  }

  private static Model buildAdresTargetModel() {
    return Model.builder()
        .objectType(ObjectType.builder()
            .name("Adres")
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
            .property(Attribute.builder()
                .name("straatnaam")
                .type(ScalarTypes.STRING)
                .cardinality(Cardinality.REQUIRED)
                .build())
            .property(Attribute.builder()
                .name("plaatsnaam")
                .type(ScalarTypes.STRING)
                .cardinality(Cardinality.REQUIRED)
                .build())
            .property(Attribute.builder()
                .name("isHoofdadres")
                .type(ScalarTypes.BOOLEAN)
                .cardinality(Cardinality.REQUIRED)
                .build())
            .property(Attribute.builder()
                .name("omschrijving")
                .type(ScalarTypes.STRING)
                .cardinality(Cardinality.REQUIRED)
                .build())
            .build())
        .objectType(ObjectType.builder()
            .name("Gebouw")
            .property(Attribute.builder()
                .name("identificatie")
                .type(ScalarTypes.STRING)
                .cardinality(Cardinality.REQUIRED)
                .identifier(true)
                .build())
            .property(Attribute.builder()
                .name("bouwjaar")
                .type(ScalarTypes.STRING)
                .cardinality(Cardinality.OPTIONAL)
                .build())
            .property(Attribute.builder()
                .name("bgtStatus")
                .type(ScalarTypes.STRING)
                .cardinality(Cardinality.OPTIONAL)
                .build())
            .property(Attribute.builder()
                .name("maaiveldgeometrie")
                .type(new GeometryType())
                .cardinality(Cardinality.OPTIONAL)
                .build())
            .property(Attribute.builder()
                .name("bovenaanzichtgeometrie")
                .type(new GeometryType())
                .cardinality(Cardinality.OPTIONAL)
                .build())
            .build())
        .build();
  }

  private static Model buildCoreLocationTargetModel() {
    return Model.builder()
        .objectType(ObjectType.builder()
            .name("Address")
            .property(Attribute.builder()
                .name("_id")
                .type(ScalarTypes.STRING)
                .cardinality(Cardinality.REQUIRED)
                .build())
//            .property(Attribute.builder()
//                .name("addressArea")
//                .type(ScalarTypes.STRING)
//                .cardinality(Cardinality.MULTI)
//                .build())
            .property(Attribute.builder()
                .name("addressID")
                .type(ScalarTypes.STRING)
                .cardinality(Cardinality.OPTIONAL)
                .identifier(true)
                .build())
//            .property(Attribute.builder()
//                .name("adminUnitL1")
//                .type(ScalarTypes.STRING)
//                .cardinality(Cardinality.MULTI)
//                .build())
//            .property(Attribute.builder()
//                .name("adminUnitL2")
//                .type(ScalarTypes.STRING)
//                .cardinality(Cardinality.MULTI)
//                .build())
            .property(Attribute.builder()
                .name("fullAddress")
                .type(ScalarTypes.STRING)
                .cardinality(Cardinality.OPTIONAL)
                .build())
            .property(Attribute.builder()
                .name("locatorDesignator")
                .type(ScalarTypes.STRING)
                .cardinality(Cardinality.OPTIONAL)
                .build())
//            .property(Attribute.builder()
//                .name("locatorName")
//                .type(ScalarTypes.STRING)
//                .cardinality(Cardinality.MULTI)
//                .build())
//            .property(Attribute.builder()
//                .name("poBox")
//                .type(ScalarTypes.STRING)
//                .cardinality(Cardinality.MULTI)
//                .build())
            .property(Attribute.builder()
                .name("postCode")
                .type(ScalarTypes.STRING)
                .cardinality(Cardinality.OPTIONAL)
                .build())
            .property(Attribute.builder()
                .name("postName")
                .type(ScalarTypes.STRING)
                .cardinality(Cardinality.OPTIONAL)
                .build())
            .property(Attribute.builder()
                .name("thoroughfare")
                .type(ScalarTypes.STRING)
                .cardinality(Cardinality.OPTIONAL)
                .build())
            .build())
        .build();
  }
}
