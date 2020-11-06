package GoBackN;

import java.util.*;
import java.io.*;

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

    // GBN A
    private int N;
    private int left;
    private int seqNo;
    private ArrayList<Packet> buffer;
    private int buffMaximum;
    private Queue<Message> Disk;
    private  int seqPtr;
    private double waitTime;
    private int[] sackA = new int[5];
    private int tmp;


    // GBN B
    private int sequenceNoExpected;
    private int[] sack = new int[5];
    private int count;
    private HashMap<Integer,String> bufferB;

    // result
    private int originalPacketsNumber = 0;
    private int retransmissionsNumber = 0;
    private int dataTo5AtB = 0;
    private int ACKByB = 0;
    private double rttStarted;
    private double totalRtt = 0.0;
    private int totalRttTime = 0;
    private int corruptNum = 0;


    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message)
    {
        System.out.println("A: get message "+ message.getData());
        if(buffer.size() < buffMaximum + left + WindowSize){
            String data = message.getData();
            int seqA = buffer.size();
            int ACK = seqA;
            int check = getCheckSumFromMessage(data) + seqA + ACK;
            buffer.add(new Packet(seqA,ACK,check,data));
            try{
                while(seqPtr < left + WindowSize){
                    if (seqPtr < buffer.size())
                        System.out.println("Sender: Sending packet " + seqPtr + " to receiver");
                    toLayer3(A, buffer.get(seqPtr));
                    if (left == seqPtr){
                        startTimer(A,waitTime);
                        rttStarted = getTime();
                    }

                    seqPtr++;
                    originalPacketsNumber++;
                    //tmp = left;
                }
            }catch (IndexOutOfBoundsException e){
                System.err.println("Sender: Window and buffer are empty. No more packets to send.");
            }
        }
        else{
            System.out.println("A: Buffer is full, saving the packet in Disk.");
            //Disk.add(message);
        }
    }

    // This routine will be called whenever a packet sent from the B-side
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {
        System.out.println("A: Packet from layer 3 has been received");
        if (isCorrupted(packet)) {
            corruptNum++;
            System.out.println("A: Packet corrupted!");
        } else {
            System.out.println("Sender: ACK packet correct");
            sackA = packet.getSackNum();
            if(left == packet.getSeqnum()) left++;
            if (left == seqPtr){
                totalRtt = totalRtt + getTime() - rttStarted;
                totalRttTime++;
                stopTimer(A);
            }

        }
    }

    // This routine will be called when A's timer expires (thus generating a
    // timer interrupt). You'll probably want to use this routine to control
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped.
    protected void aTimerInterrupt()
    {
        System.out.println("A: The timer was interrupted, resending the message.");
        startTimer(A, waitTime);
        rttStarted = getTime();
        Set<Integer> q = new HashSet<>();
        for(int i = 0; i < 5; i++){
            if(sackA[i] != -1) q.add(sackA[i]);
        }
        for (int i = left; i < seqPtr; i++) {
            System.out.println("A: Retransmitting unacknowledged packet " + i + ".");
            if(q.contains(i)) continue;
            toLayer3(A,buffer.get(i));
            //rttStarted[buffer.get(i).getSeqnum()] = getTime();
            retransmissionsNumber++;
        }
    }

    // This routine will be called once, before any of your other A-side
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit()
    {
        System.out.println("A: init");
        //N = 5;
        seqNo= 0;
        left = 0;
        //WindowSize = 8;
        buffMaximum = 50;
        buffer = new ArrayList<>();
        // Disk = new LinkedList<>();
        seqPtr = 0;
        //RxmtInterval = 200.0;
        waitTime = 5 * RxmtInterval;
        for(int i = 0; i < 5; i++){
            sackA[i] = -1;
        }
    }

    // This routine will be called whenever a packet sent from the B-side
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)
    {
        System.out.println("B: Package from A was received through layer 3 ("+packet.getPayload()+").");

        if (isCorrupted(packet)) {
            System.out.println("B: Packet corrupted!");
            if (isCorrupted(packet))
                corruptNum++;
            //return;
        } else {
            System.out.println("B: Packet received from A checks out.");
            String data = packet.getPayload();
            sack[count] = packet.getSeqnum();
            count++;
            count = count%5;
            bufferB.put(packet.getSeqnum(),data);
            //toLayer5(data);
            int seqB = packet.getSeqnum();
            int ACK = seqB;
            String message = "";
            int check = getCheckSumFromMessage(message) + seqB + ACK;
            Packet newPacket = new Packet(seqB,ACK,check,message,sack);
            toLayer3(B, newPacket);
            while(bufferB.containsKey(sequenceNoExpected)){
                System.out.println("B: toLayer5: " + sequenceNoExpected);
                toLayer5(bufferB.get(sequenceNoExpected));
                sequenceNoExpected++;
                dataTo5AtB++;
            }
            ACKByB++;
        }


    }

    // This routine will be called once, before any of your other B-side
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit()
    {
        System.out.println("B: init");
        sequenceNoExpected = 0;
        for(int i = 0; i < 5; i++){
            sack[i] = -1;
        }
        count = 0;
        bufferB = new HashMap<>();
    }

    // Use to print final statistics
    protected void Simulation_done()
    {
        // TO PRINT THE STATISTICS, FILL IN THE DETAILS BY PUTTING VARIBALE NAMES. DO NOT CHANGE THE FORMAT OF PRINTED OUTPUT
        int totalPacket = originalPacketsNumber + retransmissionsNumber + ACKByB;
        double lostRatio = (retransmissionsNumber - corruptNum) / (double) totalPacket;
        double corruptionRatio = (corruptNum) / (double) (totalPacket - (retransmissionsNumber - corruptNum));


        System.out.println("\n\n===============STATISTICS=======================");
        System.out.println("Number of original packets transmitted by A:" + originalPacketsNumber);
        System.out.println("Number of retransmissions by A:" + retransmissionsNumber);
        System.out.println("Number of data packets delivered to layer 5 at B:" + dataTo5AtB);
        System.out.println("Number of ACK packets sent by B:" + ACKByB);
        System.out.println("Number of corrupted packets:" + corruptNum);
//        System.out.println("Ratio of lost packets:" +  (double)(retransmissionsNumber-numOfCorruptedPacket)/(double)(originalPacketsNumber));
//        System.out.println("Ratio of corrupted packets:" + (double)(corruptNum)/(double)(originalPacketsNumber));
        System.out.println("Ratio of lost packets:" +  lostRatio);
        System.out.println("Ratio of corrupted packets:" + corruptionRatio);
        System.out.println("Average RTT:" );
        System.out.println("Average communication time:" + "<YourVariableHere>");
        System.out.println("==================================================");

        // PRINT YOUR OWN STATISTIC HERE TO CHECK THE CORRECTNESS OF YOUR PROGRAM
        System.out.println("\nEXTRA:");
        // EXAMPLE GIVEN BELOW
        //System.out.println("Example statistic you want to check e.g. number of ACK packets received by A :" + "<YourVariableHere>");
    }
    private Packet makePacket(String container, int sender, int seqNo, int ACKNum) {
        int checksum = getCheckSumFromMessage(container) + seqNo + ACKNum + sender;

        return new Packet(seqNo, ACKNum, checksum, container);
    }
    private int getCheckSumFromPacket (Packet packet) {
        String payLoad = packet.getPayload();
        int checkSum = 0;
        for (int i = 0; i < payLoad.length(); i++) {
            checkSum += (int) payLoad.charAt(i);
        }
        checkSum += packet.getSeqnum() + packet.getAcknum();
        return checkSum;
    }

    private  int getCheckSumFromMessage(String data){
        int checkSum = 0;
        for (int i = 0; i < data.length(); i++) {
            checkSum += (int) data.charAt(i);
        }
        return checkSum;
    }

    private boolean isCorrupted (Packet packet) {
        return getCheckSumFromPacket(packet) != packet.getChecksum();
    }
    protected boolean corrupt(Packet p, int receiver) {
        int toCompare = p.getAcknum() + p.getSeqnum();
        int checksum = p.getChecksum();
        // if (receiver == B) {
        for (char c : p.getPayload().toCharArray()) {
            toCompare += Character.getNumericValue(c);
        }
        // }
        return checksum != toCompare;

    }
}