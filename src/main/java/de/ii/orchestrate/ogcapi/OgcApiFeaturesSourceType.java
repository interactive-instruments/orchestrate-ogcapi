package de.ii.orchestrate.ogcapi;

import java.util.Map;
import java.util.Objects;
import org.dotwebstack.orchestrate.model.Model;
import org.dotwebstack.orchestrate.source.Source;
import org.dotwebstack.orchestrate.source.SourceException;
import org.dotwebstack.orchestrate.source.SourceType;

public class OgcApiFeaturesSourceType implements SourceType {

  private static final String SOURCE_TYPE = "ogcapi";

  private static final String URL_KEY = "url";
  private static final String LIMIT_KEY = "limit";
  private static final String SUPPORTS_PROPERTY_SELECTION_KEY = "supportsPropertySelection";
  private static final String SUPPORTS_REL_PROFILES_KEY = "supportsRelProfiles";

  @Override
  public String getName() {
    return SOURCE_TYPE;
  }

  @Override
  public Source create(Model model, Map<String, Object> options) {
    validateBasic(model, options);

    var landingPage = (String)options.get(URL_KEY);
    var limit = Objects.requireNonNullElse((Integer)options.get(LIMIT_KEY), 10);
    var supportsPropertySelection = Objects.requireNonNullElse((Boolean)options.get(SUPPORTS_PROPERTY_SELECTION_KEY), false);
    var supportsRelProfiles = Objects.requireNonNullElse((Boolean)options.get(SUPPORTS_REL_PROFILES_KEY), false);
    var configuration = new OgcApiFeaturesConfiguration(model, landingPage, limit, supportsPropertySelection, supportsRelProfiles);
    return new OgcApiFeaturesSource(configuration);
  }

  private void validateBasic(Model model, Map<String, Object> options) {
    if (model == null) {
      throw new SourceException("Model can't be null.");
    }
    if (!options.containsKey(URL_KEY)) {
      throw new SourceException(String.format("Config '%s' is missing.", URL_KEY));
    }
  }
}