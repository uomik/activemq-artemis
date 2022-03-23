/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.activemq.artemis.core.management.impl;

import org.apache.activemq.artemis.api.core.management.AddressControl;
import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.junit.Assert;
import org.junit.Test;

public class MBeanInfoHelperTest extends Assert {

   @Test
   public void testOperationsInfosCache() {
      MBeanInfoHelper.clearOperationsInfoCache();
      for (int i = 0; i < 10; i++) {
         MBeanInfoHelper.getMBeanOperationsInfo(QueueControl.class);
         MBeanInfoHelper.getMBeanOperationsInfo(AddressControl.class);
      }
      assertEquals(2, MBeanInfoHelper.getOperationsInfoCacheSize());
   }

   @Test
   public void testAttributesInfosCache() {
      MBeanInfoHelper.clearAttributesInfoCache();
      for (int i = 0; i < 10; i++) {
         MBeanInfoHelper.getMBeanAttributesInfo(QueueControl.class);
         MBeanInfoHelper.getMBeanAttributesInfo(AddressControl.class);
      }
      assertEquals(2, MBeanInfoHelper.getAttributesInfoCacheSize());
   }
}
