package geek;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.*;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.*;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

import entities.Charclass;

@SuppressWarnings("serial")
public class GeekQuestServlet extends HttpServlet {

	private DatastoreService datastore;
	private UserService userService;
	private User user;

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		initializeData();
		
		//generate test data in databasae
//		putHighscorePlayerToDatabase();
//		generateMissions();

		if(user==null){
				resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
			}
		else{
			if(hasCharacter(user)){
				if(userWantsToEditData(req)){
					//set player data in view
					handleEditCharacter(req, resp);
				}
				else{
				resp.sendRedirect("/welcome");
				}
			}else{
				handleUserHasNoCharacter(req, resp);
			}
		}
	}

	public void initializeData(){
		datastore = DatastoreServiceFactory.getDatastoreService();
		userService = UserServiceFactory.getUserService();
		user = userService.getCurrentUser();
	}
	
	public void handleEditCharacter(HttpServletRequest req, HttpServletResponse resp){
		setPlayerAttributesInView(req);
		dispatchRequest(req, resp, "/pages/characterdesign.jsp");
	}
	
	public void setPlayerAttributesInView(HttpServletRequest req){
		Entity player = getPlayerFromDatabase();
		req.setAttribute("pname", player.getProperty("playername"));
		List<Entity> missions = getAllMissions();
		List<Entity> playerMissions = getPlayerMissionsFromEmbedded(player);
		for(int i=0; i<missions.size();i++){
			Entity mission = missions.get(i);
			for(Entity playerMission: playerMissions){
				if(playerMission.getKey().equals(mission.getKey())){
					mission.setProperty("isset", true);
				}
			}
			if(mission.getProperty("isset")==null){
					mission.setProperty("isset", false);
			}
		}
		req.setAttribute("missions", missions);
		String[] charclasses = Charclass.names();
		String[] newCharclasses= new String[Charclass.names().length];
		int index = 1;
		newCharclasses[0]= (String) player.getProperty("character");
		for(String charclass:charclasses){
			if(!charclass.equals(player.getProperty("character"))){
				newCharclasses[index]=charclass;
				index++;
			}
		}
		req.setAttribute("characters", newCharclasses);
		req.setAttribute("user", user);
//		req.setAttribute("character", player.getProperty("character"));
//		req.setAttribute("health", player.getProperty("health"));
//		req.setAttribute("missions", getPlayerMissionsFromEmbedded(player));
//		// req.setAttribute("fileurl",
//		// getUpoadedFileURL((String)player.getProperty("fileblobkey")));
//		req.setAttribute("fileurl", player.getProperty("fileurl"));
//		req.setAttribute("user", user.getEmail());
	}
	
	public ArrayList<Entity> getPlayerMissionsFromEmbedded(Entity player) {
		ArrayList<Entity> missions = new ArrayList<Entity>();
		ArrayList<EmbeddedEntity> embeddedMissions = (ArrayList<EmbeddedEntity>) player
				.getProperty("missions");
		if (embeddedMissions != null) {
			for (EmbeddedEntity embeddedMission : embeddedMissions) {
				Key infoKey = embeddedMission.getKey();
				Entity mission = new Entity(infoKey);

				mission.setPropertiesFrom(embeddedMission);
				missions.add(mission);
			}
		}
		return missions;
	}
	
	public Entity getPlayerFromDatabase() {
		Query q = new Query("Player");
		Filter userFilter = new FilterPredicate("email", FilterOperator.EQUAL,
				user.getEmail());
		q.setFilter(userFilter);
		PreparedQuery pq = datastore.prepare(q);
		Entity entity = pq.asSingleEntity();
		return entity;
	}

	public boolean hasCharacter(User user){
		Query q = new Query("Player");
		Filter userFilter =  new FilterPredicate("email", FilterOperator.EQUAL, user.getEmail());
		q.setFilter(userFilter);
		PreparedQuery pq = datastore.prepare(q);
		Entity entity= pq.asSingleEntity();
		return entity!=null;
	}
	
	public boolean userWantsToEditData(HttpServletRequest req){
		return req.getParameter("edit")!=null;
	}

	public void handleUserHasNoCharacter(HttpServletRequest req, HttpServletResponse resp){

			if(!areMissionsSetInGUI(req)){
				List<Entity> missions = getAllMissions();
				for(Entity mission: missions){
					mission.setProperty("isset", false);
				}
				req.setAttribute("missions", missions);
				req.setAttribute("characters", Charclass.names());
				req.setAttribute("user", user);
				dispatchRequest(req, resp, "/pages/characterdesign.jsp");
			}
	}


	public boolean areMissionsSetInGUI(HttpServletRequest req){
		return req.getAttribute("missions")!=null && !req.getAttribute("missions").equals("");
	}

	public void dispatchRequest(HttpServletRequest req, HttpServletResponse resp, String forwardedFile){
		RequestDispatcher rd = getServletContext().getRequestDispatcher(forwardedFile);
		try {
			rd.forward(req, resp);
		} catch (ServletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public List<Entity> generateMissions(){
		List<Entity> missions= new ArrayList<Entity>();

		String kindDestroyRing = "Mission";
		String nameDestroyRing = "Destroying Ring";
		Key keyDestroyRing = KeyFactory.createKey(kindDestroyRing, nameDestroyRing);
		Entity missionDestroyRing = new Entity(keyDestroyRing);
		missionDestroyRing.setProperty("description", "Destroying Ring");
		missionDestroyRing.setProperty("isAccomplished", false);
		missions.add(missionDestroyRing);
		datastore.put(missionDestroyRing);

		String kindVisitRivendell = "Mission";
		String nameVisitRivendell = "Visit Rivendell";
		Key keyVisitRivendell = KeyFactory.createKey(kindVisitRivendell, nameVisitRivendell);
		Entity missionVisitRivendell = new Entity(keyVisitRivendell);
		missionVisitRivendell.setProperty("description", "Visit Rivendell");
		missionVisitRivendell.setProperty("isAccomplished", false);
		missions.add(missionVisitRivendell);
		datastore.put(missionVisitRivendell);

		return missions;


	}

	public void putHighscorePlayerToDatabase(){
		String kindPlayer = "Player";

		String emailPlayer1 = "email1@test.de";
		Key keyPlayer1 = KeyFactory.createKey(kindPlayer, emailPlayer1);
		Entity player1 = new Entity(kindPlayer);
		player1.setProperty("playername", "Hans");
		player1.setProperty("character", "Mage");
		player1.setProperty("health", 25);
		player1.setProperty("email", emailPlayer1);
		player1.setProperty("score", 199);
		datastore.put(player1);

		String emailPlayer2 = "email2@test.de";
		Key keyPlayer2 = KeyFactory.createKey(kindPlayer, emailPlayer2);
		Entity player2 = new Entity(kindPlayer);
		player2.setProperty("playername", "Maria");
		player2.setProperty("character", "Mage");
		player2.setProperty("health", 25);
		player2.setProperty("email", emailPlayer2);
		player2.setProperty("score", 200);
		datastore.put(player2);

		String emailPlayer3 = "email3@test.de";
		Key keyPlayer3 = KeyFactory.createKey(kindPlayer, emailPlayer3);
		Entity player3 = new Entity(kindPlayer);
		player3.setProperty("playername", "Vivien");
		player3.setProperty("character", "Mage");
		player3.setProperty("health", 25);
		player3.setProperty("email", emailPlayer3);
		player3.setProperty("score", 251);
		datastore.put(player3);

		String emailPlayer4 = "email4@test.de";
		Key keyPlayer4 = KeyFactory.createKey(kindPlayer, emailPlayer4);
		Entity player4 = new Entity(kindPlayer);
		player4.setProperty("playername", "Johann");
		player4.setProperty("character", "Mage");
		player4.setProperty("health", 25);
		player4.setProperty("email", emailPlayer4);
		player4.setProperty("score", 270);
		datastore.put(player4);

		String emailPlayer5 = "email5@test.de";
		Key keyPlayer5 = KeyFactory.createKey(kindPlayer, emailPlayer5);
		Entity player5 = new Entity(kindPlayer);
		player5.setProperty("playername", "Fiona");
		player5.setProperty("character", "Warrior");
		player5.setProperty("health", 25);
		player5.setProperty("email", emailPlayer5);
		player5.setProperty("score", 245);
		datastore.put(player5);

		String emailPlayer6 = "email6@test.de";
		Key keyPlayer6 = KeyFactory.createKey(kindPlayer, emailPlayer6);
		Entity player6 = new Entity(kindPlayer);
		player6.setProperty("playername", "Alex");
		player6.setProperty("character", "Hobbit");
		player6.setProperty("health", 25);
		player6.setProperty("email", emailPlayer6);
		player6.setProperty("score", 300);
		datastore.put(player6);

		String emailPlayer7 = "email7@test.de";
		Key keyPlayer7 = KeyFactory.createKey(kindPlayer, emailPlayer7);
		Entity player7 = new Entity(kindPlayer);
		player7.setProperty("playername", "Janina");
		player7.setProperty("character", "Hobbit");
		player7.setProperty("health", 25);
		player7.setProperty("email", emailPlayer7);
		player7.setProperty("score", 123);
		datastore.put(player7);

		String emailPlayer8 = "email8@test.de";
		Key keyPlayer8 = KeyFactory.createKey(kindPlayer, emailPlayer8);
		Entity player8 = new Entity(kindPlayer);
		player8.setProperty("playername", "Max");
		player8.setProperty("character", "Mage");
		player8.setProperty("health", 25);
		player8.setProperty("email", emailPlayer8);
		player8.setProperty("score", 654);
		datastore.put(player8);

		String emailPlayer9 = "email9@test.de";
		Key keyPlayer9 = KeyFactory.createKey(kindPlayer, emailPlayer9);
		Entity player9 = new Entity(kindPlayer);
		player9.setProperty("playername", "Janina");
		player9.setProperty("character", "Hobbit");
		player9.setProperty("health", 25);
		player9.setProperty("email", emailPlayer9);
		player9.setProperty("score", 344);
		datastore.put(player9);

		String emailPlayer10 = "email10@test.de";
		Key keyPlayer10 = KeyFactory.createKey(kindPlayer, emailPlayer10);
		Entity player10 = new Entity(kindPlayer);
		player10.setProperty("playername", "Julian");
		player10.setProperty("character", "Warior");
		player10.setProperty("health", 25);
		player10.setProperty("email", emailPlayer10);
		player10.setProperty("score", 734);
		datastore.put(player10);

	}

	public List<Entity> getAllMissions(){
		Query q= new Query("Mission");
		PreparedQuery pq = datastore.prepare(q);
		List<Entity> result = pq.asList(FetchOptions.Builder.withLimit(5));
		return result;
	}
}