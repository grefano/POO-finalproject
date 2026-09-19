package com.meuprojeto.app;



import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.google.gson.Gson;


// TODO: serialiazar e desserializar objetos (com GSon?)
// TODO: terminal
record GameId(Integer id) {};
record UserId(Integer id) {};
record RentId(Integer id) {};

enum GameGenre {
  two_d, three_d, platformer, fps, triple_a, indie, schmovement, bossfight
}
// ? dispositivo (console / pc), versão (ps2 / ps5), marca (nintendo / playstation), sistema operacional (windows / linux)
enum GamePlatform {
  console, mobile, pc
}
/*

public class UserRegistry {
  private UserId id;
}
public class UserProfile { // FIXME: UserProfile vs UserRegistry
  // historico de alugueis

}

public class UserManage {
  public static void register_user() {

  }
  public static void register_admin() {

  }
}
public class Auth {
  public static void login(String username, String password) {

  }
}
*/
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
  public static void insert_game(Game game) {

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
    // try {
    //
    //
    //   String game_str = model_object_to_string(game);
    //   System.out.println(game_str);
    //
    //   Files.write(path_file, game_str.getBytes());
    // } catch (IOException e) {
    //
    //   System.out.println(e);
    // }

    try {
      Files.lines(path_file).forEach(line -> {

	Gson gson = new Gson();
	System.out.println(line);
	Game game_read = gson.fromJson(line, Game.class);

	System.out.println(gson.toJson(game_read));
      });

    } catch (IOException e) {

      System.out.println(e);
    }

  }
  // public static GameRegistry get_game(GameId id) {
  //
  // }
  // public static GameRegistry[] get_games_all() {
  //
  // }
  // public static void insert_rent() {
  //
  // }
  // public static RentRegistry get_rent(RentId id) {
  //
  // }
  // public static void insert_user() {
  //
  // }
  // public static UserRegistry get_user(UserId id) {
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
    // GameRegistry[] registry_games = DataPersistence.get_games_all();
    System.out.println("main");


    Game game_sample = GameRegistry.create("titulo", new GameGenre[]{GameGenre.fps, GameGenre.indie}, new GamePlatform[]{GamePlatform.pc, GamePlatform.mobile}, 12);
    DataPersistence.insert_game(game_sample);
    // GameManage.register_game();
  }
  
}
