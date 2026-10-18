package com.tetanusmod.client;
public final class ClientPacketWarnings {
    private static final java.util.concurrent.atomic.AtomicBoolean REPORTED=new java.util.concurrent.atomic.AtomicBoolean();
    public static void invalid(){if(REPORTED.compareAndSet(false,true))org.slf4j.LoggerFactory.getLogger("tetanusmod").warn("Ignored malformed Rust & Ruin server packet");}
    public static void reset(){REPORTED.set(false);}
}