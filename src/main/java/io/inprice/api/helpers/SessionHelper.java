package io.inprice	.api.helpers;

import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import io.inprice.api.session.info.ForCookie;
import io.inprice.common.config.Secrets;
import io.inprice.common.config.Secrets.Realm;
import io.inprice.common.helpers.JsonConverter;
import io.inprice.common.models.User;
import io.inprice.common.utils.AES;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultClaims;
import io.jsonwebtoken.jackson.io.JacksonDeserializer;
import io.jsonwebtoken.jackson.io.JacksonSerializer;
import io.jsonwebtoken.security.Keys;

public class SessionHelper {

	private static final Logger log = LoggerFactory.getLogger(SessionHelper.class);

	private static final JwtBuilder builderForUser, builderForSuper;
	private static final JwtParser parserForUser, parserForSuper;

	static {
		final byte[] SUPER_KEY = Secrets.getKey(Realm.SUPER_USER);
		final byte[] USER_KEY = Secrets.getKey(Realm.USER);
		
		if (SUPER_KEY == null) {
			System.err.println("Super user key is empty!");
			System.exit(-1);
		}

		if (USER_KEY == null) {
			System.err.println("User user key is empty!");
			System.exit(-1);
		}

		SecretKey keyForSuper = Keys.hmacShaKeyFor(SUPER_KEY);
		SecretKey keyForUsers = Keys.hmacShaKeyFor(USER_KEY);

		builderForSuper = Jwts
			.builder()
			.signWith(keyForSuper)
			.serializeToJsonWith(new JacksonSerializer<>(JsonConverter.mapper));

		parserForSuper = Jwts
			.parserBuilder()
			.setSigningKey(keyForSuper)
			.deserializeJsonWith(new JacksonDeserializer<>(JsonConverter.mapper))
			.build();

		builderForUser = Jwts
			.builder()
			.signWith(keyForUsers)
			.serializeToJsonWith(new JacksonSerializer<>(JsonConverter.mapper));

		parserForUser = Jwts
			.parserBuilder()
			.setSigningKey(keyForUsers)
	    .deserializeJsonWith(new JacksonDeserializer<>(JsonConverter.mapper))
	    .build();
	}

	public static String toTokenForSuper(User user) {
		DefaultClaims claims = new DefaultClaims();
		String json = JsonConverter.toJson(user);
		claims.put("payload", AES.encrypt(json));
		Date exp = new Date(System.currentTimeMillis() + (60 * 60 * 1000)); // one hour later!
		return builderForSuper.setExpiration(exp).setClaims(claims).compact();
	}

	public static User fromTokenForSuper(String token) {
		if (StringUtils.isNotBlank(token)) {
  		try {
  			Claims claims = parserForSuper.parseClaimsJws(token).getBody();
  			String encrypted = claims.get("payload", String.class);
  			String plainText = AES.decrypt(encrypted);
  			return JsonConverter.fromJson(plainText, User.class);
  		} catch (Exception e) {
  			log.error("Super user token error!", e);
  		}
		}
		return null;
	}

	public static String toTokenForUser(List<ForCookie> sessions) {
		DefaultClaims claims = new DefaultClaims();
		claims.put("payload", sessions);
		return builderForUser.setClaims(claims).compact();
	}

	public static List<ForCookie> fromTokenForUser(String token) {
		if (StringUtils.isNotBlank(token)) {
  		try {
  			Claims claims = parserForUser.parseClaimsJws(token).getBody();
  			Object raw = claims.get("payload", List.class);
  			return JsonConverter.mapper.convertValue(raw, new TypeReference<List<ForCookie>>() {});
  		} catch (Exception e) {
  			log.error("User token error!", e);
  		}
		}
		return null;
	}

}