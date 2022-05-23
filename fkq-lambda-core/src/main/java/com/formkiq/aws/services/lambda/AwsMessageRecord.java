package com.formkiq.aws.services.lambda;

import com.formkiq.graalvm.annotations.Reflectable;
import com.google.gson.annotations.SerializedName;

/**
 * 
 * AWS SNS/SQS Message Record.
 *
 */
@Reflectable
public class AwsMessageRecord {

  /** Type. */
  @SerializedName("Type")
  private String type;
  /** Message Id. */
  @SerializedName("MessageId")
  private String messageId;
  /** Topic Arn. */
  @SerializedName("TopicArn")
  private String topicArn;
  /** Message. */
  @SerializedName("Message")
  private String message;
  /** Timestamp. */
  @SerializedName("Timestamp")
  private String timestamp;
  /** SignatureVersion. */
  @SerializedName("SignatureVersion")
  private String signatureVersion;
  /** Signature. */
  @SerializedName("Signature")
  private String  signature;
  /** SigningCertURL. */
  @SerializedName("SigningCertURL")
  private String signingCertUrl;
  /** UnsubscribeURL. */
  @SerializedName("UnsubscribeURL")
  private String unsubscribeUrl;
  
  /**
   * constructor.
   */
  public AwsMessageRecord() {
  }

  /**
   * Get Message.
   * @return {@link String}
   */
  public String getMessage() {
    return this.message;
  }

  /**
   * Get Message Id.
   * @return {@link String}
   */
  public String getMessageId() {
    return this.messageId;
  }

  /**
   * Get Signature.
   * @return {@link String}
   */
  public String getSignature() {
    return this.signature;
  }

  /**
   * Get Signature Version.
   * @return {@link String}
   */
  public String getSignatureVersion() {
    return this.signatureVersion;
  }

  /**
   * Get Signing Certificate Url.
   * @return {@link String}
   */
  public String getSigningCertUrl() {
    return this.signingCertUrl;
  }

  /**
   * Get Timestamp.
   * @return {@link String}
   */
  public String getTimestamp() {
    return this.timestamp;
  }

  /**
   * Get Topic ARN.
   * @return {@link String}
   */
  public String getTopicArn() {
    return this.topicArn;
  }

  /**
   * Get Type.
   * @return {@link String}
   */
  public String getType() {
    return this.type;
  }

  /**
   * Get Unsubscribe Url.
   * @return {@link String}
   */
  public String getUnsubscribeUrl() {
    return this.unsubscribeUrl;
  }

  /**
   * Set Message.
   * @param s {@link String}
   */
  public void setMessage(final String s) {
    this.message = s;
  }

  /**
   * Set Message Id.
   * @param id {@link String}
   */
  public void setMessageId(final String id) {
    this.messageId = id;
  }

  /**
   * Set Signature.
   * @param s {@link String}
   */
  public void setSignature(final String s) {
    this.signature = s;
  }

  /**
   * Set Signature Version.
   * @param s {@link String}
   */
  public void setSignatureVersion(final String s) {
    this.signatureVersion = s;
  }

  /**
   * Set Signing Certificate Url.
   * @param s {@link String}
   */
  public void setSigningCertUrl(final String s) {
    this.signingCertUrl = s;
  }

  /**
   * Set Timestamp.
   * @param s {@link String}
   */
  public void setTimestamp(final String s) {
    this.timestamp = s;
  }

  /**
   * Set Topic Arn.
   * @param s {@link String}
   */
  public void setTopicArn(final String s) {
    this.topicArn = s;
  }

  /**
   * Set Type.
   * @param s {@link String}
   */
  public void setType(final String s) {
    this.type = s;
  }

  /**
   * Set Unsubscribe Url.
   * @param s {@link String}
   */
  public void setUnsubscribeUrl(final String s) {
    this.unsubscribeUrl = s;
  }
}
