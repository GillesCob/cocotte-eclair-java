package com.gillescobigo.cocotteeclair.service;

import com.gillescobigo.cocotteeclair.dto.AuthResponse;
import com.gillescobigo.cocotteeclair.dto.LoginRequest;
import com.gillescobigo.cocotteeclair.dto.RegisterRequest;
import com.gillescobigo.cocotteeclair.entity.PasswordResetToken;
import com.gillescobigo.cocotteeclair.entity.User;
import com.gillescobigo.cocotteeclair.exception.ConflictException;
import com.gillescobigo.cocotteeclair.exception.InvalidTokenException;
import com.gillescobigo.cocotteeclair.repository.PasswordResetTokenRepository;
import com.gillescobigo.cocotteeclair.repository.UserRepository;
import com.gillescobigo.cocotteeclair.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final String resetPasswordUrlBase;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            PasswordResetTokenRepository resetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailService emailService,
            @Value("${app.reset-password-url}") String resetPasswordUrlBase
    ) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.resetPasswordUrlBase = resetPasswordUrlBase;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Cet email est déjà utilisé");
        }

        User user = new User(request.email(), passwordEncoder.encode(request.password()));
        userRepository.save(user);

        return new AuthResponse(
                jwtService.generateAccessToken(user.getId()),
                jwtService.generateRefreshToken(user.getId())
        );
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Email ou mot de passe incorrect"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Email ou mot de passe incorrect");
        }

        return new AuthResponse(
                jwtService.generateAccessToken(user.getId()),
                jwtService.generateRefreshToken(user.getId())
        );
    }

    public AuthResponse refresh(String refreshToken) {
        UUID userId = jwtService.parseRefreshToken(refreshToken);

        if (!userRepository.existsById(userId)) {
            throw new InvalidTokenException("Utilisateur introuvable");
        }

        return new AuthResponse(
                jwtService.generateAccessToken(userId),
                jwtService.generateRefreshToken(userId)
        );
    }

    @Transactional
    public void forgotPassword(String email) {
        // Ne jamais révéler si l'email existe ou non : même comportement (200, pas de detail)
        // que l'email soit connu ou pas, pour ne pas exposer la base d'utilisateurs.
        userRepository.findByEmail(email).ifPresent(user -> {
            String rawToken = generateRawToken();
            String tokenHash = hashToken(rawToken);

            PasswordResetToken resetToken = new PasswordResetToken(
                    tokenHash, user, Instant.now().plusSeconds(15 * 60)
            );
            resetTokenRepository.save(resetToken);

            String resetUrl = resetPasswordUrlBase + "?token=" + rawToken;
            emailService.sendPasswordResetEmail(user.getEmail(), resetUrl);
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken = resetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Lien de réinitialisation invalide ou expiré"));

        if (resetToken.isExpired()) {
            resetTokenRepository.delete(resetToken);
            throw new InvalidTokenException("Lien de réinitialisation invalide ou expiré");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        resetTokenRepository.delete(resetToken);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
