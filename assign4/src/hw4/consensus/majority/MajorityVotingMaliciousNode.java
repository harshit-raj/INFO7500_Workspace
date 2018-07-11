package hw4.consensus.majority;

import hw4.net.Message;
import hw4.net.Send;
import hw4.net.Node;

import java.util.ArrayList;
import java.util.List;

public class MajorityVotingMaliciousNode extends Node {

    public MajorityVotingMaliciousNode() {

    }

    @Override
    public List<Send> send(int round) {
        List<Send> sends = new ArrayList();

        return sends;
    }

    @Override
    public void receive(int round, List<Message> messages) {

    }

    @Override
    public void commit() {

    }

    public void addSybil(MajorityVotingMaliciousNode n) {

    }
}
