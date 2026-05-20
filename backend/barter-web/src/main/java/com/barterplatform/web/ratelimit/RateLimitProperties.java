package com.barterplatform.web.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "barter.rate-limits")
public class RateLimitProperties {

    private boolean enabled = true;
    private String clientIpHeader = "";
    private Policy login = new Policy(20, Duration.ofMinutes(1));
    private Policy register = new Policy(10, Duration.ofMinutes(1));
    private Policy refreshToken = new Policy(60, Duration.ofMinutes(1));
    private Policy forgotPassword = new Policy(5, Duration.ofMinutes(15));
    private Policy resetPassword = new Policy(10, Duration.ofMinutes(15));
    private Policy resendVerificationCode = new Policy(5, Duration.ofMinutes(15));
    private Policy imageUpload = new Policy(30, Duration.ofMinutes(10));
    private Policy tradeOfferCreate = new Policy(20, Duration.ofMinutes(10));
    private Policy tradeMessageSend = new Policy(60, Duration.ofMinutes(10));
    private Policy favoriteMutation = new Policy(120, Duration.ofMinutes(10));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getClientIpHeader() {
        return clientIpHeader;
    }

    public void setClientIpHeader(String clientIpHeader) {
        this.clientIpHeader = clientIpHeader;
    }

    public Policy getLogin() {
        return login;
    }

    public void setLogin(Policy login) {
        this.login = normalize(login, this.login);
    }

    public Policy getRegister() {
        return register;
    }

    public void setRegister(Policy register) {
        this.register = normalize(register, this.register);
    }

    public Policy getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(Policy refreshToken) {
        this.refreshToken = normalize(refreshToken, this.refreshToken);
    }

    public Policy getForgotPassword() {
        return forgotPassword;
    }

    public void setForgotPassword(Policy forgotPassword) {
        this.forgotPassword = normalize(forgotPassword, this.forgotPassword);
    }

    public Policy getResetPassword() {
        return resetPassword;
    }

    public void setResetPassword(Policy resetPassword) {
        this.resetPassword = normalize(resetPassword, this.resetPassword);
    }

    public Policy getResendVerificationCode() {
        return resendVerificationCode;
    }

    public void setResendVerificationCode(Policy resendVerificationCode) {
        this.resendVerificationCode = normalize(resendVerificationCode, this.resendVerificationCode);
    }

    public Policy getImageUpload() {
        return imageUpload;
    }

    public void setImageUpload(Policy imageUpload) {
        this.imageUpload = normalize(imageUpload, this.imageUpload);
    }

    public Policy getTradeOfferCreate() {
        return tradeOfferCreate;
    }

    public void setTradeOfferCreate(Policy tradeOfferCreate) {
        this.tradeOfferCreate = normalize(tradeOfferCreate, this.tradeOfferCreate);
    }

    public Policy getTradeMessageSend() {
        return tradeMessageSend;
    }

    public void setTradeMessageSend(Policy tradeMessageSend) {
        this.tradeMessageSend = normalize(tradeMessageSend, this.tradeMessageSend);
    }

    public Policy getFavoriteMutation() {
        return favoriteMutation;
    }

    public void setFavoriteMutation(Policy favoriteMutation) {
        this.favoriteMutation = normalize(favoriteMutation, this.favoriteMutation);
    }

    private Policy normalize(Policy candidate, Policy fallback) {
        if (candidate == null) {
            return fallback;
        }
        int limit = candidate.getLimit() > 0 ? candidate.getLimit() : fallback.getLimit();
        Duration window = candidate.getWindow() != null && !candidate.getWindow().isZero() && !candidate.getWindow().isNegative()
                ? candidate.getWindow()
                : fallback.getWindow();
        return new Policy(limit, window);
    }

    public static class Policy {
        private int limit;
        private Duration window;

        public Policy() {
        }

        public Policy(int limit, Duration window) {
            this.limit = limit;
            this.window = window;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }
    }
}

