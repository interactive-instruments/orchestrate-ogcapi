package de.ii.orchestrate.ogcapi;

import org.dotwebstack.orchestrate.model.Model;
import org.dotwebstack.orchestrate.source.DataRepository;
import org.dotwebstack.orchestrate.source.Source;

public class OgcApiFeaturesSource implements Source {

  private final OgcApiFeaturesConfiguration configuration;

  public OgcApiFeaturesSource(OgcApiFeaturesConfiguration configuration) {
    this.configuration = configuration;
  }

  // method should be added to Source
  public Model getModel() {
    return configuration.getModel();
  }

  @Override
  public DataRepository getDataRepository() {
    return new OgcApiFeaturesDataRepository(configuration);
  }

}
