package com.barterplatform.web.security;

import com.barterplatform.api.model.ErrorResponse;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.web.observability.CorrelationIdFilter;
import com.barterplatform.web.observability.RequestLoggingFilter;
import com.barterplatform.web.ratelimit.RateLimitProperties;
import com.barterplatform.web.ratelimit.RateLimitService;
import com.barterplatform.web.ratelimit.RateLimitingFilter;
import com.barterplatform.web.security.jwt.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({RateLimitProperties.class, SecurityProperties.class})
public class SecurityConfig {

    public static final String STRICT_CONTENT_SECURITY_POLICY =
            "default-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'; object-src 'none'";

    public static final String SWAGGER_CONTENT_SECURITY_POLICY = STRICT_CONTENT_SECURITY_POLICY
            + "; connect-src 'self'; font-src 'self'; img-src 'self' data:; script-src 'self'; style-src 'self' 'unsafe-inline'";

    private static final String[] SWAGGER_PATHS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/v3/api-docs",
            "/api/v1/swagger-ui/**",
            "/api/v1/swagger-ui.html",
            "/api/v1/v3/api-docs/**",
            "/api/v1/v3/api-docs"
    };

    private final SecurityProperties securityProperties;

    public SecurityConfig(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CorsConfigurationSource corsConfigurationSource,
                                                   CorrelationIdFilter correlationIdFilter,
                                                   RequestLoggingFilter requestLoggingFilter,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   RateLimitingFilter rateLimitingFilter,
                                                   ObjectProvider<ObjectMapper> objectMapperProvider) {
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(() -> new ObjectMapper().findAndRegisterModules());

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .headers(headers -> {
                    headers.httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .maxAgeInSeconds(Duration.ofDays(365).toSeconds()));
                    headers.contentTypeOptions(Customizer.withDefaults());
                    headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny);
                    headers.referrerPolicy(referrer -> referrer
                            .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                    headers.addHeaderWriter((request, response) -> response.setHeader(
                            "Content-Security-Policy",
                            resolveContentSecurityPolicy(request)));
                    headers.addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", "camera=(), geolocation=(), microphone=()"));
                })
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint(objectMapper))
                        .accessDeniedHandler((request, response, ex) ->
                                writeJsonError(response, objectMapper, request, HttpServletResponse.SC_FORBIDDEN,
                                        "Forbidden", ErrorCode.FORBIDDEN, "Access is denied.")))
                .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(requestLoggingFilter, CorrelationIdFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, RequestLoggingFilter.class)
                .addFilterAfter(rateLimitingFilter, JwtAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> {
                    authorize.dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll();
                    authorize.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    authorize.requestMatchers(HttpMethod.POST,
                            "/auth/register",
                            "/auth/login",
                            "/auth/refresh",
                            "/auth/logout",
                            "/auth/verify-email",
                            "/auth/resend-verification-code",
                            "/auth/forgot-password",
                            "/auth/reset-password",
                            "/api/v1/auth/register",
                            "/api/v1/auth/login",
                            "/api/v1/auth/refresh",
                            "/api/v1/auth/logout",
                            "/api/v1/auth/verify-email",
                            "/api/v1/auth/resend-verification-code",
                            "/api/v1/auth/forgot-password",
                            "/api/v1/auth/reset-password").permitAll();
                    authorize.requestMatchers(HttpMethod.GET,
                            "/ping",
                            "/api/v1/ping",
                            "/actuator/health",
                            "/actuator/health/**",
                            "/catalog/categories",
                            "/catalog/categories/popular",
                            "/catalog/tags",
                            "/catalog/items",
                            "/catalog/items/*",
                            "/catalog/items/*/images",
                            "/api/v1/catalog/categories",
                            "/api/v1/catalog/categories/popular",
                            "/api/v1/catalog/tags",
                            "/api/v1/catalog/items",
                            "/api/v1/catalog/items/*",
                            "/api/v1/catalog/items/*/images",
                            "/profiles/**",
                            "/api/v1/profiles/**").permitAll();
                    authorize.requestMatchers("/files/**", "/api/v1/files/**").permitAll();
                    if (securityProperties.isSwaggerEnabled()) {
                        authorize.requestMatchers(SWAGGER_PATHS).permitAll();
                    }
                    authorize.requestMatchers("/error").permitAll();
                    authorize.anyRequest().authenticated();
                })
                .anonymous(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public RequestLoggingFilter requestLoggingFilter() {
        return new RequestLoggingFilter();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(securityProperties.isAllowCredentials());
        configuration.setAllowedOrigins(securityProperties.getAllowedOrigins());
        configuration.setAllowedHeaders(securityProperties.getAllowedHeaders());
        configuration.setAllowedMethods(securityProperties.getAllowedMethods());
        configuration.setExposedHeaders(securityProperties.getExposedHeaders());
        configuration.setMaxAge(securityProperties.getCorsMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public RateLimitingFilter rateLimitingFilter(RateLimitProperties rateLimitProperties,
                                                 ObjectProvider<RateLimitService> rateLimitServiceProvider,
                                                 ObjectProvider<ObjectMapper> objectMapperProvider) {
        RateLimitService rateLimitService = rateLimitServiceProvider.getIfAvailable(RateLimitService::new);
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(() -> new ObjectMapper().findAndRegisterModules());
        return new RateLimitingFilter(rateLimitProperties, rateLimitService, objectMapper);
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration(CorrelationIdFilter correlationIdFilter) {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>(correlationIdFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilterRegistration(RequestLoggingFilter requestLoggingFilter) {
        FilterRegistrationBean<RequestLoggingFilter> registration = new FilterRegistrationBean<>(requestLoggingFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilterRegistration(RateLimitingFilter rateLimitingFilter) {
        FilterRegistrationBean<RateLimitingFilter> registration = new FilterRegistrationBean<>(rateLimitingFilter);
        registration.setEnabled(false);
        return registration;
    }

    private String resolveContentSecurityPolicy(HttpServletRequest request) {
        if (securityProperties.isSwaggerEnabled() && isSwaggerRequest(request)) {
            return SWAGGER_CONTENT_SECURITY_POLICY;
        }
        return STRICT_CONTENT_SECURITY_POLICY;
    }

    private boolean isSwaggerRequest(HttpServletRequest request) {
        return isSwaggerPath(request.getServletPath()) || isSwaggerPath(request.getRequestURI());
    }

    private boolean isSwaggerPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }

        return path.equals("/swagger-ui")
                || path.startsWith("/swagger-ui/")
                || path.equals("/swagger-ui.html")
                || path.equals("/v3/api-docs")
                || path.startsWith("/v3/api-docs/")
                || path.equals("/api/v1/swagger-ui")
                || path.startsWith("/api/v1/swagger-ui/")
                || path.equals("/api/v1/swagger-ui.html")
                || path.equals("/api/v1/v3/api-docs")
                || path.startsWith("/api/v1/v3/api-docs/");
    }

    private AuthenticationEntryPoint jsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, ex) -> writeJsonError(
                response,
                objectMapper,
                request,
                HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized",
                ErrorCode.UNAUTHORIZED,
                "Authentication is required to access this resource.");
    }

    private void writeJsonError(HttpServletResponse response,
                                ObjectMapper objectMapper,
                                HttpServletRequest request,
                                int status,
                                String error,
                                ErrorCode code,
                                String message) throws IOException, ServletException {
        if (response.isCommitted()) {
            return;
        }

        response.setStatus(status);
        response.setContentType("application/json");

        ErrorResponse errorResponse = new ErrorResponse()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(status)
                .error(error)
                .code(code.name())
                .message(message)
                .path(request.getRequestURI())
                .fieldErrors(new ArrayList<>());

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
