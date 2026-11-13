package com.fightverse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FightVerseApplication {

  public static void main(String[] args) {
    SpringApplication.run(FightVerseApplication.class, args);
  }
}
