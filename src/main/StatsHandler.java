package main;

import java.util.Arrays;
import java.util.List;

import org.jibble.pircbot.User;

/*
 * This class processes IRC messages and events to generate statistics.
 */

public class StatsHandler {
	private static StatsHandler instance;
	private List<String> profanities = Arrays.asList(new String[] { "fuck","cock", "dick", "cunt", "bitch", "shit", "piss", "nigger","asshole", "faggot", "wank" });
	
	private String[] emoticons = new String[] {":)", ":(", ":/", ":D", "D:", ":o", "o:", ":S", ":|", ":v", ":>", ":<", "^-^", "^_^", ">.>", "<.<", ">.<", ":3", ":P", ":p", "=)", "=D", "xD", "XD", ":$", "o.o", "c:", ":c", ":y", ">:c", ">:C", "C:", ":3"};

	public void addProfanity(String word) {
		for (int i = 0; i < profanities.size(); i++) {
			if (word.equals(profanities.get(i)))
				return;
		}
		profanities.add(word);
	}

	public static StatsHandler getInstance() {
		if (instance == null) {
			instance = new StatsHandler();
		}
		return instance;
	}

	public void processMessage(String channel, String sender, String login, String hostname, String message) {
		incrementLineCount(login);
		processAlts(login, sender);
		String userNick = SqlConnector.getInstance().sendSelectQuery("SELECT nick FROM users WHERE nick = '" + sender + "'");
		if(!userNick.equals("")){
			SqlConnector.getInstance().sendQuery("UPDATE users SET nick = '" + login + "' WHERE nick = '" + sender + "'");
		}
		String[] words = message.split(" ");
		if(words.length > 6){
			if(Math.random() < 0.05){
				setRandomQuote(login, message);
			}
		}
		processEmoticons(channel, sender, login, hostname, message, words);
		processWords(channel, sender, login, hostname, message, words);
	}

	private void setRandomQuote(String login, String message) {
		SqlConnector.getInstance().sendQuery("UPDATE users SET random_quote = '" + message + "'");
	}

	private void processAlts(String login, String sender) {
		String dbLogin = SqlConnector.getInstance().sendSelectQuery("SELECT login FROM alts WHERE `login` = '" + login + "'");
		System.out.println("Dblogin = " + dbLogin);
		if(dbLogin.equals("") || dbLogin == null){
			SqlConnector.getInstance().sendQuery("INSERT INTO alts VALUES ('" + login + "', '" + sender + "', '')");
		}else{
			String primaryNick = SqlConnector.getInstance().sendSelectQuery("SELECT `primary` FROM alts WHERE login = '" + login + "'");
			if(!sender.equals(primaryNick)){
				String altNicks = SqlConnector.getInstance().sendSelectQuery("SELECT `additional` FROM alts WHERE login = '" + login + "'");
				if(!altNicks.contains(sender.toLowerCase())){
					SqlConnector.getInstance().sendQuery("UPDATE alts SET additional = CONCAT_WS(',', additional, '" + sender.toLowerCase() + "') WHERE login = '" + login + "'");
				}
			}
		}
	}

	private void processEmoticons(String channel, String sender, String login, String hostname, String message, String[] words) {
		for(String word : words){
			for(int i = 0; i < emoticons.length; i++){
				String emoticon = emoticons[i];
				if(word.contains(emoticon)){
					SqlConnector.getInstance().tryIncrementLastUsedBy("emoticons", "emoticon", emoticon, "frequency", login, "'"+ emoticon + "', 1, '" + login + "'");
				}
			}
		}
	}

	private void processWords(String channel, String sender, String login, String hostname, String message, String[] words) {
		User[] users = SimpleBot.instance.getUsers(channel);
		
		SqlConnector.getInstance().tryIncrement("users", "nick", login, "words", words.length, "'"+ sender + "', 0, 0, 0, 0, 0, 0, 0, 0, " + words.length + ", ''");
		
		int nickCount = 0;
		for (String word : words) {
			word = word.toLowerCase();
			for (User user : users) {
				String nick = user.getNick().toLowerCase();
				if (!sender.equals(word) && !sender.equals("Cadbury") && word.replaceAll("$|,|:", "").equalsIgnoreCase(nick)) {
					nickCount++;
					if(nickCount < 3){
						String pingedLogin = SqlConnector.getInstance().sendSelectQuery("SELECT login FROM alts WHERE (primary = '" + nick + "' OR additional LIKE '%" + nick + "'%");
						if(pingedLogin.equals("") || pingedLogin == null){
							//Unable to retrieve the login of the person who was pinged
						}else{
							SqlConnector.getInstance().tryIncrement("users", "nick", pingedLogin, "pings", "'" + nick + "', 1, 0, 0, 0, 0, 0, 0, 0, ''");
						}
						
					}
				}
			}
			if (!word.startsWith("http://")) {
				word = word.replaceAll("[^A-Za-z0-9]", "");
			} else {
				// Add support for URLs here later
				System.out.println("Dropping URL.");
				continue;
			}
			if (word.equals("")) {
				System.out.println("Dropping empty word.");
				continue;
			}
			if (word.length() > 32 || word.length() < 3) {
				continue;
			}
			if (isArticle(word)) {
				System.out.println("Dropping article.");
				continue;
			}
			if (isConjunction(word)) {
				System.out.println("Dropping conjunction.");
				continue;
			}
			if (isProfanity(word)) {
				SqlConnector.getInstance().increment("users", "nick", login, "profanities");
			}
			SqlConnector.getInstance().tryIncrement("words", "word", word, "frequency", "'" + word+ "', 1");
		}
	}

	private boolean isProfanity(String word) {
		for (int i = 0; i < profanities.size(); i++) {
			String profanity = profanities.get(i);
			if (word.contains(profanity)) {
				return true;
			}
		}
		return false;
	}

	private void incrementLineCount(String nick) {
		SqlConnector.getInstance().tryIncrement("users", "nick", nick, "lines", "'" + nick + "', 0, 0, 0, 0, 0, 0, 0, 1, 0, ''");
	}

	private boolean isConjunction(String word) {
		String[] conjunctions = { "and", "but", "or", "yet", "for", "nor", "so" };
		for (String conjunction : conjunctions) {
			if (word.equals(conjunction))
				return true;
		}
		return false;
	}

	private boolean isArticle(String word) {
		return (word.equals("the") || word.equals("an") || word.equals("a"));
	}
}
