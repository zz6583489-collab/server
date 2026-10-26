package com.teamflow.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Uses a proxy supplied address only on explicitly trusted, private deployments. */
@Component
public class ClientIpResolver {
  private final boolean trustForwardedIp;

  public ClientIpResolver(
      @Value("${teamflow.auth.trust-forwarded-ip:false}") boolean trustForwardedIp) {
    this.trustForwardedIp = trustForwardedIp;
  }

  public String resolve(HttpServletRequest request) {
    String directAddress = request.getRemoteAddr();
    if (!trustForwardedIp) return directAddress;
    String header = request.getHeader("X-Real-IP");
    if (header == null
        || header.length() > 45
        || !header.matches("[0-9A-Fa-f:.]+")
        || (!header.contains(".") && !header.contains(":"))) {
      return directAddress;
    }
    try {
      return InetAddress.getByName(header).getHostAddress();
    } catch (UnknownHostException exception) {
      return directAddress;
    }
  }
}
