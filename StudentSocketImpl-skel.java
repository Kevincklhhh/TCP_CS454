
import java.net.*;
import java.io.*;
//import java.util.Set;
import java.util.Timer;

class StudentSocketImpl extends BaseSocketImpl {

  private int ackNum;
  private int seqNumber;
  private int sourcePort;
  private InetAddress sourcAddr;
  private Demultiplexer D;
  private Timer tcpTimer;
  private TCPPacket lastpack1;
  private TCPPacket lastpack2;
  private int counter = 1;


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

  private void SendPacket(Boolean resend, TCPPacket thisPack, InetAddress address, int source, int dest, int seqNum, int localAN, boolean ack, boolean syn, boolean fin){
    TCPPacket synpack;
    if(this.state == state.CLOSED && counter > 0){
      notifyAll();
      return;
    }
    if(resend){
      System.out.println("RESENDING PACKET");
    }
    counter++;
    if(resend){
      synpack = thisPack;
    }
    else{
      synpack = new TCPPacket(source, dest, seqNum+1, localAN, ack, syn, fin, 1, null);
    }

    TCPWrapper.send(synpack, address);

    if (!synpack.ackFlag || synpack.synFlag){
      lastpack1 = synpack;
      createTimerTask(1000, null);
    }
    else{
      lastpack2 = synpack;
    }

  }

  StudentSocketImpl(Demultiplexer D) {
    this.state = state.CLOSED;
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
    sourcAddr = address;
    D.registerConnection(address, localport, port,this);
    counter--;
    SendPacket(false, lastpack1, address, localport, port,1,0,false,true,false);
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
      case CLOSED:

      case LISTEN:
        System.out.print("haha");
        if(!p.ackFlag && p.synFlag){
          seqNumber = p.seqNum;
          sourcAddr = p.sourceAddr;
          ackNum = p.ackNum;
          sourcePort = p.sourcePort;
          SendPacket(false, lastpack1, sourcAddr,localport, sourcePort, p.seqNum+1, ackNum, true, true, false) ;
          SetState(States.SYN_RCVD);
        }
        try {
          D.unregisterListeningSocket(localport, this);
          D.registerConnection(sourcAddr, localport, p.sourcePort, this);
        } catch (IOException e) {
          e.printStackTrace();
        }
        break;

      case SYN_RCVD:
        if(p.ackFlag){
          if (tcpTimer != null) {
            tcpTimer.cancel();
            tcpTimer = null;
          }
          SetState(States.ESTABLISHED);
        }else if (p.synFlag){
          SendPacket(true, lastpack1, sourcAddr,0,0,0,0,true,true,false);}
        break;

      case SYN_SENT:
        if(p.ackFlag && p.synFlag){
          if (tcpTimer != null) {
            tcpTimer.cancel();
            tcpTimer = null;
          }
          seqNumber = p.seqNum;
          sourcAddr = p.sourceAddr;
          ackNum = p.ackNum;
          sourcePort = p.sourcePort;
          SendPacket(false, lastpack1, sourcAddr, localport, sourcePort,-2, seqNumber +1,true,false,false);
          SetState(States.ESTABLISHED);
        }

        break;

      case ESTABLISHED:
        if(p.finFlag){
          seqNumber = p.seqNum;
          sourcAddr = p.sourceAddr;
          ackNum = p.ackNum;
          sourcePort = p.sourcePort;
          SendPacket(false, lastpack1, sourcAddr, localport, sourcePort,-2, seqNumber +1,true,false,false);
          SetState(States.CLOSE_WAIT);
        }else if (p.ackFlag&&p.synFlag){
          SendPacket(false, lastpack2, sourcAddr, localport, sourcePort,-2, seqNumber +1,true,false,false);
        }
        break;

      case FIN_WAIT_1:
        if (p.ackFlag){
          SetState(States.FIN_WAIT_2);
          tcpTimer.cancel();
          tcpTimer = null;
        }
        else if(p.finFlag){
          seqNumber = p.seqNum;
          sourcAddr = p.sourceAddr;
          ackNum = p.ackNum;
          sourcePort = p.sourcePort;
          SendPacket(false, lastpack1, sourcAddr, localport, sourcePort,-2, seqNumber +1,true,false,false);
          SetState(States.CLOSING);
        }
        break;

      case FIN_WAIT_2:
        if (p.finFlag){
          seqNumber = p.seqNum;
          sourcAddr = p.sourceAddr;
          ackNum = p.ackNum;
          sourcePort = p.sourcePort;
          SendPacket(false, lastpack1, sourcAddr, localport, sourcePort,-2, seqNumber +1,true,false,false);
          if (tcpTimer != null) {
            tcpTimer.cancel();
            tcpTimer = null;
          }
          SetState(States.TIME_WAIT);
          createTimerTask(30*1000,null);
        }
        break;

      case CLOSING:
        if (p.finFlag) {
          SendPacket(true, lastpack2, sourcAddr,0,0,0,0,false,false,false);
        }
        else if (p.ackFlag){
          if (tcpTimer != null){
            tcpTimer.cancel();
            tcpTimer = null;
          }
          SetState(States.TIME_WAIT);
          createTimerTask(30*1000,null);
        }
        break;

      case TIME_WAIT:
        try {
          if (p.finFlag) {
            SendPacket(true, lastpack2, sourcAddr, 0, 0, 0, 0, false, false, false);
          }
        }catch (Exception e) {
          e.printStackTrace();
        }
        break;

      case CLOSE_WAIT:
        if(p.finFlag){
          SendPacket(true, lastpack2, sourcAddr,0,0,0,0,false,false,false);
        }
        break;

      case LAST_ACK:
        if (p.ackFlag){
          if (tcpTimer != null) {
            tcpTimer.cancel();
            tcpTimer = null;
          }
            SetState(States.TIME_WAIT);
            createTimerTask(30 * 1000, null);
        }
        if(p.finFlag){
          SendPacket(true, lastpack2, sourcAddr,0,0,0,0,false,false,false);
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
    D.registerListeningSocket(localport,this);
    SetState(States.LISTEN);
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
    if (this.state == null){
    }
    else if (this.state == state.ESTABLISHED) {
      SendPacket(false, lastpack1, sourcAddr, localport, sourcePort, -2, seqNumber + 1, false, false, true);
      SetState(state.FIN_WAIT_1);
    } else if (this.state == state.CLOSE_WAIT) {
      SendPacket(false, lastpack1, sourcAddr, localport, sourcePort, -2, seqNumber + 1, false, false, true);
      SetState(state.LAST_ACK);
    }

    try{
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

    tcpTimer.cancel();
    tcpTimer = null;

    if(this.state == state.TIME_WAIT){
      try {
        SetState(state.CLOSED);
      }
      catch (Exception e) {
        notifyAll();
      }

      notifyAll();
      try {
        D.unregisterConnection(sourcAddr, localport, sourcePort, this);
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
    else{
      SendPacket(true, lastpack1, sourcAddr, localport, sourcePort, 0, seqNumber, false, false, false);
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