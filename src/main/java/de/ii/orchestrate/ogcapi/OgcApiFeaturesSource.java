package de.ii.orchestrate.ogcapi;

import org.dotwebstack.orchestrate.source.DataRepository;
import org.dotwebstack.orchestrate.source.Source;

public class OgcApiFeaturesSource implements Source {

  private final OgcApiFeaturesConfiguration configuration;

  public OgcApiFeaturesSource(OgcApiFeaturesConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public DataRepository getDataRepository() {
    return new OgcApiFeaturesDataRepository(configuration);
  }

}
