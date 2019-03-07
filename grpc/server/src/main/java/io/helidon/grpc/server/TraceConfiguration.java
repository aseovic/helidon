package io.helidon.grpc.server;

import io.opentracing.contrib.grpc.OperationNameConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TraceConfiguration
    {
    // constructor
    public TraceConfiguration()
        {
        }

    /**
     * @param operationNameConstructor for all spans created by this interceptor
     *
     * @return this Builder with configured operation name
     */
    public TraceConfiguration withOperationName(OperationNameConstructor operationNameConstructor)
        {
        this.operationNameConstructor = operationNameConstructor;
        return this;
        }

    /**
     * @param attributes to set as tags on server spans
     *                   created by this interceptor
     *
     * @return this Builder configured to trace request attributes
     */
    public TraceConfiguration withTracedAttributes(ServerRequestAttribute... attributes)
        {
        tracedAttributes = new HashSet<>(Arrays.asList(attributes));
        return this;
        }

    /**
     * Logs streaming events to server spans.
     *
     * @return this Builder configured to log streaming events
     */
    public TraceConfiguration withStreaming()
        {
        streaming = true;
        return this;
        }

    /**
     * Logs all request life-cycle events to server spans.
     *
     * @return this Builder configured to be verbose
     */
    public TraceConfiguration withVerbosity()
        {
        verbose = true;
        return this;
        }

    /**
     * Return true if verbose tracing.
     *
     * @return
     */
    public boolean isVerbose()
        {
        return verbose;
        }

    /**
     * Return true if streaming.
     *
      * @return
     */
    public boolean isStreaming()
        {
        return streaming;
        }

    /**
     * Return the set of tracedAttributes.
     *
     * @return
     */
    public Set<ServerRequestAttribute> tracedAttributes()
        {
        return tracedAttributes;
        }

    /**
     * Return the operationNameConstructor.
     *
     * @return
     */
    public OperationNameConstructor operationNameConstructor()
        {
        return operationNameConstructor;
        }


    // ----- data members -----------------------------------------------

    /**
     * A flag indicating whether to log streaming.
     */
    private OperationNameConstructor operationNameConstructor = OperationNameConstructor.DEFAULT;;

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
    private Set<ServerRequestAttribute> tracedAttributes = Collections.emptySet();
    }
