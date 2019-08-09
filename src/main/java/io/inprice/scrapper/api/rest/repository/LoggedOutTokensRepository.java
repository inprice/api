package io.inprice.scrapper.api.rest.repository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LoggedOutTokensRepository {

    private final List<TokenHolder> tokens = new CopyOnWriteArrayList<>();

    public void addToken(String token, long expirationDate) {
        tokens.add(new TokenHolder(token, expirationDate));
    }

    public boolean isTokenLoggedOut(String token) {
        return tokens.stream().anyMatch(b -> b.getToken().equals(token));
    }

    public void removeExpired() {
        final long currentTimestamp = System.currentTimeMillis();

        for (TokenHolder token : tokens) {
            if (token.getExpirationDate() < currentTimestamp) {
                tokens.remove(token);
            }
        }
    }

    private final class TokenHolder {

        final String token;
        final long expirationDate;

        TokenHolder(String token, long expirationDate) {
            this.token = token;
            this.expirationDate = expirationDate;
        }

        String getToken() {
            return token;
        }

        long getExpirationDate() {
            return expirationDate;
        }
    }

}
