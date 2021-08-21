package io.inprice.api.session.info;

import java.io.Serializable;

import com.google.gson.annotations.SerializedName;

import io.inprice.api.helpers.CodeGenerator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Used in session cookie
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ForCookie implements Serializable {

  private static final long serialVersionUID = -2758435636435796934L;

  @SerializedName("e")
  private String email;

  @SerializedName("r")
  private String role;

  @SerializedName("h")
  private String hash;

  public ForCookie(String email, String role) {
    this.email = email;
    this.role = role;
    this.hash = CodeGenerator.hash();
  }

  @Override
  public String toString() {
    return "[email=" + email + ", role=" + role + "]";
  }

}