import java.net.*;
import java.io.*;


abstract class BaseSocketImpl extends SocketImpl {

  // SocketImpl data members:
  //   protected InetAddress address;
  //   protected int port;
  //   protected int localport;

  /**
   * Creates either a stream or a datagram socket. 
   *
   * @param      stream   if <code>true</code>, create a stream socket;
   *                      otherwise, create a datagram socket.
   * @exception  IOException  if an I/O error occurs while creating the
   *               socket.
   */
  protected void create(boolean stream) throws IOException {
    if(!stream)
      throw new IOException("Datagram socket not implemented!");
  }
      

  /**
   * Connects this socket to the specified port on the named host. 
   *
   * @param      host   the name of the remote host.
   * @param      port   the port number.
   * @exception  IOException  if an I/O error occurs when connecting to the
   *               remote host.
   */
  protected void connect(String host, int port) throws IOException {
    connect(InetAddress.getByName(host), port);
  }

/**
  * Connects this socket to the specified port on the named host.
  * This method connects, but does not implement the timeout.
  *
  * @param address IP and port to connect to
  * @param timeout thrown away
  * @exception  IOException  if an I/O error occurs when connecting to the
  *               remote host.
  */
  protected void connect(SocketAddress address,
			 int timeout) throws IOException {
    connect(((InetSocketAddress)address).getAddress(),
	    ((InetSocketAddress)address).getPort());
  }
  
  /**
   * Binds this socket to the specified port number on the specified host. 
   *
   * @param      host   the IP address of the remote host.
   * @param      port   the port number.
   * @exception  IOException  if an I/O error occurs when binding this socket.
   */
  protected void bind(InetAddress host, int port) throws IOException {
    localport = port;
  }
  
  /**
   * Sets the maximum queue length for incoming connection indications 
   * (a request to connect) to the <code>count</code> argument. If a 
   * connection indication arrives when the queue is full, the 
   * connection is refused. 
   *
   * @param      backlog   the maximum length of the queue.
   * @exception  IOException  if an I/O error occurs when creating the queue.
   */
  protected void listen(int backlog) throws IOException {
    return;
  }
  
  

  /**
   * Returns the number of bytes that can be read from this socket
   * without blocking.
   *
   * @return     the number of bytes that can be read from this socket
   *             without blocking.
   * @exception  IOException  if an I/O error occurs when determining the
   *               number of bytes available.
   */
  protected int available() throws IOException {
    return getInputStream().available();
  }


  /**
   * Returns the value of this socket's <code>fd</code> field.
   *
   * @return  the value of this socket's <code>fd</code> field.
   * @see     java.net.SocketImpl#fd
   */
  protected FileDescriptor getFileDescriptor() {
    return new FileDescriptor(); // constructor creates invalid fd
  }


  /**
   * Accepts a connection. 
   *
   * @param      s   the accepted connection.
   * @exception  IOException  if an I/O error occurs when accepting the
   *               connection.
   */
  protected void accept(SocketImpl s) throws IOException {
    ((BaseSocketImpl)s).localport = localport;
    ((BaseSocketImpl)s).acceptConnection();
  }


  /**
   * ignore urgent data
   *
   * @param d byte of urgent data to (ignore in this case)
   * @exception IOException if an error occurs (it can't)
   */
  protected void sendUrgentData(int data) throws IOException {
  }


  protected abstract void acceptConnection() throws IOException;

  protected abstract void handleTimer(Object ref);

  public void setOption(int optID, Object value)
    throws SocketException {
    throw new SocketException("option not supported");
  } 
  
  
  public Object getOption(int optID) throws SocketException {
    throw new SocketException("option not supported");
  } 
    

}

