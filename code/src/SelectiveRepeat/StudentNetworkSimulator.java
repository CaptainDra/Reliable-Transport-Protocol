package SelectiveRepeat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class StudentNetworkSimulator extends NetworkSimulator
{
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B
     *
     * Predefined Member Methods:
     *
     *  void stopTimer(int entity):
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment):
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(String dataSent)
     *       Passes "dataSent" up to layer 5
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  int getTraceLevel()
     *       Returns TraceLevel
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      Message(String inputData):
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet that is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          chreate a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      int getPayload()
     *          returns the Packet's payload
     *
     */

    /*   Please use the following variables in your routines.
     *   int WindowSize  : the window size
     *   double RxmtInterval   : the retransmission timeout
     *   int LimitSeqNo  : when sequence number reaches this value, it wraps around
     */

    public static final int FirstSeqNo = 0;
    private int WindowSize;
    private double RxmtInterval;
    private int LimitSeqNo;


    // Add any necessary class variables here.  Remember, you cannot use
    // these variables to send messages error free!  They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)

    // This is the constructor.  Don't touch!
    public StudentNetworkSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   int seed,
                                   int winsize,
                                   double delay)
    {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
        WindowSize = winsize;
        LimitSeqNo = winsize*2; // set appropriately; assumes SR here!
        RxmtInterval = delay;
    }
    // A
    private int seqNo = 0;

    private LinkedList<Packet> senderBuffer = new LinkedList<>();
    private int head = 0;

    // init
    private boolean[] ACKed;
    Packet[] senderWindow;
    Packet[] receiverWindow;
    HashMap<Integer, Double> seqToTime = new HashMap<>();
    private int numOfOriginalTransPacket = 0;
    private int numOfCorruptedPacket = 0;
    private int numOfRetransmittedPacket = 0;
    private int numOfACKedPacket = 0;
    private int numOfDeliverTo5 = 0;
    private Packet lastReceivedPacket;
    private int lastReceivedSeq = -1;


    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    // where message is a structure of type msg, containing data to be sent to
    //the B-side.
    protected void aOutput(Message message)
    {
        String data = message.getData();
        //seq ack check playLoad
        Packet newPacket = new Packet(seqNo, -1, 0, data);
        newPacket.setChecksum(getCheckSumFromPacket(newPacket));
        senderBuffer.add(newPacket);
        seqNo++;

        bufferContent(head);
        sendAllInWindow();

    }


    // This routine will be called whenever a packet sent from the B-side
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {
//        1. check if the ACK packet is corrupted and take appropriate action
//        2. if get new ACK, slide window and sent new data packets waiting
//        3. if duplicate, retransmit first unACK data packet


            // If new ack, stop timer and update ack status
//            if (ACKed[packet.getAcknum() % WindowSize]) {
//                stopTimer(A);
//                ACKed[packet.getAcknum() % WindowSize] = false;
//
//                // If ack is head or greater, update head
//                if (packet.getAcknum() >= head) {
//                    if (packet.getAcknum() > head) {
//                        System.out.printf("A Input: Received Cumulative ACK | Expected: %d Received: %d\n", head, packet.getAcknum());
//                    } else {
//                        System.out.println("A Input: Received ACK: " + packet.getAcknum());
//                    }
//                    int oldHead = head;
//                    head = packet.getAcknum() + 1;
//
//                    // Slide window up then transmit new data packets from buffer
//                    for (int i = oldHead; i < head; i++) {
//                        ACKed[i % WindowSize] = false;
//                        senderWindow[i % WindowSize] = null;
//                    }
//
//                    // Add from buffer into window where space allows
//                    for (int i = head; i < head + WindowSize && i < senderBuffer.size(); i++) {
//                        if (senderWindow[i % WindowSize] == null) {
//                            senderWindow[i % WindowSize] = senderBuffer.get(i);
//                        }
//                    }
//
//                    // For every packet in window, check if unsent. If so, send
//                    for (int i = 0; i < ACKed.length; i ++) {
//                        if (senderWindow[i] != null && !ACKed[i]) {
//                            ACKed[i] = true;
//                            toLayer3(A, senderWindow[i]);
//                            seqToTime.put(senderWindow[i].getSeqnum(), getTime());
//                            numOfOriginalTransPacket++;
//                            stopTimer(A);
//                            startTimer(A, RxmtInterval);
//                        }
//                    }
//
//                }
//                // If duplicate ack, resend next packet
//            } else if (ACKed[packet.getAcknum() % WindowSize]) {
//                toLayer3(A, senderWindow[(packet.getAcknum() + 1) % WindowSize]);
//                seqToTime.put(senderWindow[(packet.getAcknum() + 1) % WindowSize].getSeqnum(), getTime());
//                numOfRetransmittedPacket++;
//                stopTimer(A);
//                startTimer(A, RxmtInterval);
//            }








        if (isCorrupted(packet)) {
            System.out.println("Receive corrupted packet");
            numOfCorruptedPacket++;
        } else {
            stopTimer(A);
            ACKed[packet.getAcknum() % WindowSize] = false;
            if (ACKed[packet.getAcknum() % WindowSize]) {
                if (packet.getAcknum() >= head) {
                    int oldHead = head;
                    head = packet.getAcknum() + 1;

                    for (int i = oldHead; i < head; i++) {
                        ACKed[i % WindowSize] = false;
                        senderWindow[i % WindowSize] = null;
                    }

                    bufferContent(head);

                    sendAllInWindow();

                }
            } else if (isDuplicated(packet)){
                toLayer3(A, senderWindow[(packet.getAcknum() + 1) % WindowSize]);
                seqToTime.put(senderWindow[(packet.getAcknum() + 1) % WindowSize].getSeqnum(), getTime());
                numOfRetransmittedPacket++;
                stopTimer(A);
                startTimer(A, RxmtInterval);
                }
            }
        }
//        if (ACKed[packet.getAcknum() % WindowSize]){
//            System.out.println("Receive duplicated packet");
//            stopTimer(A);
//            numOfRetransmittedPacket++;
//            //get last unACKed packet
//
//            for (int i = 0; i < senderWindow.length; i++) {
//                if (ACKed[i]) {
//                    continue;
//                }
//                if (senderWindow[i] == null) {
//                    continue;
//                }
//                toLayer3(A, senderWindow[i]);
//                ACKed[i] = true;
//                seqToTime.put(senderWindow[i].getSeqnum(), getTime());
//                startTimer(A, RxmtInterval);
//                return;
//            }
//        } else {
//            // new packet
//            stopTimer(A);
//            ACKed[packet.getAcknum() % WindowSize] = true;
//
//            if (packet.getAcknum() >= head) {
//
//                int oldHead = head;
//                head = packet.getAcknum() + 1;
//
//                // Slide window up then transmit new data packets from buffer
//                for (int i = oldHead; i < head; i++) {
//                    ACKed[i % WindowSize] = false;
//                    senderWindow[i % WindowSize] = null;
//                }
//
//                // Add from buffer into window where space allows
//                for (int i = head; i < head + WindowSize && i < senderBuffer.size(); i++) {
//                    if (senderWindow[i % WindowSize] == null) {
//                        senderWindow[i % WindowSize] = senderBuffer.get(i);
//                    }
//                }
//
//                // For every packet in window, check if unsent. If so, send
//                for (int i = 0; i < ACKed.length; i ++) {
//                    if (senderWindow[i] != null && !ACKed[i]) {
//                        ACKed[i] = true;
//                        toLayer3(A, senderWindow[i]);
//                        seqToTime.put(senderWindow[i].getSeqnum(), getTime());
//                        numOfOriginalTransPacket++;
//                        stopTimer(A);
//                        startTimer(A, RxmtInterval);
//                    }
//                }
//            }

//            System.out.println("Receive right packet");
//            stopTimer(A);
//            ACKed[packet.getAcknum() % WindowSize] = true;
//            if (packet.getAcknum() > head) {
//                System.out.println("Received Cumulative ACK");
//            } else {
//                System.out.println("Received ACK");
//            }
//
//            //slide window
//            for (int i = head; i < packet.getAcknum() + 1; i++) {
//                ACKed[i % WindowSize] = false;
//                senderWindow[i % WindowSize] = null;
//            }
//            head = packet.getAcknum() + 1;
//
//            bufferContent(head);
//            sendAllInWindow();
//        }


    // This routine will be called when A's timer expires (thus generating a
    // timer interrupt). You'll probably want to use this routine to control
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped.
    protected void aTimerInterrupt()
    {
        toLayer3(A, senderWindow[head % WindowSize]);
        seqToTime.put(senderWindow[head % WindowSize].getSeqnum(), getTime());
        numOfRetransmittedPacket++;
        stopTimer(A);
        startTimer(A, RxmtInterval);
    }

    // This routine will be called once, before any of your other A-side
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit()
    {
        senderWindow = new Packet[WindowSize];
        ACKed = new boolean[WindowSize]; // by initialization, all false
    }

    // This routine will be called whenever a packet sent from the B-side
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)
    {

//        1. Check if the packet is corrupted and take appropriate actions
//        2.If the data packet is new, and in-order, deliver the data to layer5 and send ACK to A.
//          Note that you might have subsequent data packets waiting in the buffer at B that also need to be delivered to layer5
//        3.If the data packet is new, and out of order, buffer the data packet and send an ACK
//        4.If the data packet is duplicate, drop it and send an ACK

        if (isCorrupted(packet)) {
            System.out.println("Receive corrupted packet");
            numOfCorruptedPacket++;
            return;
        }
        packet.setAcknum(packet.getSeqnum());
        packet.setChecksum(getCheckSumFromPacket(packet));
        if (packet.getSeqnum() == lastReceivedSeq) {
            System.out.println("Receive duplicated packet");
            toLayer3(B, lastReceivedPacket);
            numOfACKedPacket++;
        } else if (packet.getSeqnum() == lastReceivedSeq + 1) {
            toLayer5(packet.getPayload());
            numOfDeliverTo5++;
            lastReceivedPacket = packet;
            lastReceivedSeq++;
            numOfACKedPacket++;
            while (receiverWindow[(lastReceivedSeq + 1) % WindowSize] != null &&
                    receiverWindow[(lastReceivedSeq + 1) % WindowSize].getSeqnum() == (lastReceivedSeq + 1)) {
                toLayer5(receiverWindow[(lastReceivedSeq + 1) % WindowSize].getPayload());
                numOfDeliverTo5++;
                lastReceivedPacket = receiverWindow[(lastReceivedSeq + 1) % WindowSize];
                numOfACKedPacket++;
                lastReceivedSeq++;
            }
            toLayer3(B, lastReceivedPacket);
        } else {
            receiverWindow[packet.getSeqnum() % WindowSize] = packet;
            toLayer3(B, lastReceivedPacket);
            numOfACKedPacket++;
        }

    }


    // This routine will be called once, before any of your other B-side
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit()
    {
        receiverWindow = new Packet[WindowSize];
    }

    // Use to print final statistics
    protected void Simulation_done()
    {
        int totalPacket = numOfOriginalTransPacket + numOfRetransmittedPacket + numOfACKedPacket;
        double lostRatio = (numOfRetransmittedPacket - numOfCorruptedPacket) / (double) totalPacket;
        double corruptionRatio = (numOfCorruptedPacket) / (double) totalPacket;
        // TO PRINT THE STATISTICS, FILL IN THE DETAILS BY PUTTING VARIBALE NAMES. DO NOT CHANGE THE FORMAT OF PRINTED OUTPUT
        System.out.println("\n\n===============STATISTICS=======================");
        System.out.println("Number of original packets transmitted by A:" + numOfOriginalTransPacket);
        System.out.println("Number of retransmissions by A:" + numOfRetransmittedPacket);
        System.out.println("Number of data packets delivered to layer 5 at B:" + numOfDeliverTo5);
        System.out.println("Number of ACK packets sent by B:" + numOfACKedPacket);
        System.out.println("Number of corrupted packets:" + numOfCorruptedPacket);
        System.out.println("Ratio of lost packets:" + lostRatio);
        System.out.println("Ratio of corrupted packets:" + corruptionRatio);
        System.out.println("Average RTT:" + "<YourVariableHere>");
        System.out.println("Average communication time:" + "<YourVariableHere>");
        System.out.println("==================================================");

        // PRINT YOUR OWN STATISTIC HERE TO CHECK THE CORRECTNESS OF YOUR PROGRAM
        System.out.println("\nEXTRA:");
        // EXAMPLE GIVEN BELOW
        //System.out.println("Example statistic you want to check e.g. number of ACK packets received by A :" + "<YourVariableHere>");
    }

    private int getCheckSumFromPacket (Packet packet) {
        String payLoad = packet.getPayload();
        int checkSum = 0;
        for (int i = 0; i < payLoad.length(); i++) {
            checkSum += (int)payLoad.charAt(i);
        }
        checkSum += packet.getSeqnum() + packet.getAcknum();
        return checkSum;
    }

    private boolean isCorrupted (Packet packet) {
        return getCheckSumFromPacket(packet) != packet.getChecksum();
    }

    private boolean isDuplicated (Packet packet) {
        return ACKed[packet.getAcknum() % WindowSize];
    }

    private void bufferContent(int windowHead) {
        for (int i = head; i < head + WindowSize && i < senderBuffer.size(); i++) {
            if (senderWindow[i % WindowSize] == null) {
                senderWindow[i % WindowSize] = senderBuffer.get(i);
            }
        }
//        int iter = windowHead;
//        int end = windowHead + WindowSize;
//        while (iter < end && iter < senderBuffer.size()) {
//            if (senderWindow[iter % WindowSize] != null) {
//                iter++;
//                continue;
//            }
//            senderWindow[iter % WindowSize] = senderBuffer.get(iter);
//            iter++;
//        }
    }

    private void sendAllInWindow() {
        for (int i = 0; i < senderWindow.length; i++) {
            if (senderWindow[i] == null) {
                continue;
            }
            if (ACKed[i]) {
                continue;
            }
            toLayer3(A, senderWindow[i]);
            ACKed[i] = true;
            seqToTime.put(senderWindow[i].getSeqnum(), getTime());
            numOfOriginalTransPacket++;
            stopTimer(A);
            startTimer(A, RxmtInterval);
        }
    }

}