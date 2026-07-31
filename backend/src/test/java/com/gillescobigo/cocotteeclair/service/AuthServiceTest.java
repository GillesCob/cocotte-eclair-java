package com.gillescobigo.cocotteeclair.service;

import com.gillescobigo.cocotteeclair.dto.AuthResponse;
import com.gillescobigo.cocotteeclair.dto.LoginRequest;
import com.gillescobigo.cocotteeclair.entity.RefreshToken;
import com.gillescobigo.cocotteeclair.entity.User;
import com.gillescobigo.cocotteeclair.exception.InvalidTokenException;
import com.gillescobigo.cocotteeclair.repository.PasswordResetTokenRepository;
import com.gillescobigo.cocotteeclair.repository.RefreshTokenRepository;
import com.gillescobigo.cocotteeclair.repository.UserRepository;
import com.gillescobigo.cocotteeclair.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository resetTokenRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private EmailService emailService;

    private AuthService authService;
    private User proprietaire;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, resetTokenRepository, refreshTokenRepository,
                passwordEncoder, jwtService, emailService, "http://localhost:4200/reset-password"
        );
        proprietaire = new User("proprietaire@cocotte.fr", "hash");

        lenient().when(jwtService.getRefreshTtl()).thenReturn(Duration.ofDays(7));
        lenient().when(userRepository.getReferenceById(proprietaire.getId())).thenReturn(proprietaire);
    }

    @Test
    void login_succes_persisteUnRefreshTokenHacheEnBase() {
        when(userRepository.findByEmail("proprietaire@cocotte.fr")).thenReturn(Optional.of(proprietaire));
        when(passwordEncoder.matches("motdepasse123", proprietaire.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(proprietaire.getId())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(proprietaire.getId())).thenReturn("refresh-token-brut");

        AuthResponse response = authService.login(new LoginRequest("proprietaire@cocotte.fr", "motdepasse123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token-brut");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken persiste = captor.getValue();

        assertThat(persiste.getUser()).isEqualTo(proprietaire);
        assertThat(persiste.getTokenHash()).isNotEqualTo("refresh-token-brut");
        assertThat(persiste.isRevoked()).isFalse();
        assertThat(persiste.getExpiresAt()).isAfter(Instant.now().plusSeconds(6 * 24 * 3600));
    }

    @Test
    void refresh_tokenValideEtActif_genereNouveauCoupleEtRevoqueLAncien() {
        UUID userId = proprietaire.getId();
        RefreshToken stored = new RefreshToken("hash-ancien", proprietaire, Instant.now().plusSeconds(3600));

        when(jwtService.parseRefreshToken("ancien-refresh")).thenReturn(userId);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
        when(userRepository.existsById(userId)).thenReturn(true);
        when(jwtService.generateAccessToken(userId)).thenReturn("nouveau-access");
        when(jwtService.generateRefreshToken(userId)).thenReturn("nouveau-refresh");

        AuthResponse response = authService.refresh("ancien-refresh");

        assertThat(response.accessToken()).isEqualTo("nouveau-access");
        assertThat(response.refreshToken()).isEqualTo("nouveau-refresh");
        assertThat(stored.isRevoked()).isTrue();
        assertThat(stored.getReplacedByTokenHash()).isNotNull();
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
        verify(refreshTokenRepository, never()).saveAll(any());
    }

    @Test
    void refresh_tokenInconnuEnBase_leveInvalidTokenException() {
        UUID userId = proprietaire.getId();
        when(jwtService.parseRefreshToken("token-inconnu")).thenReturn(userId);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("token-inconnu"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refresh_tokenExpireEnBase_leveInvalidTokenException() {
        UUID userId = proprietaire.getId();
        RefreshToken expire = new RefreshToken("hash-expire", proprietaire, Instant.now().minusSeconds(60));

        when(jwtService.parseRefreshToken("token-expire")).thenReturn(userId);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expire));

        assertThatThrownBy(() -> authService.refresh("token-expire"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refresh_tokenDejaRevoque_revoqueTouteLaFamilleEtLeveInvalidTokenException() {
        UUID userId = proprietaire.getId();
        RefreshToken dejaRevoque = new RefreshToken("hash-vole", proprietaire, Instant.now().plusSeconds(3600));
        dejaRevoque.revoke();
        RefreshToken autreSessionActive = new RefreshToken("hash-autre", proprietaire, Instant.now().plusSeconds(3600));

        when(jwtService.parseRefreshToken("token-deja-tourne")).thenReturn(userId);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(dejaRevoque));
        when(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId))
                .thenReturn(List.of(autreSessionActive));

        assertThatThrownBy(() -> authService.refresh("token-deja-tourne"))
                .isInstanceOf(InvalidTokenException.class);

        assertThat(autreSessionActive.isRevoked()).isTrue();
        verify(refreshTokenRepository).saveAll(List.of(autreSessionActive));
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void logout_tokenValide_leMarqueRevoqueEnBase() {
        UUID userId = proprietaire.getId();
        RefreshToken stored = new RefreshToken("hash-session", proprietaire, Instant.now().plusSeconds(3600));

        when(jwtService.parseRefreshToken("token-a-deconnecter")).thenReturn(userId);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        authService.logout("token-a-deconnecter");

        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void logout_tokenInexistantOuDejaRevoque_neLeveRienResteIdempotent() {
        when(jwtService.parseRefreshToken("token-invalide")).thenThrow(new InvalidTokenException("invalide"));

        authService.logout("token-invalide");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void resetPassword_succes_revoqueTousLesRefreshTokensActifsDeLUtilisateur() {
        var resetToken = new com.gillescobigo.cocotteeclair.entity.PasswordResetToken(
                "hash-reset", proprietaire, Instant.now().plusSeconds(600)
        );
        RefreshToken sessionActive = new RefreshToken("hash-session", proprietaire, Instant.now().plusSeconds(3600));

        when(resetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("nouveauMotDePasse")).thenReturn("nouveau-hash");
        when(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(proprietaire.getId()))
                .thenReturn(List.of(sessionActive));

        authService.resetPassword("token-brut", "nouveauMotDePasse");

        assertThat(sessionActive.isRevoked()).isTrue();
        verify(refreshTokenRepository).saveAll(List.of(sessionActive));
    }
}
