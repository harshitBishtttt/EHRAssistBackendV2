package ehrAssist.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TEMPORARY diagnostic controller to inspect reverse-proxy / IP-related headers.
 * Remove once proxy → client-IP chain is confirmed working.
 */
@RestController
@RequestMapping("/debug")
public class DebugIpController {

    @GetMapping("/ip")
    public ResponseEntity<Map<String, Object>> inspectIp(HttpServletRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("remoteAddr", request.getRemoteAddr());
        result.put("remoteHost", request.getRemoteHost());
        result.put("remotePort", request.getRemotePort());
        result.put("isSecure", request.isSecure());
        result.put("scheme", request.getScheme());
        result.put("host", request.getHeader("Host"));

        Map<String, String> proxyHeaders = new LinkedHashMap<>();
        proxyHeaders.put("X-Forwarded-For", request.getHeader("X-Forwarded-For"));
        proxyHeaders.put("X-Forwarded-Proto", request.getHeader("X-Forwarded-Proto"));
        proxyHeaders.put("X-Forwarded-Host", request.getHeader("X-Forwarded-Host"));
        proxyHeaders.put("X-Forwarded-Port", request.getHeader("X-Forwarded-Port"));
        proxyHeaders.put("X-Real-IP", request.getHeader("X-Real-IP"));
        proxyHeaders.put("Forwarded", request.getHeader("Forwarded"));
        proxyHeaders.put("CF-Connecting-IP", request.getHeader("CF-Connecting-IP"));
        proxyHeaders.put("True-Client-IP", request.getHeader("True-Client-IP"));
        proxyHeaders.put("Via", request.getHeader("Via"));
        result.put("proxyHeaders", proxyHeaders);

        Map<String, String> allHeaders = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        if (names != null) {
            for (String name : Collections.list(names)) {
                if ("authorization".equalsIgnoreCase(name)
                        || "cookie".equalsIgnoreCase(name)) {
                    allHeaders.put(name, "***redacted***");
                    continue;
                }
                allHeaders.put(name, request.getHeader(name));
            }
        }
        result.put("allHeaders", allHeaders);

        return ResponseEntity.ok(result);
    }
}
