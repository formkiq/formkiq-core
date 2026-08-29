/**
 * MIT License
 * 
 * Copyright (c) 2018 - 2020 FormKiQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.formkiq.module.httpsigv4;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

/** Validates requests signed by {@link HttpServiceSigv4}. */
public final class HttpServiceSigv4Validator {

  /** SigV4 authorization algorithm. */
  private static final String ALGORITHM = "AWS4-HMAC-SHA256";
  /** Authorization fields required by SigV4. */
  private static final Set<String> AUTHORIZATION_FIELDS =
      Set.of("Credential", "SignedHeaders", "Signature");
  /** Number of components in a SigV4 credential scope. */
  private static final int CREDENTIAL_COMPONENTS = 5;
  /** Credential scope date component. */
  private static final int CREDENTIAL_DATE = 1;
  /** Credential scope region component. */
  private static final int CREDENTIAL_REGION = 2;
  /** Credential scope service component. */
  private static final int CREDENTIAL_SERVICE = 3;
  /** Credential scope terminator component. */
  private static final int CREDENTIAL_TERMINATOR = 4;
  /** Default allowed difference between the signing time and server time. */
  private static final Duration DEFAULT_CLOCK_SKEW = Duration.ofMinutes(5);
  /** Length of a SigV4 credential date. */
  private static final int SIGNING_DATE_LENGTH = 8;
  /** SigV4 timestamp format. */
  private static final DateTimeFormatter TIMESTAMP_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
  /** SigV4 credential terminator. */
  private static final String TERMINATOR = "aws4_request";

  /** Allowed difference between the signing time and server time. */
  private final Duration allowedClockSkew;
  /** Credentials accepted by this validator. */
  private final AwsCredentials credentials;
  /** Current time source. */
  private final Clock clock;
  /** Region accepted by this validator. */
  private final Region region;
  /** Service name accepted by this validator. */
  private final String serviceName;

  /**
   * constructor.
   *
   * @param service signing service name
   * @param signingRegion signing region
   * @param signingCredentials signing credentials
   */
  public HttpServiceSigv4Validator(final String service, final Region signingRegion,
      final AwsCredentials signingCredentials) {
    this(service, signingRegion, signingCredentials, DEFAULT_CLOCK_SKEW, Clock.systemUTC());
  }

  /**
   * constructor.
   *
   * @param service signing service name
   * @param signingRegion signing region
   * @param signingCredentials signing credentials
   * @param clockSkew allowed difference between the signing time and server time
   */
  public HttpServiceSigv4Validator(final String service, final Region signingRegion,
      final AwsCredentials signingCredentials, final Duration clockSkew) {
    this(service, signingRegion, signingCredentials, clockSkew, Clock.systemUTC());
  }

  HttpServiceSigv4Validator(final String service, final Region signingRegion,
      final AwsCredentials signingCredentials, final Duration clockSkew, final Clock timeSource) {
    if (service == null || signingRegion == null || signingCredentials == null) {
      throw new IllegalArgumentException();
    }
    if (clockSkew == null || timeSource == null) {
      throw new IllegalArgumentException();
    }
    if (clockSkew.isNegative()) {
      throw new IllegalArgumentException();
    }

    this.serviceName = service;
    this.region = signingRegion;
    this.credentials = signingCredentials;
    this.allowedClockSkew = clockSkew;
    this.clock = timeSource;
  }

  private boolean appendSignedHeaders(final SdkHttpFullRequest.Builder request,
      final Map<String, List<String>> headers, final String[] signedHeaders) {
    for (String header : signedHeaders) {
      if (header.isEmpty() || "authorization".equalsIgnoreCase(header)) {
        return false;
      }

      List<String> headerValues = findHeaders(headers, header);
      if (headerValues.isEmpty()) {
        return false;
      }

      if (!"host".equalsIgnoreCase(header)) {
        headerValues.forEach(value -> request.appendHeader(header, value));
      }
    }

    return true;
  }

  private boolean constantTimeEquals(final String expected, final String supplied) {
    return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
        supplied.getBytes(StandardCharsets.UTF_8));
  }

  private Optional<Map<String, String>> createExpectedAuthorization(final String method,
      final URI uri, final Map<String, List<String>> headers, final byte[] payload,
      final String[] signedHeaders, final Instant signedAt) {
    try {
      SdkHttpFullRequest.Builder request = SdkHttpFullRequest.builder().uri(uri)
          .method(SdkHttpMethod.valueOf(method.toUpperCase(Locale.ROOT)))
          .contentStreamProvider(() -> new ByteArrayInputStream(payload));
      if (!appendSignedHeaders(request, headers, signedHeaders)) {
        return Optional.empty();
      }

      Aws4SignerParams params = Aws4SignerParams.builder().signingName(this.serviceName)
          .signingRegion(this.region).awsCredentials(this.credentials)
          .signingClockOverride(Clock.fixed(signedAt, ZoneOffset.UTC)).build();
      SdkHttpFullRequest expected = Aws4Signer.create().sign(request.build(), params);
      return expected.firstMatchingHeader("Authorization").flatMap(this::parseAuthorization);
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  private Optional<String> findHeader(final Map<String, List<String>> headers, final String name) {
    return headers.entrySet().stream().filter(e -> name.equalsIgnoreCase(e.getKey()))
        .flatMap(e -> e.getValue().stream()).findFirst();
  }

  private List<String> findHeaders(final Map<String, List<String>> headers, final String name) {
    return headers.entrySet().stream().filter(e -> name.equalsIgnoreCase(e.getKey()))
        .flatMap(e -> e.getValue().stream()).toList();
  }

  private Optional<Map<String, String>> getAuthorizationValues(final URI uri,
      final Map<String, List<String>> headers) {
    List<String> authorizations = findHeaders(headers, "Authorization");
    List<String> dates = findHeaders(headers, "X-Amz-Date");
    List<String> hosts = findHeaders(headers, "Host");
    if (authorizations.size() != 1 || dates.size() != 1 || hosts.size() != 1) {
      return Optional.empty();
    }

    String authority = uri.getRawAuthority();
    if (authority == null || !authority.equalsIgnoreCase(hosts.getFirst().trim())) {
      return Optional.empty();
    }

    return parseAuthorization(authorizations.getFirst());
  }

  private boolean hasMissingRequestValue(final String method, final URI uri,
      final Map<String, List<String>> headers, final byte[] payload) {
    return method == null || uri == null || headers == null || payload == null;
  }

  private boolean hasValidCredentialScope(final String credentialValue, final Instant signedAt) {
    String[] credential = credentialValue.split("/", -1);
    if (credential.length != CREDENTIAL_COMPONENTS
        || !constantTimeEquals(this.credentials.accessKeyId(), credential[0])) {
      return false;
    }
    if (!this.region.id().equals(credential[CREDENTIAL_REGION])
        || !this.serviceName.equals(credential[CREDENTIAL_SERVICE])
        || !TERMINATOR.equals(credential[CREDENTIAL_TERMINATOR])) {
      return false;
    }

    String signingDate = TIMESTAMP_FORMAT.format(signedAt).substring(0, SIGNING_DATE_LENGTH);
    return credential[CREDENTIAL_DATE].equals(signingDate) && isWithinAllowedClockSkew(signedAt);
  }

  /**
   * Validate a signed HTTP request.
   *
   * @param method HTTP method
   * @param uri complete request URI
   * @param headers request headers
   * @param payload request body
   * @return true when the SigV4 signature and credential scope are valid
   */
  public boolean isValid(final String method, final URI uri,
      final Map<String, List<String>> headers, final byte[] payload) {
    if (hasMissingRequestValue(method, uri, headers, payload)) {
      return false;
    }

    Optional<Map<String, String>> authorizationValues = getAuthorizationValues(uri, headers);
    Optional<Instant> requestTime = signingTime(headers);
    if (authorizationValues.isEmpty() || requestTime.isEmpty()) {
      return false;
    }

    Map<String, String> values = authorizationValues.get();
    Instant signedAt = requestTime.get();
    if (!hasValidCredentialScope(values.get("Credential"), signedAt)) {
      return false;
    }

    String[] signedHeaders = values.get("SignedHeaders").split(";", -1);
    Optional<Map<String, String>> expectedValues =
        createExpectedAuthorization(method, uri, headers, payload, signedHeaders, signedAt);
    return expectedValues.filter(expected -> signaturesMatch(expected, values)).isPresent();
  }

  private boolean isWithinAllowedClockSkew(final Instant signedAt) {
    return Duration.between(this.clock.instant(), signedAt).abs()
        .compareTo(this.allowedClockSkew) <= 0;
  }

  private Optional<Map<String, String>> parseAuthorization(final String authorization) {
    if (authorization == null || !authorization.startsWith(ALGORITHM + " ")) {
      return Optional.empty();
    }

    Map<String, String> values = new HashMap<>();
    String[] parts = authorization.substring(ALGORITHM.length() + 1).split(",", -1);

    for (String part : parts) {
      Optional<Map.Entry<String, String>> authorizationPart = parseAuthorizationPart(part);
      if (authorizationPart.isEmpty()) {
        return Optional.empty();
      }

      Map.Entry<String, String> entry = authorizationPart.get();
      if (values.putIfAbsent(entry.getKey(), entry.getValue()) != null) {
        return Optional.empty();
      }
    }

    return AUTHORIZATION_FIELDS.equals(values.keySet()) ? Optional.of(values) : Optional.empty();
  }

  private Optional<Map.Entry<String, String>> parseAuthorizationPart(final String part) {
    int equals = part.indexOf('=');
    if (equals < 1) {
      return Optional.empty();
    }

    String key = part.substring(0, equals).trim();
    String value = part.substring(equals + 1).trim();
    return value.isEmpty() ? Optional.empty() : Optional.of(Map.entry(key, value));
  }

  private boolean signaturesMatch(final Map<String, String> expected,
      final Map<String, String> supplied) {
    return constantTimeEquals(expected.get("SignedHeaders"), supplied.get("SignedHeaders"))
        && constantTimeEquals(expected.get("Signature"), supplied.get("Signature"));
  }

  private Optional<Instant> signingTime(final Map<String, List<String>> headers) {
    try {
      return findHeader(headers, "X-Amz-Date").map(TIMESTAMP_FORMAT::parse).map(Instant::from);
    } catch (DateTimeParseException e) {
      return Optional.empty();
    }
  }
}
