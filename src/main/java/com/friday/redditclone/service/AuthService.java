package com.friday.redditclone.service;

import com.friday.redditclone.dto.AuthenticationResponse;
import com.friday.redditclone.dto.LoginRequest;
import com.friday.redditclone.dto.RefreshTokenRequest;
import com.friday.redditclone.dto.RegisterRequest;
import com.friday.redditclone.exception.SpringRedditException;
import com.friday.redditclone.models.NotificationEmail;
import com.friday.redditclone.models.User;
import com.friday.redditclone.models.VerificationToken;
import com.friday.redditclone.repository.UserRepository;
import com.friday.redditclone.repository.VerificationTokenRepository;
import com.friday.redditclone.security.JwtProvider;
import io.jsonwebtoken.Jwts;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final MailService mailService;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;


    @Transactional
    public void signup(RegisterRequest registerRequest) {
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setCreated(Instant.now());
        user.setEnabled(false);

        userRepository.save(user);

        String token = generateVerificationToken(user);

        mailService.sendMail(new NotificationEmail(
                "Please Activate your Account",
                registerRequest.getEmail(),
                "ThankYou for signing up for Spring Reddit, " +
                        "please click on the below url to activate your account : \n" +
                        "http://localhost:8080/api/auth/accountVerification/" + token
        ));




    }

    private String generateVerificationToken(User user) {
        String token = UUID.randomUUID().toString();

        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);

        verificationTokenRepository.save(verificationToken);
        return token;
    }

    public void verifyAccount(String token) {
        Optional<VerificationToken> verificationToken = verificationTokenRepository.findByToken(token);
        verificationToken.orElseThrow(() -> new SpringRedditException("Invalid Token"));
        fetchUserAndEnable(verificationToken.get());

    }

    @Transactional
    private void fetchUserAndEnable(VerificationToken verificationToken) {
        String username = verificationToken.getUser().getUsername();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new SpringRedditException("user not found with name - " + username));
        user.setEnabled(true);
        userRepository.save(user);
    }



    public AuthenticationResponse login(LoginRequest loginRequest) {
        Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(),
                loginRequest.getPassword()
        ));

        //check if a user is logged in or not
        SecurityContextHolder.getContext().setAuthentication(authenticate);
        String token = jwtProvider.generateToken(authenticate);

        //to send this token we will use a dto
        return AuthenticationResponse.builder()
                .username(loginRequest.getUsername())
                .authenticationToken(token)
                .expiresAt(Instant.now().plusMillis(jwtProvider.getJwtExpirationInMillis()))
                .refreshToken(refreshTokenService.generateRefreshToken().getToken())
                .build();


    }

    @Transactional
    public User getCurrentUser() {

        org.springframework.security.core.userdetails.User prinipal =
                (org.springframework.security.core.userdetails.User)
                        SecurityContextHolder
                                .getContext().getAuthentication().getPrincipal();

        return userRepository.findByUsername(prinipal.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User with username " + prinipal.getUsername()));
    }

    public boolean isLoggedIn() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext().getAuthentication();

        return !(authentication instanceof AnonymousAuthenticationToken && authentication.isAuthenticated());
    }

    public AuthenticationResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        refreshTokenService.validateRefreshToken(
                refreshTokenRequest.getRefreshToken()
        );

        String token = jwtProvider.generateTokenWithUsername(refreshTokenRequest.getUsername());

        return AuthenticationResponse.builder()
                .authenticationToken(token)
                .refreshToken(refreshTokenRequest.getRefreshToken())
                .expiresAt(Instant.now().plusMillis(jwtProvider.getJwtExpirationInMillis()))
                .username(refreshTokenRequest.getUsername())
                .build();

    }
}
