package com.community.waste.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    public static class Jwt {
        private String secret;
        private long ttlMinutes = 720;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getTtlMinutes() {
            return ttlMinutes;
        }

        public void setTtlMinutes(long ttlMinutes) {
            this.ttlMinutes = ttlMinutes;
        }
    }

    public static class Points {
        private Map<String, Integer> perKg = new HashMap<>();
        private int bagBrokenBonus = 1;
        private int reviewDeduct = 10;

        public Map<String, Integer> getPerKg() {
            return perKg;
        }

        public void setPerKg(Map<String, Integer> perKg) {
            this.perKg = perKg;
        }

        public int getBagBrokenBonus() {
            return bagBrokenBonus;
        }

        public void setBagBrokenBonus(int bagBrokenBonus) {
            this.bagBrokenBonus = bagBrokenBonus;
        }

        public int getReviewDeduct() {
            return reviewDeduct;
        }

        public void setReviewDeduct(int reviewDeduct) {
            this.reviewDeduct = reviewDeduct;
        }
    }

    public static class Rules {
        private double missortRateThreshold = 0.15;
        private int missortMinDisposals = 5;
        private int reviewSlaHours = 24;
        private int redeemViolationLimit = 3;
        private int redeemViolationDays = 90;
        private double weighAnomalyRatio = 0.2;
        private long watchdogDelayMs = 60000;
        /** 预约到货后的领取窗口（分钟），超时自动回滚 */
        private int reservationPickupMinutes = 4320;

        public double getMissortRateThreshold() {
            return missortRateThreshold;
        }

        public void setMissortRateThreshold(double missortRateThreshold) {
            this.missortRateThreshold = missortRateThreshold;
        }

        public int getMissortMinDisposals() {
            return missortMinDisposals;
        }

        public void setMissortMinDisposals(int missortMinDisposals) {
            this.missortMinDisposals = missortMinDisposals;
        }

        public int getReviewSlaHours() {
            return reviewSlaHours;
        }

        public void setReviewSlaHours(int reviewSlaHours) {
            this.reviewSlaHours = reviewSlaHours;
        }

        public int getRedeemViolationLimit() {
            return redeemViolationLimit;
        }

        public void setRedeemViolationLimit(int redeemViolationLimit) {
            this.redeemViolationLimit = redeemViolationLimit;
        }

        public int getRedeemViolationDays() {
            return redeemViolationDays;
        }

        public void setRedeemViolationDays(int redeemViolationDays) {
            this.redeemViolationDays = redeemViolationDays;
        }

        public double getWeighAnomalyRatio() {
            return weighAnomalyRatio;
        }

        public void setWeighAnomalyRatio(double weighAnomalyRatio) {
            this.weighAnomalyRatio = weighAnomalyRatio;
        }

        public long getWatchdogDelayMs() {
            return watchdogDelayMs;
        }

        public void setWatchdogDelayMs(long watchdogDelayMs) {
            this.watchdogDelayMs = watchdogDelayMs;
        }

        public int getReservationPickupMinutes() {
            return reservationPickupMinutes;
        }

        public void setReservationPickupMinutes(int reservationPickupMinutes) {
            this.reservationPickupMinutes = reservationPickupMinutes;
        }
    }

    private Jwt jwt = new Jwt();
    private Points points = new Points();
    private Rules rules = new Rules();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Points getPoints() {
        return points;
    }

    public void setPoints(Points points) {
        this.points = points;
    }

    public Rules getRules() {
        return rules;
    }

    public void setRules(Rules rules) {
        this.rules = rules;
    }
}
