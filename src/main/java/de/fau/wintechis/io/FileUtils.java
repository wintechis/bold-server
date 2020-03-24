package de.fau.wintechis.io;

import de.fau.wintechis.sim.SimulationEngine;

import java.io.*;
import java.net.URL;

public class FileUtils {

    /**
     * First tries to open the file from the file system. If it does not exist, interpret it as a resource file.
     *
     * @param filename name of the file or resource
     * @return an input stream pointing to the content of the file or resource
     * @throws IOException
     */
    public static InputStream getFileOrResource(String filename) throws IOException {
        File f = new File(filename);
        URL url = SimulationEngine.class.getClassLoader().getResource(filename);

        return f.exists() ? new FileInputStream(f) : url.openStream();
    }

    /**
     * Buffers the content of an input stream into a string.
     *
     * @param is the input stream
     * @return the content of the stream buffered into a string
     * @throws IOException
     */
    public static String asString(InputStream is) throws IOException {
        StringWriter w = new StringWriter();

        int buf = -1;
        while ((buf = is.read()) > -1) w.write(buf);

        return w.toString();
    }

}
