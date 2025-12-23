package com.petadoption.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetImageResponse {
    private Long id;
    private String imageUrl;
    private Boolean isPrimary;
    private Integer sortOrder;
}
