package hw4.consensus.bft;

import hw4.net.Message;
import hw4.net.Send;
import hw4.net.Node;

import java.util.*;

public class UnauthBFTNode extends Node {

    public UnauthBFTNode() {
    }

    @Override
    public List<Send> send(int round) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public void receive(int round, List<Message> messages) {

    }

    @Override
    public void commit() {

    }
}
