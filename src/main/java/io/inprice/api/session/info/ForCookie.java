package io.inprice.api.session.info;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Used in session cookie
 */
@Getter
@NoArgsConstructor
public class ForCookie implements Serializable {

  private static final long serialVersionUID = -2758435636435796934L;

  @JsonProperty("e")
  private String email;

  @JsonProperty("r")
  private String role;

  @JsonProperty("h")
  private String hash;

  public ForCookie(String email, String role) {
    this.email = email;
    this.role = role;
    this.hash = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
  }

  @Override
  public String toString() {
    return "[email=" + email + ", role=" + role + "]";
  }

}