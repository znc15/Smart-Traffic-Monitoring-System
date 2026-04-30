package com.smarttraffic.backend.security;

import com.smarttraffic.backend.model.UserEntity;
import com.smarttraffic.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrentUserResolver {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public CurrentUserResolver(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public Optional<CurrentUser> resolve(String token) {
        return jwtService.parseToken(token)
                .flatMap(parsed -> userRepository.findById(parsed.id()))
                .filter(UserEntity::isEnabled)
                .map(CurrentUser::fromEntity);
    }
}
