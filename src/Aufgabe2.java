import org.oxoo2a.sim4da.Message;
import org.oxoo2a.sim4da.Node;
import org.oxoo2a.sim4da.Simulator;

import java.util.*;


class Aufgabe2 {
    public static class Actor extends Node {

        private final String[] networkNodeNames;
        private boolean isActive;
        private Random random;
        private  Message message;
        private double sendingProbability;
        public Actor(String name, String[] networkNodeNames) {
            super(name);
            this.networkNodeNames = networkNodeNames;
            this.isActive = true;
            random = new Random();
            message = new Message();
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
                    System.out.println(NodeName() + " received message from " + message.queryHeader("sender"));
                    isActive = true;
                    System.out.println(NodeName() + " is active!");
                    sendMessageToSubset(sendingProbability /= 2);
                    isActive = false;
                    System.out.println(NodeName() + " is passive!");
                }
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
            }
        }
    }

    public static void main(String[] args) {

        final int networkSize = 5;
        String[] nodeNames = new String[networkSize];

        //Create node name list
        for (int i = 0; i < networkSize; i++) nodeNames[i] = "ActorNode_" + i;
        for (String nodeName : nodeNames) new Actor(nodeName, nodeNames);

        Simulator simulator = Simulator.getInstance();
        simulator.simulate();
        simulator.shutdown();
    }
}
