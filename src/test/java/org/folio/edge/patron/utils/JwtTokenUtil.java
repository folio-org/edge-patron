package org.folio.edge.patron.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.Jwks;

public class JwtTokenUtil {
  private Jwk key;

  public JwtTokenUtil() {
    key = Jwks.parser().build().parse(this.getClass().getResourceAsStream("/test_jwk.json"));
  }

  public String generateToken(String externalSystemId, boolean vip) {
    return Jwts.builder()
      .header().keyId(key.getId()).and()
      .subject("test")
      .signWith(key.toKey())
      .claim("externalSystemId", externalSystemId)
      .claim("vip", vip)
      .compact();
  }

  public String generateToken() {
    return Jwts.builder()
      .header().keyId(key.getId()).and()
      .subject("test")
      .signWith(key.toKey())
      .compact();
  }
}
