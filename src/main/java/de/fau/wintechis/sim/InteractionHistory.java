package de.fau.wintechis.sim;

import de.fau.wintechis.gsp.GraphStoreListener;
import org.eclipse.rdf4j.model.IRI;

import java.util.Stack;

public class InteractionHistory extends Stack<InteractionHistory.InteractionCounter> implements History, GraphStoreListener {

    class InteractionCounter {

        private int retrievals = 0;

        private int updates = 0;

        private int deletions = 0;

        private int extensions = 0;

        public void incrementRetrievals() {
            retrievals++;
        }

        public void incrementUpdates() {
            updates++;
        }

        public void incrementDeletions() {
            deletions++;
        }

        public void incrementExtensions() {
            extensions++;
        }

        public int getRetrievals() {
            return retrievals;
        }

        public int getUpdates() {
            return updates;
        }

        public int getDeletions() {
            return deletions;
        }

        public int getExtensions() {
            return extensions;
        }
    }

    public InteractionHistory() {
        this.clear();
    }

    @Override
    public void timeIncremented() {
        this.add(new InteractionCounter());
    }

    @Override
    public void clear() {
        super.clear();
        this.add(new InteractionCounter());
    }

    @Override
    public void graphRetrieved(IRI graphName) {
        this.peek().incrementRetrievals();
    }

    @Override
    public void graphUpdated(IRI graphName) {
        this.peek().incrementUpdates();
    }

    @Override
    public void graphDeleted(IRI graphName) {
        this.peek().incrementDeletions();
    }

    @Override
    public void graphExtended(IRI graphName) {
        this.peek().incrementExtensions();
    }
}
