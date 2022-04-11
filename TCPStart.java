import java.net.*;


//---------------------------------------------------
//
// class TCPStart
//
// this is the hub of the entire socket implementation.
// all modules are initialized here.
//
//
// code that runs on TOP of this whole implementation will
// be put in this file, as separate threads.
//
// to start our implementation of TCP, type
//   java TCPStart <UDP port #>
//
//
//---------------------------------------------------
class TCPStart {
    
  public final static String PORTRESOURCE = "UDPPORT";
  public final static String LOSSRATERESOURCE = "LOSSRATE";

  static public void start() {

    // check command line args
    if (System.getProperty(PORTRESOURCE)==null) {
      System.err.println("Must set "+PORTRESOURCE+" for UDP port to use with "+
			 "-D"+PORTRESOURCE+"=<num>");
      System.exit(1);
    }        

        
    // this number will initialize what port # you want your UDP
    // wrapper to run on.
    int portForUDP = Integer.parseInt(System.getProperty(PORTRESOURCE));

        
    // initialize TCPWrapper's port number for UDP wrapping
    TCPWrapper.setUDPPortNumber( portForUDP );

        
    // initialize more TCPWrapper stuff here, if you want to test packet
    // dropping, or if you want to change the sending-rate limit

        
    // create an instance of the Demultiplexer
    Demultiplexer D = new Demultiplexer( portForUDP );

    // create an instance of OUR SocketImplFactory
    StudentSocketImplFactory myFactory = new StudentSocketImplFactory(D);
        

    // tell all Socket objects of this program to use OUR
    // implementation of SockImpl
    try {
      Socket.setSocketImplFactory( myFactory );
      ServerSocket.setSocketFactory( myFactory );
    } catch (Exception e) {
      System.out.println(e);
      System.exit(1);
    }


    // start the demultiplexer
    D.start();

    if (System.getProperty(LOSSRATERESOURCE)!=null) {
      TCPWrapper.dropRandomPackets
	(System.currentTimeMillis(),
	 Double.parseDouble(System.getProperty(LOSSRATERESOURCE)));
    }        

        
  }
}

