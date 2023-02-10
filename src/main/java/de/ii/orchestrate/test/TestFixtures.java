package de.ii.orchestrate.test;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import org.dotwebstack.orchestrate.model.Attribute;
import org.dotwebstack.orchestrate.model.Cardinality;
import org.dotwebstack.orchestrate.model.ComponentRegistry;
import org.dotwebstack.orchestrate.model.Model;
import org.dotwebstack.orchestrate.model.ModelMapping;
import org.dotwebstack.orchestrate.model.ObjectType;
import org.dotwebstack.orchestrate.model.Relation;
import org.dotwebstack.orchestrate.model.combiners.Concat;
import org.dotwebstack.orchestrate.model.transforms.TestPredicate;
import org.dotwebstack.orchestrate.model.types.ObjectTypeRef;
import org.dotwebstack.orchestrate.model.types.ScalarTypes;
import org.dotwebstack.orchestrate.parser.yaml.YamlModelMappingParser;

final class TestFixtures {

  public enum TargetModelType {
    ADRES, CORE_LOCATION
  }

  public static Model SOURCE_MODEL = Model.builder()
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

  public static ModelMapping createModelMapping(TargetModelType targetModelType, InputStream mappingInputStream) {
    Model targetModel = null;

    if (targetModelType == TestFixtures.TargetModelType.ADRES) {
      targetModel = buildAdresTargetModel();
    } else if (targetModelType == TestFixtures.TargetModelType.CORE_LOCATION) {
      targetModel = buildCoreLocationTargetModel();
    }

    var sourceModel = SOURCE_MODEL;

    var componentRegistry = new ComponentRegistry()
        .registerTransform(TestPredicate.builder()
            .name("nonNull")
            .predicate(Objects::nonNull)
            .build());

    var yamlMapper = YamlModelMappingParser.getInstance(Map.of("concat", Concat.class, "nonNull", TestPredicate.class),
        componentRegistry);

    var modelMapping = yamlMapper.parse(mappingInputStream);

    return modelMapping.toBuilder()
        .targetModel(targetModel)
        .sourceModel("bag", sourceModel)
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
