package de.fau.wintechis.sim;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.event.NotifyingRepositoryConnection;
import org.eclipse.rdf4j.repository.event.RepositoryConnectionListener;
import org.eclipse.rdf4j.repository.event.base.RepositoryConnectionListenerAdapter;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

class UpdateHistory extends Stack<UpdateHistory.UpdateSequence> implements History, SailConnectionListener {

    enum UpdateOperation { INSERT, DELETE }

    abstract class Update {

        private final Statement statement;

        Update(Statement st) {
            this.statement = st;
        }

        abstract public UpdateOperation getOperation();

        public Statement getStatement() {
            return statement;
        }

    }

    class Insert extends Update {

        public Insert(Statement st) {
            super(st);
        }

        @Override
        public UpdateOperation getOperation() {
            return UpdateOperation.INSERT;
        }

    }

    class Delete extends Update {

        public Delete(Statement st) {
            super(st);
        }

        @Override
        public UpdateOperation getOperation() {
            return UpdateOperation.DELETE;
        }

    }

    class UpdateSequence extends ArrayList<Update> {}

    public UpdateHistory() {
        this.clear();
    }

    @Override
    public void timeIncremented() {
        this.add(new UpdateSequence());
    }

    @Override
    public void clear() {
        super.clear();
        this.add(new UpdateSequence());
    }

    @Override
    public void statementAdded(Statement st) {
        this.peek().add(new Insert(st));
    }

    @Override
    public void statementRemoved(Statement st) {
        this.peek().add(new Delete(st));
    }

    // TODO ignore statements that were first inserted and then removed in the same time slot

}
