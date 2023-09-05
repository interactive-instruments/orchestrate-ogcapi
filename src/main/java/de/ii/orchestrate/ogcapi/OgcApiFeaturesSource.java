package de.ii.orchestrate.ogcapi;

import nl.geostandaarden.imx.orchestrate.source.DataRepository;
import nl.geostandaarden.imx.orchestrate.source.Source;

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
