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
    // private int N; // assume N is equal to WindowSize
    private int left; // left pointer for the windows
    private int seqNo;
    private ArrayList<Packet> buffer; // using arraylist to perform the buffer work
    private int buffMaximum; // max storage of buffer
    private Queue<Message> Disk; // Using disk to store the message when buffer is full, not work for this assignment
    private  int seqPtr; // pointer for sender windows
    private double waitTime; // value for startTimer()
    private int[] sackA = new int[5]; // SACK we in sender
    private HashMap<Integer, Double> send; // hashmap to save the original sending time
    private HashMap<Integer, Double> get; // hashmap to save the time we get the ACK for the first time
    private double[] RTTBegin = new double[1000];
    private double[] RTTEnd = new double[1000];

    // GBN B
    private int sequenceNoExpected; // sequence number in B side
    private int[] sack = new int[5]; // sack to store the 5 latest packet's sequence number
    private int count; // pointer for sack
    private HashMap<Integer,String> bufferB; // buffer in B to store packet

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
        // when a new message come from layer5
        // If the buffer is full, add the message into disk
        // otherwise, transfer the message into packet and save that in buffer
        System.out.println("A: get message "+ message.getData());
        if(buffer.size() < buffMaximum + left + WindowSize){
            String data = message.getData();
            int seqA = buffer.size();
            int ACK = -1;
            int check = getCheckSumFromMessage(data) + seqA + ACK;
            buffer.add(new Packet(seqA,ACK,check,data));
            try{
                // send all of the packet in the sender's windows
                while(seqPtr < left + WindowSize){
                    if (seqPtr < buffer.size())
                        System.out.println("A: Sending packet " + seqPtr + " to receiver");
                    toLayer3(A, buffer.get(seqPtr));
                    RTTBegin[buffer.get(seqPtr).getSeqnum()] = getTime();
                    if (left == seqPtr){
                        // start the timer for the first packet in the windows
                        startTimer(A,waitTime);
                        rttStarted = getTime();
                    }
                    double time = getTime();
                    if(!send.containsKey(seqPtr)) send.put(seqPtr,time);
                    seqPtr++;
                    originalPacketsNumber++;
                }
            }catch (IndexOutOfBoundsException e){
                System.err.println("A: Window and buffer are empty.");
            }
        }
        else{
            System.out.println("A: Buffer is full, saving the packet in Disk.");
            Disk.add(message);
        }
    }

    // This routine will be called whenever a packet sent from the B-side
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {
        // When a packet come to sender from layer3:
        // 1. check whether the packet is corrupted
        // 2. if the packet is correct, save the sack number to local sack[]
        // 3. if the correct ACK packet is for the left one in the windows, move the left to the next packet unAck
        System.out.println("A: Packet from layer 3 has been received");
        if (isCorrupted(packet)) {
            corruptNum++;
            System.out.println("\033[31;4m" + "A: Packet corrupted!" + "\033[0m");
        } else {
            System.out.println("A: ACK packet correct");
            sackA = packet.getSackNum();
            int seq = packet.getSeqnum();
            System.out.println("A: SACK: " + sackA[0] +", " + sackA[1] +", " + sackA[2] +", " + sackA[3] +", " + sackA[4]);
            System.out.println("A: time: " + getTime() +" - " + RTTBegin[seq]);
            // save value to calculate RTT and communication time
            totalRtt += getTime() - RTTBegin[seq];
            totalRttTime++;
            if(!get.containsKey(seq)){
                get.put(seq,getTime());
            }
            if(left == packet.getSeqnum()) left++;
            // if left one is correct, we should stop the timer
            if (left == seqPtr){
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
        // when time out, restart timer and send all packets in the windows(also not in SACK)
        System.out.println("A: The timer was interrupted, resending the message.");
        rttStarted = getTime();
        Set<Integer> q = new HashSet<>();
        System.out.println("A: SACK: " + sackA[0] +", " + sackA[1] +", " + sackA[2] +", " + sackA[3] +", " + sackA[4]);
        for(int i = 0; i < 5; i++){
            if(sackA[i] != -1) q.add(sackA[i]);
        }
        for (int i = left; i < seqPtr; i++) {
            System.out.println("A: Retransmitting unacknowledged packet " + i + "." + sequenceNoExpected);
            if(q.contains(i)){
                System.out.println("A: in SACK" + i + ".");

            }
            stopTimer(A);
            startTimer(A, waitTime);
            toLayer3(A,buffer.get(i));
            RTTBegin[i] = getTime();
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
        seqNo= 0;
        left = 0;
        buffMaximum = 100;
        buffer = new ArrayList<>();
        Disk = new LinkedList<>();
        seqPtr = 0;
        waitTime = 1 * RxmtInterval;
        for(int i = 0; i < 5; i++){
            sackA[i] = -1;
        }
        send = new HashMap<>();
        get = new HashMap<>();
    }

    // This routine will be called whenever a packet sent from the B-side
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)
    {
        // when a message come to B from layer3
        // 1. check the whether the packet is corrupted
        // 2. if the packet is correct, save the message in packet in buffer at first
        // 3. if the sequence number is correct, send all messages after this which is in order to Layer5
        System.out.println("B: Package from A was received through layer 3 ("+packet.getPayload()+").");
        if (isCorrupted(packet)) {
            System.out.println("\033[31;4m" + "B: Packet corrupted!" + "\033[0m");
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
            System.out.println("B: SACK: " + sack[0] +", " + sack[1] +", " + sack[2] +", " + sack[3] +", " + sack[4]);
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
        double RTT = totalRtt/(double) totalRttTime;
        double com = 0;
        int num = 0;
        for(int i = 0; i < originalPacketsNumber; i++){
            if(send.containsKey(i) && get.containsKey(i)){
                com += get.get(i) - send.get(i);
                num++;
            }
        }
        com = com/ (double)num;

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
        System.out.println("Average RTT:"+RTT );
        System.out.println("Average communication time:" + com);
        System.out.println("==================================================");

        // PRINT YOUR OWN STATISTIC HERE TO CHECK THE CORRECTNESS OF YOUR PROGRAM
        System.out.println("\nEXTRA:");
        // EXAMPLE GIVEN BELOW
        //System.out.println("Example statistic you want to check e.g. number of ACK packets received by A :" + "<YourVariableHere>");
    }
    /**
     * get checkSum from a packet without extract the message
     * @param packet
     * @return the checksum
     */
    private int getCheckSumFromPacket (Packet packet) {
        String payLoad = packet.getPayload();
        int checkSum = 0;
        for (int i = 0; i < payLoad.length(); i++) {
            checkSum += (int) payLoad.charAt(i);
        }
        checkSum += packet.getSeqnum() + packet.getAcknum();
        return checkSum;
    }

    /**
     * get checkSum from a String
     * @param data
     * @return checksum
     */
    private  int getCheckSumFromMessage(String data){
        int checkSum = 0;
        for (int i = 0; i < data.length(); i++) {
            checkSum += (int) data.charAt(i);
        }
        return checkSum;
    }

    /**
     * check the checksum and the packet
     * @param packet
     * @return true if checksum equal to the packet checksum
     */
    private boolean isCorrupted (Packet packet) {
        return getCheckSumFromPacket(packet) != packet.getChecksum();
    }
    private boolean corrupt(Packet p, int receiver) {
        int toCompare = p.getAcknum() + p.getSeqnum();
        int checksum = p.getChecksum();
        for (char c : p.getPayload().toCharArray()) {
            toCompare += Character.getNumericValue(c);
        }
        return checksum != toCompare;

    }
}