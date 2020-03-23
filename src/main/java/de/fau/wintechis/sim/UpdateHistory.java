package de.fau.wintechis.sim;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.sail.SailConnectionListener;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

class UpdateHistory extends Stack<UpdateHistory.Update> implements SailConnectionListener {

    class Update {

        private final Set<Statement> insertions = new HashSet<>();

        private final Set<Statement> deletions = new HashSet<>();

        public Set<Statement> getInsertions() {
            return insertions;
        }

        public Set<Statement> getDeletions() {
            return deletions;
        }
    }

    public UpdateHistory() {
        this.add(new Update());
    }

    public void timeIncremented() {
        this.add(new Update());
    }

    @Override
    public void statementAdded(Statement st) {
        this.peek().getInsertions().add(st);
    }

    @Override
    public void statementRemoved(Statement st) {
        this.peek().getDeletions().add(st);
    }

}
