package crw.ui.teleop;

/**
 * A interface for a class that provides thrust and/or rudder values to the VelocityPanel
 * 
 * @author nbb
 */
public interface TeleopSourceInt {

    public void enable(boolean enabled);

    public void setActive(boolean active);
}
