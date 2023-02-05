/*
 * $Id: Chatter.java 8952 2013-11-27 23:26:33Z uckelman $
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
package HTMLChatter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.plaf.basic.BasicTextAreaUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainView;
import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import javax.swing.text.Utilities;
import javax.swing.text.View;
import javax.swing.text.WrappedPlainView;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.*;
import javax.swing.text.*;

import VASSAL.build.Buildable;
import VASSAL.build.GameModule;
import VASSAL.build.module.GlobalOptions; //BR//
import VASSAL.command.Command;
import VASSAL.command.CommandEncoder;
import VASSAL.configure.ColorConfigurer;
import VASSAL.configure.FontConfigurer;
import VASSAL.i18n.Resources;
import VASSAL.preferences.Prefs;
import VASSAL.tools.ErrorDialog;
import VASSAL.tools.KeyStrokeSource;
import VASSAL.tools.ScrollPane;
import net.sf.wraplog.Logger;
import VASSAL.tools.ComponentSplitter;
import VASSAL.tools.ComponentSplitter.SplitPane;
import VASSAL.tools.DataArchive;

//BR// Drop-in replacement Vassal Chatter - provides full HTML support for the chat window. In addition to allowing multiple colors/fonts, and images,
//BR// allows module creators to send <b>bold</b> and <i>italic</i> tags in Reports, along with tags for images, text styling, etc.  

/**
 * The chat window component. Displays text messages and accepts input. Also
 * acts as a {@link CommandEncoder}, encoding/decoding commands that display
 * message in the text area
 */
public class HTMLChatter extends VASSAL.build.module.Chatter implements CommandEncoder, Buildable {
	private static final long serialVersionUID = 1L;

	protected JTextPane conversationPane;
	protected HTMLDocument doc;
	protected HTMLEditorKit kit;
	protected StyleSheet style;

	protected JTextField input;
	protected JScrollPane scroll = new ScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	protected JScrollPane scroll2 = new ScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	protected static final String MY_CHAT_COLOR    = "HTMLChatColor";          //$NON-NLS-1$ //BR// Different tags to "restart" w/ new default scheme
	protected static final String OTHER_CHAT_COLOR = "HTMLotherChatColor";     //$NON-NLS-1$
	protected static final String GAME_MSG1_COLOR  = "HTMLgameMessage1Color";  //$NON-NLS-1$
	protected static final String GAME_MSG2_COLOR  = "HTMLgameMessage2Color";  //$NON-NLS-1$ //BR//
	protected static final String GAME_MSG3_COLOR  = "HTMLgameMessage3Color";  //$NON-NLS-1$ //BR//
	protected static final String GAME_MSG4_COLOR  = "HTMLgameMessage4Color";  //$NON-NLS-1$ //BR//
	protected static final String GAME_MSG5_COLOR  = "HTMLgameMessage5Color";  //$NON-NLS-1$ //BR//
	protected static final String SYS_MSG_COLOR    = "HTMLsystemMessageColor"; //$NON-NLS-1$

	protected Font myFont;

	protected Color gameMsg, gameMsg2, gameMsg3, gameMsg4, gameMsg5;
	protected Color systemMsg, myChat, otherChat;


	public HTMLChatter() {		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		//BR// Conversation is now a JTextPane w/ HTMLEditorKit to process HTML.
		conversationPane = new JTextPane();
		conversationPane.setPreferredSize(new Dimension(500, 120));
		conversationPane.setContentType("text/html");
		doc = (HTMLDocument) conversationPane.getDocument();
		kit = (HTMLEditorKit) conversationPane.getEditorKit();

		style = kit.getStyleSheet();
		myFont = new Font("SansSerif", Font.PLAIN, 12); //BR// Will be overridden by the font from Chat preferences

		for (int i = 0; i < 15; ++i) {
			try {
				kit.insertHTML(doc, doc.getLength(), "<br>", 0, 0, null);
			} catch (BadLocationException ble) {
				ErrorDialog.bug(ble);
			} catch (IOException ex) {
				ErrorDialog.bug(ex);
			}
		}

		conversation.setEditable(false);
		conversation.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
			}
		});

		input = new JTextField(60);
		input.setFocusTraversalKeysEnabled(false);
		input.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				send(formatChat(e.getActionCommand()));
				input.setText(""); //$NON-NLS-1$
			}
		});
		input.setMaximumSize(new Dimension(input.getMaximumSize().width, input.getPreferredSize().height));

		conversation.setPreferredSize(new Dimension(input.getMaximumSize().width, input.getPreferredSize().height * 11));

		scroll.setViewportView(conversation);
		scroll.getVerticalScrollBar().setUnitIncrement(input.getPreferredSize().height); // BR// Scroll this faster																									
		add(scroll);
		add(input);
		
		//BR// This turns off the default Vassal chatter window, since the chatter's
		//BR// *constructor* can't be overridden. Not needed if this were dropped in AS Vassal's chatter.
		super.conversation.setVisible(false);
		super.input.setVisible(false);
		super.scroll.setVisible(false);				
	}

	private String formatChat(String text) {
		final String id = GlobalOptions.getInstance().getPlayerId();
		return "&lt;" + (id.length() == 0 ? "(" + getAnonymousUserName() + ")" : id) + "&gt; - " + text; //$NON-NLS-1$ //$NON-NLS-2$ //BR//HTML-friendly angle brackets
	}

	public JTextField getInputField() {
		return input;
	}

	/**
	 * Display a message in the text area
	 */
	public void show(String s) {
		String style;

		//BR// Choose an appropriate style to display this message in
		s = s.trim();
		if (s.length() > 0) {
			if (s.startsWith("*")) {
				//BR// Here we just extend the convention of looking at first characters, this time to the second character.
				//BR// ! = msg2
				//BR// ? = msg3
				//BR// ~ = msg4
				//BR// These characters can be pre-pended to Report messages to produce the color changes. The characters themselves are removed before display.
				//BR// Reports can also include <b></b> tags for bold and <i></i> for italic.
				if (s.startsWith("* !") || s.startsWith("*!")) {
					style = "msg2";
					s = s.replaceFirst("!", "");
				} else if (s.startsWith("* ?") || s.startsWith("*?")) {
					style = "msg3";
					s = s.replaceFirst("\\?", "");
				} else if (s.startsWith("* ~") || s.startsWith("*~")) {
					style = "msg4";
					s = s.replaceFirst("~", "");
				} else if (s.startsWith("* `") || s.startsWith("*`")) {
					style = "msg5";
					s = s.replaceFirst("`", "");
				} else {
					style = "msg";
				}
			} else if (s.startsWith("-")) {
				style = "sys";
			} else {
				//BR// Ideally we'd actually be passing in a player-side or player-number so that we'd have the option of 
				//BR// having e.g. Union player in Blue, Confederate Player in Grey, and Referee in Green, etc. But that would
				//BR// involve intercepting stuff in other java files, and I'm trying for a one-class-replacement feature here.
				if (s.startsWith(formatChat("").trim())) { //$NON-NLS-1$
					style = "mychat";
				} else {
					style = "other";
				}
			}
		} else {
			style = "msg";
		}

		//BR// Systematically search through for html image tags. When we find one, try
		//BR// to match it with an image from our DataArchive, and substitute the correct
		//BR// fully qualified URL into the tag.
		
		//BR// There are without a doubt "more efficient ways to do this", but this loop
		//BR// miraculously ran perfectly the very first time I compiled it, so superstition...
		
		URL url;
		String keystring = "<img src=\"";
		String file, tag, replace;
		int base;
		while (s.toLowerCase().contains(keystring)) { // Find next key (to-lower so we're not case sensitive)
			base = s.toLowerCase().indexOf(keystring);
			file = s.substring(base + keystring.length(), s.length()).split("\"")[0]; // Pull the filename out from between the quotes
    	    tag  = s.substring(base, base + keystring.length()) + file + "\""; // Reconstruct the part of the tag we want to remove, leaving all attributes after the filename alone, and properly matching the upper/lowercase of the keystring						
			
			try {
				url = GameModule.getGameModule().getDataArchive().getURL("images/" + file);
				replace = "<img  src=\"" + url.toString() + "\""; // Fully qualified URL if we are successful. The extra
																	// space between IMG and SRC in the processed
																	// version ensures we don't re-find THIS tag as we
																	// iterate.
			} catch (IOException ex) {
				replace = "<img  src=\"" + file + "\""; // Or just leave in except alter just enough that we won't find
														// this tag again.
			}

			if (s.contains(tag)) {
				s = s.replaceFirst(tag, replace); // Swap in our new URL-laden tag for the old one.
			} else {
				break; // BR// If something went wrong in matching up the tag, don't loop forever
			}
		}
		
		//BR// Now we have to fix up any legacy angle brackets around the word <observer>
	    keystring = Resources.getString("PlayerRoster.observer");
	    replace   = keystring.replace("<","&lt;");
	    replace   = replace.replace(">","&gt;");
	    if (replace != keystring) {
	    	s = s.replace(keystring, replace);
	    }	   
	    
        //if (s.contains("/force chatter") && !s.equals("other")) {        	
        //  final ComponentSplitter splitter = new ComponentSplitter();        	
        //  ComponentSplitter.SplitPane pane;
        //  pane = (SplitPane) splitter.getSplitAncestor(GameModule.getGameModule().getControlPanel(), -1);
        //  pane.setDividerLocation(160);
        //  return;	      
        //}
	    
	    //BR// Insert a div of the correct style for our line of text. Module designer
		//BR//still free to insert <span> tags and <img> tags and the like in Report
		//BR//messages.
		try {
			kit.insertHTML(doc, doc.getLength(), "\n<div class=" + style + ">" + s + "</div>", 0, 0, null);
		} catch (BadLocationException ble) {
			ErrorDialog.bug(ble);
		} catch (IOException ex) {
			ErrorDialog.bug(ex);
		}
		conversation.update(conversation.getGraphics()); //BR// Force graphics to update
	}

	/** @deprecated use GlobalOptions.getPlayerId() */
	@Deprecated
	public void setHandle(String s) {
	}

	/** @deprecated use GlobalOptions.getPlayerId() */
	@Deprecated
	public String getHandle() {
		return GlobalOptions.getInstance().getPlayerId();
	}

	//BR// Adds or updates a CSS stylesheet entry. Styles in the color, font type, and font size.
	private void addStyle(String s, Font f, Color c, String font_weight, int size) {
		if ((style == null) || (c == null)) return;
		style.addRule(s + " {color:" + String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue())
				        + "; font-family:" + f.getFamily() + "; font-size:" + (size > 0 ? size : f.getSize()) + "; " + ((font_weight != "") ? "font-weight:" + font_weight + "; " : "") + "}");
	}

	//BR// Build ourselves a CSS stylesheet from our preference font/color settings.
	private void makeStyleSheet(Font f) {
		if (style != null) {
			if (f == null) {
				if (myFont == null) {
					f = new Font("SansSerif", 0, 12);
					myFont = f;
				} else {
					f = myFont;
				}
			}
			addStyle("body",    f, gameMsg,   "",     0);
			addStyle("p",       f, gameMsg,   "",     0);
			addStyle(".msg",    f, gameMsg,   "",     0);
			addStyle(".msg2",   f, gameMsg2,  "",     0);
			addStyle(".msg3",   f, gameMsg3,  "",     0);
			addStyle(".msg4",   f, gameMsg4,  "",     0);
			addStyle(".msg5",   f, gameMsg5,  "",     0);
			addStyle(".mychat", f, myChat,    "bold", 0);
			addStyle(".other ", f, otherChat, "bold", 0);
			addStyle(".sys",    f, systemMsg, "",     0);
			
			//BR// A fun extension would be letting the module designer provide extra class styles. 
		}
	}

	/**
	 * Set the Font used by the text area
	 */
	public void setFont(Font f) {
		myFont = f;
		if (input != null) {
			if (input.getText().length() == 0) {
				input.setText("XXX"); //$NON-NLS-1$
				input.setFont(f);
				input.setText(""); //$NON-NLS-1$
			} else
				input.setFont(f);
		}
		if (conversation != null) {
			conversation.setFont(f);
		}
		makeStyleSheet(f); // BR// When font changes, rebuild our stylesheet
	}

	public void build(org.w3c.dom.Element e) {
	}

	public org.w3c.dom.Element getBuildElement(org.w3c.dom.Document doc) {
		return doc.createElement(getClass().getName());
	}

	/**
	 * Expects to be added to a GameModule. Adds itself to the controls window and
	 * registers itself as a {@link CommandEncoder}
	 */
	public void addTo(Buildable b) {
		GameModule mod = (GameModule) b;
		mod.removeCommandEncoder((VASSAL.build.module.Chatter)mod.getChatter()); //BR//
		mod.setChatter(this);
		mod.addCommandEncoder(this);
		mod.addKeyStrokeSource(new KeyStrokeSource(this, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT));

		final FontConfigurer chatFont = new FontConfigurer("ChatFont",
				Resources.getString("Chatter.chat_font_preference"));

		chatFont.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				setFont((Font) evt.getNewValue());
			}
		});

		mod.getControlPanel().add(this, BorderLayout.CENTER);

		chatFont.fireUpdate();
		mod.getPrefs().addOption(Resources.getString("Chatter.chat_window"), chatFont); //$NON-NLS-1$

		// Bug 10179 - Do not re-read Chat colors each time the Chat Window is
		// repainted.
		final Prefs globalPrefs = Prefs.getGlobalPrefs();

		//
		// game message color
		//
		final ColorConfigurer gameMsgColor = new ColorConfigurer(GAME_MSG1_COLOR,
				Resources.getString("Chatter.game_messages_preference") + "(#1 - standard messages):  ", Color.black);

		gameMsgColor.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				gameMsg = (Color) e.getNewValue();
				makeStyleSheet(null);
			}
		});

		globalPrefs.addOption(Resources.getString("Chatter.chat_window"), gameMsgColor);

		gameMsg = (Color) globalPrefs.getValue(GAME_MSG1_COLOR);

		//BR//
		//BR// game message color #2 (messages starting with "!") 
		//BR//
		final ColorConfigurer gameMsg2Color = new ColorConfigurer(GAME_MSG2_COLOR,
				Resources.getString("Chatter.game_messages_preference") + "(#2 - first character \"!\"):  ", new Color(0, 153, 51));

		gameMsg2Color.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				gameMsg2 = (Color) e.getNewValue();
				makeStyleSheet(null);
			}
		});

		globalPrefs.addOption(Resources.getString("Chatter.chat_window"), gameMsg2Color);

		gameMsg2 = (Color) globalPrefs.getValue(GAME_MSG2_COLOR);

		//BR//
		//BR// game message color #3 (messages starting with "?")
		//BR//
		final ColorConfigurer gameMsg3Color = new ColorConfigurer(GAME_MSG3_COLOR,
				Resources.getString("Chatter.game_messages_preference") + "(#3 - first character \"?\"):  ", new Color(255, 102, 102));

		gameMsg3Color.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				gameMsg3 = (Color) e.getNewValue();
				makeStyleSheet(null);
			}
		});

		globalPrefs.addOption(Resources.getString("Chatter.chat_window"), gameMsg3Color);

		gameMsg3 = (Color) globalPrefs.getValue(GAME_MSG3_COLOR);
		
		
		//BR//
		//BR// game message color #4 (messages starting with "~")
		//BR//
		final ColorConfigurer gameMsg4Color = new ColorConfigurer(GAME_MSG4_COLOR,
				Resources.getString("Chatter.game_messages_preference") + "(#4 - first character \"~\"):  ", new Color(255, 0, 0));

		gameMsg4Color.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				gameMsg4 = (Color) e.getNewValue();
				makeStyleSheet(null);
			}
		});

		globalPrefs.addOption(Resources.getString("Chatter.chat_window"), gameMsg4Color);

		gameMsg4 = (Color) globalPrefs.getValue(GAME_MSG4_COLOR);
		
		
		//BR//
		//BR// game message color #4 (messages starting with "`")
		//BR//
		final ColorConfigurer gameMsg5Color = new ColorConfigurer(GAME_MSG5_COLOR,
				Resources.getString("Chatter.game_messages_preference") + "(#5 - first character \"`\"):  ", new Color(153, 0, 153));

		gameMsg5Color.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				gameMsg5 = (Color) e.getNewValue();
				makeStyleSheet(null);
			}
		});

		globalPrefs.addOption(Resources.getString("Chatter.chat_window"), gameMsg5Color);

		gameMsg5 = (Color) globalPrefs.getValue(GAME_MSG5_COLOR);



		//
		// system message color
		//
		final ColorConfigurer systemMsgColor = new ColorConfigurer(SYS_MSG_COLOR,
				Resources.getString("Chatter.system_message_preference"), new Color(160, 160, 160));

		systemMsgColor.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				systemMsg = (Color) e.getNewValue();
				makeStyleSheet(null);
			}
		});

		globalPrefs.addOption(Resources.getString("Chatter.chat_window"), systemMsgColor);

		systemMsg = (Color) globalPrefs.getValue(SYS_MSG_COLOR);


		//
		// my message color
		//
		final ColorConfigurer myChatColor = new ColorConfigurer( MY_CHAT_COLOR,
		Resources.getString("Chatter.my_text_preference"), new Color(9, 32, 229) );
		  
		myChatColor.addPropertyChangeListener(new PropertyChangeListener() { 
			public void propertyChange(PropertyChangeEvent e) { 
				myChat = (Color) e.getNewValue(); 
				makeStyleSheet(null); 
			} 
		});
		  
		globalPrefs.addOption( Resources.getString("Chatter.chat_window"),
		myChatColor );
		  
		myChat = (Color) globalPrefs.getValue(MY_CHAT_COLOR);		 

		//
		// other message color
		//
		
		final ColorConfigurer otherChatColor = new ColorConfigurer( OTHER_CHAT_COLOR,
		Resources.getString("Chatter.other_text_preference"), new Color (0,153,255) );
		  
		otherChatColor.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) { 
				otherChat = (Color) e.getNewValue(); 
				makeStyleSheet(null); 
			} 
		});
		  
		globalPrefs.addOption( Resources.getString("Chatter.chat_window"), otherChatColor );		  
		otherChat = (Color) globalPrefs.getValue(OTHER_CHAT_COLOR);
		 

		makeStyleSheet(myFont);
	}

	public void add(Buildable b) {
	}

	public Command decode(String s) {
		if (s.startsWith("CHAT")) { //$NON-NLS-1$
			return new DisplayText(this, s.substring(4));
		} else {
			return null;
		}
	}

	public String encode(Command c) {
		if (c instanceof DisplayText) {
			return "CHAT" + ((DisplayText) c).msg; //$NON-NLS-1$
		} else if (c instanceof VASSAL.build.module.Chatter.DisplayText) {
			return "CHAT" + ((VASSAL.build.module.Chatter.DisplayText) c).getMessage(); //$NON-NLS-1$	
		} else {
			return null;
		}
	}

	/**
	 * Displays the message, Also logs and sends to the server a {@link Command}
	 * that displays this message
	 */
	public void send(String msg) {
		if (msg != null && msg.length() > 0) {
			show(msg);
			GameModule.getGameModule().sendAndLog(new DisplayText(this, msg));
		}
	}

	/**
	 * Classes other than the Chatter itself may forward KeyEvents to the Chatter by
	 * using this method
	 */
	public void keyCommand(KeyStroke e) {
		if ((e.getKeyCode() == 0 || e.getKeyCode() == KeyEvent.CHAR_UNDEFINED)
				&& !Character.isISOControl(e.getKeyChar())) {
			input.setText(input.getText() + e.getKeyChar());
		} else if (e.isOnKeyRelease()) {
			switch (e.getKeyCode()) {
			case KeyEvent.VK_ENTER:
				if (input.getText().length() > 0)
					send(formatChat(input.getText()));
				input.setText(""); //$NON-NLS-1$
				break;
			case KeyEvent.VK_BACK_SPACE:
			case KeyEvent.VK_DELETE:
				String s = input.getText();
				if (s.length() > 0)
					input.setText(s.substring(0, s.length() - 1));
				break;
			}
		}
	}

	/**
	 * This is a {@link Command} object that, when executed, displays a text message
	 * in the Chatter's text area
	 */
	public static class DisplayText extends Command {
		private String msg;
		private HTMLChatter c;

		public DisplayText(HTMLChatter c, String s) {
			this.c = c;
			msg = s;
			if (msg.startsWith("<>")) {
				msg = "&lt;(" + HTMLChatter.getAnonymousUserName() + ")&gt;" + s.substring(2); // BR// HTML-friendly
																								// angle brackets
			} else {
				msg = s;
			}
		}

		public void executeCommand() {
			c.show(msg);
		}

		public Command myUndoCommand() {
			return new DisplayText(c, Resources.getString("Chatter.undo_message", msg)); //$NON-NLS-1$
		}

		public String getMessage() {
			return msg;
		}

		public String getDetails() {
			return msg;
		}
	}

	public static void main(String[] args) {
		HTMLChatter chat = new HTMLChatter();
		JFrame f = new JFrame();
		f.add(chat);
		f.pack();
		f.setVisible(true);
	}
}
