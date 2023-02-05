
//BR// Trait to allow a piece to be forced de-selected and dropped from its stack

package ForThePeople;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Point;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import java.util.ArrayList;
import java.util.List;

import VASSAL.build.module.Map;
import VASSAL.build.module.documentation.HelpFile;
import VASSAL.command.ChangeTracker;
import VASSAL.command.Command;
import VASSAL.configure.NamedHotKeyConfigurer;
import VASSAL.counters.Decorator;
import VASSAL.counters.GamePiece;
import VASSAL.counters.KeyBuffer;
import VASSAL.counters.KeyCommand;
import VASSAL.counters.PieceEditor;
import VASSAL.counters.Properties;
import VASSAL.counters.DragBuffer;
import VASSAL.counters.Stack;
import VASSAL.i18n.TranslatablePiece;
import VASSAL.tools.NamedKeyStroke;
import VASSAL.tools.SequenceEncoder;
import VASSAL.i18n.PieceI18nData;
import VASSAL.configure.StringConfigurer;


public class Deselect extends Decorator implements TranslatablePiece {
  public static final String ID = "deselect;";
  protected KeyCommand[] command;
  protected String commandName;
  protected NamedKeyStroke key;
  protected KeyCommand deselectCommand;

  public Deselect() {
    this(ID + "Deselect;K", null);
  }

  public Deselect(String type, GamePiece inner) {
    mySetType(type);
    setInner(inner);
  }

  public void mySetType(String type) {
    type = type.substring(ID.length());
    SequenceEncoder.Decoder st = new SequenceEncoder.Decoder(type, ';');
    commandName = st.nextToken();
    key = st.nextNamedKeyStroke('K');
    command = null;
  }

  public String myGetType() {
    SequenceEncoder se = new SequenceEncoder(';');
    se.append(commandName).append(key);
    return ID + se.getValue();
  }

  protected KeyCommand[] myGetKeyCommands() {
    if (command == null) {
      deselectCommand = new KeyCommand(commandName, key, Decorator.getOutermost(this), this);
      if (commandName.length() > 0 && key != null && ! key.isNull()) {
        command =
            new KeyCommand[]{deselectCommand};
      }
      else {
        command = new KeyCommand[0];
      }
    }
    if (command.length > 0) {
      command[0].setEnabled(getMap() != null);
    }
    return command;
  }

  public String myGetState() {
    return "";
  }

  public Command myKeyEvent(KeyStroke stroke) {
    Command c = null;
    myGetKeyCommands();
    if (deselectCommand.matches(stroke)) {
      GamePiece outer = Decorator.getOutermost(this);
      
      final Map m = getMap();      

      Stack stack = outer.getParent();      //BR// If we're now being dragged around as part of a stack                                            
      if (stack != null) {
    	Point pos = outer.getPosition();    //BR// Figure out where stack was/is
        stack.setExpanded(true);            //BR// Expand the stack
        stack.remove(outer);                //BR// Remove our piece from the stack
        c = m.placeAt(outer, pos);          //BR// Put it back on the map so it won't be missing
      }
      outer.setProperty(Properties.SELECTED, new Boolean(false)); //BR// Mark as not selected
      DragBuffer.getBuffer().remove(outer); //BR// Remove from the drag buffer
      KeyBuffer.getBuffer().remove(outer);  //BR//                                             
    }
    return c;
  }

  public void mySetState(String newState) {
  }

  public Rectangle boundingBox() {
    return piece.boundingBox();
  }

  public void draw(Graphics g, int x, int y, Component obs, double zoom) {
    piece.draw(g, x, y, obs, zoom);
  }

  public String getName() {
    return piece.getName();
  }

  public Shape getShape() {
    return piece.getShape();
  }

  public PieceEditor getEditor() {
    return new Ed(this);
  }

  public String getDescription() {
    return "Deselect";
  }

  public HelpFile getHelpFile() {
    return HelpFile.getReferenceManualPage("GamePiece.htm","");
  }

  public PieceI18nData getI18nData() {
    return getI18nData(commandName, "Deselect command");
  }

  public static class Ed implements PieceEditor {
    private StringConfigurer nameInput;
    private NamedHotKeyConfigurer keyInput;
    private JPanel controls;

    public Ed(Deselect p) {
      controls = new JPanel();
      controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

      nameInput = new StringConfigurer(null, "Command name:  ", p.commandName);
      controls.add(nameInput.getControls());

      keyInput = new NamedHotKeyConfigurer(null,"Keyboard Command:  ",p.key);
      controls.add(keyInput.getControls());
    }

    public Component getControls() {
      return controls;
    }

    public String getType() {
      SequenceEncoder se = new SequenceEncoder(';');
      se.append(nameInput.getValueString()).append(keyInput.getValueString());
      return ID + se.getValue();
    }

    public String getState() {
      return "";
    }
  }
  
  
  /**
   * Return Property names exposed by this trait
   */
  public List<String> getPropertyNames() {
    ArrayList<String> l = new ArrayList<String>();
    l.add(Properties.SELECTED);
    return l;
  }
}






