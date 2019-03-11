package io.helidon.grpc.server;

import io.opentracing.contrib.grpc.OperationNameConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration class for tracer.
 */
public class TracingConfiguration
    {
    // ---- constructor ------------------------------------------------

    /**
     * Private constructor called by the {@link Builder}.
     *
     * @param operationNameConstructor  the operation name constructor
     * @param streaming                 flag indicating whether to log streaming
     * @param verbose                   flag indicating verbose logging
     * @param tracedAttributes          the set of attributes to log in spans
     */
    private TracingConfiguration(OperationNameConstructor operationNameConstructor,
                              Set<ServerRequestAttribute> tracedAttributes,
                              boolean streaming,
                              boolean verbose)
        {
        this.operationNameConstructor = operationNameConstructor;
        this.tracedAttributes         = tracedAttributes;
        this.streaming                = streaming;
        this.verbose                  = verbose;
        }

    /**
     * @return the configured verbose.
     */
    public boolean isVerbose()
        {
        return verbose;
        }

    /**
     * @return the configured streaming.
     */
    public boolean isStreaming()
        {
        return streaming;
        }

    /**
     * @return the set of configured tracedAttributes.
     */
    public Set<ServerRequestAttribute> tracedAttributes()
        {
        return tracedAttributes;
        }

    /**
     * @return the configured operationNameConstructor.
     */
    public OperationNameConstructor operationNameConstructor()
        {
        return operationNameConstructor;
        }


    // ----- inner class: Builder -------------------------------------------

    /**
     * Builds the configuration of a tracer.
     */
    public static class Builder
        {
        /**
         * Creates a Builder with default configuration
         */
        public Builder()
            {
            operationNameConstructor = OperationNameConstructor.DEFAULT;
            streaming                = false;
            verbose                  = false;
            tracedAttributes         = Collections.emptySet();
            }

        /**
         * @param operationNameConstructor for all spans
         *
         * @return this Builder with configured operation name
         */
        public Builder withOperationName(OperationNameConstructor operationNameConstructor)
            {
            this.operationNameConstructor = operationNameConstructor;
            return this;
            }

        /**
         * @param attributes to set as tags on server spans
         *
         * @return this Builder configured to trace request
         * attributes
         */
        public Builder withTracedAttributes(ServerRequestAttribute... attributes)
            {
            tracedAttributes = new HashSet<>(Arrays.asList(attributes));
            return this;
            }

        /**
         * Logs streaming events to server spans.
         *
         * @return this Builder configured to log streaming events
         */
        public Builder withStreaming()
            {
            streaming = true;
            return this;
            }

        /**
         * Logs all request life-cycle events to server spans.
         *
         * @return this Builder configured to be verbose
         */
        public Builder withVerbosity()
            {
            verbose = true;
            return this;
            }

        /**
         * @return a TracingConfiguration with this Builder's configuration
         */
        public TracingConfiguration build()
            {
            return new TracingConfiguration(operationNameConstructor, tracedAttributes, streaming, verbose);
            }

        /**
         * A flag indicating whether to log streaming.
         */
        private OperationNameConstructor operationNameConstructor;

        /**
         * A flag indicating verbose logging.
         */
        private boolean streaming;

        /**
         * A flag indicating verbose logging.
         */
        private boolean verbose;

        /**
         * The set of attributes to log in spans.
         */
        private Set<ServerRequestAttribute> tracedAttributes;
        }


    // ----- data members -----------------------------------------------

    /**
     * A flag indicating whether to log streaming.
     */
    private OperationNameConstructor operationNameConstructor;

    /**
     * A flag indicating verbose logging.
     */
    private boolean streaming;

    /**
     * A flag indicating verbose logging.
     */
    private boolean verbose;

    /**
     * The set of attributes to log in spans.
     */
    private Set<ServerRequestAttribute> tracedAttributes;
    }
