package geek;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HobbitNameHelper extends HttpServlet {

	public void doGet(HttpServletRequest req, HttpServletResponse resp) {

		String value = req.getParameter("generate");
		if (value != null && value.equals("Generate")) {
			try {
				URL url = new URL("http://www.chriswetherell.com/hobbit/");
				HttpURLConnection connection = (HttpURLConnection) url
						.openConnection();
				connection.setDoOutput(true);
				connection.setRequestMethod("POST");

				OutputStreamWriter writer = new OutputStreamWriter(
						connection.getOutputStream());
				String name = req.getParameter("gender");
				String vorname = req.getParameter("vname");
				String nachname = req.getParameter("nname");
				if (vorname == null || vorname.equals("") || nachname == null
						|| nachname.equals("")) {
					dispatchRequest(req, resp, "/pages/nameform.jsp");
				}

				String parameterFirstName = "";
				String parameterSecondName = "";
				if (name.equals("female")) {
					parameterFirstName = "f_firstname";
					parameterSecondName = "f_lastname";
				} else {
					parameterFirstName = "m_firstname";
					parameterSecondName = "m_lastname";
				}
				String parameters = "name=f&" + parameterFirstName + "="
						+ vorname + "&" + parameterSecondName + "=" + nachname;

				// writer.write("name=f&f_firstname=Christiane&f_lastname=Arnold");
				writer.write(parameters);
				writer.close();

				String gesamt = "";
				if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(connection.getInputStream()));
					for (String line; (line = reader.readLine()) != null;) {
						gesamt += line;
					}
					String answer = getAnswer(gesamt);
					req.setAttribute("edit", "edit");
					req.setAttribute("hobbitname", answer);
					dispatchRequest(req, resp, "/geekquest");

				} else {
					// Server returned HTTP error code.
				}
			} catch (MalformedURLException e) {
				// ...
			} catch (IOException e) {
				// ...
			}
		} else {
			dispatchRequest(req, resp, "/pages/nameform.jsp");
		}
	}

	public void dispatchRequest(HttpServletRequest req,
			HttpServletResponse resp, String forwardedFile) {
		RequestDispatcher rd = getServletContext().getRequestDispatcher(
				forwardedFile);
		try {
			rd.forward(req, resp);
		} catch (ServletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getAnswer(String gesamt) {

		int indexAnswer = gesamt.indexOf("class=\"answer\"");
		int indexStart = gesamt.indexOf(">", indexAnswer);
		int endIndex = gesamt.indexOf("<", indexAnswer);

		String answer = gesamt.substring(indexStart + 1, endIndex);
		return answer;
	}
}
