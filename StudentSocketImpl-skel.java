import com.sun.source.tree.SwitchExpressionTree;

import java.net.*;
import java.io.*;
import java.util.Set;
import java.util.Timer;

class StudentSocketImpl extends BaseSocketImpl {

//   SocketImpl data members:
     protected InetAddress address;
//     protected int port;
//     protected int localport;
  private int localAckNum; // Local copy of ackNum
  private int localSeqNumber;  // Local copy of SeqNum
  private int localSourcePort; // Local copy of SourcePort
  private int localSeqNumberStep;
  private InetAddress localSourcAddr;
  private Demultiplexer D;
  private Timer tcpTimer;


  private enum States{
    CLOSED,
    LISTEN,
    SYN_RCVD,
    ESTABLISHED,
    SYN_SENT,
    FIN_WAIT_1,
    FIN_WAIT_2,
    CLOSING,
    TIME_WAIT,
    CLOSE_WAIT,
    LAST_ACK
  }
  private States state;

  private void SetState(States state){
    System.out.println("!!!" + this.state + "->" + state);
    this.state = state;
  }
  private void SendPacket(InetAddress address, int source, int dest, int seqNum, int localAN, boolean ack, boolean syn, boolean fin){

    TCPPacket SynAck = new TCPPacket(source, dest, seqNum+1, localAN, ack, syn, fin, 1, null);
    TCPWrapper.send(SynAck, address);
  }

  StudentSocketImpl(Demultiplexer D) {  // default constructor
    this.D = D;
  }


  /**
   * Connects this socket to the specified port number on the specified host.
   *
   * @param      address   the IP address of the remote host.
   * @param      port      the port number.
   * @exception  IOException  if an I/O error occurs when attempting a
   *               connection.
   */
  public synchronized void connect(InetAddress address, int port) throws IOException{
    localport = D.getNextAvailablePort();
    D.registerConnection(address,localport,port,this);
    TCPPacket packet = new TCPPacket(localport,port,1,0,false,true,false,1,null);
    TCPWrapper.send(packet,address);
    SetState(States.SYN_SENT);
    while (this.state != state.ESTABLISHED){//
      try{
        wait();
      }
      catch(Exception e){
        e.printStackTrace();
      }
    }
  }

  /**
   * Called by Demultiplexer when a packet comes in for this connection
   * @param p The packet that arrived
   */
  public synchronized void receivePacket(TCPPacket p){
    String output = p.toString();
    System.out.println(output);
    this.notifyAll();

    switch (state){
      case LISTEN:
        System.out.print("haha");
        if(!p.ackFlag && p.synFlag){
          localSeqNumber = p.seqNum; // Value from a wrapped TCP packet
          localSeqNumberStep = localSeqNumber + 1;
          localSourcAddr = p.sourceAddr;
          localAckNum = p.ackNum;
          localSourcePort = p.sourcePort;
          SendPacket(localSourcAddr,localport, localSourcePort, p.seqNum+1, localAckNum, true, true, false) ;
//          TCPPacket SynAck = new TCPPacket(localport, localSourcePort, p.seqNum+1, localAckNum, true, true, false, 1, null);
//          TCPWrapper.send(SynAck, localSourcAddr);
          SetState(States.SYN_RCVD);
        }
        try {
          D.unregisterListeningSocket(localport, this);
          D.registerConnection(localSourcAddr, localport, p.sourcePort, this);
        } catch (IOException e) {
          e.printStackTrace();
        }
        break;

      case SYN_RCVD:
        if(p.ackFlag && !p.synFlag){
          SetState(States.ESTABLISHED);
        }
        break;

      case SYN_SENT:
        if(p.ackFlag && p.synFlag){//send an ACK packet
          localSeqNumber = p.seqNum; // Value from a wrapped TCP packet
          localSeqNumberStep = localSeqNumber + 1;
          localSourcAddr = p.sourceAddr;
          localAckNum = p.ackNum;
          localSourcePort = p.sourcePort;
          SendPacket(localSourcAddr, localport, localSourcePort,-2,localSeqNumber+1,true,false,false);
          SetState(States.ESTABLISHED);
        }
        break;

      case ESTABLISHED:
        if(p.finFlag){
          localSeqNumber = p.seqNum; // Value from a wrapped TCP packet
          localSeqNumberStep = localSeqNumber + 1;
          localSourcAddr = p.sourceAddr;
          localAckNum = p.ackNum;
          localSourcePort = p.sourcePort;
          SendPacket(localSourcAddr, localport, localSourcePort,-2,localSeqNumber+1,true,false,false);
          SetState(States.CLOSE_WAIT);
        }
        break;

      case FIN_WAIT_1:
        if (p.ackFlag){
          SetState(States.FIN_WAIT_2);
        }
        else if(p.finFlag){
          localSeqNumber = p.seqNum; // Value from a wrapped TCP packet
          localSeqNumberStep = localSeqNumber + 1;
          localSourcAddr = p.sourceAddr;
          localAckNum = p.ackNum;
          localSourcePort = p.sourcePort;
          SendPacket(localSourcAddr, localport, localSourcePort,-2,localSeqNumber+1,true,false,false);
          SetState(States.CLOSING);
        }
        break;

      case FIN_WAIT_2:
        if (p.finFlag){

          localSeqNumber = p.seqNum; // Value from a wrapped TCP packet
          localSeqNumberStep = localSeqNumber + 1;
          localSourcAddr = p.sourceAddr;
          localAckNum = p.ackNum;
          localSourcePort = p.sourcePort;
          SendPacket(localSourcAddr, localport, localSourcePort,-2,localSeqNumber+1,true,false,false);
          tcpTimer.cancel();
          tcpTimer = null;
          SetState(States.TIME_WAIT);
          createTimerTask(30*1000,null);

        }
        break;

      case CLOSING:
        if (p.ackFlag){
//          localSeqNumber = p.seqNum; // Value from a wrapped TCP packet
//          localSeqNumberStep = localSeqNumber + 1;
//          localSourcAddr = p.sourceAddr;
//          localAckNum = p.ackNum;
//          localSourcePort = p.sourcePort;
//          SendPacket(localSourcAddr, localport, localSourcePort,-2,localSeqNumber+1,false,false,false);
          tcpTimer.cancel();
          tcpTimer = null;
          SetState(States.TIME_WAIT);
          createTimerTask(30*1000,null);
        }
        break;

      case TIME_WAIT:
        SetState(States.CLOSED);
        break;

      case CLOSE_WAIT:
        if(p.finFlag){
          SetState(States.LAST_ACK);
        }

        break;

      case LAST_ACK:
        if (p.ackFlag){
          tcpTimer.cancel();
          tcpTimer = null;
          SetState(States.TIME_WAIT);
          createTimerTask(30*1000,null);
        }
    }

  }
  
  /** 
   * Waits for an incoming connection to arrive to connect this socket to
   * Ultimately this is called by the application calling 
   * ServerSocket.accept(), but this method belongs to the Socket object 
   * that will be returned, not the listening ServerSocket.
   * Note that localport is already set prior to this being called.
   */
  public synchronized void acceptConnection() throws IOException {
    SetState(States.LISTEN);
    D.registerListeningSocket(localport,this);
    while (this.state != state.ESTABLISHED){
      try{
        wait();
      }
      catch(Exception e){
        e.printStackTrace();
      }
    }
  }

  
  /**
   * Returns an input stream for this socket.  Note that this method cannot
   * create a NEW InputStream, but must return a reference to an 
   * existing InputStream (that you create elsewhere) because it may be
   * called more than once.
   *
   * @return     a stream for reading from this socket.
   * @exception  IOException  if an I/O error occurs when creating the
   *               input stream.
   */
  public InputStream getInputStream() throws IOException {
    // project 4 return appIS;
    return null;
    
  }

  /**
   * Returns an output stream for this socket.  Note that this method cannot
   * create a NEW InputStream, but must return a reference to an 
   * existing InputStream (that you create elsewhere) because it may be
   * called more than once.
   *
   * @return     an output stream for writing to this socket.
   * @exception  IOException  if an I/O error occurs when creating the
   *               output stream.
   */
  public OutputStream getOutputStream() throws IOException {
    // project 4 return appOS;
    return null;
  }


  /**
   * Closes this socket. 
   *
   * @exception  IOException  if an I/O error occurs when closing this socket.
   */
  public synchronized void close() throws IOException {
    System.out.println("closing");
    if (this.state == null){
    }
    else if (this.state == state.ESTABLISHED) {
      SendPacket(localSourcAddr, localport, localSourcePort, -2, localSeqNumber + 1, false, false, true);
      SetState(state.FIN_WAIT_1);
    } else if (this.state == state.CLOSE_WAIT) {
      SendPacket(localSourcAddr, localport, localSourcePort, -2, localSeqNumber + 1, false, false, true);
      SetState(state.LAST_ACK);
    }

    try{
      //create a new thread that waits until connection closes
      backgroundThread newThread = new backgroundThread(this);
      newThread.run();
    }
    catch (Exception e){
      e.printStackTrace();
    }

  }

  public States returnState(boolean currState){
    if(currState){
      return this.state;
    }
    else{
      return States.CLOSED;
    }
  }


  /** 
   * create TCPTimerTask instance, handling tcpTimer creation
   * @param delay time in milliseconds before call
   * @param ref generic reference to be returned to handleTimer
   */
  private TCPTimerTask createTimerTask(long delay, Object ref){
    if(tcpTimer == null)
      tcpTimer = new Timer(false);
    return new TCPTimerTask(tcpTimer, delay, this, ref);
  }


  /**
   * handle timer expiration (called by TCPTimerTask)
   * @param ref Generic reference that can be used by the timer to return 
   * information.
   */
  public synchronized void handleTimer(Object ref){

    // this must run only once the last timer (30 second timer) has expired
    tcpTimer.cancel();
    tcpTimer = null;

    //this must run only once
    if(this.state.equals("TIME_WAIT")){
      try {
        SetState(state.CLOSED);
      }
      catch (Exception e) {
        notifyAll();
      }

      notifyAll();


      try {
        D.unregisterConnection(localSourcAddr, localport, localSourcePort, this);
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }

    //resend the packet due to an ack not being transmitted
    else{
      SendPacket(localSourcAddr, localport, localSourcePort, 0, localSeqNumber, false, false, false);
    }
  }
}

class backgroundThread implements Runnable{

  public StudentSocketImpl waitToClose;
  public backgroundThread(StudentSocketImpl here) {
    this.waitToClose = here;
  }

  public void run(){
    while (waitToClose.returnState(true) != waitToClose.returnState(false)){
      try {
        waitToClose.wait();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}