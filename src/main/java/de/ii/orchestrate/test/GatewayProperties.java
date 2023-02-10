package de.ii.orchestrate.test;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties("orchestrate.gateway")
public class GatewayProperties {

  private TestFixtures.TargetModelType targetModel;

  private String mapping;
}
