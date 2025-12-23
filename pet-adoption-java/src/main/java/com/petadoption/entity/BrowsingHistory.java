package com.petadoption.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "browsing_history", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "pet_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrowsingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Builder.Default
    @Column(name = "view_count")
    private Integer viewCount = 1;

    @CreationTimestamp
    @Column(name = "first_viewed_at", updatable = false)
    private LocalDateTime firstViewedAt;

    @UpdateTimestamp
    @Column(name = "last_viewed_at")
    private LocalDateTime lastViewedAt;
}
