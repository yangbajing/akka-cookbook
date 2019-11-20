/*
 * Copyright 2019 yangbajing.me
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cookbook.integration.springweb;

import akka.actor.ActorSystem;
import akka.stream.Materializer;
import akka.stream.alpakka.spring.web.AkkaStreamsRegistrar;
import akka.stream.alpakka.spring.web.SpringWebAkkaStreamsProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ReactiveAdapterRegistry;

import java.util.Objects;

@Configuration
@ConditionalOnClass(akka.stream.javadsl.Source.class)
@EnableConfigurationProperties(SpringWebAkkaStreamsProperties.class)
public class SpringWebAkkaStreamsConfiguration {

  private static final String DEFAULT_ACTORY_SYSTEM_NAME = "SpringWebAkkaStreamsSystem";

  private final ActorSystem system;
  private final Materializer mat;
  private final SpringWebAkkaStreamsProperties properties;

  public SpringWebAkkaStreamsConfiguration(final SpringWebAkkaStreamsProperties properties) {
    this.properties = properties;
    final ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();

    system = ActorSystem.create(getActorSystemName(properties));
    mat = Materializer.createMaterializer(system);
    new AkkaStreamsRegistrar(mat).registerAdapters(registry);
  }

  @Bean
  @ConditionalOnMissingBean(ActorSystem.class)
  public ActorSystem getActorSystem() {
    return system;
  }

  @Bean
  @ConditionalOnMissingBean(Materializer.class)
  public Materializer getMaterializer() {
    return mat;
  }

  public SpringWebAkkaStreamsProperties getProperties() {
    return properties;
  }

  private String getActorSystemName(final SpringWebAkkaStreamsProperties properties) {
    Objects.requireNonNull(
        properties,
        String.format(
            "%s is not present in application context",
            SpringWebAkkaStreamsProperties.class.getSimpleName()));

    if (isBlank(properties.getActorSystemName())) {
      return DEFAULT_ACTORY_SYSTEM_NAME;
    }

    return properties.getActorSystemName();
  }

  private boolean isBlank(String str) {
    return (str == null || str.isEmpty());
  }
}
