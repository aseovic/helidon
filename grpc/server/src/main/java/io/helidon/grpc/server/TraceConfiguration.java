package io.helidon.grpc.server;

import io.opentracing.contrib.grpc.OperationNameConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration class for tracer.
 */
public class TraceConfiguration
    {
    // ---- constructors ------------------------------------------------

    // Default constructor
    public TraceConfiguration()
        {
        }

    /**
     * @param operationNameConstructor for all spans
     *
     * @return this TraceConfiguration with configured operation name
     */
    public TraceConfiguration withOperationName(OperationNameConstructor operationNameConstructor)
        {
        this.operationNameConstructor = operationNameConstructor;
        return this;
        }

    /**
     * @param attributes to set as tags on server spans
     *
     * @return this TraceConfiguration configured to trace request attributes
     */
    public TraceConfiguration withTracedAttributes(ServerRequestAttribute... attributes)
        {
        tracedAttributes = new HashSet<>(Arrays.asList(attributes));
        return this;
        }

    /**
     * Logs streaming events to server spans.
     *
     * @return this TraceConfiguration configured to log streaming events
     */
    public TraceConfiguration withStreaming()
        {
        streaming = true;
        return this;
        }

    /**
     * Logs all request life-cycle events to server spans.
     *
     * @return this TraceConfiguration configured to be verbose
     */
    public TraceConfiguration withVerbosity()
        {
        verbose = true;
        return this;
        }

    /**
     * Return the configured verbose.
     *
     * @return
     */
    public boolean isVerbose()
        {
        return verbose;
        }

    /**
     * Return the configured streaming.
     *
      * @return
     */
    public boolean isStreaming()
        {
        return streaming;
        }

    /**
     * Return the set of configured tracedAttributes.
     *
     * @return
     */
    public Set<ServerRequestAttribute> tracedAttributes()
        {
        return tracedAttributes;
        }

    /**
     * Return the configured operationNameConstructor.
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
