package org.bold.http;

import org.eclipse.jetty.server.Handler;

public interface GraphHandler extends Handler {
    public void addGraphListener(GraphListener listener);

    public void removeGraphListener(GraphListener listener);
}
