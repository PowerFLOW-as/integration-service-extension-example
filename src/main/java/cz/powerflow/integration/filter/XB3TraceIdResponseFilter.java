package cz.powerflow.integration.filter;

import cz.notix.logging.tracing.MDCTracingInfo;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static cz.notix.logging.tracing.HttpRequestTracingInfo.OPEN_TRACE_TRACE_ID;

/**
 * In case of API need, it's recommended to log trace ID information that is stored in MDC
 */
@Component
public class XB3TraceIdResponseFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, OPEN_TRACE_TRACE_ID);

        String headerXB3TraceId = request.getHeader(OPEN_TRACE_TRACE_ID);

        // When traceId is not present, generate it
        if (ObjectUtils.isEmpty(headerXB3TraceId)) {
            response.setHeader(OPEN_TRACE_TRACE_ID, MDC.get(MDCTracingInfo.REQUEST_ID_LOGGING_PARAM));
        } else {
            // When traceId is present, use it
            response.setHeader(OPEN_TRACE_TRACE_ID, headerXB3TraceId);
            // persist to MDC for use traceId in logs
            MDC.put(MDCTracingInfo.REQUEST_ID_LOGGING_PARAM, headerXB3TraceId);
        }

        filterChain.doFilter(request, response);
    }
}
