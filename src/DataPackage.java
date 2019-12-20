package rnBeleg;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class DataPackage extends UdpPackage
{
    public static final int PAYLOAD_SIZE = 512;


    byte packageId;
    public byte[] data;
    boolean checksumSet;
    int checksumValue;

    public DataPackage(byte pid, byte[] data, boolean crc)
    {
        super();
        this.packageId = pid;
        this.data = data;
        
        this.size = this.getSize();

        this.checksumSet = crc;
        
    }

    public void setChecksum(int sum){
        this.checksumValue = sum;
        this.checksumSet = true;
    }

    public byte[] getBytes()
    {
        
        byte[] tmp = new byte[this.getSize()];
        System.out.println("datapackage size "+ this.size);

        ByteBuffer buff = ByteBuffer.wrap(tmp);

        // byteorder ala protocoll
        buff.putShort(this.sessionId);
        buff.put(this.packageId);
        buff.put(this.data);
        if(this.checksumSet)
            buff.putInt(this.checksumValue);

        return buff.array();
    }

    public int getSize()
    {
        int tmp = 
        super.getSize()
        + 1
        + this.data.length;

        // crc is 4 bytes long
        if (this.checksumSet)
            tmp += Integer.BYTES;

        return tmp;
    }

    // returns a new DataPackage parsed from raw network data
    // -- OBSOLETE --
    public static DataPackage parseFromBytes(byte[] data)
    {
        ByteBuffer buff = ByteBuffer.wrap(data);
        short tmpsid = buff.getShort();
        byte tmppid = buff.get();

        // our payload size
        byte[] tmpdata = new byte[50];
        buff.get(tmpdata);
        long tmpchecksum = buff.getLong();

        return new DataPackage(tmppid, tmpdata, false);
    }

}