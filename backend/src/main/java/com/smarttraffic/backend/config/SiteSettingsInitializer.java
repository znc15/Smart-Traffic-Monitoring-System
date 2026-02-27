package com.smarttraffic.backend.config;

import com.smarttraffic.backend.model.CameraEntity;
import com.smarttraffic.backend.model.ApiClientEntity;
import com.smarttraffic.backend.model.SiteSettingsEntity;
import com.smarttraffic.backend.model.UserEntity;
import com.smarttraffic.backend.repository.ApiClientRepository;
import com.smarttraffic.backend.repository.CameraRepository;
import com.smarttraffic.backend.repository.SiteSettingsRepository;
import com.smarttraffic.backend.repository.UserRepository;
import org.springframework.util.StringUtils;
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
    private final InitAdminProperties initAdminProperties;
    private final ApiClientRepository apiClientRepository;
    private final MaasProperties maasProperties;

    public SiteSettingsInitializer(SiteSettingsRepository repo, UserRepository userRepository,
                                   PasswordEncoder passwordEncoder, CameraRepository cameraRepository,
                                   TrafficProperties trafficProperties, InitAdminProperties initAdminProperties,
                                   ApiClientRepository apiClientRepository, MaasProperties maasProperties) {
        this.repo = repo;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.cameraRepository = cameraRepository;
        this.trafficProperties = trafficProperties;
        this.initAdminProperties = initAdminProperties;
        this.apiClientRepository = apiClientRepository;
        this.maasProperties = maasProperties;
    }

    @Override
    public void run(String... args) {
        if (repo.findById(1L).isEmpty()) {
            repo.save(new SiteSettingsEntity());
        }

        if (userRepository.count() == 0 && initAdminProperties.isEnabled()) {
            createInitialAdmin();
        } else if (userRepository.count() == 0) {
            log.warn("当前系统无用户，已禁用默认管理员初始化。若需要自动初始化，请设置 INIT_ADMIN=true 并提供 INIT_ADMIN_* 配置。");
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

        ensureDefaultApiClient();
    }

    private void createInitialAdmin() {
        String username = safeTrim(initAdminProperties.getUsername());
        String email = safeTrim(initAdminProperties.getEmail());
        String password = initAdminProperties.getPassword();

        if (!StringUtils.hasText(username) || !StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            throw new IllegalStateException("INIT_ADMIN=true 时必须提供 INIT_ADMIN_USERNAME、INIT_ADMIN_EMAIL、INIT_ADMIN_PASSWORD");
        }
        if (!isStrongPassword(password)) {
            throw new IllegalStateException("INIT_ADMIN_PASSWORD 不符合复杂度要求（至少 8 位，包含大小写字母、数字和特殊字符）");
        }

        UserEntity admin = new UserEntity();
        admin.setUsername(username);
        admin.setEmail(email);
        admin.setPhoneNumber("0000000000");
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRoleId(0);
        userRepository.save(admin);

        log.info("已根据 INIT_ADMIN_* 配置创建初始管理员: {}", username);
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (char ch : password.toCharArray()) {
            if (Character.isUpperCase(ch)) hasUpper = true;
            else if (Character.isLowerCase(ch)) hasLower = true;
            else if (Character.isDigit(ch)) hasDigit = true;
            else hasSpecial = true;
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    private void ensureDefaultApiClient() {
        if (apiClientRepository.count() > 0) {
            return;
        }
        ApiClientEntity entity = new ApiClientEntity();
        entity.setName(safeTrim(maasProperties.getDefaultClientName()).isEmpty()
                ? "default-dev-client"
                : safeTrim(maasProperties.getDefaultClientName()));
        String key = safeTrim(maasProperties.getDefaultApiKey());
        entity.setApiKey(key.isEmpty() ? "dev-maas-key-change-me" : key);
        entity.setEnabled(true);
        apiClientRepository.save(entity);
        log.info("已初始化默认 MaaS API 客户端: {}", entity.getName());
    }
}
