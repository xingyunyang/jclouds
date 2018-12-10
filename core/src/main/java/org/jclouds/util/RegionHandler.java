/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Copyright(C) Inspur Corp. 2017,2018
 * 
 * COMPONENT NAME: indigo_MCSTORE
 * FILE NAME: core/src/main/java/org/jclouds/util/RegionHandler.java
 * DESCRIPTION: add this class for handling the region information
 * when the csp (cloud service provider) is aliyunoss.
 * 
 * Change Log.
 * 
 * 14-06-2017 zreal,Inspur File creation.
 * 
 */
package org.jclouds.util;


public class RegionHandler {

	/* identify the CSP_REGION during handle the request */
	public static String CSP_REGION = "";
	
	/* zreal the length of the oss accesskeyid */
	public static final int OSS_ACCESS_KEY_ID_LENGTH = 16;
	
	/* zreal the length of the oss secretaccesskey */
	public static final int OSS_SECRET_ACCESS_KEY_LENGTH = 30;
	
}
