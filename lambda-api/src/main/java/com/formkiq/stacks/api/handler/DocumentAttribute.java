package com.formkiq.stacks.api.handler;

import java.util.List;
import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Document Attributes Request.
 */
@Reflectable
public class DocumentAttribute {

  /** Boolean value. */
  private Boolean booleanValue;
  /** Key of Attribute. */
  private String key;
  /** Number value. */
  private Double numberValue;
  /** Number value. */
  private List<Double> numberValues;
  /** String valueAttribute. */
  private String stringValue;
  /** String valueAttribute. */
  private List<String> stringValues;

  /**
   * constructor.
   */
  public DocumentAttribute() {

  }

  /**
   * Set Boolean Value.
   * 
   * @param attributeValue {@link Boolean}
   * @return {@link DocumentAttribute}
   */
  public DocumentAttribute booleanValue(final Boolean attributeValue) {
    this.booleanValue = attributeValue;
    return this;
  }

  /**
   * Get Boolean Value.
   * 
   * @return {@link Boolean}
   */
  public Boolean getBooleanValue() {
    return this.booleanValue;
  }

  /**
   * Get Key.
   * 
   * @return {@link String}
   */
  public String getKey() {
    return this.key;
  }

  /**
   * Get Number Value.
   * 
   * @return {@link Double}
   */
  public Double getNumberValue() {
    return this.numberValue;
  }

  /**
   * Get Number Values.
   * 
   * @return {@link List} {@link Double}
   */
  public List<Double> getNumberValues() {
    return this.numberValues;
  }

  /**
   * Get String Value.
   * 
   * @return {@link String}
   */
  public String getStringValue() {
    return this.stringValue;
  }

  /**
   * Get String Values.
   * 
   * @return {@link List} {@link String}
   */
  public List<String> getStringValues() {
    return this.stringValues;
  }

  /**
   * Set Key.
   * 
   * @param attributeKey {@link String}
   * @return {@link DocumentAttribute}
   */
  public DocumentAttribute key(final String attributeKey) {
    this.key = attributeKey;
    return this;
  }

  /**
   * Set Number Value.
   * 
   * @param attributeValue {@link Double}
   * @return {@link DocumentAttribute}
   */
  public DocumentAttribute numberValue(final Double attributeValue) {
    this.numberValue = attributeValue;
    return this;
  }

  /**
   * Set Number Values.
   * 
   * @param attributeValues {@link List} {@link Double}
   * @return {@link DocumentAttribute}
   */
  public DocumentAttribute numberValues(final List<Double> attributeValues) {
    this.numberValues = attributeValues;
    return this;
  }

  /**
   * Set String Value.
   * 
   * @param attributeValue {@link String}
   * @return {@link DocumentAttribute}
   */
  public DocumentAttribute stringValue(final String attributeValue) {
    this.stringValue = attributeValue;
    return this;
  }

  /**
   * Set String Values.
   * 
   * @param attributeValues {@link List} {@link String}
   * @return {@link DocumentAttribute}
   */
  public DocumentAttribute stringValues(final List<String> attributeValues) {
    this.stringValues = attributeValues;
    return this;
  }
}
