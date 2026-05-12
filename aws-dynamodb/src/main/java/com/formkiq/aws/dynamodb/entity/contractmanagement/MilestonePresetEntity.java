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
package com.formkiq.aws.dynamodb.entity.contractmanagement;

import com.formkiq.aws.dynamodb.entity.PresetEntity;

import java.util.List;

/**
 * PresetEntity for Contract Management Milestone.
 */
public class MilestonePresetEntity implements PresetEntity {

  /** Entity Name. */
  public static final String ENTITY_NAME = "Milestone";
  /** Milestone Date attribute. */
  public static final String MILESTONE_DATE = "MilestoneDate";
  /** Milestone Description attribute. */
  public static final String MILESTONE_DESCRIPTION = "MilestoneDescription";
  /** Milestone Fee Percentage Due attribute. */
  public static final String MILESTONE_FEE_PERCENTAGE_DUE = "MilestoneFeePercentageDue";

  @Override
  public List<String> getAttributeKeys() {
    return List.of(MILESTONE_DATE, MILESTONE_DESCRIPTION, MILESTONE_FEE_PERCENTAGE_DUE);
  }

  @Override
  public String getName() {
    return ENTITY_NAME;
  }
}
