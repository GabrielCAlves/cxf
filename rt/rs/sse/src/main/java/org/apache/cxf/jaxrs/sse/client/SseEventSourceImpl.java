/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.jaxrs.sse.client;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;

import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.InboundSseEvent;
import jakarta.ws.rs.sse.SseEventSource;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.impl.RetryAfterHeaderProvider;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;

/**
 * SSE Event Source implementation 
 */
public class SseEventSourceImpl implements SseEventSource {
    // Whether or not incomplete SSE events should be discarded (by default, they will be discarded)
    public static final String DISCARD_INCOMPLETE_EVENTS = "org.apache.cxf.sse.discard.incomplete.events";

    private static final Logger LOG = LogUtils.getL7dLogger(SseEventSourceImpl.class);
    
    private final WebTarget target;
    private final Collection<InboundSseEventListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicReference<SseSourceState> state = new AtomicReference<>(SseSourceState.CLOSED);
    
    // It may happen that open() and close() could be called on separate threads
    private volatile ScheduledExecutorService executor;
    private volatile boolean managedExecutor = true;
    private volatile InboundSseEventProcessor processor; 
    private volatile TimeUnit unit;
    private volatile long delay;
    private volatile boolean open;

    private class InboundSseEventListenerDelegate implements InboundSseEventListener {
        private String lastEventId;
        
        InboundSseEventListenerDelegate(String lastEventId) {
            this.lastEventId = lastEventId;
        }
        
        @Override
        public void onNext(InboundSseEvent event) {
            lastEventId = event.getId();
            listeners.forEach(listener -> listener.onNext(event));
            
            // Reconnect delay is set in milliseconds
            if (event.isReconnectDelaySet()) {
                unit = TimeUnit.MILLISECONDS;
                delay = event.getReconnectDelay();
            }
        }

        @Override
        public void onError(Throwable ex) {
            listeners.forEach(listener -> listener.onError(ex));
            if (delay >= 0 && unit != null) {
                scheduleReconnect(delay, unit, lastEventId);
            }
        }

        @Override
        public void onComplete() {
            listeners.forEach(InboundSseEventListener::onComplete);
            if (delay >= 0 && unit != null) {
                scheduleReconnect(delay, unit, lastEventId);
            }
        }
    }
    
    private class InboundSseEventListenerImpl implements InboundSseEventListener {
        private final Consumer<InboundSseEvent> onEvent;
        private final Consumer<Throwable> onError;
        private final Runnable onComplete;
        
        InboundSseEventListenerImpl(Consumer<InboundSseEvent> e) {
            this(e, ex -> { }, () -> { });
        }
        
        InboundSseEventListenerImpl(Consumer<InboundSseEvent> e, Consumer<Throwable> t) {
            this(e, t, () -> { });    
        }

        InboundSseEventListenerImpl(Consumer<InboundSseEvent> e, Consumer<Throwable> t, Runnable c) {
            this.onEvent = e;
            this.onError = t;
            this.onComplete = c;
        }

        @Override
        public void onNext(InboundSseEvent event) {
            onEvent.accept(event);
        }

        @Override
        public void onError(Throwable ex) {
            onError.accept(ex);
        }

        @Override
        public void onComplete() {
            onComplete.run();
        }
    }
    
    /**
     * https://www.w3.org/TR/2012/WD-eventsource-20120426/#dom-eventsource-connecting
     */
    private enum SseSourceState {
        CONNECTING,
        OPEN,
        CLOSED
    }
    
    SseEventSourceImpl(WebTarget target, long delay, TimeUnit unit) {
        this.target = target;
        this.delay = delay;
        this.unit = unit;
    }

    @Override
    public void register(Consumer<InboundSseEvent> onEvent) {
        listeners.add(new InboundSseEventListenerImpl(onEvent));
    }

    @Override
    public void register(Consumer<InboundSseEvent> onEvent, Consumer<Throwable> onError) {
        listeners.add(new InboundSseEventListenerImpl(onEvent, onError));
    }

    @Override
    public void register(Consumer<InboundSseEvent> onEvent, Consumer<Throwable> onError, Runnable onComplete) {
        listeners.add(new InboundSseEventListenerImpl(onEvent, onError, onComplete));
    }

    @Override
    public void open() {
        if (!tryChangeState(SseSourceState.CLOSED, SseSourceState.CONNECTING)) {
            throw new IllegalStateException("The SseEventSource is already in " + state.get() + " state");
        }

        // Create the executor for scheduling the reconnect tasks 
        final Configuration configuration = target.getConfiguration();
        if (executor == null) {
            executor = (ScheduledExecutorService)configuration
                .getProperty("scheduledExecutorService");
            
            if (executor == null) {
                executor = Executors.newSingleThreadScheduledExecutor();
                managedExecutor = false; /* we manage lifecycle */
            }
        }
        
        final Object lastEventId = configuration.getProperty(HttpHeaders.LAST_EVENT_ID_HEADER);
        connect(lastEventId != null ? lastEventId.toString() : null);
    }

    private void connect(String lastEventId) {
        final InboundSseEventListenerDelegate delegate = new InboundSseEventListenerDelegate(lastEventId);
        Response response = null;
        
        try {
            Invocation.Builder builder = target.request(MediaType.SERVER_SENT_EVENTS);
            if (lastEventId != null) {
                builder.header(HttpHeaders.LAST_EVENT_ID_HEADER, lastEventId);
            }
            response = builder.get();

            // A client can be told to stop reconnecting using the HTTP 204 No Content 
            // response code. In this case, we should give up.
            final int status = response.getStatus();
            if (status == 204) {
                LOG.fine("SSE endpoint " + target.getUri() + " returns no data, disconnecting");
                changeState(SseSourceState.CLOSED);
                response.close();
                return;
            }
            
            // In addition to handling the standard connection loss failures, JAX-RS SseEventSource automatically 
            // deals with any HTTP 503 Service Unavailable responses from an SSE endpoint, that contain a "Retry-After" 
            // HTTP header with a valid value. The HTTP 503 + "Retry-After" technique is often used by HTTP endpoints 
            // as a means of connection and traffic throttling. In case a HTTP 503 + "Retry-After" response is received 
            // in return to a connection request, JAX-RS SSE event source will automatically schedule a new reconnect 
            // attempt and use the received "Retry-After" HTTP header value as a one-time override of the reconnect 
            // delay.
            if (status == 503) {
                final Object retryAfterHeader = response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER);
                if (retryAfterHeader != null) {
                    final Duration retryAfter = RetryAfterHeaderProvider.valueOf(retryAfterHeader.toString()); 
                    final long retryAfterMillis = retryAfter.toMillis();
                    if (retryAfterMillis >= 0) {
                        LOG.fine("SSE endpoint " + target.getUri() + " is unavailable, retrying after " 
                            + retryAfterMillis + "ms");
                        scheduleReconnect(retryAfterMillis, TimeUnit.MILLISECONDS, lastEventId);
                        response.close();
                        return;
                    }
                }
            }

            // Convert unsuccessful responses to instances of WebApplicationException
            if (status != 304 && status >= 300) {
                LOG.fine("SSE connection to " + target.getUri() + " returns " + status);
                throw ExceptionUtils.toWebApplicationException(response);
            }

            // Should not happen but if close() was called from another thread, we could
            // end up there.
            if (state.get() == SseSourceState.CLOSED) {
                LOG.fine("SSE connection to " + target.getUri() + " has been closed already");
                response.close();
                return;
            }
            
            final Endpoint endpoint = WebClient.getConfig(target).getEndpoint();
            // Create new processor if this is the first time or the old one has been closed 
            if (processor == null || processor.isClosed()) {
                final boolean discardIncomplete = getConfigurationProperty(DISCARD_INCOMPLETE_EVENTS, true);
                LOG.fine("Creating new instance of SSE event processor (discard incomplete events is set to '" 
                    + discardIncomplete + "') ...");
                processor = new InboundSseEventProcessor(endpoint, delegate, discardIncomplete);
            }
            
            // Start consuming events
            processor.run(response);
            LOG.fine("SSE event processor has been started ...");
            
            if (!tryChangeState(SseSourceState.CONNECTING, SseSourceState.OPEN)) {
                throw new IllegalStateException("The SseEventSource is already in " + state.get() + " state");
            }
            
            LOG.fine("Successfuly opened SSE connection to " + target.getUri());
        } catch (final Exception ex) {
            if (processor != null) {
                processor.close(1, TimeUnit.SECONDS);
                processor = null;
            }
            
            if (response != null) {
                response.close();
            }

            // We don't change the state here as the reconnection will be scheduled (if configured)
            LOG.fine("Failed to open SSE connection to " + target.getUri() + ". " + ex.getMessage());
            delegate.onError(ex);
        }
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public boolean close(long timeout, TimeUnit tunit) {
        if (state.get() == SseSourceState.CLOSED) {
            return true;
        }
        
        if (tryChangeState(SseSourceState.CONNECTING, SseSourceState.CLOSED)) {
            LOG.fine("The SseEventSource was not connected, closing anyway");
        } else if (!tryChangeState(SseSourceState.OPEN, SseSourceState.CLOSED)) {
            throw new IllegalStateException("The SseEventSource is not opened, but in " + state.get() + " state");
        }
        
        if (executor != null && !managedExecutor) {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                executor.shutdown();
                return null;
            });
            
            executor = null;
            managedExecutor = true;
        }

        // Should never happen
        if (processor == null) {
            return true;
        }
        
        return processor.close(timeout, tunit); 
    }
    
    private void scheduleReconnect(long tdelay, TimeUnit tunit, String lastEventId) {
        // If delay == RECONNECT_NOT_SET, no reconnection attempt should be performed
        if (tdelay < 0 || executor == null) {
            return;
        }
        
        // If the event source is already closed, do nothing
        if (state.get() == SseSourceState.CLOSED) {
            return;
        }
        
        // If the connection was still on connecting state, just try to reconnect
        if (state.get() != SseSourceState.CONNECTING) { 
            LOG.fine("The SseEventSource is still opened, moving it to connecting state");
            if (!tryChangeState(SseSourceState.OPEN, SseSourceState.CONNECTING)) {
                throw new IllegalStateException("The SseEventSource is not opened, but in " + state.get()
                    + " state, unable to reconnect");
            }
        }
                
        executor.schedule(() -> {
            // If we are still in connecting state (not closed/open), let's try to reconnect
            if (state.get() == SseSourceState.CONNECTING) {
                LOG.fine("Reestablishing SSE connection to " + target.getUri());
                connect(lastEventId);
            }
        }, tdelay, tunit);
        
        LOG.fine("The reconnection attempt to " + target.getUri() + " is scheduled in "
            + tunit.toMillis(tdelay) + "ms");
    }
    
    private void changeState(SseSourceState updated) {
        state.set(updated);
        onStateChanged(updated);
    }
    
    private boolean tryChangeState(SseSourceState expected, SseSourceState updated) {
        final boolean result = state.compareAndSet(expected, updated);
        
        if (result) {
            onStateChanged(updated);
        }
        
        return result;
    }

    private void onStateChanged(SseSourceState updated) {
        if (state.get() == SseSourceState.OPEN) {
            open = true;
        } else if (state.get() == SseSourceState.CLOSED) {
            open = false;
        }
    }
    
    private boolean getConfigurationProperty(String name, boolean defaultValue) {
        final Configuration configuration = target.getConfiguration();
        
        if (configuration != null) {
            final Object value = configuration.getProperty(name);
            if (value instanceof Boolean) {
                return (Boolean)value;
            } else if (value != null) {
                return Boolean.valueOf(value.toString());
            }
        }
        
        return defaultValue;
    }
}
