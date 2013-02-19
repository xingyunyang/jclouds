/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.rackspace.cloudloadbalancers.domain;

import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;

import com.google.common.base.Objects;

/**
 * @author Everett Toews
 */
public final class LoadBalancerInfo {
   private int loadBalancerId;
   private String loadBalancerName;
   private Iterable<LoadBalancerUsage> loadBalancerUsageRecords;

   @ConstructorProperties({ "loadBalancerId", "loadBalancerName", "loadBalancerUsageRecords" })
   protected LoadBalancerInfo(int id, String name, Iterable<LoadBalancerUsage> loadBalancerUsageRecords) {
      this.loadBalancerId = id;
      this.loadBalancerName = checkNotNull(name, "name");
      this.loadBalancerUsageRecords = checkNotNull(loadBalancerUsageRecords, "loadBalancerUsageRecords");
   }

   public int getLoadBalancerId() {
      return loadBalancerId;
   }

   public String getLoadBalancerName() {
      return loadBalancerName;
   }

   public Iterable<LoadBalancerUsage> getLoadBalancerUsage() {
      return loadBalancerUsageRecords;
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(loadBalancerId);
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null || getClass() != obj.getClass())
         return false;
      LoadBalancerInfo that = LoadBalancerInfo.class.cast(obj);

      return Objects.equal(this.loadBalancerId, that.loadBalancerId);
   }

   @Override
   public String toString() {
      return Objects.toStringHelper(this).omitNullValues().add("loadBalancerId", loadBalancerId)
            .add("loadBalancerName", loadBalancerName).add("loadBalancerUsage", loadBalancerUsageRecords).toString();
   }
}