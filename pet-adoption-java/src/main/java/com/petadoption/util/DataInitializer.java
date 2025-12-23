package com.petadoption.util;

import com.petadoption.entity.Pet;
import com.petadoption.entity.User;
import com.petadoption.enums.PetGender;
import com.petadoption.enums.PetType;
import com.petadoption.enums.UserRole;
import com.petadoption.repository.PetRepository;
import com.petadoption.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            initializeData();
        }
    }

    private void initializeData() {
        // Create admin user
        User admin = User.builder()
                .email("admin@petadopt.com")
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .role(UserRole.ADMIN)
                .build();
        userRepository.save(admin);

        // Create shelter user
        User shelter = User.builder()
                .email("shelter@petadopt.com")
                .username("shelter")
                .password(passwordEncoder.encode("shelter123"))
                .role(UserRole.SHELTER)
                .address("123 Main Street")
                .build();
        shelter = userRepository.save(shelter);

        // Create sample pets
        List<Pet> samplePets = Arrays.asList(
                Pet.builder()
                        .name("旺财")
                        .petType(PetType.DOG)
                        .breed("金毛寻回犬")
                        .ageMonths(24)
                        .gender(PetGender.MALE)
                        .description("性格温顺友善的金毛，喜欢和人玩耍，非常适合家庭饲养。已完成基础训练，会握手、坐下等指令。")
                        .isNeutered(true)
                        .isVaccinated(true)
                        .location("北京市朝阳区")
                        .adoptionRequirements("需要有足够的活动空间，每天至少遛狗1小时")
                        .owner(shelter)
                        .build(),
                Pet.builder()
                        .name("咪咪")
                        .petType(PetType.CAT)
                        .breed("英国短毛猫")
                        .ageMonths(12)
                        .gender(PetGender.FEMALE)
                        .description("温柔安静的英短蓝猫，喜欢被抚摸和拥抱。不挑食，已适应猫砂盆。")
                        .isNeutered(true)
                        .isVaccinated(true)
                        .location("上海市浦东新区")
                        .adoptionRequirements("适合公寓饲养，需要定期梳毛")
                        .owner(shelter)
                        .build(),
                Pet.builder()
                        .name("大黄")
                        .petType(PetType.DOG)
                        .breed("中华田园犬")
                        .ageMonths(18)
                        .gender(PetGender.MALE)
                        .description("忠诚可靠的田园犬，从小被救助。身体健康，适应能力强，看家护院的好帮手。")
                        .isNeutered(false)
                        .isVaccinated(true)
                        .location("广州市天河区")
                        .adoptionRequirements("最好有院子或阳台，需要耐心陪伴")
                        .owner(shelter)
                        .build(),
                Pet.builder()
                        .name("橘子")
                        .petType(PetType.CAT)
                        .breed("橘猫")
                        .ageMonths(6)
                        .gender(PetGender.MALE)
                        .description("活泼好动的小橘猫，好奇心强，喜欢探索。食量较大，是个小吃货。")
                        .isNeutered(false)
                        .isVaccinated(true)
                        .location("深圳市南山区")
                        .adoptionRequirements("需要准备猫爬架，注意控制饮食")
                        .owner(shelter)
                        .build(),
                Pet.builder()
                        .name("豆豆")
                        .petType(PetType.DOG)
                        .breed("泰迪犬")
                        .ageMonths(36)
                        .gender(PetGender.MALE)
                        .description("聪明伶俐的泰迪，已完成全部训练。不掉毛，适合对毛发过敏的家庭。性格亲人，喜欢撒娇。")
                        .isNeutered(true)
                        .isVaccinated(true)
                        .location("杭州市西湖区")
                        .adoptionRequirements("需要定期美容修剪，每天陪伴时间不少于2小时")
                        .owner(shelter)
                        .build(),
                Pet.builder()
                        .name("花花")
                        .petType(PetType.CAT)
                        .breed("三花猫")
                        .ageMonths(8)
                        .gender(PetGender.FEMALE)
                        .description("漂亮的三花猫，性格独立但也亲人。白天喜欢晒太阳，晚上爱玩逗猫棒。")
                        .isNeutered(true)
                        .isVaccinated(true)
                        .location("成都市锦江区")
                        .adoptionRequirements("需要封窗，准备基本的猫用品")
                        .owner(shelter)
                        .build(),
                Pet.builder()
                        .name("小黑")
                        .petType(PetType.DOG)
                        .breed("拉布拉多")
                        .ageMonths(10)
                        .gender(PetGender.MALE)
                        .description("精力充沛的拉布拉多幼犬，非常聪明好学。喜欢玩球和游泳，是运动爱好者的最佳伴侣。")
                        .isNeutered(false)
                        .isVaccinated(true)
                        .location("南京市鼓楼区")
                        .adoptionRequirements("需要大量运动，适合有运动习惯的主人")
                        .owner(shelter)
                        .build(),
                Pet.builder()
                        .name("团团")
                        .petType(PetType.CAT)
                        .breed("布偶猫")
                        .ageMonths(15)
                        .gender(PetGender.FEMALE)
                        .description("温顺黏人的布偶猫，毛发柔软如丝。喜欢被抱着，是名副其实的布偶猫。")
                        .isNeutered(true)
                        .isVaccinated(true)
                        .location("武汉市武昌区")
                        .adoptionRequirements("需要每天梳毛，注意毛球问题")
                        .owner(shelter)
                        .build(),
                Pet.builder()
                        .name("阿福")
                        .petType(PetType.DOG)
                        .breed("柯基犬")
                        .ageMonths(20)
                        .gender(PetGender.MALE)
                        .description("短腿小可爱柯基，屁股圆圆的特别萌。性格开朗，喜欢和其他狗狗玩耍。")
                        .isNeutered(true)
                        .isVaccinated(true)
                        .location("西安市雁塔区")
                        .adoptionRequirements("注意控制体重，避免爬楼梯")
                        .owner(shelter)
                        .build(),
                Pet.builder()
                        .name("小白")
                        .petType(PetType.OTHER)
                        .breed("垂耳兔")
                        .ageMonths(8)
                        .gender(PetGender.FEMALE)
                        .description("可爱的垂耳兔，毛茸茸的大耳朵。性格温顺，喜欢吃胡萝卜和干草。")
                        .isNeutered(false)
                        .isVaccinated(true)
                        .location("重庆市渝中区")
                        .adoptionRequirements("需要准备兔笼和磨牙棒，定期清理")
                        .owner(shelter)
                        .build()
        );

        petRepository.saveAll(samplePets);
        System.out.println("Sample data initialized successfully!");
    }
}
