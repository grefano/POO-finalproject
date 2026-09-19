package com.meuprojeto.app;

import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Array;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.Gson;


// TODO: terminal
// FIXME: repetição classe XRegistry que define id
//
record GameId(Integer id) {};
record UserId(Integer id) {};
record RentId(Integer id) {};

enum GameGenre { two_d, three_d, platformer, fps, triple_a, indie, schmovement, bossfight }
// ? dispositivo (console / pc), versão (ps2 / ps5), marca (nintendo / playstation), sistema operacional (windows / linux)
enum GamePlatform { console, mobile, pc }

record User(UserId id, String name){}

class UserRegistry {
  static UserId last_id = new UserId(0);

  public static User create(String name) {
    // FIXME: se eu defino o last_id antes de criar os users ele pode ser maior que o necessario se os users criados ja existirem
    UserRegistry.last_id = new UserId(UserRegistry.last_id.id() + 1);
    return new User(new UserId(UserRegistry.last_id.id() - 1), name);
  }
}
class UserProfile { // FIXME: UserProfile vs UserRegistry
  // historico de alugueis

}

class UserManage {
  public static void register_user() {

  }
  public static void register_admin() {

  }
}
class Auth {
  public static void login(String username, String password) {

  }
}
record Game( GameId id, String title, GameGenre[] genre, GamePlatform[] platform, Integer quantity ) {}


class GameRegistry {
  static GameId last_id = new GameId(0);

  public static Game create(String title, GameGenre[] genre, GamePlatform[] platform, Integer quantity) {
    GameRegistry.last_id = new GameId(GameRegistry.last_id.id() + 1);
    return new Game(new GameId(GameRegistry.last_id.id() - 1), title, genre, platform, quantity);
  }
}
/*
public class GameManage {
  static GameRegistry[] games;

  // FIXME: >1 genre, >1 platform, etc
  public static void register_game(String title, GameGenre genre, GamePlatform platform) { 
    GameManage.games = [];

  }

  // TODO: search by X AND Y
  public static GameRegistry[] games_by_title(String title) {

  }
  public static GameRegistry[] games_by_platform() {

  }
  public static GameRegistry[] games_by_genre() {

  }
}

public class Rent {
  private RentId id;
  private Date return_deadline;
  private UserId user;
  private GameId[] games;
}
public class GameRentManage {
  public static void rent(GameId game, UserId user) {
    DataPersistence.insert_rent();

  }
  public static void rent_return(RentId rent) {
    // TODO: penalidade por atraso

  }

}
*/
// abstração do banco de dados ou filesystem
class DataPersistence {
  static String directory = "data/";

  // converte qualquer um dos modelos (game, rent, ...) em uma string no formato que sera armazenado
  private static <T> String model_object_to_string(T obj) {
    Gson gson = new Gson();
    String result = gson.toJson(obj);
    System.out.println(result);
    return result;


  }
  private static <T> List<T> read_all_rows(Path path_file, Class<T> type) throws IOException{
    Gson gson = new Gson();
    try (Stream<String> lines = Files.lines(path_file)){
      return lines.map(line -> gson.fromJson(line, type)).collect(Collectors.toList());
    }


  }
  public static void insert_game(Game game) {
    // TODO: falhar se nome já está sendo usado

    Path path = Paths.get(DataPersistence.directory + "games/");
    Path path_file = Path.of(path.toString(), "teste.txt");
    // try {
    //   Files.createDirectories(Path.of(path.toString(), "teste.txt"));
    // } catch (IOException e){
    //   System.out.println(e);
    // }
    try {



      Files.createFile(path_file);
    } catch (IOException e) {
      System.out.println(e);

    }
      List<Game> games = get_games_all();
      for (Game g : games) {
	if (game.title().equals(g.title())){
	  System.out.println("equal");
	  return;

	}
      	
      }


      String game_str = model_object_to_string(game) + System.lineSeparator();
      System.out.println(game_str);

      try {

      Files.write(path_file, game_str.getBytes(), StandardOpenOption.APPEND);
      } catch (IOException e) {
	System.out.println(e);
      }


  }
  // public static GameRegistry get_game(GameId id) {
  //
  // }
  // public static GameRegistry get_game_by_name(String name) {
  //
  // }
  public static List<Game> get_games_all() {

    Path path = Paths.get(DataPersistence.directory + "games/teste.txt");
    try {

    List<Game> games = DataPersistence.read_all_rows(path, Game.class);
    return games;
    } catch (IOException e){
      return new ArrayList<Game>();
    }
    

  }
  public static void insert_rent() {

  }
  // public static Rent get_rent(RentId id) {
  //
  // }
  public static void insert_user() {

  }
  // public static User get_user(UserId id) {
  //
  // }



}
class Cli {

}
// não deve se responsabilizar por detalhes de implementação do terminal ou da gui
class UI {

}
class App {

  public static void main(String[] args) {
    System.out.println("main");


    List<Game> games = DataPersistence.get_games_all();
    for (Game g : games) {
      if (g.id().id() > GameRegistry.last_id.id()){
	GameRegistry.last_id = g.id();
      }
    	
    }
    
    DataPersistence.insert_game(
      GameRegistry.create("celeste", new GameGenre[]{GameGenre.indie, GameGenre.platformer}, new GamePlatform[]{GamePlatform.pc}, 12)
    );
    DataPersistence.insert_game(
      GameRegistry.create("cuphead", new GameGenre[]{GameGenre.indie, GameGenre.bossfight}, new GamePlatform[]{GamePlatform.pc}, 5)
    );
    DataPersistence.insert_game(
      GameRegistry.create("fezes", new GameGenre[]{GameGenre.triple_a, GameGenre.three_d, GameGenre.fps}, new GamePlatform[]{GamePlatform.console}, 999)
    );
    DataPersistence.insert_game(
      GameRegistry.create("testedenovo", new GameGenre[]{GameGenre.triple_a, GameGenre.three_d, GameGenre.fps}, new GamePlatform[]{GamePlatform.console}, 999)
      );

      for (Game g : games) {
      	
      System.out.print(g);
      }
    // GameManage.register_game();
  }
  
}
