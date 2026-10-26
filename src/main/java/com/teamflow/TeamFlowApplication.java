package com.teamflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TeamFlowApplication {
  public static void main(String[] args) {
    SpringApplication.run(TeamFlowApplication.class, args);
  }
}
