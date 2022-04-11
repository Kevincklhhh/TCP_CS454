import java.net.InetAddress;

//---------------------------------------------------
//
// class TCPPacket
//
// this is the object that represents a TCP packet.
// 
// NOTE WELL that there are no IP addresses in a TCP header.
//
// this class allows you to instantiate a TCP packet in one of two ways:
// either you have all the info you need to initialize a packet, or you
// have a byte[] to read in.
//
// This implementation of TCPPacket will ignore options in the header,
// and will also ignore the checksum, push flag, urgent flag and urgent
// pointer.
//
//---------------------------------------------------

class TCPPacket {

    // these bit masks are ordered from right to left.
    public static final char BIT1 = 0x001;
    public static final char BIT2 = 0x002;
    public static final char BIT3 = 0x004;
    public static final char BIT4 = 0x008;
    public static final char BIT5 = 0x0010;
    public static final char BIT6 = 0x0020;
    public static final char BIT7 = 0x0040;
    public static final char BIT8 = 0x0080;
    public static final char BIT9 = 0x0100;
    public static final char BIT10 = 0x0200;
    public static final char BIT11 = 0x0400;
    public static final char BIT12 = 0x0800;
    public static final char BIT13 = 0x1000;
    public static final char BIT14 = 0x2000;
    public static final char BIT15 = 0x4000;
    public static final char BIT16 = 0x8000;

    // byte masks
    public static final int BYTE1 = 0x000000ff;
    public static final int BYTE2 = 0x0000ff00;
    public static final int BYTE3 = 0x00ff0000;
    public static final int BYTE4 = 0xff000000;


    // constant for everyone to know the maximum possible packet size
    public static final int MAX_PACKET_SIZE = 1000; // in bytes
    
    // TCP header things to keep in the packet
    InetAddress sourceAddr;
    int sourcePort;
    int destPort;
    int seqNum;
    int ackNum;
    boolean ackFlag;
    boolean rstFlag;
    boolean synFlag;
    boolean finFlag;
    int windowSize;

    
    // the data part of the TCP packet
    // be sure to NOT read possible header options into the data buf.
    protected byte[] data;

    
    // private because we dont want them to be messed with accidentally
    private int headerLength;
    private int checksum;  // we dont bother with this quite yet

    
    // creates a TCPPacket from the real buffer of data... this
    // constructor will generally be used when RECEIVING data, and
    // formulating it into a packet.
    public TCPPacket( byte[] packet, InetAddress sender ) {
        sourceAddr = sender;
        sourcePort = (((char)((char)packet[0] << 8)) & BYTE2) |
            (((char)(packet[1])) & BYTE1);
        destPort = (((char)((char)packet[2] << 8)) & BYTE2) |
            (((char)(packet[3])) & BYTE1);
        
        seqNum = (((int)packet[4] << 24) & BYTE4) |
            (((int)packet[5] << 16) & BYTE3) |
            (((int)packet[6] << 8) & BYTE2)|
            (((int)packet[7]) & BYTE1);
        ackNum = (((int)packet[8] << 24) & BYTE4) |
            (((int)packet[9] << 16) & BYTE3) |
            (((int)packet[10] << 8) & BYTE2)|
            (((int)packet[11]) & BYTE1);
        
        // header length is only 4 bits in the TCP header
        // this number represents how many 32-bit words in the header...
        // i.e. 4 bytes to a word  (hence the *4 at the end)
        headerLength = (packet[12] >> 4)*4;

        // ignore the reserved bits
        // ignore the URG flag
        // and, ignore PSH

        // the next 4 statements use bitmasking so we can see the value
        // of one bit, for the TCP flags
        
        if ((packet[13] & BIT5)==0)  ackFlag=false;
        else ackFlag=true;

        if ((packet[13] & BIT3)==0)  rstFlag=false;
        else rstFlag=true;

        if ((packet[13] & BIT2)==0)  synFlag=false;
        else synFlag=true;

        if ((packet[13] & BIT1)==0)  finFlag=false;
        else finFlag=true;


        windowSize = (((char)((char)packet[14] << 8)) & BYTE2) |
            (((char)(packet[15])) & BYTE1);
        checksum = (((char)((char)packet[16] << 8)) & BYTE2) |
            (((char)(packet[17])) & BYTE1);


        // copy the data, if any
        int j=0;
        int dataSize=(packet.length-headerLength);
        if (dataSize>0)
            data = new byte[dataSize];
        else
            data = null;
        for( int i=headerLength; i<packet.length; i++,j++) {
            // starting at the end of the TCP header, and copying till
            // the end of this byte[], we have our data.
            data[j] = packet[i];
        }
    }

    
    // creates a TCPPacket from values given here. will usually be used
    // when SENDING a packet.
    public TCPPacket( int sourcePort, int destPort, int seqNum, int ackNum,
                      boolean ackFlag, boolean synFlag, boolean finFlag,
                      int windowSize, byte[] data) {
        this.sourcePort = sourcePort;
        this.destPort = destPort;
        this.seqNum = seqNum;
        this.ackNum = ackNum;
        this.ackFlag = ackFlag;
        this.synFlag = synFlag;
        this.finFlag = finFlag;
        this.windowSize = windowSize;
	if(data != null){
	  this.data = new byte[data.length];
	  System.arraycopy(data, 0, this.data, 0, data.length);
	}
	else
	  this.data = null;
        
        this.rstFlag = false;
        this.headerLength = 20; // no options, so will always be 20 bytes.
        
        this.checksum = 0; // WILL SET WHEN WE WRITE THE PACKET.
    }
    
    
    // returns the whole packet as an array.  this array can then be
    // used in a DatagramPacket, for example.
    public byte[] getBufferPacket () {
        // TCP Header that we create will ALWAYS be 20 bytes
        byte packet[];
        if (data==null)
            packet = new byte[20];
        else
            packet = new byte[20+data.length];

        int flags = 0;
        if (ackFlag)
            flags = flags|BIT5;
        if (rstFlag)
            flags = flags|BIT3;
        if (synFlag)
            flags = flags|BIT2;
        if (finFlag)
            flags = flags|BIT1;

        
        packet[0] = (byte) (sourcePort>>8);
        packet[1] = (byte)  sourcePort;
        packet[2] = (byte) (destPort>>8);
        packet[3] = (byte)  destPort;
        packet[4] = (byte) (seqNum>>24);
        packet[5] = (byte) (seqNum>>16);
        packet[6] = (byte) (seqNum>>8);
        packet[7] = (byte)  seqNum;
        packet[8] = (byte) (ackNum>>24);
        packet[9] = (byte) (ackNum>>16);
        packet[10] =(byte) (ackNum>>8);
        packet[11] =(byte)  ackNum;
        packet[12] =(byte) ((headerLength/4)<<4);
        packet[13] = (byte)  flags;
        packet[14] = (byte) (windowSize>>8);
        packet[15] = (byte)  windowSize;
        packet[16] = (byte) (checksum>>8);
        packet[17] = (byte)  checksum;
        packet[18] = 0;
        packet[19] = 0;

        
        // add data to packet, if needed
        if (data!=null) {
            for (int i=0; i<data.length; i++) {
                packet[i+20] = data[i];
            }
        }

        return(packet);
    }

    // returns ONLY the data part of the packet
    // is possible that it may return null.
    public byte[] getData() {
        return(data);
    }

    // for debugging
    public static String getBinary( char c ) {
        String bin = Integer.toBinaryString((int)c);
        return(bin);
    }

    // more visual output for debugging than toString()
    public String getDebugOutput() {
        String flags = "";
        if (ackFlag)
            flags = "\n ackFlag IS TRUE";
        if (synFlag)
            flags = flags+"\n synFlag IS TRUE";
        if (finFlag)
            flags = flags+"\n finFlag IS TRUE";


        String toReturn =
            "\n==================="+
            "\n sourcePort = "+sourcePort+
            "\n destPort = "  +destPort +
            "\n sequence # = "+seqNum+
            "\n ACK # = "+ackNum+
            flags+
            "\n windowSize = "+windowSize+
            "\n ----------------- "+
            "\n checksum = "+checksum+
            "\n headerLength = "+headerLength;
        if (data!=null)
            toReturn = toReturn +
                "\n datalen = "+data.length;
        else
            toReturn = toReturn +
                "\n data is null";

        toReturn = toReturn +           
            "\n===================\n";

        return(toReturn);
    }


    // outputs the core values of the packet
    public String toString() {
        String flags;
        if (ackFlag)
            flags = " A";
        else flags = "  ";
        if (synFlag)
            flags = flags+"S";
        else flags = flags+" ";
        if (finFlag)
            flags = flags+"F";
        else flags = flags+" ";

        // hopefully this all fits in one line with the remote IP
        // address to spare - that will be output from TCPConnection, i
        // would think.

        String output = "    "+"srcPort="+ sourcePort+" destPort="+destPort+
            " seq="+seqNum+" ack="+ackNum+flags+" wndSize="+windowSize;

        if (data!=null){
	  output = output+" datalen="+data.length;
	}
        else
	  output = output+" (no data)";

        
        return(output);
    }
    
    public static void main(String args[]){
        // for DEBUGGING ONLY!!!

        // this little test will test 3 things:
        // (1) the easy packet constructor
        // (2) conversion of packet to byte[]
        // (3) the reading of the byte[] into packet

        byte buf[] = new byte[3];
        buf[0] = 2;
        buf[1] = 4;
        buf[2] = 6;
        
        TCPPacket packet = new
            TCPPacket(12345,23456,1234567,2345678,true,false,true,56,buf);
        System.out.println(packet.getDebugOutput());
        System.out.println(packet);
        System.out.println("packet.data[3] = "
                           +packet.data[0]+packet.data[1]+packet.data[2]);

        
        TCPPacket grub = new TCPPacket(packet.getBufferPacket(), 
				       packet.sourceAddr);
        System.out.println(grub.getDebugOutput());
        System.out.println(grub);
        System.out.println("grub.data[3] = "
                           +grub.data[0]+grub.data[1]+grub.data[2]);

                
    }
    
}
