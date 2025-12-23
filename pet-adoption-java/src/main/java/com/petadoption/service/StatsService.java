package com.petadoption.service;

import com.petadoption.dto.response.ChartDataResponse;
import com.petadoption.dto.response.StatsResponse;
import com.petadoption.enums.ApplicationStatus;
import com.petadoption.enums.PetType;
import com.petadoption.repository.ApplicationRepository;
import com.petadoption.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final PetRepository petRepository;
    private final ApplicationRepository applicationRepository;

    public StatsResponse getStats() {
        long totalPets = petRepository.count();
        long availablePets = petRepository.countByIsAvailableTrue();
        long totalApplications = applicationRepository.count();
        long completedAdoptions = applicationRepository.countByStatus(ApplicationStatus.COMPLETED);

        long dogs = petRepository.countByPetType(PetType.DOG);
        long cats = petRepository.countByPetType(PetType.CAT);
        long other = totalPets - dogs - cats;

        Map<String, Long> petsByType = new LinkedHashMap<>();
        petsByType.put("dogs", dogs);
        petsByType.put("cats", cats);
        petsByType.put("other", other);

        return StatsResponse.builder()
                .totalPets(totalPets)
                .availablePets(availablePets)
                .totalApplications(totalApplications)
                .completedAdoptions(completedAdoptions)
                .petsByType(petsByType)
                .build();
    }

    public ChartDataResponse getChartData() {
        // Pet Type Distribution
        long dogs = petRepository.countByPetType(PetType.DOG);
        long cats = petRepository.countByPetType(PetType.CAT);
        long other = petRepository.count() - dogs - cats;

        ChartDataResponse.ChartData petTypeDistribution = ChartDataResponse.ChartData.builder()
                .labels(Arrays.asList("狗", "猫", "其他"))
                .data(Arrays.asList(dogs, cats, other))
                .build();

        // Monthly Adoptions (current year)
        int currentYear = LocalDate.now().getYear();
        List<String> monthLabels = Arrays.asList("1月", "2月", "3月", "4月", "5月", "6月",
                "7月", "8月", "9月", "10月", "11月", "12月");
        Long[] monthlyData = new Long[12];
        Arrays.fill(monthlyData, 0L);

        List<Object[]> monthlyAdoptions = applicationRepository
                .countByStatusAndYearGroupByMonth(ApplicationStatus.COMPLETED, currentYear);
        for (Object[] row : monthlyAdoptions) {
            int month = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            if (month >= 1 && month <= 12) {
                monthlyData[month - 1] = count;
            }
        }

        ChartDataResponse.ChartData monthlyAdoptionsChart = ChartDataResponse.ChartData.builder()
                .labels(monthLabels)
                .data(Arrays.asList(monthlyData))
                .build();

        // Application Trend
        long pending = applicationRepository.countByStatus(ApplicationStatus.PENDING);
        long approved = applicationRepository.countByStatus(ApplicationStatus.APPROVED);
        long rejected = applicationRepository.countByStatus(ApplicationStatus.REJECTED);
        long completed = applicationRepository.countByStatus(ApplicationStatus.COMPLETED);

        ChartDataResponse.ApplicationTrendData applicationTrend = ChartDataResponse.ApplicationTrendData.builder()
                .labels(Arrays.asList("待审核", "已批准", "已拒绝", "已完成"))
                .pending(Arrays.asList(pending, 0L, 0L, 0L))
                .approved(Arrays.asList(0L, approved, 0L, 0L))
                .completed(Arrays.asList(0L, 0L, rejected, completed))
                .build();

        // Age Distribution
        List<Integer> allAges = petRepository.findAllAgeMonths();
        long under6 = allAges.stream().filter(age -> age < 6).count();
        long from6to12 = allAges.stream().filter(age -> age >= 6 && age < 12).count();
        long from1to3 = allAges.stream().filter(age -> age >= 12 && age < 36).count();
        long from3to5 = allAges.stream().filter(age -> age >= 36 && age < 60).count();
        long over5 = allAges.stream().filter(age -> age >= 60).count();

        ChartDataResponse.ChartData ageDistribution = ChartDataResponse.ChartData.builder()
                .labels(Arrays.asList("<6月", "6-12月", "1-3岁", "3-5岁", ">5岁"))
                .data(Arrays.asList(under6, from6to12, from1to3, from3to5, over5))
                .build();

        return ChartDataResponse.builder()
                .petTypeDistribution(petTypeDistribution)
                .monthlyAdoptions(monthlyAdoptionsChart)
                .applicationTrend(applicationTrend)
                .ageDistribution(ageDistribution)
                .build();
    }
}
