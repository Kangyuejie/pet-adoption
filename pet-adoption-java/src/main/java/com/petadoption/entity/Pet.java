package com.petadoption.entity;

import com.petadoption.enums.PetGender;
import com.petadoption.enums.PetType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "pet_type", nullable = false)
    private PetType petType;

    private String breed;

    @Column(name = "age_months", nullable = false)
    private Integer ageMonths;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PetGender gender;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "is_neutered")
    private Boolean isNeutered = false;

    @Builder.Default
    @Column(name = "is_vaccinated")
    private Boolean isVaccinated = false;

    @Column(name = "health_notes", columnDefinition = "TEXT")
    private String healthNotes;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    private String location;

    private Double latitude;

    private Double longitude;

    @Column(name = "adoption_requirements", columnDefinition = "TEXT")
    private String adoptionRequirements;

    @Builder.Default
    @Column(name = "is_available")
    private Boolean isAvailable = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("sortOrder ASC")
    private List<PetImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL)
    @Builder.Default
    private List<AdoptionApplication> applications = new ArrayList<>();
}
