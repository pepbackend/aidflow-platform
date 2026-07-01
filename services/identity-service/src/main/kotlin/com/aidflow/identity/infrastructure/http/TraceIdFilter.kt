package com.aidflow.identity.infrastructure.http

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class TraceIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val traceId = resolveTraceId(request)
        MDC.put(MDC_TRACE_ID_KEY, traceId)
        response.setHeader(TRACE_ID_HEADER, traceId)

        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(MDC_TRACE_ID_KEY)
        }
    }

    private fun resolveTraceId(request: HttpServletRequest): String {
        val traceId = request.getHeader(TRACE_ID_HEADER)
        if (StringUtils.hasText(traceId)) {
            return traceId
        }
        return UUID.randomUUID().toString()
    }

    companion object {
        const val TRACE_ID_HEADER = "X-Trace-Id"
        private const val MDC_TRACE_ID_KEY = "traceId"
    }
}
