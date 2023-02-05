/*
 * $Id: MenuDisplayer.java 8393 2012-10-04 16:14:24Z uckelman $
 *
 * Copyright (c) 2000-2012 by Rodney Kinney, Brent Easton
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License (LGPL) as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, copies are available
 * at http://www.opensource.org.
 */
package ForThePeople;


//BR// (1) Clear a global property before each group-piece operation launched from here
//BR// (2) Clean up the ugly and over-long default hotkey names

import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import VASSAL.build.Buildable;
import VASSAL.build.GameModule;
import VASSAL.build.module.Map;
import VASSAL.build.module.properties.MutableProperty;
import VASSAL.build.module.map.DrawPile;
import VASSAL.command.Command;
import VASSAL.command.NullCommand;
import VASSAL.counters.Deck;
import VASSAL.counters.EventFilter;
import VASSAL.counters.GamePiece;
import VASSAL.counters.KeyBuffer;
import VASSAL.counters.KeyCommand;
import VASSAL.counters.KeyCommandSubMenu;
import VASSAL.counters.PieceFinder;
import VASSAL.counters.Properties;

public class FTPMenuDisplayer extends MouseAdapter implements Buildable {
  public static Font POPUP_MENU_FONT = new Font("Dialog", 0, 10);

  protected Map map;
  protected PieceFinder targetSelector;

  public void addTo(Buildable b) {
    targetSelector = createTargetSelector();
    map = (Map) b;
    map.addLocalMouseListener(this);
  }

  /**
   * Return a {@link PieceFinder} instance that will select a
   * {@link GamePiece} whose menu will be displayed when the
   * user clicks on the map
   * @return
   */
  protected PieceFinder createTargetSelector() {
    return new PieceFinder.PieceInStack() {
      public Object visitDeck(Deck d) {
        Point pos = d.getPosition();
        Point p = new Point(pt.x - pos.x, pt.y - pos.y);
        if (d.getShape().contains(p)) {
          return d;
        }
        else {
          return null;
        }
      }
    };
  }

  public void add(Buildable b) {
  }

  public Element getBuildElement(Document doc) {
    return doc.createElement(getClass().getName());
  }

  public void build(Element e) {
  }

  public static JPopupMenu createPopup(GamePiece target) {
    return createPopup(target, false);
  }


	
  //BR// Clean up the ugly default hotkey names
  public static String fixMenuString(String target) {
	  String fixed = target;
	  fixed = fixed.replace("NUMPAD_+", "Num+PLUS");
	  fixed = fixed.replace("NUMPAD_-", "Num-MINUS");
	  fixed = fixed.replace("PAGE_UP", "PGUP");
	  fixed = fixed.replace("PAGE_DOWN", "PGDN");	  
	  return fixed;
  }
  
  //BR// Clears a global property flag once per stack move (to avoid duplication of certain global key commands)
  //BR// A trigger processing this can then set the flag to indicate the once-per-stack-move event has been handled.
  public static class FTPClearer implements ActionListener
  {
	public void actionPerformed(ActionEvent e) {
		
	  Command comm = null;
	  
	  MutableProperty.Impl existingValue = (MutableProperty.Impl) GameModule.getGameModule().getMutableProperty("ClearedOncePerStackMove");
	  if (existingValue != null) {
	    comm = existingValue.setPropertyValue("0");	
	  }
	  
	  MutableProperty.Impl existingValue2 = (MutableProperty.Impl) GameModule.getGameModule().getMutableProperty("SizeOfStackMove");
	  if (comm != null && existingValue2 != null) {
		//BR// An absolutely appalling way to get the number of pieces being sent the command.
		int size = 0;
		Iterator<GamePiece> i = KeyBuffer.getBuffer().getPiecesIterator();
		while (i.hasNext() ) {
			size++;
			i.next();
		}
			
		comm = comm.append(existingValue2.setPropertyValue(Integer.toString(size)));
		//GameModule.getGameModule().sendAndLog(existingValue2.setPropertyValue(Integer.toString(size)));
	  }
	  
	  if ((comm != null) && !comm.isNull()) {
		  GameModule.getGameModule().sendAndLog(comm);
	  }
	}
  }

  /**
  *
  * @param target
  * @param global If true, then apply the KeyCommands globally,
  * i.e. to all selected pieces
  * @return
  */
  public static JPopupMenu createPopup(GamePiece target, boolean global) {
    JPopupMenu popup = new JPopupMenu();
    KeyCommand c[] = (KeyCommand[]) target.getProperty(Properties.KEY_COMMANDS);
    if (c != null) {
      ArrayList<JMenuItem> commands = new ArrayList<JMenuItem>();
      ArrayList<KeyStroke> strokes = new ArrayList<KeyStroke>();      

      // Maps instances of KeyCommandSubMenu to corresponding JMenu
      HashMap<KeyCommandSubMenu,JMenu> subMenus =
        new HashMap<KeyCommandSubMenu,JMenu>();
      // Maps name to a list of commands with that name
      HashMap<String,ArrayList<JMenuItem>> commandNames =
        new HashMap<String,ArrayList<JMenuItem>>();

      for (int i = 0; i < c.length; ++i) {
        c[i].setGlobal(global);
        KeyStroke stroke = c[i].getKeyStroke();
        JMenuItem item = null;
        if (c[i] instanceof KeyCommandSubMenu) {
          JMenu subMenu = new JMenu(fixMenuString(c[i].getLocalizedMenuText())); //BR//
          subMenu.setFont(POPUP_MENU_FONT);
          subMenus.put((KeyCommandSubMenu) c[i], subMenu);
          item = subMenu;
          commands.add(item);
          strokes.add(KeyStroke.getKeyStroke('\0'));
        }
        else {
          if (strokes.contains(stroke)) {
            JMenuItem command = commands.get(strokes.indexOf(stroke));
            Action action = command.getAction();
            if (action != null) {
              String commandName =
                (String) command.getAction().getValue(Action.NAME);
              if (commandName == null ||
                  commandName.length() < c[i].getName().length()) {
                item = new JMenuItem(fixMenuString(c[i].getLocalizedMenuText())); //BR//
                item.addActionListener(c[i]);
                if (!(target instanceof DrawPile)) {
                  item.addActionListener(new FTPClearer()); //BR//
                }
                item.setFont(POPUP_MENU_FONT);
                item.setEnabled(c[i].isEnabled());
                commands.set(strokes.indexOf(stroke), item);
              }
            }
          }
          else {
            strokes.add(stroke != null ? stroke : KeyStroke.getKeyStroke('\0'));
            item = new JMenuItem(fixMenuString(c[i].getLocalizedMenuText()));     //BR//
            item.addActionListener(c[i]);
            if (!(target instanceof DrawPile)) {
              item.addActionListener(new FTPClearer()); //BR//
            }
            item.setFont(POPUP_MENU_FONT);
            item.setEnabled(c[i].isEnabled());
            commands.add(item);
          }
        }
        if (c[i].getName() != null &&
            c[i].getName().length() > 0 &&
            item != null) {
          ArrayList<JMenuItem> l = commandNames.get(c[i].getName());
          if (l == null) {
            l = new ArrayList<JMenuItem>();
            commandNames.put(c[i].getName(), l);
          }
          l.add(item);
        }
      }

      // Move commands from main menu into submenus
      for (java.util.Map.Entry<KeyCommandSubMenu,JMenu> e :
                                                        subMenus.entrySet()) {
        final KeyCommandSubMenu menuCommand = e.getKey();
        final JMenu subMenu = e.getValue();

        for (Iterator<String> it2 = menuCommand.getCommands(); it2.hasNext();) {
          final ArrayList<JMenuItem> matchingCommands =
            commandNames.get(it2.next());
          if (matchingCommands != null) {
            for (JMenuItem item : matchingCommands) {
              subMenu.add(item);
              commands.remove(item);
            }
          }
        }
      }

      // Promote contents of a single submenu [Removed as per Bug 4775]
      // if (commands.size() == 1) {
      //   if (commands.get(0) instanceof JMenu) {
      //     JMenu submenu = (JMenu) commands.get(0);
      //     commands.remove(submenu);
      //     for (int i = 0; i < submenu.getItemCount(); i++) {
      //       commands.add(submenu.getItem(i));
      //     }
      //   }
      // }

      for (JMenuItem item : commands) {
        popup.add(item);
      }
    }
    return popup;
  }
  
  
  public void mouseReleased(MouseEvent e) {
    if (e.isMetaDown()) {
      final GamePiece p = map.findPiece(e.getPoint(), targetSelector);
      if (p != null) {
        EventFilter filter = (EventFilter) p.getProperty(Properties.SELECT_EVENT_FILTER);
        if (filter == null
            || !filter.rejectEvent(e)) {
          JPopupMenu popup = createPopup(p, true);
          Point pt = map.componentCoordinates(e.getPoint());
          popup.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled
                (javax.swing.event.PopupMenuEvent evt) {
              map.repaint();
            }

            public void popupMenuWillBecomeInvisible
                (javax.swing.event.PopupMenuEvent evt) {
              map.repaint();
            }

            public void popupMenuWillBecomeVisible
                (javax.swing.event.PopupMenuEvent evt) {
            }
          });
          // It is possible for the map to close before the menu is displayed
          if (map.getView().isShowing()) {
            popup.show(map.getView(), pt.x, pt.y);
          }
          e.consume();
        }
      }
    }
  }

}
