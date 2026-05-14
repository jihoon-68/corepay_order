package org.example.corepayorderservice.global.config;

import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public @NonNull Runnable decorate(@NonNull Runnable runnable) {
        // 1. 현재(부모) 스레드의 MDC 컨텍스트를 추출
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return () -> {
            try {
                // 2. 자식 스레드가 실행되기 직전에 부모의 MDC를 주입
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                runnable.run();
            } finally {
                // 3. 자식 스레드 실행이 끝나면 MDC 초기화 (스레드 풀 오염 방지)
                MDC.clear();
            }
        };
    }
}