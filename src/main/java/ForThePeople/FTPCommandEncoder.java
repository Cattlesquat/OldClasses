//BR// This allows custom traits to be recognizable by the reader

package ForThePeople;

import ForThePeople.Deselect;
import ForThePeople.FTPFootprint;
import VASSAL.build.module.BasicCommandEncoder;
import VASSAL.counters.GamePiece;
import VASSAL.counters.Decorator;

public class FTPCommandEncoder extends BasicCommandEncoder {

  public Decorator createDecorator(String type, GamePiece inner) {
    if (type.startsWith(Deselect.ID)) {
      return new Deselect(type, inner);
    }
    if (type.startsWith(FTPFootprint.ID)) {
	  return new FTPFootprint(type, inner);
    }
    return super.createDecorator(type, inner);
  }  
} 