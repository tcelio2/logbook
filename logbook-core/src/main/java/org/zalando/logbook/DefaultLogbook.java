package org.zalando.logbook;

/*
 * #%L
 * Logbook: Core
 * %%
 * Copyright (C) 2015 Zalando SE
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

final class DefaultLogbook implements Logbook {

    private final HttpLogFormatter formatter;
    private final HttpLogWriter writer;
    private final Obfuscation obfuscation;

    DefaultLogbook(final HttpLogFormatter formatter, final HttpLogWriter writer, final Obfuscation obfuscation) {
        this.formatter = formatter;
        this.writer = writer;
        this.obfuscation = obfuscation;
    }

    @Override
    public Optional<Correlator> write(final RawHttpRequest rawHttpRequest) throws IOException {
        if (writer.isActive(rawHttpRequest)) {
            final String correlationId = UUID.randomUUID().toString(); // TODO should this be a dependency?
            final HttpRequest request = obfuscation.obfuscate(rawHttpRequest.withBody());

            writer.writeRequest(formatter.format(new SimplePrecorrelation(correlationId, request)));

            return Optional.of(rawHttpResponse -> {
                final HttpResponse response = obfuscation.obfuscate(rawHttpResponse.withBody());
                final String message = formatter.format(new SimpleCorrelation(correlationId, request, response));
                writer.writeResponse(message);
            });
        } else {
            return Optional.empty();
        }
    }

    @VisibleForTesting
    static class SimplePrecorrelation implements Precorrelation {

        private final String id;
        private final HttpRequest request;

        public SimplePrecorrelation(final String id, final HttpRequest request) {
            this.id = id;
            this.request = request;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public HttpRequest getRequest() {
            return request;
        }

    }

    @VisibleForTesting
    static class SimpleCorrelation implements Correlation {

        private final String id;
        private final HttpRequest request;
        private final HttpResponse response;

        public SimpleCorrelation(final String id, final HttpRequest request, final HttpResponse response) {
            this.id = id;
            this.request = request;
            this.response = response;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public HttpRequest getRequest() {
            return request;
        }

        @Override
        public HttpResponse getResponse() {
            return response;
        }

    }

}