package org.example.corepayorderservice.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class MdcLoggingFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String MDC_TRACE_ID_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 게이트웨이가 찔러넣어준 HTTP 헤더에서 Trace ID 추출
        String traceId = request.getHeader(TRACE_ID_HEADER);

        // 방어 로직: 혹시나 게이트웨이를 거치지 않고 오더 서버로 직접 요청이 들어올 경우를 대비해 자체 발급
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().substring(0, 8);
        }

        try {
            // MDC에 Trace ID 저장
            MDC.put(MDC_TRACE_ID_KEY, traceId);

            // 다음 필터나 컨트롤러(DispatcherServlet)로 비즈니스 흐름을 넘김
            filterChain.doFilter(request, response);

        } finally {
            // 요청 처리가 끝나고 클라이언트에게 응답이 나가기 직전에 무조건 MDC 초기화 해야함
            // 톰캣은 스레드를 재사용하기 때문에, 이거 안 지우면 A유저 로그에 B유저 꼬리표가 찍히는 대참사남
            MDC.clear();
        }
    }
}