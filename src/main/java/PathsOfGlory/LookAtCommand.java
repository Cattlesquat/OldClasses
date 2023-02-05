// 
// Decompiled by Procyon v0.5.36
// 

package PathsOfGlory;

import java.awt.Point;
import VASSAL.command.Command;

class LookAtCommand extends Command
{
    private LookAt finder;
    private Point clickPoint;
    
    public LookAtCommand(final LookAt finder) {
        this.clickPoint = new Point(finder.getClickPoint());
        this.finder = finder;
    }
    
    protected void executeCommand() {
        this.finder.setClickPoint(this.clickPoint);
        this.finder.startAnimation(false);
    }
    
    protected Command myUndoCommand() {
        return null;
    }
    
    public int getValue() {
        return 0;
    }
    
    public Point getClickPoint() {
        return this.clickPoint;
    }
}
