package rnBeleg;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class ConfirmationPackage extends UdpPackage
{
    byte ACKnumber;

    public ConfirmationPackage(byte ack)
    {
        super();

        this.ACKnumber = ack;
    }

    public byte[] getBytes()
    {
        ByteBuffer buff = ByteBuffer.allocate(3);
        buff.putShort(this.sessionId);
        buff.put(this.ACKnumber);
        return buff.array();
    }

}