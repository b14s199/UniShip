package applicationlogic.usersmanagment;

import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javafx.util.Pair;
import javax.naming.AuthenticationException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import storage.PasswordHash;
import storage.beans.Azienda;
import storage.beans.RichiestaDisponibilita;
import storage.beans.Studente;
import storage.beans.Utente;
import storage.dao.AziendaDao;
import storage.dao.RichiestaDisponibilitaDao;
import storage.dao.StudenteDao;
import storage.interfaces.AziendaInterface;

//TODO: Aggiungere retrieve free companies all'ODD
// ho aggiunto il metodo retrieve free companies
@WebServlet("/HandleUserServlet")
public class HandleUserServlet extends HttpServlet {

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    PrintWriter out = response.getWriter();
    response.setContentType("application/json");
    Gson obj = new GsonBuilder().serializeNulls().create();
    Map<String, String> result = new HashMap<>();
    try {

      Utente user = (Utente) request.getSession().getAttribute("utente");
      if (user == null) {
        throw new AuthenticationException("Not Authorized");
      }

      String action = request.getParameter("action");
      if (action != null && action.equals("retrieveCompanies")) {
        ArrayList<Pair> results = retrieveFreeCompanies(request, response);
        out.println(obj.toJson(results));
        return;
      }
      if (action.equals("addCompany")) {
        if (!user.getTipo().equals(Utente.UFFICIO_CARRIERE) && !user.getTipo()
            .equals(Utente.ADMIN)) {
          throw new AuthenticationException("Not Authorized");
        } else if (addCompany(request, response)) {
          result.put("status", "200");
          // result.put("redirect", "index.jsp");
        } else {
          throw new RuntimeException();
        }
      } else if (action.equals("changeCompanyData")) {
        if (!user.getTipo().equals(Utente.UFFICIO_CARRIERE) && !user.getTipo()
            .equals(Utente.ADMIN) && !user.getEmail().equals(request.getParameter("azienda"))) {
          throw new AuthenticationException("Not Authorized");
        } else if (changeCompanyData(request, response)) {
          result.put("status", "200");
        } else {
          throw new RuntimeException();
        }
      } else {
        result.put("status", "400");
        result.put("description", "Invalid Request");
      }

    } catch (AuthenticationException e) {
      result.put("status", "403");
      result.put("description", e.getMessage());
    } catch (IllegalArgumentException e) {
      result.put("status", "422");
      result.put("description", e.getMessage());
    } catch (Exception ex) {
      result.put("status", "422");
      result.put("description", "Generic error.");
    }

    out.println(obj.toJson(result));
  }


  private boolean addCompany(HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    String email = request.getParameter("email");
    if (email.length() == 0) {
      throw new IllegalArgumentException("Email too short");
    } else if (email.length() > 50) {
      throw new IllegalArgumentException("Email too long");
    } else if (!email.matches("[0-9a-zA-Z.]+@[a-z.]+.[a-z]+")) {
      throw new IllegalArgumentException("Email not valid");
    }
    AziendaInterface dao = new AziendaDao();

    if (dao.doRetrieveByKey(email) != null) {
      throw new IllegalArgumentException("Email already registered");
    }

    String nome = request.getParameter("nome");
    if (nome.length() == 0) {
      throw new IllegalArgumentException("Nome too short.");
    } else if (nome.length() > 30) {
      throw new IllegalArgumentException("Nome too long.");
    } else if (!nome.matches("[a-zA-Z .&'-]+")) {
      throw new IllegalArgumentException("Nome invalid.");
    }

    String piva = request.getParameter("piva");
    if (!piva.matches("[0-9]{11}")) {
      throw new IllegalArgumentException("Partita IVA invalid.");
    }

    String indirizzo = request.getParameter("indirizzo");
    if (indirizzo.length() == 0) {
      throw new IllegalArgumentException("Indirizzo too short.");
    } else if (!indirizzo.matches("[A-z 0-9,]+")) {
      throw new IllegalArgumentException("Indirizzo invalid.");
    }

    String rappresentante = request.getParameter("rappresentante");
    if (rappresentante.length() == 0) {
      throw new IllegalArgumentException("rappresentante too short.");
    } else if (!rappresentante.matches("[A-z ]+")) {
      throw new IllegalArgumentException("rappresentante invalid.");
    }

    String codAteco = request.getParameter("codAteco");
    if (codAteco.length() == 0) {
      throw new IllegalArgumentException("Codice ateco too short.");
    } else if (!codAteco.matches("[A-Z0-9.]+")) {
      throw new IllegalArgumentException("Codice ateco invalid.");
    }

    String numeroDipendenti = request.getParameter("numeroDipendenti");
    if (!numeroDipendenti.matches("[0-9]+")) {
      throw new IllegalArgumentException("numeroDipendenti invalid.");
    }

    //TODO autogenerazione della password e invio tramite mail
    String password = PasswordHash.createHash("password");

    Azienda azienda = new Azienda(email, nome, password, piva, indirizzo, rappresentante, codAteco,
        Integer.parseInt(numeroDipendenti));

    return dao.doSave(azienda);
  }

  private boolean changeCompanyData(HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    PrintWriter out = response.getWriter();
    StringBuilder sb = new StringBuilder();
    BufferedReader br = request.getReader();
    String str;
    while ((str = br.readLine()) != null) {
      sb.append(str);
    }
    response.setContentType("application/json");

    JsonObject jsonRequest = Jsoner.deserialize(sb.toString(), new JsonObject());
    if (jsonRequest.containsKey("email")) {
      String email = (String) jsonRequest.get("email");
      if (email.length() < 1) {
        JsonObject obj = new JsonObject();
        obj.put("status", "422");
        obj.put("description", "Email too short");
        out.println(obj.toJson());

      } else if (email.length() > 50) {
        JsonObject obj = new JsonObject();
        obj.put("status", "422");
        obj.put("description", "Email too long");
        out.println(obj.toJson());
      } else if (!email.matches("[0-9a-zA-Z.]")) {
        JsonObject obj = new JsonObject();
        obj.put("status", "422");
        obj.put("description", "Email not valid");
        out.println(obj.toJson());
      }

      if (jsonRequest.containsKey("password")) {
        String password = (String) jsonRequest.get("password");
        if (password.length() < 8) {
          JsonObject obj = new JsonObject();
          obj.put("status", "422");
          obj.put("description", "Password too short");
          out.println(obj.toJson());

        } else if (!password.matches("[0-9a-zA-Z]{8,}")) {
          JsonObject obj = new JsonObject();
          obj.put("status", "422");
          obj.put("description", "Password invalid");
          out.println(obj.toJson());
        }

        if (jsonRequest.containsKey("nome")) {
          String nome = (String) jsonRequest.get("nome");
          if (nome.length() == 0) {
            JsonObject obj = new JsonObject();
            obj.put("status", "422");
            obj.put("description", "Nome too short");
            out.println(obj.toJson());

          } else if (nome.length() > 30) {
            JsonObject obj = new JsonObject();
            obj.put("status", "422");
            obj.put("description", "Nome too long");
            out.println(obj.toJson());

          } else if (!nome.matches("[0-9a-zA-Z]+")) {
            JsonObject obj = new JsonObject();
            obj.put("status", "422");
            obj.put("description", "Nome invalid");
            out.println(obj.toJson());
          }

          String cognome = (String) jsonRequest.get("cognome");
          if (cognome.length() == 0) {
            JsonObject obj = new JsonObject();
            obj.put("status", "422");
            obj.put("description", "Cognome too short");
            out.println(obj.toJson());
          } else if (cognome.length() > 30) {
            JsonObject obj = new JsonObject();
            obj.put("status", "422");
            obj.put("description", "Cognome too long");
            out.println(obj.toJson());
          } else if (!cognome.matches("[0-9a-zA-Z]+")) {
            JsonObject obj = new JsonObject();
            obj.put("status", "422");
            obj.put("description", "Cognome invalid");
            out.println(obj.toJson());
          }

          String codiceFiscale = (String) jsonRequest.get("codiceFiscale");
          if (codiceFiscale.length() != 11) {
            JsonObject obj = new JsonObject();
            obj.put("status", "422");
            obj.put("description", "codiceFiscale too short");
            out.println(obj.toJson());
          } else if (!codiceFiscale
              .matches("^[A-Z]{6}[0-9]{2}[A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z]$")) {
            JsonObject obj = new JsonObject();
            obj.put("status", "422");
            obj.put("description", "codiceFiscale invalid");
            out.println(obj.toJson());
          }

          String matricola = (String) jsonRequest.get("matricola");
          if (matricola.length() != 10) {
            JsonObject obj = new JsonObject();
            obj.put("status", "422");
            obj.put("description", "Matricola too short");
            out.println(obj.toJson());
          } else if (!matricola.matches("[0-9]{10}")) {
            JsonObject obj = new JsonObject();
            obj.put("status", "422");
            obj.put("description", "Matricola invalid");
            out.println(obj.toJson());
          }

          String dataDiNascita = (String) jsonRequest.get("dataDiNascita");
          if (dataDiNascita.length() != 9) {
            JsonObject obj = new JsonObject();
            obj.put("status", "422");
            obj.put("description", "dataDiNascita too short");
            out.println(obj.toJson());
          } else if (!dataDiNascita
              .matches("^([0-20-2][0-9]|(3)[0-1])(\\/)(((0)[0-9])|((1)[0-2]))(\\/)[0-9]{4}$")) {
            JsonObject obj = new JsonObject();
            obj.put("status", "422");
            obj.put("description", "dataDiNascita invalid");
            out.println(obj.toJson());
          }

          String cittadinanza = (String) jsonRequest.get("cittadinanza");
          if (cittadinanza.length() == 0) {
            JsonObject obj = new JsonObject();
            obj.put("status", "422");
            obj.put("description", "cittadinanza too short");
            out.println(obj.toJson());
          } else if (!cittadinanza.matches("[a-zA-Z]+")) {
            JsonObject obj = new JsonObject();
            obj.put("status", "422");
            obj.put("description", "cittadinanza invalid");
            out.println(obj.toJson());
          }

          String residenza = (String) jsonRequest.get("residenza");
          if (residenza.length() == 0) {
            JsonObject obj = new JsonObject();
            obj.put("status", "422");
            obj.put("description", "residenza too short");
            out.println(obj.toJson());
          } else if (!residenza.matches("[A-z0-9,]+")) {
            JsonObject obj = new JsonObject();
            obj.put("status", "422");
            obj.put("description", "residenza invalid");
            out.println(obj.toJson());
          }

          String numero = (String) jsonRequest.get("numero");
          if (!numero.matches("[0-9]{9,12}")) {
            JsonObject obj = new JsonObject();
            obj.put("status", "422");
            obj.put("description", "numero invalid");
            out.println(obj.toJson());
          }

          Studente utente = new Studente(email, nome, password, codiceFiscale, matricola,
              Date.valueOf(dataDiNascita), cittadinanza, residenza, numero, cognome);
          if ((new StudenteDao()).doSave(utente)) {
            JsonObject obj = new JsonObject();
            obj.put("status", "200");
            obj.put("description", "ok");
            out.println(obj.toJson());
          } else {
            JsonObject obj = new JsonObject();
            obj.put("status", "400");
            obj.put("description", "unknown error");
            out.println(obj.toJson());
          }
        }
      }
    }

    return true;
  }

  private ArrayList<Pair> retrieveFreeCompanies(HttpServletRequest request,
      HttpServletResponse response) {
    ArrayList<Pair> result = new ArrayList<>();
    RichiestaDisponibilitaDao richDisp = new RichiestaDisponibilitaDao();
    AziendaDao azienda = new AziendaDao();
    Utente utente = (Utente) request.getSession().getAttribute("utente");

    try {
      Studente studente = new StudenteDao().doRetrieveByKey(utente.getEmail());
      ArrayList<RichiestaDisponibilita> richieste = richDisp.doRetrieveByStudente(studente);
      ArrayList<Azienda> aziendeDispo = azienda.doRetrieveAll();
      for (Azienda a : aziendeDispo) {
        RichiestaDisponibilita ri = null;
        for (RichiestaDisponibilita r : richieste) {
          if (r.getAzienda().equals(a)) {
            ri = r;
          }
        }
        result.add(new Pair(a, ri));
      }
    } catch (SQLException e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return null;
    }
    return result;

  }
}