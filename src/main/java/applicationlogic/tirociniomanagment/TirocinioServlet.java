package applicationlogic.tirociniomanagment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import storage.beans.Tirocinio;
import storage.beans.Utente;
import storage.dao.TirocinioDao;

@WebServlet("/TirocinioServlet")
public class TirocinioServlet extends HttpServlet {

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    HttpSession ses = request.getSession(true);
    Utente user = null;
    PrintWriter out = response.getWriter();
    Gson obj = new GsonBuilder().serializeNulls().create();
    response.setContentType("application/json");
    user = (Utente) request.getSession().getAttribute("utente");
    Map<String, String> result = new HashMap<>();

    if (user == null) {
      result.put("status", "403");
      result.put("description", "Not Authorized");
      out.println(obj.toJson(result));
    } else {
      try {
        String action = request.getParameter("action");

        if (action.equals("validateInternship")) {
          if (user.getTipo().equals(Utente.ADMIN) || user.getTipo()
              .equals(Utente.UFFICIO_CARRIERE)) {
            validateInternship(request, response);
          } else {
            result.put("status", "403");
            result.put("description", "Not Authorized");
            out.println(obj.toJson(result));
          }
        } else if (action.equals("viewInternship")) {
          viewInternship(request, response);
        } else if (action.equals("changeState")) {
          changeState(request, response);
        } else if (action.equals("viewInternshipByFilter")) {
          viewInternshipByFilter(request, response);
        } else {
          result.put("status", "400");
          result.put("description", "Invalid Request");
          out.println(obj.toJson(result));
        }
      } catch (IllegalArgumentException e) {
        result.put("status", "422");
        result.put("description", e.getMessage());
        out.println(obj.toJson(result));
      } catch (Exception ex) {
        result.put("status", "422");
        result.put("description", "Generic error.");
        out.println(obj.toJson(result));
      }
    }
  }

  private void viewInternship(HttpServletRequest request, HttpServletResponse response) {
    // TODO: implement viewInternshipByFilter
  }

  private void changeState(HttpServletRequest request, HttpServletResponse response) {
    // TODO: implement viewInternshipByFilter

  }

  private void viewInternshipByFilter(HttpServletRequest request, HttpServletResponse response) {
    // TODO: implement viewInternshipByFilter

  }

  private void validateInternship(HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    TirocinioDao dao = new TirocinioDao();
    String id = request.getParameter("tirocinio");

    if (id.length() < 1) {
      throw new IllegalArgumentException("id not valid");
    }

    int tirocinioId = 0;
    try {
      tirocinioId = Integer.parseInt(id);
    } catch (Exception e) {
      throw new IllegalArgumentException("id not valid");
    }

    Tirocinio tirocinio = dao.doRetrieveByKey(tirocinioId);
    if (tirocinio == null) {
      throw new IllegalArgumentException("tirocinio not found");
    }

    String stato = request.getParameter("status");

    if (stato.equals(Tirocinio.ACCETTATA)) {
      tirocinio.setStato(Tirocinio.ACCETTATA);
    } else if (stato.equals(Tirocinio.RIFIUTATA)) {
      String description = request.getParameter("motivation");
      tirocinio.setStato(Tirocinio.RIFIUTATA);
      if (description.length() < 1) {
        throw new IllegalArgumentException("motivation not valid");
      }
      // TODO: implement description in Tirocinio bean and DB schema
    } else {
      throw new IllegalArgumentException("status not valid");
    }

    dao.doSave(tirocinio);
  }
}
