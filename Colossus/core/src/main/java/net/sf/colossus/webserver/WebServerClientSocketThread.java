package net.sf.colossus.webserver;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.webcommon.IWebClient;


/**
 *  Thread to handle one user client connection at the WebServer side.
 *  Reads always one line from the socket, hands it over to the actual
 *  WebServerClient to parse and handle it.
 *
 *  @author Clemens Katzer
 */
public class WebServerClientSocketThread extends Thread
{
    private static final Logger LOGGER = Logger
        .getLogger(WebServerClientSocketThread.class.getName());

    private static final long PING_REQUEST_INTERVAL_SECONDS = 60;
    private static final int PING_MAX_TRIES = 3;

    private static final int IDLE_WARNING_INTERVAL_MINUTES = 10;
    private static final int IDLE_WARNING_MAXCOUNT = 12;

    private final long MAX_WRITE_BLOCKTIME_MS = 1000;


    private final WebServerClient theClient;

    private final RoundtripTimeBookkeeper rttBookKeeper;

    private Socket socket;
    private PrintWriter out;

    private long lastPacketReceived = 0;
    private int pingsTried = 0;
    private int idleWarningsSent = 0;
    private boolean connLostWarningLogged = false;

    private Thread stopper = null;

    private boolean forcedLogout = false;

    private boolean done = false;
    private boolean toldToTerminate = false;


    /* During registration request and sending of confirmation code,
     * we do not have a user yet. The parseLine sets then this variable
     * according to the username argument which was send from client.
     */

    public WebServerClientSocketThread(WebServerClient theClient, Socket socket)
    {
        super("WebServerClientSocketThread");
        this.theClient = theClient;
        this.socket = socket;
        this.rttBookKeeper = new RoundtripTimeBookkeeper(10);
    }

    String getClientInfo()
    {
        String ip = "<undef>";
        if (socket != null && socket.getInetAddress() != null)
        {
            ip = socket.getInetAddress().toString();
        }
        return theClient.getUsername() + " (IP=" + ip + ")";
    }

    // A runnable that will shutdown the whole server
    public void createStopper(Runnable r)
    {
        stopper = new Thread(r);
    }

    public void tellToTerminate()
    {
        done = true;
        toldToTerminate = true;
        closeAndCleanupSocket();
    }


    private synchronized void closeAndCleanupSocket()
    {
        LOGGER.info("close & cleanup for WSCST " + this.getName());

        /*
         * If we were told to terminate (by another connection of same
         * user, or by server when server wants to shutdown), then
         * tellToTerminate did get us out of the readLine by closing socket,
         * and in this case closeAndCleanup is not needed any more.
         */
        if (socket != null)
        {
            try
            {
                out.println(IWebClient.connectionClosed);
                socket.close();
            }
            catch (IOException e)
            {
                LOGGER.log(Level.WARNING, "IOException while socket.close() "
                    + " is executed by thread "
                    + Thread.currentThread().getName()
                    + "socket in " + this.getName(), e);
            }
            finally
            {
                socket = null;
            }
        }
        else
        {
            LOGGER.info("No need to close&cleanup in thread " + this.getName()
                + " - socket already null!");
        }
    }

    /**
     * prepare socket to read/write, and then loop as long
     * as lines from client come, and parse them
     */
    @Override
    public void run()
    {
        BufferedReader in;

        String fromClient = "dummy";

        LOGGER.log(Level.FINEST, "A new WSCST started running: "
            + this.toString());
        try
        {
            in = new BufferedReader(new InputStreamReader(socket
                .getInputStream()));
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                socket.getOutputStream())), true);
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.WARNING,
                "Preparing socket streams caused IOException: ", ex);
            return;
        }

        while (!done && fromClient != null)
        {
            fromClient = null;
            try
            {
                // when remote admin user requested shutdown, the method
                // called by parseLine() created the stopper Runnable;

                if (stopper != null)
                {
                    stopper.start();
                    stopper = null;
                }
                fromClient = in.readLine();
            }

            catch (InterruptedIOException e)
            {
                LOGGER.log(Level.WARNING,
                    "run().main loop, InterruptedIOException: ", e);
                done = true;
            }
            catch (SocketException ex)
            {
                if (toldToTerminate)
                {
                    LOGGER.info("OK, toldToTerminate set and we got "
                        + "SocketException ('" + ex.getMessage()
                        + "') in WSCST " + getClientInfo()
                        + " - setting done to true.");
                }
                else
                {
                    LOGGER.warning("Strange. ToldToTerminate is not set "
                        + "but we got " + "SocketException ('"
                        + ex.getMessage() + "') in WSCST " + getClientInfo()
                        + " - setting done to true anyway.");
                }
                done = true;
            }
            catch (IOException e)
            {
                if (isInterrupted())
                {
                    LOGGER.log(Level.WARNING, "IOException and isInterrupted "
                        + "set - ups? Stack trace: ", e);
                }
                else
                {
                    LOGGER.log(Level.WARNING, "IOException was NOT caused by "
                        + "being interrupted? Stack trace: ", e);
                }
            }
            catch (Exception e)
            {
                LOGGER.log(Level.SEVERE, "Exception ('" + e.getMessage()
                    + "') in WSCST " + getClientInfo(), e);
            }

            if (fromClient != null)
            {
                lastPacketReceived = new Date().getTime();
                Throwable caught = null;
                try
                {
                    done = theClient.parseLine(fromClient);
                }
                catch (Throwable t)
                {
                    caught = t;
                }

                String tmpUsername = "<unknown>";
                if (theClient.getUser() != null)
                {
                    tmpUsername = theClient.getUsername();
                }
                else if (theClient.getUnverifiedUsername() != null)
                {
                    tmpUsername = theClient.getUnverifiedUsername();
                }
                else
                {
                    LOGGER.warning("Try to get username, but user and "
                        + "unverifiedUsername are both null?");
                }

                if (caught != null)
                {
                    LOGGER.log(Level.SEVERE, "WSCST, during parseline, "
                        + "for user " + tmpUsername
                        + ": caught throwable! Setting done to true.", caught);
                    done = true;
                }

                if (done)
                {
                    LOGGER.finest("user " + tmpUsername + ": parseLine for '"
                        + fromClient + "' returns done = " + done);
                }
            }
            else
            {
                LOGGER.finest("fromClient is null; setting done = true.");
                done = true;
            }
        }

        // Shut down the client.
        LOGGER.log(Level.FINEST, "(Trying to) shut down the client for user "
            + getClientInfo());

        // Will close and cleanup socket, if still needed
        closeAndCleanupSocket();

        theClient.handleLogout();
    }

    /**
     * Send the given string/message over the socket to the client
     * Calculate the time how long it took to write it to the socket,
     * and log a warning if it was blocked in the write for more than
     * MAX_WRITE_BLOCKTIME_MS milliseconds.
     * @param s
     */
    public void sendToClient(String s)
    {
        long writeStartedAt = new Date().getTime();
        out.println(s);
        long elapsedTime = new Date().getTime() - writeStartedAt;
        if (elapsedTime > MAX_WRITE_BLOCKTIME_MS)
        {
            int len = s.length();
            len = (len < 30 ? len : 30);
            String msg = s.substring(0, len);
            String threadName = Thread.currentThread().getName();
            LOGGER.warning("Thread " + threadName
                + " in sendToClient for user " + theClient.getUsername()
                + " took " + elapsedTime + " milliseconds! (" + msg + ")");
        }
    }

    /**
     * Simply forward the RTT entry creation to the RTT bookkeeper
     * @param requestResponseArriveTime When response arrived
     * @param roundtripTime Actual roundtrip time
     */
    public void storeEntry(long requestResponseArriveTime, long roundtripTime)
    {
        rttBookKeeper.storeEntry(requestResponseArriveTime, roundtripTime);
    }


    public void requestPingIfNeeded(long now)
    {
        long deltaMillis = now - lastPacketReceived;

        if (deltaMillis >= (pingsTried + 1) * PING_REQUEST_INTERVAL_SECONDS
            * 1000)
        {
            // Only clients >= 2 have this feature
            if (theClient.getClientVersion() >= 2)
            {
                // too many already done without response => assume dead
                if (pingsTried >= PING_MAX_TRIES)
                {
                    if (!connLostWarningLogged)
                    {
                        connLostWarningLogged = true;
                        LOGGER
                            .warning("After "
                                + pingsTried
                                + " pings, still no response from client "
                                + theClient.getUsername()
                                + " - would assume now connection lost and closing it.");
                    }
                    /*
                    String message = "@@@ Hello " + getUsername() + ", after "
                    + pingsTried
                    + " ping requests, still no response from your "
                    + "WebClientclient - assuming connection problems "
                    + "and closing connection from server side. @@@";
                    chatDeliver(IWebServer.generalChatName, now, "SYSTEM",
                        message, false);
                    if (!done)
                    {
                        // this requestPingIfNeeded code is run in the WatchDog
                        // thread; the interrupt interrupts the actual
                        // WebServerClientThread.
                        this.interrupt();
                        // prevent from checking the maxIdleMinutes stuff...
                        return;
                    }
                    else
                    {
                        LOGGER
                            .warning("done already true, let's skip the interrupting...");
                    }
                    */
                }
                // otherwise, send another one
                else
                {
                    long requestSentTime = new Date().getTime();
                    theClient.requestPing(requestSentTime + "", "dummy2",
                        "dummy3");
                    pingsTried++;
                }
            }
        }
        else if (deltaMillis >= pingsTried * PING_REQUEST_INTERVAL_SECONDS
            * 1000)
        {
            // not time for next ping, but still no response
        }
        else
        {
            // idle time < previous request time: got something
            pingsTried = 0;
        }
        return;
    }

    private void markForcedLogout()
    {
        forcedLogout = true;
    }

    boolean wasForcedLogout()
    {
        return forcedLogout;
    }

    protected void forceLogout(WebServerClientSocketThread other)
    {
        if (other == null)
        {
            LOGGER.log(Level.WARNING,
                "In forceLogout(), parameter other is null!");
            return;
        }

        try
        {
            other.markForcedLogout();
            other.sendToClient(IWebClient.forcedLogout);
            other.tellToTerminate();
            LOGGER.info("Forcing logout, before other.join()");
            other.join();
            LOGGER.info("Forcing logout, after  other.join()");
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING,
                "Oups couldn't stop the other WebServerClientSocketThread", e);
        }
    }

    public void clearIdleWarningsSent()
    {
        idleWarningsSent = 0;
    }

    /**
     * Currently this will log out only older clients, because they do not
     * respond to the ping packets.
     * TODO in future, distinct between ping packets and all other
     * activities, and log out user which hasn't done anything and left
     * WebClient standing around idle for very long.
     * @param now
     */
    public void checkMaxIdleTime(long now)
    {
        if (done)
        {
            // already gone, probably because we just logged him out because
            // of too many missing ping requests.
            return;
        }
        long deltaMillis = now - lastPacketReceived;
        if (theClient.getUser() != null && theClient.getLoggedIn())
        {
            LOGGER.finest("Checking maxIdleTime of client "
                + theClient.getUsername() + ": " + (deltaMillis / 1000)
                + " seconds");
        }
        else
        {
            LOGGER.info("When trying to check maxIdleTime of client, "
                + "user null or not logged in ?!? ...");
            return;
        }

        long idleSeconds = deltaMillis / 1000;
        int idleMinutes = (int)(idleSeconds / 60);

        if (idleWarningsSent >= IDLE_WARNING_MAXCOUNT)
        {
            LOGGER.info("Client " + theClient.getUsername()
                + " has been idle " + idleMinutes
                + " minutes - logging him out!");
            String message = "@@@ Hello " + theClient.getUsername()
                + ", you have been " + idleMinutes
                + " minutes idle; server will log you out now! @@@";
            theClient.systemMessage(now, message);
            this.interrupt();

        }
        else if (idleSeconds >= (idleWarningsSent + 1)
            * IDLE_WARNING_INTERVAL_MINUTES * 60)
        {
            String message = "@@@ Hello " + theClient.getUsername()
                + ", you have been " + idleMinutes + " minutes idle; after "
                + (IDLE_WARNING_MAXCOUNT * IDLE_WARNING_INTERVAL_MINUTES)
                + " minutes idle time WebClient server will log you out!"
                + " (Type or do something to prevent that...) @@@";
            theClient.systemMessage(now, message);
            idleWarningsSent++;
            LOGGER.fine("Idle warning sent to user " + theClient.getUsername()
                + ", idleWarnings now " + idleWarningsSent);
        }
    }

}
