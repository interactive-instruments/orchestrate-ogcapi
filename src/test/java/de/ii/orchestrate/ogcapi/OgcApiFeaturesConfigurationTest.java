package de.ii.orchestrate.ogcapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.dotwebstack.orchestrate.model.Model;
import org.junit.jupiter.api.Test;

public class OgcApiFeaturesConfigurationTest {

  private static final Model DUMMY = Model.builder().build();

  @Test
  void test_bag() {
    OgcApiFeaturesConfiguration config =
        new OgcApiFeaturesConfiguration(DUMMY, "https://wau.ldproxy.net/bag", 50, true, true);
    assertThat(config.getLimit()).isEqualTo(50);
    assertThat(config.getSrid()).isEqualTo(28992);
    assertThat(config.getApiLandingPage()).isEqualTo("https://wau.ldproxy.net/bag");
    assertThat(config.isSupportsPropertySelection()).isEqualTo(true);
    assertThat(config.isSupportsRelProfiles()).isEqualTo(true);
    assertThat(config.isSupportsIntersects()).isEqualTo(true);
    assertThat(config.isSupportsBatchLoading()).isEqualTo(true);
    assertThat(config.isSupportsCql2InOperator()).isEqualTo(true);
    assertThat(config.isSupportsAdHocQuery()).isEqualTo(false);
  }

  @Test
  void test_bgt() {
    OgcApiFeaturesConfiguration config =
        new OgcApiFeaturesConfiguration(DUMMY, "https://wau.ldproxy.net/bgt", 99, false, false);
    assertThat(config.getLimit()).isEqualTo(99);
    assertThat(config.getSrid()).isEqualTo(28992);
    assertThat(config.getApiLandingPage()).isEqualTo("https://wau.ldproxy.net/bgt");
    assertThat(config.isSupportsPropertySelection()).isEqualTo(false);
    assertThat(config.isSupportsRelProfiles()).isEqualTo(false);
    assertThat(config.isSupportsIntersects()).isEqualTo(true);
    assertThat(config.isSupportsBatchLoading()).isEqualTo(true);
    assertThat(config.isSupportsCql2InOperator()).isEqualTo(true);
    assertThat(config.isSupportsAdHocQuery()).isEqualTo(false);
  }

  @Test
  void test_404() {
    assertThatThrownBy(
        () -> new OgcApiFeaturesConfiguration(DUMMY, "https://wau.ldproxy.net/abc", 10, false, false)).hasMessage(
        "Conformance Declaration request returned a status different than 200: 404. URI: https://wau.ldproxy.net/abc/conformance");
  }

  @Test
  void test_no_cql2() {
    assertThatThrownBy(
        () -> new OgcApiFeaturesConfiguration(DUMMY, "https://ri.ldproxy.net/vineyards", 10, false, false)).hasMessage(
        "APIs must support the OGC API Feature 'CQL2 Text' conformance class.");
  }
}
