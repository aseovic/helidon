package io.helidon.grpc.server;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.contrib.grpc.OpenTracingContextKey;
import io.opentracing.contrib.grpc.OperationNameConstructor;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GrpcTracing
        implements ServerInterceptor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * public constructor.
     *
     * @param tracer       the Open Tracing {@link Tracer}
     * @param traceConfig  the trace configuration
     */
    public GrpcTracing(Tracer tracer, TraceConfiguration traceConfig)
        {
        f_tracer                   = tracer;
        f_operationNameConstructor = traceConfig.operationNameConstructor();
        f_streaming                = traceConfig.isStreaming();
        f_verbose                  = traceConfig.isVerbose();
        f_tracedAttributes         = traceConfig.tracedAttributes();
        }

    // ----- ServerTracingInterceptor methods -------------------------------

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT>        call,
                                                                 Metadata                       headers,
                                                                 ServerCallHandler<ReqT, RespT> next)
        {
        Map<String, String> headerMap = new HashMap<>();

        for (String key : headers.keys())
            {
            if (!key.endsWith(Metadata.BINARY_HEADER_SUFFIX))
                {
                String value = headers.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER));
                headerMap.put(key, value);
                }
            }

        final String operationName = f_operationNameConstructor.constructOperationName(call.getMethodDescriptor());
        final Span   span          = getSpanFromHeaders(headerMap, operationName);

        for (ServerRequestAttribute attr : f_tracedAttributes)
            {
            switch (attr)
                {
                case METHOD_TYPE:
                    span.setTag("grpc.method_type", call.getMethodDescriptor().getType().toString());
                    break;
                case METHOD_NAME:
                    span.setTag("grpc.method_name", call.getMethodDescriptor().getFullMethodName());
                    break;
                case CALL_ATTRIBUTES:
                    span.setTag("grpc.call_attributes", call.getAttributes().toString());
                    break;
                case HEADERS:
                    // copy the headers and make sure that the AUTHORIZATION header
                    // is removed as we do not want auth details to appear in tracing logs
                    Metadata metadata = new Metadata();

                    metadata.merge(headers);
                    metadata.removeAll(ContextKeys.AUTHORIZATION);

                    span.setTag("grpc.headers", metadata.toString());
                    break;
                }
            }

        Context                   ctxWithSpan         = Context.current().withValue(OpenTracingContextKey.getKey(), span);
        ServerCall.Listener<ReqT> listenerWithContext = Contexts.interceptCall(ctxWithSpan, call, headers, next);


        return new TracingListener<>(listenerWithContext, span);
        }

    // ----- helper methods -------------------------------------------------

    private Span getSpanFromHeaders(Map<String, String> headers, String operationName)
        {
        Span span;

        try
            {
            SpanContext parentSpanCtx = f_tracer.extract(Format.Builtin.HTTP_HEADERS,
                    new TextMapExtractAdapter(headers));
            if (parentSpanCtx == null)
                {
                span = f_tracer.buildSpan(operationName)
                        .start();
                }
            else
                {
                span = f_tracer.buildSpan(operationName)
                        .asChildOf(parentSpanCtx)
                        .start();
                }
            }
        catch (IllegalArgumentException iae)
            {
            span = f_tracer.buildSpan(operationName)
                    .withTag("Error", "Extract failed and an IllegalArgumentException was thrown")
                    .start();
            }

        return span;
        }

    private class TracingListener<ReqT>
            extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>
        {

        // Constructor
        public TracingListener(ServerCall.Listener<ReqT> delegate, Span span)
            {
            super(delegate);
            this.span = span;
            }

        @Override
        public void onMessage(ReqT message)
            {
            if (f_streaming || f_verbose)
                {
                span.log(Collections.singletonMap("Message received", message));
                }

            delegate().onMessage(message);
            }

        @Override
        public void onHalfClose()
            {
            if (f_streaming)
                {
                span.log("Client finished sending messages");
                }

            delegate().onHalfClose();
            }

        @Override
        public void onCancel()
            {
            span.log("Call cancelled");

            try
                {
                delegate().onCancel();
                }
            finally
                {
                span.finish();
                }
            }

        @Override
        public void onComplete()
            {
            if (f_verbose)
                {
                span.log("Call completed");
                }

            try
                {
                delegate().onComplete();
                }
            finally
                {
                span.finish();
                }
            }

        final Span span;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The Open Tracing {@link Tracer}.
     */
    private final Tracer f_tracer;

    /**
     * A flag indicating whether to log streaming.
     */
    private final OperationNameConstructor f_operationNameConstructor;

    /**
     */
    private final boolean f_streaming;

    /**
     * A flag indicating verbose logging.
     */
    private final boolean f_verbose;

    /**
     * The set of attributes to log in spans.
     */
    private final Set<ServerRequestAttribute> f_tracedAttributes;
    }
