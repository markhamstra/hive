/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hive.ql.udf.generic;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Random;

/**
 * This is copied from TestNumericHistogram, but it's frankly inadequate.  There was no attempt
 * to use any of the code that's actually implemented in NumericHistogram.  But that's okay,
 * because the tests were not being run anyway.  So I implemented testHistogramSimilarity() to
 * verify *closeness* to NumericHistogram.
 */
public class TestFastNumericHistogram extends TestCase {

  private void assertApproxEquals(double expected, double actual) {
    assertTrue(
        "Values are too far: expected=" + expected + ", actual=" + actual,
        Math.abs(expected - actual) < 1e-7);
  }

  public void testInterpolation() {
    FastNumericHistogram h = new FastNumericHistogram();
    h.allocate(100);
    h.add(10.0);
    h.add(10.0);
    h.add(20.0);
    h.add(20.0);
    h.add(20.0);
    h.add(40.0);
    h.add(40.0);
    h.add(40.0);
    h.add(50.0);
    h.add(50.0);
    assertApproxEquals(10.0, h.quantile(1 / 18.0));
    assertApproxEquals(10.0, h.quantile(2 / 18.0));
    assertApproxEquals(15.0, h.quantile(3 / 18.0));
    assertApproxEquals(20.0, h.quantile(4 / 18.0));
    assertApproxEquals(20.0, h.quantile(5 / 18.0));
    assertApproxEquals(20.0, h.quantile(6 / 18.0));
    assertApproxEquals(20.0, h.quantile(7 / 18.0));
    assertApproxEquals(20.0, h.quantile(8 / 18.0));
    assertApproxEquals(30.0, h.quantile(9 / 18.0));
    assertApproxEquals(40.0, h.quantile(10 / 18.0));
    assertApproxEquals(40.0, h.quantile(11 / 18.0));
    assertApproxEquals(40.0, h.quantile(12 / 18.0));
    assertApproxEquals(40.0, h.quantile(13 / 18.0));
    assertApproxEquals(40.0, h.quantile(14 / 18.0));
    assertApproxEquals(45.0, h.quantile(15 / 18.0));
    assertApproxEquals(50.0, h.quantile(16 / 18.0));
    assertApproxEquals(50.0, h.quantile(17 / 18.0));
  }

  public void testHistogramSimilarity() {
    int numToCheck = 50;
    int MAX_SIZE_TO_VERIFY = 100000000;
    Random rand = new Random(System.nanoTime());
    boolean passed = true;
    while (passed && numToCheck < MAX_SIZE_TO_VERIFY) {
      numToCheck = numToCheck * 2;
      NumericHistogram nh = new NumericHistogram();
      nh.allocate(100);
      FastNumericHistogram fnh = new FastNumericHistogram();
      fnh.allocate(100);
      double values[] = new double[numToCheck];
      for (int i = 0; i < numToCheck; i++) {
        values[i] = rand.nextDouble();
      }
      long startNH = System.nanoTime();
      for (int i = 0; i < numToCheck; i++) {
        nh.add(values[i]);
      }
      long totalNH = System.nanoTime() - startNH;
      long startFNH = System.nanoTime();
      for (int i = 0; i < numToCheck; i++) {
        fnh.add(values[i]);
      }
      long totalFNH = System.nanoTime() - startFNH;
      passed = true;
      Arrays.sort(values);
      System.out.println(
        "Checking " + numToCheck + " Fast is faster by: " + totalNH / (double)totalFNH);

      passed = verify(values, nh, fnh, .5) && passed;
      passed = verify(values, nh, fnh, .25) && passed;
      passed = verify(values, nh, fnh, .75) && passed;
      passed = verify(values, nh, fnh, .98) && passed;
      passed = verify(values, nh, fnh, .99) && passed;
    }
    assertTrue(passed);
  }

  public boolean verify(double[] values, NumericHistogram nh, FastNumericHistogram fnh, double quantile) {
    double expected = values[(int)(values.length * quantile)];
    double actual1 = nh.quantile(quantile);
    double actual2 = fnh.quantile(quantile);

    // There are 100 buckets in the histogram, so we should be within 1%.
    if (Math.abs(actual1 - actual2) > 1e-2) {
      System.out.println("For " + quantile + " Fast and Numeric were too far apart: " +
        expected + " vs " + actual1 + " vs " + actual2);
      return false;
    }
    return true;
  }

}
