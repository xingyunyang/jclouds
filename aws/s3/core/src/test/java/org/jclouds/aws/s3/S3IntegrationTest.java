/**
 *
 * Copyright (C) 2009 Global Cloud Specialists, Inc. <info@globalcloudspecialists.com>
 *
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
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
 * ====================================================================
 */
package org.jclouds.aws.s3;

import static org.jclouds.aws.s3.commands.options.PutBucketOptions.Builder.createIn;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jclouds.aws.s3.config.StubS3ConnectionModule;
import org.jclouds.aws.s3.domain.S3Bucket;
import org.jclouds.aws.s3.domain.S3Object;
import org.jclouds.aws.s3.domain.S3Bucket.Metadata;
import org.jclouds.aws.s3.domain.S3Bucket.Metadata.LocationConstraint;
import org.jclouds.aws.s3.reference.S3Constants;
import org.jclouds.aws.s3.util.S3Utils;
import org.jclouds.http.config.JavaUrlHttpFutureCommandClientModule;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.util.Utils;
import org.testng.ITestContext;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.inject.Module;

public class S3IntegrationTest {
   protected static final String TEST_STRING = "<apples><apple name=\"fuji\"></apple> </apples>";
   public static long INCONSISTENCY_WINDOW = 1000;
   protected static final String sysAWSAccessKeyId = System
            .getProperty(S3Constants.PROPERTY_AWS_ACCESSKEYID);
   protected static final String sysAWSSecretAccessKey = System
            .getProperty(S3Constants.PROPERTY_AWS_SECRETACCESSKEY);
   protected static int bucketCount = 20;
   protected static volatile int bucketIndex = 0;

   protected byte[] goodMd5;
   protected byte[] badMd5;
   protected S3Connection client;
   protected S3Context context = null;
   protected boolean SANITY_CHECK_RETURNED_BUCKET_NAME = false;
   private String bucketPrefix = System.getProperty("user.name") + ".s3int";

   /**
    * two test groups integration and live.
    */
   private static final BlockingQueue<String> bucketNames = new ArrayBlockingQueue<String>(
            bucketCount);

   /**
    * Due to eventual consistency, bucket commands may not return correctly immediately. Hence, we
    * will try up to the inconsistency window to see if the assertion completes.
    */
   protected void assertEventually(Runnable assertion) throws InterruptedException {
      AssertionError error = null;
      for (int i = 0; i < 5; i++) {
         try {
            assertion.run();
            return;
         } catch (AssertionError e) {
            error = e;
         }
         Thread.sleep(INCONSISTENCY_WINDOW / 5);
      }
      if (error != null)
         throw error;
   }

   protected void createBucketAndEnsureEmpty(String bucketName) throws InterruptedException,
            ExecutionException, TimeoutException {
      client.putBucketIfNotExists(bucketName).get(10, TimeUnit.SECONDS);
      emptyBucket(bucketName);
   }

   protected void addObjectToBucket(String sourceBucket, String key) throws InterruptedException,
            ExecutionException, TimeoutException, IOException {
      S3Object sourceObject = new S3Object(key);
      sourceObject.getMetadata().setContentType("text/xml");
      sourceObject.setData(TEST_STRING);
      addObjectToBucket(sourceBucket, sourceObject);
   }

   protected void addObjectToBucket(String sourceBucket, S3Object object)
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
      client.putObject(sourceBucket, object).get(10, TimeUnit.SECONDS);
   }

   protected S3Object validateContent(String sourceBucket, String key) throws InterruptedException,
            ExecutionException, TimeoutException, IOException {
      assertEventuallyBucketSize(sourceBucket, 1);
      S3Object newObject = client.getObject(sourceBucket, key).get(10, TimeUnit.SECONDS);
      assert newObject != S3Object.NOT_FOUND;
      assertEquals(S3Utils.getContentAsStringAndClose(newObject), TEST_STRING);
      return newObject;
   }

   protected void assertEventuallyBucketSize(final String bucketName, final int count)
            throws InterruptedException {
      assertEventually(new Runnable() {
         public void run() {
            try {
               assertEquals(client.listBucket(bucketName).get(10, TimeUnit.SECONDS).getContents()
                        .size(), count);
            } catch (Exception e) {
               Utils.<RuntimeException> rethrowIfRuntimeOrSameType(e);
            }
         }
      });
   }

   @BeforeGroups(groups = { "integration", "live" })
   @Parameters( { S3Constants.PROPERTY_AWS_ACCESSKEYID, S3Constants.PROPERTY_AWS_SECRETACCESSKEY })
   protected void setUpCredentials(@Optional String AWSAccessKeyId,
            @Optional String AWSSecretAccessKey, ITestContext testContext) throws Exception {
      AWSAccessKeyId = AWSAccessKeyId != null ? AWSAccessKeyId : sysAWSAccessKeyId;
      AWSSecretAccessKey = AWSSecretAccessKey != null ? AWSSecretAccessKey : sysAWSSecretAccessKey;
      if (AWSAccessKeyId != null)
         testContext.setAttribute(S3Constants.PROPERTY_AWS_ACCESSKEYID, AWSAccessKeyId);
      if (AWSSecretAccessKey != null)
         testContext.setAttribute(S3Constants.PROPERTY_AWS_SECRETACCESSKEY, AWSSecretAccessKey);
   }

   @BeforeGroups(dependsOnMethods = { "setUpCredentials" }, groups = { "integration", "live" })
   protected void setUpClient(ITestContext testContext) throws Exception {
      if (testContext.getAttribute(S3Constants.PROPERTY_AWS_ACCESSKEYID) != null) {
         String AWSAccessKeyId = (String) testContext
                  .getAttribute(S3Constants.PROPERTY_AWS_ACCESSKEYID);
         String AWSSecretAccessKey = (String) testContext
                  .getAttribute(S3Constants.PROPERTY_AWS_SECRETACCESSKEY);
         createLiveS3Context(AWSAccessKeyId, AWSSecretAccessKey);
      } else {
         createStubS3Context();
      }
      client = context.getConnection();
      assert client != null;
      goodMd5 = S3Utils.md5(TEST_STRING);
      badMd5 = S3Utils.md5("alf");
   }

   protected void createStubS3Context() {
      context = S3ContextFactory.createContext("stub", "stub").withHttpAddress("stub").withModule(
               new StubS3ConnectionModule()).build();
      SANITY_CHECK_RETURNED_BUCKET_NAME = true;
   }

   protected void createLiveS3Context(String AWSAccessKeyId, String AWSSecretAccessKey) {
      context = buildS3ContextFactory(AWSAccessKeyId, AWSSecretAccessKey).withModule(
               createHttpModule()).withModule(new Log4JLoggingModule()).build();
   }

   public String getBucketName() throws InterruptedException, ExecutionException, TimeoutException {
      String bucketName = bucketNames.poll(30, TimeUnit.SECONDS);
      assert bucketName != null : "unable to get a bucket for the test";
      emptyBucket(bucketName);
      return bucketName;
   }

   /**
    * a bucket that should be deleted and recreated after the test is complete. This is due to
    * having an ACL or otherwise that makes it not compatible with normal buckets
    */
   public String getScratchBucketName() throws InterruptedException, ExecutionException,
            TimeoutException {
      return getBucketName();
   }

   public void returnBucket(final String bucketName) throws InterruptedException,
            ExecutionException, TimeoutException {
      if (bucketName != null) {
         bucketNames.add(bucketName);
         /*
          * Ensure that any returned bucket name actually exists on the server. Return of a
          * non-existent bucket introduces subtle testing bugs, where later unrelated tests will
          * fail.
          * 
          * NOTE: This sanity check should only be run for Stub-based Integration testing -- it will
          * *substantially* slow down tests on a real server over a network.
          */
         if (SANITY_CHECK_RETURNED_BUCKET_NAME) {
            if (!Iterables.any(client.listOwnedBuckets().get(), new Predicate<Metadata>() {
               public boolean apply(Metadata md) {
                  return bucketName.equals(md.getName());
               }
            })) {
               throw new IllegalStateException("Test returned the name of a non-existent bucket: "
                        + bucketName);
            }
         }
      }
   }

   /**
    * abandon old bucket name instead of waiting for the bucket to be created.
    */
   public void returnScratchBucket(String scratchBucket) throws InterruptedException,
            ExecutionException, TimeoutException {
      if (scratchBucket != null) {
         deleteBucket(scratchBucket);
         String newScratchBucket = bucketPrefix + (++bucketIndex);
         createBucketAndEnsureEmpty(newScratchBucket);
         Thread.sleep(INCONSISTENCY_WINDOW);
         returnBucket(newScratchBucket);
      }
   }

   /**
    * There are a lot of retries here mainly from experience running inside amazon EC2.
    */
   @BeforeGroups(dependsOnMethods = { "setUpClient" }, groups = { "integration", "live" })
   public void setUpBuckets(ITestContext context) throws Exception {
      synchronized (bucketNames) {
         if (bucketNames.peek() == null) {
            deleteEverything();
            for (; bucketIndex < bucketCount; bucketIndex++) {
               String bucketName = bucketPrefix + bucketIndex;
               try {
                  createBucketAndEnsureEmpty(bucketName);
                  bucketNames.put(bucketName);
               } catch (AssertionError e) {
                  // throw away the bucket and try again with the next index
                  deleteBucket(bucketName);
                  bucketCount++;
               }
            }
         }
      }
   }

   protected S3ContextFactory buildS3ContextFactory(String AWSAccessKeyId, String AWSSecretAccessKey) {
      return S3ContextFactory.createContext(AWSAccessKeyId, AWSSecretAccessKey).withSaxDebug()
               .withHttpSecure(false).withHttpPort(80);
   }

   protected Module createHttpModule() {
      return new JavaUrlHttpFutureCommandClientModule();
   }

   protected void deleteEverything() throws Exception {
      try {
         List<S3Bucket.Metadata> metadata = client.listOwnedBuckets().get(10, TimeUnit.SECONDS);
         for (S3Bucket.Metadata metaDatum : metadata) {
            if (metaDatum.getName().startsWith(bucketPrefix.toLowerCase())) {
               deleteBucket(metaDatum.getName());
            }
         }
      } catch (CancellationException e) {
         throw e;
      }
   }

   /**
    * Remove any objects in a bucket, leaving it empty.
    */
   protected void emptyBucket(final String name) throws InterruptedException, ExecutionException,
            TimeoutException {
      if (client.bucketExists(name).get(10, TimeUnit.SECONDS)) {
         // This can fail to be zero length because of stale bucket lists. Ex. client.listBucket()
         // could return 9 keys, when there are 10. When all the deletions finish, one entry would
         // be left in this case. Instead of failing, we will attempt this entire bucket deletion
         // operation multiple times to ensure we can acheive a zero length bucket.
         assertEventually(new Runnable() {
            public void run() {
               try {
                  Map<String, InputStream> map = context.createInputStreamMap(name);
                  Set<String> keys = map.keySet();
                  if (keys.size() > 0) {
                     map.clear();
                     assertEquals(map.size(), 0, String.format(
                              "deleting %s, we still have %s left", keys, map.keySet()));
                  }
               } catch (Exception e) {
                  Utils.<RuntimeException> rethrowIfRuntimeOrSameType(e);
               }
            }
         });

      }
   }

   protected String createScratchBucketInEU() throws InterruptedException, ExecutionException,
            TimeoutException {
      String bucketName = getScratchBucketName();
      deleteBucket(bucketName);
      client.putBucketIfNotExists(bucketName, createIn(LocationConstraint.EU)).get(10,
               TimeUnit.SECONDS);
      Thread.sleep(INCONSISTENCY_WINDOW);
      return bucketName;
   }

   /**
    * Empty and delete a bucket.
    * 
    * @param name
    * @throws InterruptedException
    * @throws ExecutionException
    * @throws TimeoutException
    */
   protected void deleteBucket(String name) throws InterruptedException, ExecutionException,
            TimeoutException {
      if (client.bucketExists(name).get(10, TimeUnit.SECONDS)) {
         emptyBucket(name);
         client.deleteBucketIfEmpty(name).get(10, TimeUnit.SECONDS);
      }
   }

   @AfterGroups(groups = { "integration", "live" })
   protected void tearDownClient() throws Exception {
      context.close();
      context = null;
   }

}