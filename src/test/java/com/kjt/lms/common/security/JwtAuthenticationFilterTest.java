package com.kjt.lms.common.security;

import com.kjt.lms.service.impl.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtUtil jwtUtil;
    private UserDetailsServiceImpl userDetailsService;
    private JwtAuthenticationFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        userDetailsService = mock(UserDetailsServiceImpl.class);
        filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);
        chain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerTokenAuthenticatesUserWithCurrentAuthorities() throws Exception {
        MockHttpServletRequest request = request("Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.getEmailFromToken("valid-token")).thenReturn("student@example.com");
        when(userDetailsService.loadUserByUsername("student@example.com"))
                .thenReturn(new User("student@example.com", "hash",
                        List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("student@example.com", authentication.getName());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_STUDENT".equals(authority.getAuthority())));
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidBearerTokenLeavesRequestUnauthenticated() throws Exception {
        MockHttpServletRequest request = request("Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(userDetailsService);
        verify(chain).doFilter(request, response);
    }

    @Test
    void nonBearerHeaderIsIgnored() throws Exception {
        MockHttpServletRequest request = request("Basic credentials");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtUtil, userDetailsService);
        verify(chain).doFilter(request, response);
    }

    @Test
    void deletedUserDoesNotAuthenticateDespiteValidToken() throws Exception {
        MockHttpServletRequest request = request("Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.getEmailFromToken("valid-token")).thenReturn("deleted@example.com");
        when(userDetailsService.loadUserByUsername("deleted@example.com"))
                .thenThrow(new UsernameNotFoundException("missing"));

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    private static MockHttpServletRequest request(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/user/me");
        request.addHeader("Authorization", authorization);
        return request;
    }
}
