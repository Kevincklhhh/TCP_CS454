import java.net.*;
import java.io.*;
import java.util.*;


//---------------------------------------------------
//
// class TCPWrapper
//
// this class provides methods to wrap a TCPPacket
// class into UDP, and send it over the network.
// in addition to that, the rate of packets is limited,
// and there is a choice of random or selective packet
// dropping.
//
// everything here is static.
//
// the port number MUST be initialized.
//   use setUDPPortNumber(int port);
//   (done by TCPStart)
//
//everything else has
// default values:
//   (1) sending rate defaults to 10 packets per second
//   (2) random packet dropping is disabled
//   (3) selective packet dropping is disabled
//
//---------------------------------------------------
class TCPWrapper {

    // the max number of packets allowed to be sent per second
    static private int packetsPerSecond = 10;

    // the Date() object for rate limiting the packets.
    static long time = (new Date()).getTime();
    static long temptime;
    
    // counter of how many packets have been sent this second, so far.
    static int packetBurst = 0;
    
    // flag if we are to drop random packets (i.e. send() will not send
    // the packet - to simulate packet loss!)
    static boolean randomPacketsDropped = false;

    // flag if we are to drop specific packets
    static boolean selectedPacketsDropped = false;

    // the number generator for psuedorandom numbers.
    static Random numberGenerator=null;

    // the rate of random packets to be dropped
    static double rateToDrop=0.0;
    
    // the set of selected packets to be dropped
    static Hashtable dropSet=null;
    
    // counter for how many packets sent OR dropped (total of both).
    // so, counter will = 1 for the first packet sent.
    static long packetCounter = 0;

    // counter for how many packets have been dropped.
    static long droppedCounter = 0;
    
    // for UDP sending
    static int portForUDP = -1;

    
    // sets the packet rate, of course
    static public void setPacketRate( int pps ) {
        if (pps > 50) {
            System.out.println("packet rate should not be set higher "+
                               "than 50 packets per second.");
            System.exit(1);
        }
        packetsPerSecond = pps;
    }


    // seeds and enables the random packet dropping
    static public void dropRandomPackets( long seed, double rate ) {
        // do random packet stuff here...
        if (rate>1.0) {
            System.out.println("for dropping random packets," +
                               "loss rate should be < 1.0");
            System.exit(1);
        }
        
        randomPacketsDropped = true;
        rateToDrop = rate;
        numberGenerator = new Random(seed);
    }

    
    // adds a specific "Nth" packet to the list of packets that will be
    // dropped.  call this function for EACH packet you want to drop
    static public void dropSelectedPacket( int nthPacket ) {
        // set up a static array or something that will drop the nth
        // packet - including ACKS and ANYTHING sent through
        // TCPWrapper.send(...)
        selectedPacketsDropped = true;
        if (dropSet==null)
            dropSet = new Hashtable();
        dropSet.put( new Long((long)nthPacket), new Long((long)nthPacket));
    }



    // MUST be called before sending packets
    // or else you'll just crash =)
    static public void setUDPPortNumber( int port ) {
        portForUDP = port;
    }
    


    // sends a packet over the network, wrapped in a UDP datagram:
    //
    //   if we try to send a packet faster than the rate limit, this
    //   function will BLOCK (i.e. sit and wait) until it can send the
    //   packet.
    static synchronized public void send(TCPPacket packet, InetAddress remoteHost) {

        // the first time this happens, it increments from 0 to 1
        // which is the desired condition.
        packetCounter++;

        
        // CHECK PACKET DROPPING STUFF
        if (randomPacketsDropped){
            if (numberGenerator.nextDouble() < rateToDrop) {
                droppedCounter++;
                System.out.println("packet # " +packetCounter+
                                   " randomly dropped.");
                return;
            }
        }
        if ( (selectedPacketsDropped)&&
                  (dropSet.containsKey( new Long(packetCounter) ))) {
            droppedCounter++;
            System.out.println("packet # " +packetCounter+
                               " selectively dropped.");

            return;
        }
                

        // CHECK RATE LIMIT
        if (packetBurst>=packetsPerSecond) {
            temptime = (new Date()).getTime();

            // prevents us from proceeding till at least a second has
            // passed since our last burst of packets.
            while( temptime< time+1000)
	      temptime = (new Date()).getTime();
            time = temptime;
            packetBurst=0;
        }
        packetBurst++;

        
        try {
            byte toSend[] = packet.getBufferPacket();
            DatagramPacket p = new DatagramPacket(toSend,toSend.length,
                                                  remoteHost,portForUDP);
            DatagramSocket sock = new DatagramSocket();
            sock.send(p);
        }
        catch(Exception e) {
            System.out.println(e);
            System.exit(1);
        }


        // want this debug line?
        System.out.println("\n<<< packet # "+packetCounter+" to "+remoteHost+"...");
        System.out.println("<<< "+packet+"\n");
    }

    
    // unwraps the TCP packet from the UDP wrapper. this function does
    // NOT receive UDP packets from the network. it is invoked AFTER
    // receiving the datagram.
    static public TCPPacket unwrap(DatagramPacket d) {
        byte b[] = new byte[d.getLength()];
	System.arraycopy(d.getData(),0,b,0,d.getLength());
        return( new TCPPacket(b, d.getAddress()) );
    }
    

    public static void main(String args[]) throws Exception {
        // DEBUGGING ONLY!!
    }
    
}
