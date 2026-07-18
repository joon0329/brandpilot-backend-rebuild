package com.brandpilot.backend.user;

import com.brandpilot.backend.user.dto.SignupRequest;
import com.brandpilot.backend.user.dto.SignupResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignupServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SignupService signupService;

    @Test
    void signup_throwsDuplicateEmailException_whenEmailAlreadyExists() {
        SignupRequest request = new SignupRequest(
                "user@example.com",
                "password123",
                "홍길동"
        );

        when(userRepository.existsByEmail(request.email()))
                .thenReturn(true);

        assertThrows(
                DuplicateEmailException.class,
                () -> signupService.signup(request)
        );

        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void signup_returnsSignupResponse_whenEmailDoesNotExist() {
        SignupRequest request = new SignupRequest(
                "user@example.com",
                "password123",
                "홍길동"
        );
        String passwordHash = "encoded-password";
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 18, 15, 30);
        User savedUser = mock(User.class);

        when(userRepository.existsByEmail(request.email()))
                .thenReturn(false);
        when(passwordEncoder.encode(request.password()))
                .thenReturn(passwordHash);
        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);
        when(savedUser.getId()).thenReturn(1L);
        when(savedUser.getEmail()).thenReturn(request.email());
        when(savedUser.getName()).thenReturn(request.name());
        when(savedUser.getCreatedAt()).thenReturn(createdAt);

        SignupResponse response = signupService.signup(request);

        assertAll(
                () -> assertEquals(1L, response.userId()),
                () -> assertEquals(request.email(), response.email()),
                () -> assertEquals(request.name(), response.name()),
                () -> assertEquals(
                        createdAt.atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime(),
                        response.createdAt()
                )
        );

        verify(userRepository).existsByEmail(request.email());
        verify(passwordEncoder).encode(request.password());
        verify(userRepository).save(any(User.class));
    }
}
