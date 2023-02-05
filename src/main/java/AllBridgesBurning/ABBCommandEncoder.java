//BR// This allows custom traits to be recognizable by the reader

package AllBridgesBurning;

import AllBridgesBurning.ABBFootprint;

import VASSAL.build.module.BasicCommandEncoder;
import VASSAL.counters.GamePiece;
import VASSAL.counters.Decorator;

public class ABBCommandEncoder extends BasicCommandEncoder {

  public Decorator createDecorator(String type, GamePiece inner) {
    if (type.startsWith(ABBFootprint.ID)) {
	  return new ABBFootprint(type, inner);
    }
    return super.createDecorator(type, inner);
  }  
} 