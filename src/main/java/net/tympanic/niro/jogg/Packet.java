package net.tympanic.niro.jogg;

import java.util.logging.Logger;

/**
 *
 */
public class Packet {

    private static final Logger LOG = Logger.getLogger(Packet.class.getName());

    /**
     *
     */
    public byte[] packet_base;

    /**
     *
     */
    public int packet;

    /**
     *
     */
    public int bytes;

    /**
     *
     */
    public int b_o_s;

    /**
     *
     */
    public int e_o_s;

    /**
     *
     */
    public long granulepos;

    /**
     * sequence number for decode; the framing knows where there's a hole in the
     * data, but we need coupling so that the codec (which is in a seperate
     * abstraction layer) also knows about the gap
     */
    public long packetno;

}