package br.com.assine.auth.adapter.out.security;

import br.com.assine.auth.domain.model.AuthUser;
import br.com.assine.auth.domain.model.TokenPair;
import br.com.assine.auth.domain.port.out.JwtTokenProvider;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtAdapter implements JwtTokenProvider {

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;
    private final String keyId = UUID.randomUUID().toString();

    public JwtAdapter(
            @Value("${jwt.private-key}") String privateKeyStr,
            @Value("${jwt.public-key}") String publicKeyStr,
            @Value("${jwt.access-token-expiration}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpirationMs
    ) throws NoSuchAlgorithmException, InvalidKeySpecException {
        this.privateKey = parsePrivateKey(privateKeyStr);
        this.publicKey = parsePublicKey(publicKeyStr);
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Override
    public TokenPair generateTokenPair(AuthUser authUser) {
        String accessToken = generateToken(authUser, accessTokenExpirationMs);
        String refreshToken = generateToken(authUser, refreshTokenExpirationMs);
        return new TokenPair(accessToken, refreshToken);
    }

    @Override
    public String validateTokenAndGetEmail(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            RSASSAVerifier verifier = new RSASSAVerifier(publicKey);

            if (!signedJWT.verify(verifier)) {
                throw new IllegalArgumentException("Invalid token signature");
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            if (new Date().after(claims.getExpirationTime())) {
                throw new IllegalStateException("Token expired");
            }

            return claims.getSubject();
        } catch (ParseException | JOSEException e) {
            throw new IllegalArgumentException("Failed to validate token", e);
        }
    }

    @Override
    public Map<String, Object> getJwks() {
        RSAKey jwk = new RSAKey.Builder(publicKey)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID(keyId)
                .build();
        return new JWKSet(jwk).toJSONObject();
    }

    private String generateToken(AuthUser authUser, long expirationMs) {
        try {
            JWSSigner signer = new RSASSASigner(privateKey);
            Date now = new Date();
            Date expiration = new Date(now.getTime() + expirationMs);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(authUser.email())
                    .issuer("assine-auth")
                    .issueTime(now)
                    .expirationTime(expiration)
                    .claim("role", authUser.role().name())
                    .jwtID(UUID.randomUUID().toString())
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keyId).build(),
                    claimsSet
            );

            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Error signing JWT", e);
        }
    }

    private RSAPrivateKey parsePrivateKey(String key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String cleanKey = key.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(cleanKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private RSAPublicKey parsePublicKey(String key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String cleanKey = key.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(cleanKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}
