package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jibble.pircbot.*;

public class SimpleBot extends PircBot{
	
	// Which prefix to use for commands
	private String commandIdentifier = "-";
	
	// The current version of the bot
	public static final String version = "1.4";
	
	// More debug output?
	private static final boolean verbose = false;
	
	// UGLY EWW EWW UGLY this list contains all exception messages so they can be read directly from irc,
	// using the -ed command.
	public List<String> unreadExceptions = new ArrayList<String>();
	
	private CommandHandler ch;
	
	// Should generate a getter for this at some point and update my code to use it. CBA to do it now.
	public static SimpleBot instance;
	
	// List of all rems. Currently not persistent. Will eventually be saved in a MySQL database.
	private Map<String, String> rems = new HashMap<String,String>();

	// When true, enables rems and the $g command
	public boolean cadburyMode = false;
	
	// 
	public SimpleBot(){
		String name = SettingsManager.getInstance().getSetting("nick");
		String login = SettingsManager.getInstance().getSetting("ident");
		int port = Integer.parseInt(SettingsManager.getInstance().getSetting("sqlport"));
		String host = SettingsManager.getInstance().getSetting("sqlhost");
		String user = SettingsManager.getInstance().getSetting("sqluser");
		String pass = SettingsManager.getInstance().getSetting("sqlpass");
		
		System.out.println("\t IRC Server:\tirc.esper.net");
		System.out.println("\t IRC Nick:\t" + name);
		System.out.println("\t IRC Login:\t" + login);
		System.out.println("\t===========================");
		System.out.println("\t SQL Host:\t" + host);
		System.out.println("\t SQL Port:\t" + port);
		System.out.println("\t SQL User:\t" + user);
		System.out.println("\t SQL DB:\t" + "stats_bot");
		
		
		
		this.setName(name);
		this.setLogin(login);
		
		SqlConnector.getInstance().connect(host, port, user, pass, "stats_bot");
		
		ch = new CommandHandler();
		
		instance = this;
	}	
	public void onMessage(String channel, String sender, String login, String hostname, String message){
		if(message.startsWith(commandIdentifier) || (cadburyMode && message.startsWith("$"))){
			ch.processCommand(channel, sender, login, hostname, message);
		}else if(!message.startsWith("$")){
			StatsHandler.getInstance().processMessage(channel, sender, login, hostname, message);
		}
	}
	public void onJoin(String channel, String sender, String login, String hostname){
		if(sender.equals("Cadbury")){
			cadburyMode = false;
		}
	}
	public static void main(String args[]) throws Exception{
		splash();
		SimpleBot bot = new SimpleBot();
		System.out.println("\n==============================================");
		System.out.println("Connecting to IRC server...");
		bot.setVerbose(verbose);
		bot.connect("irc.esper.net");
		System.out.println("   Done");
		String channel = SettingsManager.getInstance().getSetting("channel");
		System.out.println("Joining " + channel);
		bot.joinChannel(channel);
		System.out.println("Ready to serve.");
	}
	
	
	private static void splash() {
		System.out.println("   ###   ##   ##   ## #   # ###   ##  #####");
		System.out.println("   #  # #  # #    #    # #  #  # #  #   #");
		System.out.println("   ###  #### #    #     #   ###  #  #   #");
		System.out.println("   #  # #  # #  # #  #  #   #  # #  #   #");
		System.out.println("   ###  #  #  ##   ##   #   ###   ##    #");
		System.out.println("\t===========================");
		System.out.println("\t Version:\t" + version);
		System.out.println("\t Author:\tbaggerboot");
		System.out.println("\t===========================");
		
	}
	public boolean remExists(String rem){
		return rems.containsKey(rem);
	}
	public void addRem(String rem, String definition) {
		rems.put(rem, definition);
	}
	public void removeRem(String rem){
		rems.remove(rem);
	}
	public void removeRemIfExists(String rem){
		if(remExists(rem)){
			removeRem(rem);
		}
	}
	public String getRem(String rem){
		return rems.get(rem);
	}
	public void shutdown(){
		disconnect();
		dispose();
		System.exit(0);
	}
}
