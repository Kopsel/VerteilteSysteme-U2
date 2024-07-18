import org.oxoo2a.sim4da.Message;
import org.oxoo2a.sim4da.Node;
import org.oxoo2a.sim4da.Simulator;

import java.util.*;


class Aufgabe3 {
    public static class Actor extends Node {

        private final String[] networkNodeNames;
        private boolean isActive;
        private Random random;
        private  Message message;
        private double sendingProbability;
        private int messagesSent;
        private int messagesReceived;

        public Actor(String name, String[] networkNodeNames) {
            super(name);
            this.networkNodeNames = networkNodeNames;
            this.isActive = true;
            random = new Random();
            message = new Message();
            messagesSent = 0;
            messagesReceived = 0;
            sendingProbability = 0.8;
        }
        @Override
        protected void engage() {

            // Wait random time (max 1 minute)
            Random random = new Random();
            sleep(random.nextInt(60001));
            System.out.println(NodeName() + " has finished waiting!");

            sendMessageToSubset(1.0);
            System.out.println(NodeName() + " is passive!");
            isActive = false;

            while (true) {
                message = receive();
                if (message.query("payload").equals("Firework")){
                    messagesReceived++;
                    System.out.println(NodeName() + " received message from " + message.queryHeader("sender"));
                    isActive = true;
                    System.out.println(NodeName() + " is active!");
                    sendMessageToSubset(sendingProbability /= 2);
                    isActive = false;
                    System.out.println(NodeName() + " is passive!");
                }
                else if (message.query("payload").equals("Control")){
                    Message controlMessage = new Message();
                    controlMessage.add("sent", messagesSent);
                    controlMessage.add("received", messagesReceived);
                    controlMessage.add("isActive", String.valueOf(isActive));
                    sendBlindly(controlMessage, message.queryHeader("sender"));
                }
                else if (message.query("payload").equals("Termination")) return;
            }
        }

        /**
         * Sends the message "Firework" to a random subset of available
         * network nodes with the probability of p.
         * @param probability Value range between 0 and 1.0
         */
        private void sendMessageToSubset(double probability) {

            if (probability < 0.0 || probability > 1.0) return;
            if (random.nextDouble() > probability) {
                System.out.println(NodeName() + " is not sending again!");
                return;
            }

            // Pick random subset of network nodes to send to
            List<String> shuffledList = new LinkedList<>(Arrays.asList(networkNodeNames));
            Collections.shuffle(shuffledList);
            int subsetSize = random.nextInt(shuffledList.size() + 1);
            List<String> subsetList = shuffledList.subList(0, subsetSize);
            subsetList.remove(NodeName());
            System.out.println(NodeName() + " is now sending messages to the following nodes: " + subsetList);

            // Send message
            message.add("payload", "Firework");
            for (String node : subsetList) {
                sendBlindly(message, node);
                messagesSent++;
            }
        }
    }

    /**
     * Double Count Method: Two rounds of counting the sum of ingoing and outgoing
     * messages. The second round starts after every node has responded to the first round.
     * This guarantees that messages still travelling during the first round multicast
     * are detected by the second round. If two consecutive counting rounds lead to the
     * same metrics (all nodes passive and sum of ingoing message = sum of outgoing messages)
     * the distributed system has terminated.
     */
    public static class Observer extends Node {

        private final String[] networkNodeNames;
        private boolean terminated;

        public Observer(String name, String[] networkNodeNames) {
            super(name);
            this.networkNodeNames = networkNodeNames;
        }

        @Override
        protected void engage() {
            int lastRoundMessageReceived = -1;
            int lastRoundMessageSent = -1;
            int lastRoundPassiveNodes = -1;

            Message controlMessageIn;
            Message controlMessageOut = new Message();
            controlMessageOut.add("payload", "Control");

            do {
                int currentRoundMessageReceived = 0;
                int currentRoundMessageSent = 0;
                int currentRoundPassiveNodes = 0;

                for (String node : networkNodeNames) {
                    sendBlindly(controlMessageOut, node);
                }

                for (String node : networkNodeNames) {
                    controlMessageIn = receive();
                    currentRoundMessageReceived += controlMessageIn.queryInteger("received");
                    currentRoundMessageSent += controlMessageIn.queryInteger("sent");
                    if (!Boolean.parseBoolean(controlMessageIn.query("isActive"))) currentRoundPassiveNodes++;
                }

                //Check for termination
                if (currentRoundMessageReceived == lastRoundMessageReceived &&
                    lastRoundMessageReceived == currentRoundMessageSent &&
                    currentRoundMessageSent == lastRoundMessageSent &&
                    currentRoundPassiveNodes == lastRoundPassiveNodes &&
                    lastRoundPassiveNodes == networkNodeNames.length) {
                    terminated = true;
                }
                else {
                    lastRoundMessageReceived = currentRoundMessageReceived;
                    lastRoundMessageSent = currentRoundMessageSent;
                    lastRoundPassiveNodes = currentRoundPassiveNodes;
                }

            } while (!terminated);

            System.out.println("The system has terminated!");
            notifyTerminateActors();
        }

        private void notifyTerminateActors() {
            Message terminationMessage = new Message();
            terminationMessage.add("payload", "Termination");
            for (String node : networkNodeNames) {
                sendBlindly(terminationMessage, node);
            }
        }
    }

    public static void main(String[] args) {

        final int networkSize = 5;
        String[] nodeNames = new String[networkSize];

        //Create node name list
        for (int i = 0; i < networkSize; i++) nodeNames[i] = "ActorNode_" + i;
        for (String nodeName : nodeNames) new Actor(nodeName, nodeNames);

        //Create observer
        Observer observer = new Observer("Observer", nodeNames);

        Simulator simulator = Simulator.getInstance();
        simulator.simulate();
        simulator.shutdown();
    }
}
