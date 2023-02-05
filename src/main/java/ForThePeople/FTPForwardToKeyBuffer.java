/*
 * $Id: ForwardToKeyBuffer.java 7738 2011-08-03 18:38:35Z uckelman $
 *
 * Copyright (c) 2000-2003 by Rodney Kinney
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

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.util.Iterator; //BR//

import javax.swing.KeyStroke;

import VASSAL.build.Buildable;
import VASSAL.build.GameModule;
import VASSAL.build.module.Map;
import VASSAL.command.Command;
import VASSAL.command.NullCommand; //BR//
import VASSAL.counters.KeyBuffer;
import VASSAL.counters.PieceIterator; //BR//
import VASSAL.counters.GamePiece;

import VASSAL.build.module.map.ForwardToKeyBuffer;
import VASSAL.build.module.properties.MutableProperty;



/**
 * This KeyListener forwards key event from a {@link Map} to the
 * {@link KeyBuffer}, where it is given to selected GamePieces to
 * interpret.  The event is forwarded only if not consumed
 *
 * @see KeyBuffer
 * @see VASSAL.counters.GamePiece#keyEvent
 * @see InputEvent#isConsumed */
public class FTPForwardToKeyBuffer extends VASSAL.build.module.map.ForwardToKeyBuffer implements Buildable, KeyListener {
  private KeyEvent lastConsumedEvent;
	
  protected void process(KeyEvent e) {
	  
	Command comm = new NullCommand();
	
    //BR// Clears a global property flag once per stack move (to avoid duplication of certain global key commands)
    //BR// A trigger processing this can then set the flag to indicate the once-per-stack-move event has been handled.
    MutableProperty.Impl existingValue = (MutableProperty.Impl) GameModule.getGameModule().getMutableProperty("ClearedOncePerStackMove");
    if (existingValue != null) {
      comm = comm.append(existingValue.setPropertyValue("0"));    	
      //GameModule.getGameModule().sendAndLog(existingValue.setPropertyValue("0"));	
    }
    
	MutableProperty.Impl existingValue2 = (MutableProperty.Impl) GameModule.getGameModule().getMutableProperty("SizeOfStackMove");
	if (existingValue2 != null) {
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
	
    // If we've consumed a KeyPressed event,
    // then automatically consume any following KeyTyped event
    // resulting from the same keypress
    // This prevents echoing characters to the Chat area if they're keycommand for selected pieces
    if (lastConsumedEvent != null
        && lastConsumedEvent.getWhen() == e.getWhen()) {
      e.consume();
    }
    else {
      lastConsumedEvent = null;
    }
    final int c = e.getKeyCode();
  //  // Don't pass SHIFT or CONTROL only to counters
    if (!e.isConsumed() && c != KeyEvent.VK_SHIFT && c != KeyEvent.VK_CONTROL) {
      comm = comm.append(KeyBuffer.getBuffer().keyCommand
          (KeyStroke.getKeyStrokeForEvent(e)));
      if (comm != null && !comm.isNull()) {
        GameModule.getGameModule().sendAndLog(comm);
        e.consume();
        lastConsumedEvent = e;
      }
    }

    //super.process(e);
  }
  
}
