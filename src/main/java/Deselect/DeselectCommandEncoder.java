//BR// This allows custom traits to be recognizable by the reader

package Deselect;

import Deselect.Deselect;
import VASSAL.build.module.BasicCommandEncoder;
import VASSAL.counters.GamePiece;
import VASSAL.counters.Decorator;

public class DeselectCommandEncoder extends BasicCommandEncoder {

  public Decorator createDecorator(String type, GamePiece inner) {
    if (type.startsWith(Deselect.ID)) {
      return new Deselect(type, inner);
    }
    return super.createDecorator(type, inner);
  }  
} 