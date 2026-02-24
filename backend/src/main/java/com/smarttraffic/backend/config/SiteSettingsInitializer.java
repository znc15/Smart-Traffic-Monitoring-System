package com.smarttraffic.backend.config;

import com.smarttraffic.backend.model.CameraEntity;
import com.smarttraffic.backend.model.SiteSettingsEntity;
import com.smarttraffic.backend.model.UserEntity;
import com.smarttraffic.backend.repository.CameraRepository;
import com.smarttraffic.backend.repository.SiteSettingsRepository;
import com.smarttraffic.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SiteSettingsInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SiteSettingsInitializer.class);

    private final SiteSettingsRepository repo;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CameraRepository cameraRepository;
    private final TrafficProperties trafficProperties;

    public SiteSettingsInitializer(SiteSettingsRepository repo, UserRepository userRepository,
                                   PasswordEncoder passwordEncoder, CameraRepository cameraRepository,
                                   TrafficProperties trafficProperties) {
        this.repo = repo;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.cameraRepository = cameraRepository;
        this.trafficProperties = trafficProperties;
    }

    @Override
    public void run(String... args) {
        if (repo.findById(1L).isEmpty()) {
            repo.save(new SiteSettingsEntity());
        }

        if (userRepository.count() == 0) {
            UserEntity admin = new UserEntity();
            admin.setUsername("admin");
            admin.setEmail("admin@system.local");
            admin.setPhoneNumber("0000000000");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRoleId(0);
            userRepository.save(admin);

            log.info("\n============================================\n"
                    + "初始管理员账户已创建\n"
                    + "用户名: admin\n"
                    + "邮箱:   admin@system.local\n"
                    + "密码:   admin123\n"
                    + "============================================");
        }

        if (cameraRepository.count() == 0) {
            for (String road : trafficProperties.roadsAsList()) {
                CameraEntity cam = new CameraEntity();
                cam.setName(road);
                cam.setLocation(road);
                cameraRepository.save(cam);
            }
            log.info("已从配置初始化 {} 个摄像头", trafficProperties.roadsAsList().size());
        }
    }
}
