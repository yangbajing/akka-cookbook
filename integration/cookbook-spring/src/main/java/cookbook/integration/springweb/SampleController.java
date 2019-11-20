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

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.event.LoggingAdapter;
import akka.stream.javadsl.Source;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

@RestController
public class SampleController {
  @Value("${akka.stream.alpakka.spring.web.actor-system-name}")
  private String actorSystemName;

  @Autowired private ActorSystem system;

  @RequestMapping("/")
  public Source<String, NotUsed> index() {
    return Source.repeat("Hello world!").intersperse("\n").take(10);
  }

  @PostConstruct
  public void setup() {
    LoggingAdapter log = system.log();
    log.info("Injected ActorSystem Name -> {}", system.name());
    log.info("Property ActorSystemName -> {}", actorSystemName);
    Assert.isTrue((system.name().equals(actorSystemName)), "Validating ActorSystem name");
  }
}
