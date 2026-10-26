package com.teamflow.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {
  @Test
  void directModeIgnoresClientSuppliedHeader() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("127.0.0.1");
    request.addHeader("X-Real-IP", "203.0.113.9");
    assertEquals("127.0.0.1", new ClientIpResolver(false).resolve(request));
  }

  @Test
  void trustedProxyModeAcceptsOnlyOneLiteralIp() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("172.18.0.4");
    request.addHeader("X-Real-IP", "203.0.113.9");
    assertEquals("203.0.113.9", new ClientIpResolver(true).resolve(request));
    request.removeHeader("X-Real-IP");
    request.addHeader("X-Real-IP", "203.0.113.9, 198.51.100.2");
    assertEquals("172.18.0.4", new ClientIpResolver(true).resolve(request));
    request.removeHeader("X-Real-IP");
    request.addHeader("X-Real-IP", "attacker.example");
    assertEquals("172.18.0.4", new ClientIpResolver(true).resolve(request));
  }
}
