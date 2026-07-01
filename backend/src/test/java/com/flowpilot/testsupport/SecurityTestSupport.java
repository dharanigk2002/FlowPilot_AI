package com.flowpilot.testsupport;

import java.util.Arrays;

import jakarta.servlet.http.Cookie;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class SecurityTestSupport {

    private SecurityTestSupport() {
    }

    public static RequestPostProcessor csrf(CsrfTokenRepository repository) {
        MockHttpServletRequest tokenRequest = new MockHttpServletRequest();
        MockHttpServletResponse tokenResponse = new MockHttpServletResponse();
        CsrfToken token = repository.generateToken(tokenRequest);
        repository.saveToken(token, tokenRequest, tokenResponse);
        Cookie csrfCookie = tokenResponse.getCookie("XSRF-TOKEN");
        if (csrfCookie == null) {
            throw new IllegalStateException("CSRF repository did not issue the expected cookie.");
        }

        return request -> addCsrf(request, csrfCookie, token.getHeaderName(), token.getToken());
    }

    private static MockHttpServletRequest addCsrf(
            MockHttpServletRequest request,
            Cookie csrfCookie,
            String headerName,
            String token
    ) {
        Cookie[] existingCookies = request.getCookies();
        Cookie[] cookies = existingCookies == null
                ? new Cookie[] { csrfCookie }
                : Arrays.copyOf(existingCookies, existingCookies.length + 1);
        if (existingCookies != null) {
            cookies[cookies.length - 1] = csrfCookie;
        }
        request.setCookies(cookies);
        request.addHeader(headerName, token);
        return request;
    }
}
