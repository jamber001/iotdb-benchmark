/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package cn.edu.tsinghua.iotdb.benchmark.influxdb;

import org.influxdb.dto.Point;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class InfluxDataModel implements Serializable {
  private static final long serialVersionUID = 1L;
  private String measurement;
  private HashMap<String, String> tagSet;
  private HashMap<String, Object> fields;
  private long timestamp;
  private String timestampPrecision;
  private long toNanoConst = 1L;

  public InfluxDataModel() {
    this.tagSet = new HashMap<>();
    this.fields = new HashMap<>();
  }

  public void setMeasurement(String measurement) {
    this.measurement = measurement;
  }

  public void setTagSet(HashMap<String, String> tagSet) {
    this.tagSet = tagSet;
  }

  public void setFields(HashMap<String, Object> fields) {
    for (Map.Entry<String, Object> entry : fields.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof Float) {
        // If float type passes through the lineProtocol, there will be a problem with the accuracy
        value = Double.valueOf(value.toString());
      }
      this.fields.put(entry.getKey(), value);
    }
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public void setTimestampPrecision(String timestampPrecision) {
    this.timestampPrecision = timestampPrecision;
    if (timestampPrecision.equals("ns")) {
      this.toNanoConst = 1L;
    } else if (timestampPrecision.equals("us")) {
      this.toNanoConst = 1000L;
    } else if (timestampPrecision.equals("ms")) {
      this.toNanoConst = 1000000L;
    }
  }

  @Override
  /** e.g. cpu,HOST=server03,region=useast load=15.4 1434055562000000000 */
  public String toString() {
    StringBuilder builder = new StringBuilder(measurement);
    // attach tags
    if (tagSet.size() > 0) {
      // sort by key to make the sequence unique
      ArrayList<Map.Entry<String, String>> entries = new ArrayList<>();
      entries.addAll(tagSet.entrySet());
      entries.sort(
          new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
              return o1.getKey().compareTo(o2.getKey());
            }
          });
      for (Map.Entry<String, String> entry : entries) {
        builder.append(",");
        builder.append(entry.getKey());
        builder.append("=");
        builder.append(entry.getValue());
      }
    }
    // attach fields
    builder.append(" ");
    ArrayList<Map.Entry<String, Object>> entries = new ArrayList<>();
    entries.addAll(fields.entrySet());
    entries.sort(
        new Comparator<Map.Entry<String, Object>>() {
          @Override
          public int compare(Map.Entry<String, Object> o1, Map.Entry<String, Object> o2) {
            return o1.getKey().compareTo(o2.getKey());
          }
        });
    boolean isFirstField = true;
    for (Map.Entry<String, Object> entry : entries) {
      if (isFirstField) {
        isFirstField = false;
      } else {
        builder.append(",");
      }
      builder.append(entry.getKey());
      builder.append("=");
      builder.append(entry.getValue().toString());
    }
    // attach timestamp
    builder.append(" ");
    builder.append(this.timestamp * this.toNanoConst);
    return builder.toString();
  }

  public Point toInfluxPoint() {
    HashMap<String, Object> fields = new HashMap<>();
    fields.putAll(this.fields);
    if (this.timestampPrecision.equals("ns")) {
      Point point =
          Point.measurement(this.measurement)
              .time(this.timestamp, TimeUnit.NANOSECONDS)
              .tag(this.tagSet)
              .fields(fields)
              .build();
      return point;
    } else if (this.timestampPrecision.equals("us")) {
      Point point =
          Point.measurement(this.measurement)
              .time(this.timestamp, TimeUnit.MICROSECONDS)
              .tag(this.tagSet)
              .fields(fields)
              .build();
      return point;
    } else {
      Point point =
          Point.measurement(this.measurement)
              .time(this.timestamp, TimeUnit.MILLISECONDS)
              .tag(this.tagSet)
              .fields(fields)
              .build();
      return point;
    }
  }
}
