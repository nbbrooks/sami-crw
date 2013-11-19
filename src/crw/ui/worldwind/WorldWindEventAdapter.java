/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package crw.ui.worldwind;

import gov.nasa.worldwind.awt.AbstractViewInputHandler;
import gov.nasa.worldwind.awt.KeyEventState;
import gov.nasa.worldwind.awt.MouseInputActionHandler;
import gov.nasa.worldwind.awt.ViewInputAttributes;
import gov.nasa.worldwind.geom.Position;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Random;

/**
 *
 * @author nbb
 */
public class WorldWindEventAdapter implements MouseInputActionHandler {

    Random rand = new Random();
    private Position down = null;
    
    @Override
    public boolean inputActionPerformed(KeyEventState arg0, String arg1, ViewInputAttributes.ActionAttributes arg2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean inputActionPerformed(AbstractViewInputHandler arg0, MouseEvent arg1, ViewInputAttributes.ActionAttributes arg2) {
//            if(rand.nextDouble() > 0.5) {
//                System.out.println("true");
//            System.out.println(arg0 + " + " + arg1 + " + " + arg2);
//            return handler.inputActionPerformed(arg0, arg1, arg2);
//            }
//            System.out.println("false");
//            return false;


//            if(arg1.getID() == MouseEvent.MOUSE_DRAGGED) {
//                Position cur = wwd.getView().getCurrentEyePosition();
//                Position targetWithoutElv = wwd.getView().computePositionFromScreenPoint(arg1.getX(), arg1.getY());
//                Position target = new Position(targetWithoutElv.getLatitude(), targetWithoutElv.getLongitude(), cur.elevation);
//                wwd.getView().setEyePosition(target);
//                return true;
//            }
//            return false;
        

        System.out.println("" + arg1);
        if (arg1.getID() == MouseEvent.MOUSE_PRESSED) {
            System.out.println("pressed");
            if (down == null) {
                System.out.println("set");
                down = arg0.getWorldWindow().getView().getCurrentEyePosition();
                return true;
            }
        } else if (arg1.getID() == MouseEvent.MOUSE_DRAGGED) {
            System.out.println("dragged");
            if (down != null) {
                System.out.println("move");
                Position targetWithoutElv = arg0.getWorldWindow().getView().computePositionFromScreenPoint(arg1.getX(), arg1.getY());
                Position target = new Position(targetWithoutElv.getLatitude(), targetWithoutElv.getLongitude(), down.elevation);
                arg0.getWorldWindow().getView().setEyePosition(target);
                return true;
            }
        } else if (arg1.getID() == MouseEvent.MOUSE_RELEASED) {
            System.out.println("released");
            down = null;
        }
        return false;
    }

    @Override
    public boolean inputActionPerformed(AbstractViewInputHandler arg0, MouseWheelEvent arg1, ViewInputAttributes.ActionAttributes arg2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}