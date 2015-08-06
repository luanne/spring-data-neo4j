/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and licence terms.  Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's licence, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import junit.framework.AssertionFailedError;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.neo4j.graphdb.GraphDatabaseService;

/**
 * JUnit {@link TestRule} that runs a {@link GraphDatabaseService} behind a Spring Data REST API to provide an infrastructure
 * for testing integration of SDN with Spring Data REST.
 *
 * @author Adam George
 */
public class RestIntegrationTestRule implements TestRule {

    private static final int REST_SERVER_PORT = 9090;

    private final RestTestServer restServer;

    /**
     * Constructs a new {@link RestIntegrationTestRule} that initialises a test {@link RestTestServer} listening on TCP port 9090.
     */
    public RestIntegrationTestRule() {
        this.restServer = new RestTestServer(REST_SERVER_PORT);
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        this.restServer.start();

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    base.evaluate();
                } finally {
                    restServer.stop();
                }
            }
        };
    }

    public GraphDatabaseService getGraphDatabaseService() {
        return this.restServer.getNeo4jDatabase().getGraphDatabaseService();
    }

    /**
     * @return The URL of the REST-enabled HTTP server (without a trailing slash)
     */
    public String getRestBaseUrl() {
        return "http://localhost:" + REST_SERVER_PORT;
    }

    /**
     * Sends an HTTP GET request to the REST rest server with the given path, which should include a leading '/' character.
     * Alternatively you can pass an absolute URL to this method and it will be used as-is.
     *
     * @param urlPath The path to the resource to request on the server
     * @return The resultant {@link HttpResponse}
     */
    public HttpResponse sendGetRequest(String urlPath) {
        return sendRequest(new HttpGet(resolveAbsoluteUrl(urlPath)));
    }

    public HttpResponse sendPostRequest(byte[] data, String urlPath) {
        return sendRequestWithData(new HttpPost(resolveAbsoluteUrl(urlPath)), data);
    }

    public HttpResponse sendPatchRequest(byte[] data, String urlPath) {
        return sendRequestWithData(new HttpPatch(resolveAbsoluteUrl(urlPath)), data);
    }

    public HttpResponse sendPutRequest(byte[] data, String urlPath) {
        return sendRequestWithData(new HttpPut(resolveAbsoluteUrl(urlPath)), data);
    }

    public HttpResponse sendDeleteRequest(String urlPath) {
        return sendRequest(new HttpDelete(resolveAbsoluteUrl(urlPath)));
    }

    private String resolveAbsoluteUrl(String urlPath) {
        return urlPath.startsWith("http://") ? urlPath : getRestBaseUrl() + urlPath;
    }

    private static HttpResponse sendRequestWithData(HttpEntityEnclosingRequestBase request, byte[] data) {
        BasicHttpEntity httpEntity = new BasicHttpEntity();
        httpEntity.setContent(new ByteArrayInputStream(data));
        request.setEntity(httpEntity);

        return sendRequest(request);
    }

    private static HttpResponse sendRequest(HttpUriRequest request) {
        HttpClient client = HttpClientBuilder.create()
                .setDefaultHeaders(Arrays.asList(new BasicHeader("Content-Type", "application/hal+json"))).build();
        try {
            return client.execute(request);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            throw new AssertionFailedError(e.getMessage());
        }
    }

}
