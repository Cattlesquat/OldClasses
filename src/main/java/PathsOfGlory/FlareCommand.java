// 
// Decompiled by Procyon v0.5.36
// 

package PathsOfGlory;

import java.awt.Point;
import VASSAL.command.Command;

class FlareCommand extends Command
{
    private Flare finder;
    private Point clickPoint;
    
    public FlareCommand(final Flare finder) {
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
