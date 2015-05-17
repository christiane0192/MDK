<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="com.google.appengine.api.blobstore.BlobstoreServiceFactory" %>
<%@ page import="com.google.appengine.api.blobstore.BlobstoreService" %>
<%
BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
%>
<html>
<head>
<title>Generate your hobbit name:</title>
</head>
<body>

<form action="/namehelper">
<h1>Insert your real name:</h1>
<label for="vname"> Vorname:
      <input id="vname" name="vname">
</label>
<label for="nname"> Nachname: 
      <input id="nname" name="nname">
 </label>
 <br> 
 <fieldset>
    <input type="radio" id="f" name="gender" value="female" checked><label for="f">female</label><br> 
    <input type="radio" id="m" name="gender" value="male"><label for="m">male</label><br> 
</fieldset>
<br> 
<input type="submit" style="height: 30px; width: 250px;" id="generate" name="generate" value="Generate" /> 


</form>
</body>
</html>
