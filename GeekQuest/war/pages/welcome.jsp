<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
<head>
	<title>Welcome to GeekQuest</title>
</head>
<body>

<h1>Welcome to GeekQuest ${user}!</h1>
<h2>You are challenging GeekQuest with the following character:</h2>
<table>
	<tr>
		<td>Your photo:</td>
	</tr>
	<tr>
		<td><img width="200" height="150" src="${fileurl}"></td>
	</tr>
	<tr>
		<td>Name:</td>
		<td>${playername}</td>
	</tr>
	<tr>
		<td>Character class:</td>
		<td>${character}</td>
	</tr>
	<tr>
		<td>Health status:</td>
		<td>${health}</td>
	</tr>
</table>
<h2>Your currently activated Missions:</h2>
<table>
<c:forEach items="${missions}" var="mission">
	<tr><td>${mission.description}</td></tr>
</c:forEach>
</table>
<h2>Highscore</h2>
<table>
<c:forEach items="${highscores}" var="highscore">
	<tr><td>${highscore.name}</td>
	<td>${highscore.score}</td></tr>
</c:forEach>
</table>
<form action="/welcome">
<input type="text" size="30" maxlength="30" id="newscore" name="newscore">
<button type="submit" style="height: 30px; width: 200px;">Add new Score for Player</button>
</form>
<form action="/welcome">
<input type=hidden id="edit" name="edit" value="edit">
<button type="submit" style="height: 30px; width: 150px;">Edit your Character</button>
</form>
<form action="/welcome">
<input type=hidden id="logout" name="logout" value="logout">
<button type="submit" style="height: 30px; width: 100px;">Logout</button>
</form>
</body>
</html>