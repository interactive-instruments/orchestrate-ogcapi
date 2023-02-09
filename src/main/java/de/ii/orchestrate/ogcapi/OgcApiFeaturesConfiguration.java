package de.ii.orchestrate.ogcapi;

import lombok.Builder;
import lombok.Getter;
import org.dotwebstack.orchestrate.model.Model;

@Getter
public class OgcApiFeaturesConfiguration {

  private final Model model;
  private final String apiLandingPage;
  private final int limit;
  private final boolean supportsPropertySelection;

  @Builder(toBuilder = true)
  public OgcApiFeaturesConfiguration(Model model, String apiLandingPage, int limit, boolean supportsPropertySelection) {
    this.model = model;
    this.apiLandingPage = apiLandingPage;
    this.limit = limit;
    this.supportsPropertySelection = supportsPropertySelection;
  }
}
