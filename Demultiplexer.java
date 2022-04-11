import java.net.*;
import java.util.*;
import java.io.*;

//------------------------------------------------------------
//
// class Demultiplexer
//
// this class will run a thread to listen for UDP datagrams. when a UDP
// datagram is received, it will "unwrap" the data into a TCPPacket.
//
// THEN, this class will find the specific connection that should
// receive this TCPPacket, and call that connection's receivePacket()
// method.
//
//
//
// **** a unique connection is defined by 5 numbers: ****
//   1.  localPort
//   2.  destPort
//   3.  IPAddress of remoteHost
//   4.  IPAddress of the localhost
//   5.  TCP or UDP
//
// the LAST 2 things listed are already unique:
// 1. this program will run on ONE host (~ one IP address) so we can
// safely assume that in most cases, this program will always
// receive ONE unique IP address.
// 2. TCP/UDP -- will always be TCP if we are in this progam, so
// thats unique, too!
//
//
//
// ALSO
//   be careful of a confusing distinction:
//
//   localPort is the port of the current SIDE of the connection.  on
//   the OTHER side of the connection, localPort and remotePort will be
//   switched. 
//
//   when sending a packet,
//     sourcePort = localPort;
//     destPort = remotePort;
//
//   when receiving a packet,
//     localPort = destPort;
//     remotePort = sourcePort;
//
//------------------------------------------------------------

class Demultiplexer extends Thread {

    // this number is for connecting requests, where each connection
    // should have a unique port number but nothing specific.
    private static int nextAvailablePortNumber;

    // port to listen for UDP datagrams
    private int portForUDP;

    // Hashtable provides an easy way to identify unique connections.
    // will be filled with StudentSocketImpl objects.
    private Hashtable connectionTable;

    // need a second table for connections that are listening for a
    // connect() attempt.
    // will be filled with StudentSocketImpl objects.
    private Hashtable listeningTable;


    // constructor, of course
    Demultiplexer( int portNum ) {
        super();
        this.setDaemon(true);
        nextAvailablePortNumber = 12345; // as long as its a high port number
        portForUDP = portNum; // will listen on this port number
        connectionTable = new Hashtable();
        listeningTable = new Hashtable();
    }


    // thread will loop forever, continuously listening for incoming
    // packets, which will then be unwrapped and demultiplexed BEFORE
    // receiving the next packet.
    public void run() {
        TCPPacket packet;
        byte buf[] = new byte[TCPPacket.MAX_PACKET_SIZE+20];
        DatagramPacket p;
        DatagramSocket ds;
        try{
            ds = new DatagramSocket(portForUDP);
            
            // MAIN LOOP OF THE THREAD:
            //--------------------------------------
            while (true) {
                
                // listen for UDP datagrams this function blocks, which
                // is what we want.
                p = new DatagramPacket(buf,TCPPacket.MAX_PACKET_SIZE+20);

                ds.receive(p);

                // when received, invoke TCPWrapper.unwrap(datagram)
                packet = TCPWrapper.unwrap(p);

                System.out.println("\n>>> packet received from "+
                                   p.getAddress()+" size="+p.getLength());
                System.out.println(">>> "+packet+"\n");

                
                // invoke demultiplex - will NOT return until the packet
                // has been processed completely.
                demultiplex(packet);
            }
            //--------------------------------------

        } catch (IOException e) {
            System.out.println("EXCEPTION RECEIVED: \n"+e);
            System.exit(1);
        }
    
    }

    
    // receives the TCP packet, decides which connection to pass it off
    // to.  also handles special case of a new connection, or an
    // un-usable packet.
    //
    // NOTE:  this function will NOT return until the
    // StudentSocketImpl.receivePacket() method has returned.  in other
    // words, this function will not return until the packet has been
    // processed completely.
    public void demultiplex(TCPPacket packet) {

        // remember, when receiving, destPort is the localPort.
        String hashString = getHashTableKey(packet.sourceAddr,packet.destPort,
                                            packet.sourcePort);

        StudentSocketImpl c = (StudentSocketImpl)connectionTable.get(hashString);


        // either we find connection in the connectionTable, or we find
        // it in the listeningTable, or we dont find the connection at all.

        
        if (c!=null){ // if connection found
            // System.out.println("%% connection found: "+c);
            c.receivePacket( packet );
        }
        else if (packet.synFlag) { // if packet is a SYN to open connection

            // if the connection wasnt found yet, then we check for
            // listening sockets - of course, the packet received had to
            // be a SYN for this search to be necessary.

            
            // try and find the listener in the listeningTable this time.
            hashString = getHashTableKey(packet.destPort);

            c = (StudentSocketImpl) listeningTable.get(hashString);

            // if the listeningSocket was found.
            if (c!=null){
                // System.out.println("%% listeningSocket found: "+c);
  	        c.receivePacket(packet);
            }
	    else
	      System.err.println("!!! UNMATCHED PACKET");
        }
	else
	  System.err.println("!!! UNMATCHED PACKET");

    }


    // adds a StudentSocketImpl to the listeningTable hashTable.
    synchronized public void registerListeningSocket (
        int localPort, StudentSocketImpl connection) throws IOException {

        // String is a reasonable hash key to identify a unique
        // combination of these 2 variables.
        String hashKey =  getHashTableKey(localPort);

        if (listeningTable.get(hashKey)!=null )
            throw(new IOException("%% CONNECTION EXISTS ALREADY"));
        listeningTable.put(hashKey, connection);
    }
    
    // adds a StudentSocketImpl to the connectionTable hashTable.
    synchronized public void registerConnection(InetAddress remoteHost,
                                   int localPort, int remotePort,
                                   StudentSocketImpl connection) throws IOException{
        
        // String is a reasonable hash key to identify a unique
        // combination of these 3 variables.
        String hashKey = getHashTableKey(remoteHost,localPort,remotePort);
        if (connectionTable.get(hashKey)!=null)
            throw(new IOException("%% CONNECTION EXISTS ALREADY"));
        connectionTable.put( hashKey, connection);
    }

    synchronized public void unregisterListeningSocket (
        int localPort, StudentSocketImpl connection) throws IOException {

        String hashKey =  getHashTableKey(localPort);

        if (listeningTable.get(hashKey)!=connection )
            // must be the EXACT SAME reference
            throw(new IOException("%% CANNOT UNREGISTER LISTENING SOCKET"));
        
        listeningTable.remove(hashKey);
    }

    synchronized public void unregisterConnection(InetAddress remoteHost,
                                   int localPort, int remotePort,
                                   StudentSocketImpl connection) throws IOException{
        
        String hashKey = getHashTableKey(remoteHost,localPort,remotePort);
        if (connectionTable.get(hashKey)!=connection)
            // must be the EXACT SAME reference
            throw(new IOException("%% CANNOT UNREGISTER CONNECTION"));
        connectionTable.remove(hashKey);
    }


    // for the listeningTable
    public String getHashTableKey(int localPort) {
        return( Integer.toString(localPort));
    }

    // for the ConnectionTable
    public String getHashTableKey(InetAddress remoteHost,
                                int localPort, int remotePort) {
        return( remoteHost.getHostAddress() + Integer.toString(localPort)
                + Integer.toString(remotePort));
    }

    // for connections that are actively connecting (as opposed to
    // passively listening for a connection)
    synchronized public int getNextAvailablePort() {
        nextAvailablePortNumber++;
        return(nextAvailablePortNumber);
    }

    static public void main (String args[]) {
        // for DEBUGGING only!!!

    }
    
}
