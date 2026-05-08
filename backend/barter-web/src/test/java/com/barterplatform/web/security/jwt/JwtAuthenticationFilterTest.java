package com.barterplatform.web.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private JwtAuthenticationService jwtAuthenticationService;
    private JwtAuthenticationFilter filter;
    private FilterChain filterChain;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        jwtAuthenticationService = mock(JwtAuthenticationService.class);
        filter = new JwtAuthenticationFilter(jwtAuthenticationService);
        filterChain = mock(FilterChain.class);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPopulateSecurityContextWithValidToken() throws ServletException, IOException {
        String token = "Bearer valid-token";
        request.addHeader("Authorization", token);

        AuthenticatedUser principal = new AuthenticatedUser(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "alex99",
                List.of("USER"));
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(jwtAuthenticationService.authenticate(token)).thenReturn(auth);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(auth);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isInstanceOf(AuthenticatedUser.class);

        AuthenticatedUser user = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(user.getUserUuid()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(user.getUsername()).isEqualTo("alex99");
        assertThat(user.getRoles()).containsExactly("USER");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotSetAuthenticationWhenNoAuthorizationHeader() throws ServletException, IOException {
        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtAuthenticationService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotSetAuthenticationWhenTokenIsInvalid() throws ServletException, IOException {
        String token = "Bearer invalid-token";
        request.addHeader("Authorization", token);

        when(jwtAuthenticationService.authenticate(token)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotOverrideExistingAuthentication() throws ServletException, IOException {
        String token = "Bearer some-token";
        request.addHeader("Authorization", token);

        AuthenticatedUser existingPrincipal = new AuthenticatedUser(
                UUID.randomUUID(), "existing-user", List.of("ADMIN"));
        Authentication existingAuth = new UsernamePasswordAuthenticationToken(
                existingPrincipal, null, existingPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(existingAuth);
        verifyNoInteractions(jwtAuthenticationService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldContinueFilterChainAlways() throws ServletException, IOException {
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}

