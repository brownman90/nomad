/*
 * ====================================================================
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package com.nevilon.nomad;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * An example that performs GETs from multiple threads.
 *
 */
public class ClientMultiThreadedExecution {

    public static void main(String[] args) throws Exception {
        // Create an HttpClient with the ThreadSafeClientConnManager.
        // This connection manager must be used if more than one thread will
        // be using the HttpClient.
        PoolingClientConnectionManager cm = new PoolingClientConnectionManager();
        cm.setMaxTotal(100);
        cm.setDefaultMaxPerRoute(30);

        long start = System.currentTimeMillis();
        DefaultHttpClient httpclient = new DefaultHttpClient(cm);
        httpclient.setKeepAliveStrategy(new ConnectionKeepAliveStrategy() {

            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                // Honor 'keep-alive' header
                HeaderElementIterator it = new BasicHeaderElementIterator(
                        response.headerIterator(HTTP.CONN_KEEP_ALIVE));
                while (it.hasNext()) {
                    HeaderElement he = it.nextElement();
                    String param = he.getName();
                    String value = he.getValue();
                    if (value != null && param.equalsIgnoreCase("timeout")) {
                        try {
                            return Long.parseLong(value) * 1000;
                        } catch(NumberFormatException ignore) {
                        }
                    }
                }
                HttpHost target = (HttpHost) context.getAttribute(
                        ExecutionContext.HTTP_TARGET_HOST);
                if ("www.naughty-server.com".equalsIgnoreCase(target.getHostName())) {
                    // Keep alive for 5 seconds only
                    return 5 * 1000;
                } else {
                    // otherwise keep alive for 30 seconds
                    return 30 * 1000;
                }
            }

        });




        try {
            // create an array of URIs to perform GETs on
            String[] urisToGet = {
                "http://hc.apache.org/",
                "http://hc.apache.org/httpcomponents-core-ga/",
                "http://hc.apache.org/httpcomponents-client-ga/",
                "http://svn.apache.org/viewvc/httpcomponents/"
            };

            List<String> items = new ArrayList<String>();
            for(String item:urisToGet){
                for(int i=0;i<100;i++){
                     items.add(item);
                }
            }
            urisToGet = items.toArray(new String[]{});

            // create a thread for each URI
            GetThread[] threads = new GetThread[urisToGet.length];
            for (int i = 0; i < threads.length; i++) {
                HttpGet httpget = new HttpGet(urisToGet[i]);
                threads[i] = new GetThread(httpclient, httpget, i + 1);
            }

            // start the threads
            for (int j = 0; j < threads.length; j++) {
                threads[j].start();
            }

            // join the threads
            for (int j = 0; j < threads.length; j++) {
                threads[j].join();
            }

        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            System.out.println(System.currentTimeMillis()-start );
            httpclient.getConnectionManager().shutdown();
        }
    }

    /**
     * A thread that performs a GET.
     */
    static class GetThread extends Thread {

        private final HttpClient httpClient;
        private final HttpContext context;
        private final HttpGet httpget;
        private final int id;

        public GetThread(HttpClient httpClient, HttpGet httpget, int id) {
            this.httpClient = httpClient;
            this.context = new BasicHttpContext();
            this.httpget = httpget;
            this.id = id;
        }

        /**
         * Executes the GetMethod and prints some status information.
         */
        @Override
        public void run() {

            System.out.println(id + " - about to get something from " + httpget.getURI());

            try {

                // execute the method
                HttpResponse response = httpClient.execute(httpget, context);

                System.out.println(id + " - get executed");
                // get the response body as an array of bytes
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    byte[] bytes = EntityUtils.toByteArray(entity);
                    System.out.println(id + " - " + bytes.length + " bytes read");
                }

                EntityUtils.consume(entity);

            } catch (Exception e) {
                httpget.abort();
                System.out.println(id + " - error: " + e);
            }
        }

    }

}
