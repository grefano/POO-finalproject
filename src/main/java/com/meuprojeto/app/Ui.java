package com.meuprojeto.app;
// não deve se responsabilizar por detalhes de implementação do terminal ou da gui

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.meuprojeto.app.DataPersistence.Query;

class UI {
  static private HashMap<GameId, Integer> rent_games;
  class Permission {
    static private UserId token = null;
    static private String username = "guest";

    // gerar token
    static public void init(String cpf){
      User u = Query.get_by_attribute(User.class, o -> o.cpf().equals(cpf)).get(0);
      token = u.id();
      username = u.name();
      System.out.println("username = " + u.name());

    }

    static public boolean can_insert_persistence(){
      if (token.equals(new UserId(0))) return true;
      if (token == null) return false;
      User u = Query.get_first_by_attribute(User.class, o -> o.id().equals(token));
      
      if (u == null || u.perms() == null) {
	return false;
      }
      for (UserPermissions perm : u.perms()) {
	if (perm == UserPermissions.adm) {
	  return true;
	}
      }
      return false;
    } 

    static public UserId get_user_id() {
      return token;
    }
    static public String get_user_name() {
      return username;
    }

  }

  static void insert_game(Game game) throws PermissionException {
    if (!Permission.can_insert_persistence()){
      throw new PermissionException("não tem permissão pra inserir objetos");
    }

    DataPersistence.insert(game);

  }

  static void start_rent(){
    if (rent_games == null){
      rent_games = new HashMap<>();
    } else {
      rent_games.clear();

    }
  }
  static void add_game_to_rent(GameId game, Integer amount){
    Integer a = rent_games.get(game);
    Integer total = (a == null ? 0 : a) + amount;
    rent_games.put(game, total);
  }
  static boolean finish_rent(){
    System.out.println(rent_games.toString());
    return GameRentManage.rent(rent_games, Permission.get_user_id());

  }

  static List<Rent> get_rent_history() {
    UserId id = Permission.get_user_id();
    List<Rent> rents = Query.get_by_attribute(Rent.class, o -> o.user().equals(id));

    Collections.sort(rents, (a, b) -> a.date().compareTo(b.date())); 

    return rents;


  }

  static void rent_return(RentId rentid) {
    GameRentManage.rent_return(rentid);

  }

  static void register(String nome, String cpf, String password) throws Exception {
    // verificar se usuario já existe
    User u = Query.get_first_by_attribute(User.class, o -> o.cpf().equals(cpf));
    if (u != null){
      login(u);
      return;
    }
    // TODO: validar cpf
    if (cpf.length() != 9) {
      throw new Exception("cpf inválido");
    }
    User newuser = Registry.create_user(nome, cpf);
    DataPersistence.insert(
      newuser
    );
    login(newuser);
  }
  static enum EVENTS_AUTH {
    SUCCESS,
    USER_INVALID,
    PASSWORD_INVALID,
  }
  static EVENTS_AUTH login(String cpf, String password) {
    User u = Query.get_first_by_attribute(User.class, o -> o.cpf().equals(cpf));
    if (u == null){
      return EVENTS_AUTH.USER_INVALID;
    }

    // TODO: comparar com a senha certa
    if (!password.equals("1234")){
      return EVENTS_AUTH.PASSWORD_INVALID;
    }
    login(u);
    return EVENTS_AUTH.SUCCESS;

  }
  static void login(User u){
    Permission.init(u.cpf());


  }

  enum EModel {
    rent, user, game
  }


}
