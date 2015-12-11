package crw.event.output.proxy;

import java.awt.Color;

/**
 *
 * @author nbb
 */
public class BoatProxyId implements java.io.Serializable {

//    static final long serialVersionUID = 0L;

    public String name;
    public Color color;
    public String server;
    public String imageStorageDirectory;

    public BoatProxyId() {
    }

    public String toString() {
        return "BoatProxyId [" + name + ", " + color + ", " + server + ", " + imageStorageDirectory + "]";
    }
}
