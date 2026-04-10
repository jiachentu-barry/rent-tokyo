package com.jiachentu.rent_tokyo.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "properties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 150)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(length = 100)
    private String ward;

    @Column(name = "nearest_station", length = 120)
    private String nearestStation;

    @Column(name = "walk_minutes")
    private Integer walkMinutes;

    @Column(length = 40)
    private String layout;

    @Column(name = "area_sqm")
    private Float areaSqm;

    @Column(name = "built_year")
    private Integer builtYear;

    @Column
    private Integer rent;

    @Column(name = "management_fee")
    private Integer managementFee;

    @Column
    private Integer deposit;

    @Column(name = "key_money")
    private Integer keyMoney;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Favorite> favorites = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PropertyChangeLog> changeLogs = new HashSet<>();

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
