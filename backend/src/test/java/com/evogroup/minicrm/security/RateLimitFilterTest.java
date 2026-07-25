package com.evogroup.minicrm.security;

import com.evogroup.minicrm.model.User;
import com.evogroup.minicrm.model.UserRole;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private static final String SECRET = "test-secret-key-at-least-32-bytes-long-for-hs256";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60_000);
    }

    private MockHttpServletRequest request(String method, String uri, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(ip);
        return request;
    }

    private String tokenFor(String username) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setRole(UserRole.MANAGER);
        return jwtService.generateToken(user);
    }

    @Test
    void sixthLoginAttempt_returns429WithRetryAfter() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true, jwtService);

        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = request("POST", "/api/auth/login", "1.2.3.4");
            MockHttpServletResponse res = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilterInternal(req, res, chain);

            assertThat(res.getStatus()).isNotEqualTo(429);
            verify(chain).doFilter(req, res);
        }

        MockHttpServletRequest sixthReq = request("POST", "/api/auth/login", "1.2.3.4");
        MockHttpServletResponse sixthRes = new MockHttpServletResponse();
        FilterChain sixthChain = mock(FilterChain.class);

        filter.doFilterInternal(sixthReq, sixthRes, sixthChain);

        assertThat(sixthRes.getStatus()).isEqualTo(429);
        assertThat(sixthRes.getHeader("Retry-After")).isNotNull();
        verify(sixthChain, never()).doFilter(any(), any());
    }

    @Test
    void loginLimit_isPerIp() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true, jwtService);

        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(
                    request("POST", "/api/auth/login", "1.1.1.1"),
                    new MockHttpServletResponse(),
                    mock(FilterChain.class));
        }

        MockHttpServletResponse otherIpResponse = new MockHttpServletResponse();
        filter.doFilterInternal(
                request("POST", "/api/auth/login", "2.2.2.2"),
                otherIpResponse,
                mock(FilterChain.class));

        assertThat(otherIpResponse.getStatus()).isNotEqualTo(429);
    }

    @Test
    void oneHundredFirstApiRequest_returns429() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true, jwtService);

        for (int i = 0; i < 100; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilterInternal(
                    request("GET", "/api/clients", "9.9.9.9"), res, mock(FilterChain.class));
            assertThat(res.getStatus()).isNotEqualTo(429);
        }

        MockHttpServletResponse res101 = new MockHttpServletResponse();
        filter.doFilterInternal(
                request("GET", "/api/clients", "9.9.9.9"), res101, mock(FilterChain.class));

        assertThat(res101.getStatus()).isEqualTo(429);
        assertThat(res101.getHeader("Retry-After")).isNotNull();
    }

    @Test
    void differentUsers_haveIndependentBuckets() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true, jwtService);
        String tokenA = tokenFor("alice");

        for (int i = 0; i < 100; i++) {
            MockHttpServletRequest req = request("GET", "/api/clients", "9.9.9.9");
            req.addHeader("Authorization", "Bearer " + tokenA);
            filter.doFilterInternal(req, new MockHttpServletResponse(), mock(FilterChain.class));
        }

        // alice's bucket is now empty
        MockHttpServletRequest aliceReq = request("GET", "/api/clients", "9.9.9.9");
        aliceReq.addHeader("Authorization", "Bearer " + tokenA);
        MockHttpServletResponse aliceRes = new MockHttpServletResponse();
        filter.doFilterInternal(aliceReq, aliceRes, mock(FilterChain.class));
        assertThat(aliceRes.getStatus()).isEqualTo(429);

        // bob, same IP, different (valid) token — independent bucket, still succeeds
        String tokenB = tokenFor("bob");
        MockHttpServletRequest bobReq = request("GET", "/api/clients", "9.9.9.9");
        bobReq.addHeader("Authorization", "Bearer " + tokenB);
        MockHttpServletResponse bobRes = new MockHttpServletResponse();
        filter.doFilterInternal(bobReq, bobRes, mock(FilterChain.class));
        assertThat(bobRes.getStatus()).isNotEqualTo(429);
    }

    @Test
    void disabled_alwaysPassesThrough() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(false, jwtService);

        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);
            filter.doFilterInternal(request("POST", "/api/auth/login", "1.2.3.4"), res, chain);

            assertThat(res.getStatus()).isEqualTo(200); // MockHttpServletResponse default, untouched
            verify(chain).doFilter(any(), any());
        }
    }
}
