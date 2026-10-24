package com.teamflow.common.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.teamflow.common.response.ApiResponse;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {
  @Test
  void generatesOwnTraceIdAndClearsMdcAfterRequest() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
    request.addHeader(TraceIdFilter.HEADER, "untrusted-client-value");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> responseTrace = new AtomicReference<>();

    new TraceIdFilter()
        .doFilter(
            request,
            response,
            (servletRequest, servletResponse) ->
                responseTrace.set(ApiResponse.success("ok").traceId()));

    String traceId = response.getHeader(TraceIdFilter.HEADER);
    assertNotNull(traceId);
    assertNotEquals("untrusted-client-value", traceId);
    assertEquals(traceId, responseTrace.get());
    assertNull(MDC.get(TraceIdFilter.MDC_KEY));
  }
}
