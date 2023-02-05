package TinjawChatter;

import VASSAL.build.module.Chatter;

public class TinjawChatter extends Chatter
{
  /**
   * A hook for inserting a console class that accepts commands
   * @param s            - chat message
   * @param style        - current style name (contains information that might be useful)
   * @param html_allowed - flag if html_processing is enabled for this message (allows console to apply security considerations)
   */
  @Override
  public void consoleHook(String s, String style, boolean html_allowed) {
    // Put some code here to do things based on contents of String "s"
  }
}

