package com.sivco.gestion_archivos.configuracion;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        StatusCaptureResponseWrapper responseWrapper = new StatusCaptureResponseWrapper(response);
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, responseWrapper);
        } finally {
            int status = responseWrapper.getStatus();
            long time = System.currentTimeMillis() - start;
            
            // Filtrar logging de GETs repetitivos en endpoints de listado
            String uri = request.getRequestURI();
            String method = request.getMethod();
            boolean skipLogging = "GET".equals(method) && status == 200 && 
                (uri.contains("/ensayos") || uri.contains("/reportes") || uri.contains("/maquinas"));
            
            if (!skipLogging) {
                logger.info("REQUEST {} {} -> status={} time={}ms", method, uri, status, time);
            } else {
                logger.debug("REQUEST {} {} -> status={} time={}ms", method, uri, status, time);
            }
        }
    }

    private static class StatusCaptureResponseWrapper extends HttpServletResponseWrapper {
        private int httpStatus = HttpServletResponse.SC_OK;

        public StatusCaptureResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setStatus(int sc) {
            super.setStatus(sc);
            this.httpStatus = sc;
        }

        @Override
        public void sendError(int sc) throws IOException {
            super.sendError(sc);
            this.httpStatus = sc;
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            super.sendError(sc, msg);
            this.httpStatus = sc;
        }

        @Override
        public int getStatus() {
            return this.httpStatus;
        }
    }
}
