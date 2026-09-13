package com.community.waste.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "campaign_signups", uniqueConstraints = @UniqueConstraint(columnNames = {"campaign_id", "user_id"}))
public class CampaignSignup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "signed_at", nullable = false)
    private OffsetDateTime signedAt = OffsetDateTime.now();

    @Column(nullable = false)
    private boolean attended = false;

    @Column(name = "attended_at")
    private OffsetDateTime attendedAt;

    @JsonProperty("campaignId")
    public Long campaignId() {
        return campaign == null ? null : campaign.getId();
    }

    @JsonProperty("campaignTitle")
    public String campaignTitle() {
        return campaign == null ? null : campaign.getTitle();
    }

    @JsonProperty("userId")
    public Long userId() {
        return user == null ? null : user.getId();
    }

    @JsonProperty("userName")
    public String userName() {
        return user == null ? null : user.getDisplayName();
    }

    public Long getId() {
        return id;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public OffsetDateTime getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(OffsetDateTime signedAt) {
        this.signedAt = signedAt;
    }

    public boolean isAttended() {
        return attended;
    }

    public void setAttended(boolean attended) {
        this.attended = attended;
    }

    public OffsetDateTime getAttendedAt() {
        return attendedAt;
    }

    public void setAttendedAt(OffsetDateTime attendedAt) {
        this.attendedAt = attendedAt;
    }
}
