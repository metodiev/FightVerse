package com.fightverse.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SecretsTest {

  @Test
  void hashesWithSha256LowercaseHex() {
    // FIPS 180-2 test vector for "abc".
    assertThat(Secrets.sha256Hex("abc"))
        .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
  }

  @Test
  void hashesEmptyString() {
    assertThat(Secrets.sha256Hex(""))
        .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
  }

  @Test
  void hashingIsDeterministic() {
    assertThat(Secrets.sha256Hex("refresh-token")).isEqualTo(Secrets.sha256Hex("refresh-token"));
  }

  @Test
  void differentInputsProduceDifferentHashes() {
    assertThat(Secrets.sha256Hex("token-a")).isNotEqualTo(Secrets.sha256Hex("token-b"));
  }

  @Test
  void newTokenIsUrlSafeBase64Of256Bits() {
    String token = Secrets.newToken();

    // 32 random bytes encode to 43 unpadded base64url characters.
    assertThat(token).hasSize(43);
    assertThat(token).matches("[A-Za-z0-9_-]+");
  }

  @Test
  void newTokenDoesNotRepeat() {
    Set<String> tokens = new HashSet<>();
    for (int i = 0; i < 100; i++) {
      tokens.add(Secrets.newToken());
    }

    assertThat(tokens).hasSize(100);
  }
}
