
package com.meuprojeto.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

import com.meuprojeto.app.DataPersistence.Query;
import com.meuprojeto.app.DataPersistence.Row;
import com.meuprojeto.app.UI.EVENTS_AUTH;
import com.meuprojeto.app.UI.Permission;

class CLI {
  // terminal
  static String line;
  static public void rent() {
    UI.start_rent();

    System.out.println("user: ");
    line = get_line();
    if (!line.equals("")){
      System.out.println("não é possível alugar para outros usuários no momento");

    } else {
      while(true){
	System.out.println("game: ");
	line = get_line();
	if (line.equals("")){
	  break;
	}
	GameId game = get_game(line);
	System.out.println("game chosen: " + game);
	UI.add_game_to_rent(game, 1);


      }

    }
    boolean couldrent = UI.finish_rent();
    if (!couldrent){
      System.out.println("couldnt rent");
    }
  }
  static public void rent_return() {
    RentId rentid = get_rent();
    UI.rent_return(rentid);

  }
  static public RentId get_rent() {
    // TODO: paginação
    List<Rent> rents = Query.get_by_attribute(Rent.class, o ->
    ( o.state().equals(RentState.pending)
      || o.state().equals(RentState.pending_late) )
      && o.user().equals(Permission.get_user_id())
    );
    for(int i = 0; i < rents.size(); i++) {
      Rent r = rents.get(i);
      System.out.println(i + ": " + r.state().toString() + " R$ " + GameRentManage.get_price(r));
    }
    while (true){
      String line = get_line();
      if (line.equals("")) {
	return null;
      }
      Integer sel = 0;
      try {
	sel = Integer.valueOf(line);
	if (sel < 0 || sel >= rents.size()) {
	  System.out.println("invalid value");
	  continue;
	}

      } catch (NumberFormatException e) {
	System.out.println("invalid value");
	continue;

      }
      return rents.get(sel).id();

    }

  }
  static public void show_all_games() {
    List<Game> games = Query.get_all(Game.class);
    for (Game g : games) {
      System.out.println(g.title() + " (" + g.quantity() + ")" + " R$"+g.price());
    }
  }
  static public void show_rent_history() {
    List<Rent> rents = UI.get_rent_history();
    rents.forEach(r -> {
      List<Game> games = new ArrayList<>();
      for (GameId g : r.games()){
	games.add(
	  Query.get_first_by_attribute(Game.class, o -> o.id().equals(g))
	);

      }
      String titles = "";
      for (Game g : games) {
	titles += " " + g.title();

      }
      System.out.println(r.state().toString() + " games [" + titles + " ]");
    });

  }
  static public void login() {
    String cpf;
    String password;
    while(true){
      System.out.println("cpf: ");
      cpf = get_line();
      System.out.println("senha: ");
      password = get_line();

      UI.EVENTS_AUTH event = UI.login(cpf, password);
      if (event == EVENTS_AUTH.SUCCESS) {
	return;
      }
      if (event == EVENTS_AUTH.USER_INVALID) {
	System.out.println("failed. user invalid");
      } 
      if (event == EVENTS_AUTH.PASSWORD_INVALID) {
	System.out.println("failed. password invalid");
      }

      System.out.println("- 1 try again \n- 2 create account\n- 3 exit");
      line =  get_line();
      if (line.equals("1")) {
	continue;
      }
      if (line.equals("2")) {
	// TODO: se usuário existe não passar o mesmo form pro registro
	if (event == UI.EVENTS_AUTH.USER_INVALID) {
	  register(cpf, password);

	}
	return;
      }
      if (line.equals("3")) {
	return;
      }


    }
  }
  static public void register(String cpf, String password) {
    System.out.println("name: ");
    String name = get_line();
    // TODO: scan input qnd caller não passa
    while(true){
      try {
	UI.register(name, cpf, password);
	break;

      } catch (Exception e) {
	System.out.println(e.toString());
      }
      System.out.println("- 1 try again\n- 2 exit");
      line = get_line();
      if (line.equals("2")) {
	break;
      }

      System.out.println("cpf: ");
      cpf = get_line();
      System.out.println("senha: ");
      password = get_line();

    }
  }
  static public void loop() {
    while(true){

      System.out.println("=== MENU ===");
      System.out.println("you are logged as " + Permission.get_user_name());
      System.out.println("- 1 rent\n- 2 show all games\n- 3 show rent history\n- 4 login\n- 5 insert game\n- 6 rent return\n- 0 quit");
      String line = get_line();
      if (line.equals("1")) {
	rent();
      }
      if (line.equals("2")) {
	show_all_games();
      }
      if (line.equals("3")) {
	show_rent_history();
      }
      if (line.equals("4")) {
	login();
      }
      if (line.equals("5")) {
	insert_game();
      }
      if (line.equals("6")) {
	rent_return();
      }
      if (line.equals("0") || line.equals("quit") || line.equals("q")) {
	break;
      }

    }
    scan.close();

  }
  public static void insert_game() {
    if (!Permission.can_insert_persistence()) {
      System.out.println("have no permission to insert new game");
      return;
    }
    Game game = get_new_game();
    try {
      System.out.println("insert new get game: " + game.toString());
      UI.insert_game(game);

    } catch (PermissionException e){
      System.out.println("have no permission to insert new games");
    }
  }
  public static <T extends Enum<T>> T get_enum(String l, Class<T> classe) {
    try {
      return Enum.valueOf(classe, l);
    } catch (IllegalArgumentException e) {
      System.out.println("chosen option doesnt exist");
      System.out.println("- 1 continue\n- 2 show all options\n- 3 create genre");
      String line = get_line();
      if (line.equals("2")) {
	for (T enu : classe.getEnumConstants()) {
	  System.out.print(enu.toString() + ", ");
	}
	System.out.println();

      }
      if (line.equals("3")) {
	// TODO
	System.out.println("unimplemented");
      }
      return null;

    }

  }
  public static Game get_new_game() {
    System.out.println("title: ");
    String title = get_line();

    System.out.println("genres: ");
    List<GameGenre> genre = get_list(l -> {
      return get_enum(l, GameGenre.class);
    });

    System.out.println("platforms: ");
    List<GamePlatform> platform = get_list(l -> {
      return get_enum(l, GamePlatform.class);
    });

    System.out.println("quantity: ");
    Integer quantity = 0;
    quantity = Integer.valueOf(get_line());

    System.out.println("price: ");
    Float price = 0.0f;
    price = Float.valueOf(get_line());



    return Registry.create_game(title, genre.toArray(new GameGenre[0]), platform.toArray(new GamePlatform[0]), quantity, price);
    

  }
  public static <T> List<T> get_list(Function<String, T> on_input) {
    List<T> result = new ArrayList<>();

    while(true){
      String line = get_line();
      T o = on_input.apply(line);
      if (o != null){
	result.add(o);
      }
      for (T i : result) {
	System.out.println(i.toString());
      }

      if (line.isBlank()){
	System.out.println(result.toString());
	return result;
      }


    }

  }

  public static String get_game_title() {
    return get_line();
  }
  public static Integer get_game_quantity() {
    String line = get_line();
    return Integer.valueOf(line);
  }

  static Scanner scan = new Scanner(System.in);
  public static String get_line(){
    return scan.nextLine();
  }


  public static GameId get_game(String title) {
    Row<Game> result = Query.search_first_by_attribute(Game.class, o -> o.get().title().equals(title));
    if (result == null) {
      return null;
    }
    return result.get().id();

  } 


}
