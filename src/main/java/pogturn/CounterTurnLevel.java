/*
 * $Id: CounterTurnLevel.java 7755 2011-08-03 22:17:56Z uckelman $
 *
 * Copyright (c) 2005 by Rodney Kinney, Brent Easton
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

package pogturn;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import VASSAL.build.module.documentation.HelpFile;
import VASSAL.configure.IntConfigurer;
import VASSAL.configure.VisibilityCondition;
import VASSAL.tools.ArrayUtils;
import VASSAL.tools.SequenceEncoder;

public class CounterTurnLevel extends TurnLevel {

  protected static final String START = "start"; //$NON-NLS-1$
  protected static final String INCR = "incr"; //$NON-NLS-1$
  protected static final String LOOP = "loop"; //$NON-NLS-1$
  protected static final String LOOP_LIMIT = "loopLimit"; //$NON-NLS-1$

  protected int incr = 1;
  protected boolean loop = false;
  protected int loopLimit = -1;

  public CounterTurnLevel() {
    super();
  }

  /*
   *  Reset counter to initial state
   */
  protected void reset() {
    super.reset();
    setLow();
  }

  protected void setLow() {
    current = start;
    super.setLow();
  }

  protected void setHigh() {
    current = loopLimit;
    super.setHigh();
  }
  /*
   * Generate the state of the level
   */
  protected String getState() {
    SequenceEncoder se = new SequenceEncoder(';');
    se.append(current);
    se.append(currentSubLevel);
    se.append(loop);
    se.append(loopLimit);
    for (int i = 0; i < getTurnLevelCount(); i++) {
      se.append(getTurnLevel(i).getState());
    }
    return se.getValue();
  }

  /*
   * Set the state of the level
   */
  protected void setState(String code) {
    SequenceEncoder.Decoder sd = new SequenceEncoder.Decoder(code, ';');
    current = sd.nextInt(start);
    currentSubLevel = sd.nextInt(0);  // Change to 0 as default due to issue 3500
    loop = sd.nextBoolean(false);
    loopLimit = sd.nextInt(-1);
    for (int i = 0; i < getTurnLevelCount(); i++) {
      getTurnLevel(i).setState(sd.nextToken("")); //$NON-NLS-1$
    }
    myValue.setPropertyValue(getValueString());
  }

  protected String getValueString() {
    return String.valueOf(current);
  }

  /* (non-Javadoc)
   * @see turn.TurnLevel#getLongestValueName()
   */
  protected String getLongestValueName() {
    return start < 10000 ? "9999" : String.valueOf(start); //$NON-NLS-1$
  }

  /*
   * Advance this level.
   * 1. If there are any sub-levels, Advance the current sub-level first.
   * 2. If the sublevels roll over, then advance the counter
   * 3. If LOOP is reached, roll over the counter
   */
  protected void advance() {
    // Advance sub-levels
    super.advance();

    // If no sub-levels, or they rolled over, advance this level
    if (getTurnLevelCount() == 0 || (getTurnLevelCount() > 0 && hasSubLevelRolledOver())) {
      current += incr;
      if (loop && current > loopLimit) {
        current = start;
        setRolledOver(true);
      }
    }
    myValue.setPropertyValue(getValueString());
  }

  protected void retreat() {
    // Retreat sub-levels
    super.retreat();

    // If no sub-levels, or they rolled over, retreat this level
    int oldCurrent = current;
    if (getTurnLevelCount() == 0 || (getTurnLevelCount() > 0 && hasSubLevelRolledOver())) {
      current -= incr;
      if (loop && oldCurrent <= start) {
        current = loopLimit;
        setRolledOver(true);
      }
    }
    myValue.setPropertyValue(getValueString());
  }

  protected Component getSetControl() {

    final IntConfigurer config = new IntConfigurer("", " "+getConfigureName()+":  ", current); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    config.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
        current = (Integer) ((IntConfigurer) e.getSource()).getValue();
        myValue.setPropertyValue(getValueString());
      }});

    return config.getControls();
  }

  public String[] getAttributeDescriptions() {
    return ArrayUtils.append(
      super.getAttributeDescriptions(),
      "Start Value:  ",
      "Increment By:  ",
      "Loop?",
      "Maximum value:  "
    );
  }

  public Class<?>[] getAttributeTypes() {
    return ArrayUtils.append(
      super.getAttributeTypes(),
      Integer.class,
      Integer.class,
      Boolean.class,
      Integer.class
    );
  }

  public String[] getAttributeNames() {
    return ArrayUtils.append(
      super.getAttributeNames(),
      START,
      INCR,
      LOOP,
      LOOP_LIMIT
    );
  }

  public void setAttribute(String key, Object value) {

    if (START.equals(key)) {
      if (value instanceof String) {
        value = Integer.valueOf((String) value);
      }
      start = ((Integer) value).intValue();
      current = start;
      myValue.setPropertyValue(getValueString());
    }
    else if (INCR.equals(key)) {
      if (value instanceof String) {
        value = Integer.valueOf((String) value);
      }
      incr = ((Integer) value).intValue();
    }
    else if (LOOP.equals(key)) {
      if (value instanceof String) {
        value = Boolean.valueOf((String) value);
      }
      loop = ((Boolean) value).booleanValue();
    }
    else if (LOOP_LIMIT.equals(key)) {
      if (value instanceof String) {
        value = Integer.valueOf((String) value);
      }
      loopLimit = ((Integer) value).intValue();
    }
    else {
      super.setAttribute(key, value);
    }

  }

  public String getAttributeValueString(String key) {
    if (START.equals(key)) {
      return start + ""; //$NON-NLS-1$
    }
    else if (INCR.equals(key)) {
      return incr + ""; //$NON-NLS-1$
    }
    else if (LOOP.equals(key)) {
      return loop + ""; //$NON-NLS-1$
    }
    else if (LOOP_LIMIT.equals(key)) {
      return loopLimit + ""; //$NON-NLS-1$
    }
    else
      return super.getAttributeValueString(key);
  }

  public static String getConfigureTypeName() {
    return "Counter";
  }

  public HelpFile getHelpFile() {
    return HelpFile.getReferenceManualPage("TurnTracker.htm","Counter"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  public VisibilityCondition getAttributeVisibility(String name) {
    if (LOOP_LIMIT.equals(name)) {
      return loopCond;
    }
    else {
      return null;
    }
  }

  private VisibilityCondition loopCond = new VisibilityCondition() {
    public boolean shouldBeVisible() {
      return loop;
    }
  };
 }
