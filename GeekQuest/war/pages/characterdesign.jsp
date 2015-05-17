<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="com.google.appengine.api.blobstore.BlobstoreServiceFactory" %>
<%@ page import="com.google.appengine.api.blobstore.BlobstoreService" %>
<%
BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
%>
<html>
<head>
<title>GeekQuest - Design your Character</title>
</head>
<body>

<h1>Edit your Character:</h1>
<form action="<%= blobstoreService.createUploadUrl("/welcome") %>" method="post" enctype="multipart/form-data">
<table>
<tr><td>Upload your Photo:</td></tr>
<tr>
	<td><input type="file" name="file"/></td>
</tr>
</table>
<table>
  <tr>
    <td>Name:</td>
    <td><input type="text" size="30" maxlength="30" id="playername" name="playername" value="${pname}"></td>
  </tr>
      <tr><td><input type="submit" style="height: 30px; width: 250px;" id="link" name="link" value="Need help finding a good hobbit name?" /> </td></tr>
  <tr>
    <td>Character class:</td>
   <td><select name="character" id="character">
	<c:forEach items="${characters}" var="temp">
		<option value='${temp}'>${temp}</option>
	</c:forEach>
</select></td>
  </tr>
  <tr>
    <td>Health status:</td>
    <td>10</td>
  </tr>
  <tr>
    <td>Missions:</td>
    <td>
	    <table border="1">
	    <thead>
		    <tr>
	      		<td>Description</td>
	      		<td>Accomplished</td>
	    	</tr>
   		</thead>
	    <c:forEach items="${missions}" var="mission">
	        <tr>
	        	<td>${mission.properties.description}</td>
	        	<c:choose>
				    <c:when test="${mission.properties.isset}">
				      <td><input type="checkbox" name="checkbox${mission.properties.description}" checked /></td>
				    </c:when>
				    <c:otherwise>
				        <td><input type="checkbox" name="checkbox${mission.properties.description}" /></td>
				    </c:otherwise>
				</c:choose>
	        </tr>
	    </c:forEach>
	    </table>
    </td>
  </tr>
  <tr>
	  <td>
		  <input type="submit" name="link" style="height: 30px; width: 100px;" value="Save" />
	  </td>
  </tr>
 </table>
 </form>
</body>
</html>
<script type="text/javascript">
   var sel = document.getElementById('character');
   var link = document.getElementById('link');
   if(sel.value=='HOBBIT'){
  	link.style.visibility = 'visible';
   }
   else{
   	link.style.visibility = 'hidden';
   }
   sel.onchange = function() {
      if(sel.value=='HOBBIT'){
  			link.style.visibility = 'visible';
  	 }
	   else{
	   	link.style.visibility = 'hidden';
	   }
   }
</script>