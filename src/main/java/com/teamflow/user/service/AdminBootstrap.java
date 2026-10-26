package com.teamflow.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Explicit first-admin bootstrap; no credentials are stored in source or logged. */
@Component
public class AdminBootstrap implements ApplicationRunner {
  private final AdminService admins;
  private final String username;
  private final String password;

  public AdminBootstrap(
      AdminService admins,
      @Value("${teamflow.bootstrap.username:}") String username,
      @Value("${teamflow.bootstrap.password:}") String password) {
    this.admins = admins;
    this.username = username;
    this.password = password;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (username.isBlank() && password.isBlank()) return;
    if (username.isBlank() || password.isBlank()) {
      throw new IllegalStateException("Both bootstrap admin variables are required");
    }
    admins.bootstrap(username, password);
  }
}
