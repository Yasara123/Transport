/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.transport.http.netty.contentaware;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.ResourceLeakDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.messaging.exceptions.ServerConnectorException;
import org.wso2.carbon.transport.http.netty.config.TransportsConfiguration;
import org.wso2.carbon.transport.http.netty.config.YAMLTransportConfigurationBuilder;
import org.wso2.carbon.transport.http.netty.listener.HTTPServerConnector;
import org.wso2.carbon.transport.http.netty.passthrough.PassthroughMessageProcessor;
import org.wso2.carbon.transport.http.netty.util.TestUtil;
import org.wso2.carbon.transport.http.netty.util.server.HTTPServer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.testng.AssertJUnit.assertEquals;

/**
 * A test case for echo message from MessageProcessor level.
 */
public class ContentAwareMessageProcessorTestCase {
    private static final Logger log = LoggerFactory.getLogger(ContentAwareMessageProcessorTestCase.class);

    private List<HTTPServerConnector> serverConnectors;
    private CarbonMessageProcessor carbonMessageProcessor;
    private TransportsConfiguration configuration;

    private HTTPServer httpServer;
    private URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 8490));

    @BeforeClass
    public void setUp() {
        //for logging the leakage
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);
        configuration = YAMLTransportConfigurationBuilder
                .build("src/test/resources/simple-test-config/netty-transports.yml");
        serverConnectors = TestUtil.startConnectors(configuration, new PassthroughMessageProcessor());
        httpServer = TestUtil.startHTTPServer(TestUtil.TEST_SERVER_PORT);
    }

    @Test
    public void messageEchoingFromProcessorTestCase() {
        String testValue = "Test Message";
        try {
            HttpURLConnection urlConn = TestUtil.request(baseURI, "/", HttpMethod.POST.name(), true);
            TestUtil.writeContent(urlConn, testValue);
            assertEquals(200, urlConn.getResponseCode());
            String content = TestUtil.getContent(urlConn);
            assertEquals(testValue, content);
            urlConn.disconnect();
        } catch (IOException e) {
            TestUtil.handleException("IOException occurred while running messageEchoingFromProcessorTestCase", e);
        } finally {
            TestUtil.removeMessageProcessor(carbonMessageProcessor);
        }

    }

    @Test
    public void requestResponseTransformFromProcessorTestCase() {

        String requestValue = "XXXXXXXX";
        String responseValue = "YYYYYYY";
        String expectedValue = responseValue + ":" + requestValue;
        try {
            carbonMessageProcessor = new RequestResponseTransformProcessor(responseValue);
            TestUtil.updateMessageProcessor(carbonMessageProcessor, configuration);
            HttpURLConnection urlConn = TestUtil.request(baseURI, "/", HttpMethod.POST.name(), true);
            TestUtil.writeContent(urlConn, requestValue);
            assertEquals(200, urlConn.getResponseCode());
            String content = TestUtil.getContent(urlConn);
            assertEquals(expectedValue, content);
            urlConn.disconnect();
        } catch (IOException e) {
            TestUtil.handleException(
                    "IOException occurred while running requestResponseTransformFromProcessorTestCase", e);
        } finally {
            TestUtil.removeMessageProcessor(carbonMessageProcessor);
        }
    }

    @Test
    public void requestResponseCreationFromProcessorTestCase() {
        String requestValue = "XXXXXXXX";
        String responseValue = "YYYYYYY";
        String expectedValue = responseValue + ":" + requestValue;
        try {
            carbonMessageProcessor = new RequestResponseCreationProcessor(responseValue);
            TestUtil.updateMessageProcessor(carbonMessageProcessor, configuration);
            HttpURLConnection urlConn = TestUtil.request(baseURI, "/", HttpMethod.POST.name(), true);
            TestUtil.writeContent(urlConn, requestValue);
            assertEquals(200, urlConn.getResponseCode());
            String content = TestUtil.getContent(urlConn);
            assertEquals(expectedValue, content);
            urlConn.disconnect();
        } catch (IOException e) {
            TestUtil.handleException(
                    "IOException occurred while running requestResponseCreationFromProcessorTestCase", e);
        } finally {
            TestUtil.removeMessageProcessor(carbonMessageProcessor);
        }

    }

    @Test
    public void requestResponseStreamingFromProcessorTestCase() {
        String requestValue = "<A><B><C>Test Message</C></B></A>";
        try {
            carbonMessageProcessor = new RequestResponseCreationStreamingProcessor();
            TestUtil.updateMessageProcessor(carbonMessageProcessor, configuration);
            HttpURLConnection urlConn = TestUtil.request(baseURI, "/", HttpMethod.POST.name(), true);
            TestUtil.writeContent(urlConn, requestValue);
            assertEquals(200, urlConn.getResponseCode());
            String content = TestUtil.getContent(urlConn);
            assertEquals(requestValue, content);
            urlConn.disconnect();
        } catch (IOException e) {
            TestUtil.handleException(
                    "IOException occurred while running requestResponseStreamingFromProcessorTestCase", e);
        } finally {
            TestUtil.removeMessageProcessor(carbonMessageProcessor);
        }

    }

    @Test
    public void requestResponseTransformStreamingFromProcessorTestCase() {

        String requestValue = "<A><B><C>Test Message</C></B></A>";
        try {
            carbonMessageProcessor = new RequestResponseTransformStreamingProcessor();
            TestUtil.updateMessageProcessor(carbonMessageProcessor, configuration);
            HttpURLConnection urlConn = TestUtil.request(baseURI, "/", HttpMethod.POST.name(), true);
            TestUtil.writeContent(urlConn, requestValue);
            assertEquals(200, urlConn.getResponseCode());
            String content = TestUtil.getContent(urlConn);
            assertEquals(requestValue, content);
            urlConn.disconnect();
        } catch (IOException e) {
            TestUtil.handleException(
                    "IOException occurred while running requestResponseTransformStreamingFromProcessorTestCase", e);
        } finally {
            TestUtil.removeMessageProcessor(carbonMessageProcessor);
        }
    }

    @Test
    public void responseStreamingWithoutBufferingTestCase() {

        String requestValue = "<A><B><C>Test Message</C></B></A>";
        try {
            carbonMessageProcessor = new ResponseStreamingWithoutBufferingProcessor();
            TestUtil.updateMessageProcessor(carbonMessageProcessor, configuration);
            HttpURLConnection urlConn = TestUtil.request(baseURI, "/", HttpMethod.POST.name(), true);
            urlConn.setChunkedStreamingMode(-1); // Enable Chunking
            TestUtil.writeContent(urlConn, requestValue);
            assertEquals(200, urlConn.getResponseCode());
            String content = TestUtil.getContent(urlConn);
            assertEquals(requestValue, content);
            urlConn.disconnect();
        } catch (IOException e) {
            TestUtil.handleException("IOException occurred while running responseStreamingWithoutBufferingTestCase", e);
        } finally {
            TestUtil.removeMessageProcessor(carbonMessageProcessor);
        }
    }

    @Test
    public void responseStreamingWithoutBufferingTestCaseLoadTest1() throws InterruptedException {
        TestUtil.removeMessageProcessor(carbonMessageProcessor);
        carbonMessageProcessor = new ResponseStreamingWithoutBufferingProcessor();
        TestUtil.updateMessageProcessor(carbonMessageProcessor, configuration);
        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void responseStreamingWithoutBufferingTestCaseLoadTest2() {
        URI baseURI1 = URI.create(String.format("http://%s:%d", "localhost", 8005));
        URI baseURI2 = URI.create(String.format("http://%s:%d", "localhost", 9005));
        URI baseURI3 = URI.create(String.format("http://%s:%d", "localhost", 8009));
        URI baseURI4= URI.create(String.format("http://%s:%d", "localhost", 8055));
        ExecutorService executor = Executors.newFixedThreadPool(30);
        TestUtil.removeMessageProcessor(carbonMessageProcessor);
        carbonMessageProcessor = new ResponseStreamingWithoutBufferingProcessor();
        TestUtil.updateMessageProcessor(carbonMessageProcessor, configuration);
        for (int i=0;;i++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    String requestValue = "<A><B><C>Test Message</C></B></A>";
                    try {
                        HttpURLConnection urlConn = TestUtil.request(baseURI1, "/", HttpMethod.POST.name(), true);
                        urlConn.setChunkedStreamingMode(-1); // Enable Chunking
                        TestUtil.writeContent(urlConn, requestValue);
                        Thread.sleep(10);
                        log.info(String.valueOf(urlConn.getResponseCode()));
                        HttpURLConnection urlConn2 = TestUtil.request(baseURI2, "/", HttpMethod.POST.name(), true);
                        urlConn2.setChunkedStreamingMode(-1); // Enable Chunking
                        TestUtil.writeContent(urlConn2, requestValue);
                        Thread.sleep(10);
                        log.info(String.valueOf(urlConn2.getResponseCode()));
                        HttpURLConnection urlConn3 = TestUtil.request(baseURI3, "/", HttpMethod.POST.name(), true);
                        urlConn3.setChunkedStreamingMode(-1); // Enable Chunking
                        TestUtil.writeContent(urlConn3, requestValue);
                        Thread.sleep(10);
                        log.info(String.valueOf(urlConn3.getResponseCode()));
                        HttpURLConnection urlConn4 = TestUtil.request(baseURI4, "/", HttpMethod.POST.name(), true);
                        urlConn4.setChunkedStreamingMode(-1); // Enable Chunking
                        TestUtil.writeContent(urlConn4, requestValue);
                        Thread.sleep(10);
                        log.info(String.valueOf(urlConn4.getResponseCode()));
                    } catch (IOException e) {
                        TestUtil.handleException("IOException occurred while running responseStreamingWithoutBufferingTestCase", e);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }


                }
            });
        }
    }

    @AfterClass
    public void cleanUp() throws ServerConnectorException {
        TestUtil.cleanUp(serverConnectors, httpServer);
    }
}
