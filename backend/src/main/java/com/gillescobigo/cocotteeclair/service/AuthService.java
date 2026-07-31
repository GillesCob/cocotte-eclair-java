package com.gillescobigo.cocotteeclair.service;

import com.gillescobigo.cocotteeclair.dto.AuthResponse;
import com.gillescobigo.cocotteeclair.dto.LoginRequest;
import com.gillescobigo.cocotteeclair.dto.RegisterRequest;
import com.gillescobigo.cocotteeclair.entity.PasswordResetToken;
import com.gillescobigo.cocotteeclair.entity.RefreshToken;
import com.gillescobigo.cocotteeclair.entity.User;
import com.gillescobigo.cocotteeclair.exception.ConflictException;
import com.gillescobigo.cocotteeclair.exception.InvalidTokenException;
import com.gillescobigo.cocotteeclair.repository.PasswordResetTokenRepository;
import com.gillescobigo.cocotteeclair.repository.RefreshTokenRepository;
import com.gillescobigo.cocotteeclair.repository.UserRepository;
import com.gillescobigo.cocotteeclair.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final String resetPasswordUrlBase;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            PasswordResetTokenRepository resetTokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailService emailService,
            @Value("${app.reset-password-url}") String resetPasswordUrlBase
    ) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
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

        return issueTokenPair(user.getId());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Email ou mot de passe incorrect"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Email ou mot de passe incorrect");
        }

        return issueTokenPair(user.getId());
    }

    // Rotation systematique a chaque refresh + detection de reutilisation : si le
    // token presente est deja marque revoque en base, c'est qu'un token deja tourne
    // est represente (vol probable), toute la famille de sessions de l'utilisateur
    // est revoquee par precaution plutot que de faire confiance a ce seul jeton.
    //
    // Volontairement PAS @Transactional : la revocation de la famille doit rester
    // persistee meme quand la methode leve ensuite une exception pour refuser la
    // requete. Avec @Transactional, l'exception aurait annule (rollback) toute la
    // transaction, y compris la revocation qu'on vient de faire -> la contre-mesure
    // de securite se serait annulee elle-meme silencieusement. Constate en
    // verification manuelle reelle (curl), pas visible avec des tests qui simulent
    // les repositories. Chaque appel a un repository ci-dessous commit seul (chaque
    // methode Spring Data est deja transactionnelle individuellement).
    public AuthResponse refresh(String refreshToken) {
        UUID userId = jwtService.parseRefreshToken(refreshToken);
        String tokenHash = hashToken(refreshToken);

        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Session invalide, veuillez vous reconnecter"));

        if (stored.isRevoked()) {
            revokeAllActiveTokensFor(userId);
            throw new InvalidTokenException("Session invalide, veuillez vous reconnecter");
        }

        if (stored.isExpired()) {
            throw new InvalidTokenException("Session invalide, veuillez vous reconnecter");
        }

        if (!userRepository.existsById(userId)) {
            throw new InvalidTokenException("Utilisateur introuvable");
        }

        AuthResponse newTokens = issueTokenPair(userId);
        stored.revoke(hashToken(newTokens.refreshToken()));
        refreshTokenRepository.save(stored);

        return newTokens;
    }

    // Tolerant par design : un logout doit toujours reussir cote client meme si le
    // refresh token presente est deja expire/invalide/inconnu.
    @Transactional
    public void logout(String refreshToken) {
        UUID userId;
        try {
            userId = jwtService.parseRefreshToken(refreshToken);
        } catch (RuntimeException e) {
            log.debug("Logout avec un refresh token deja invalide, rien a revoquer");
            return;
        }

        String tokenHash = hashToken(refreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(stored -> {
            if (!stored.isRevoked()) {
                stored.revoke();
                refreshTokenRepository.save(stored);
            }
        });
    }

    private AuthResponse issueTokenPair(UUID userId) {
        String accessToken = jwtService.generateAccessToken(userId);
        String refreshToken = jwtService.generateRefreshToken(userId);

        User user = userRepository.getReferenceById(userId);
        RefreshToken entity = new RefreshToken(
                hashToken(refreshToken), user, Instant.now().plus(jwtService.getRefreshTtl())
        );
        refreshTokenRepository.save(entity);

        return new AuthResponse(accessToken, refreshToken);
    }

    private void revokeAllActiveTokensFor(UUID userId) {
        var activeTokens = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId);
        activeTokens.forEach(RefreshToken::revoke);
        refreshTokenRepository.saveAll(activeTokens);
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
        // Toute session existante (refresh token deja emis) est invalidee : un
        // changement de mot de passe doit deconnecter partout, pas seulement ici.
        revokeAllActiveTokensFor(user.getId());
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
