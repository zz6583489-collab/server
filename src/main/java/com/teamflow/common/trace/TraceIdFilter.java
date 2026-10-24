package com.teamflow.common.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {
  public static final String MDC_KEY = "traceId";
  public static final String HEADER = "X-Trace-Id";
  private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String traceId = UUID.randomUUID().toString();
    long start = System.nanoTime();
    MDC.put(MDC_KEY, traceId);
    response.setHeader(HEADER, traceId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      long elapsedMs = (System.nanoTime() - start) / 1_000_000;
      log.info(
          "HTTP {} {} status={} durationMs={}",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          elapsedMs);
      MDC.remove(MDC_KEY);
    }
  }
}
