package com.meuprojeto.app;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.Gson;

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;


// TODO: criar sample do model pra facilitar testes, e alterar so alguns valores
// TODO: terminal

record GameId(Integer id) {};
record UserId(Integer id) {};
record RentId(Integer id) {};

enum GameGenre { two_d, three_d, platformer, fps, triple_a, indie, schmovement, bossfight }
enum GamePlatform { console, mobile, pc }

record User(UserId id, String name, String cpf) implements Model<User>{
  @Override
  public boolean equivalent(User o){
    return this.cpf.equals(o.cpf);
  }
  @Override
  public boolean same(User o){
    return this.id.equals(o.id);
  }
}

class UserRegistry {
  static UserId last_id = new UserId(0);

  public static User create(String name, String cpf) {
    // FIXME: se eu defino o last_id antes de criar os users ele pode ser maior que o necessario se os users criados ja existirem
    UserRegistry.last_id = new UserId(UserRegistry.last_id.id() + 1);
    return new User(new UserId(UserRegistry.last_id.id() - 1), name, cpf);
  }
}
record UserProfile(ArrayList<Rent> rent_story)  implements Model<UserProfile> { // FIXME: UserProfile vs UserRegistry
  // historico de alugueis
  @Override
  public boolean equivalent(UserProfile o){
    return true;
  }

  @Override
  public boolean same(UserProfile o) {
    return true;
  }


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
interface Model <T extends Model<T>> {
  // usado pra verificar se um objeto semelhante já foi criado (visto que não pode comparar ids nem cada atributo individualemente)
  boolean equivalent(T o);
  // usado pra verificar se é o objeto que estou procurando (geralmente pelo id)
  // comparar primary key
  boolean same(T o);

}
record Game( GameId id, String title, GameGenre[] genre, GamePlatform[] platform, Integer quantity, Float price) implements Model<Game> {
  @Override
  public boolean equivalent(Game o) {
    return o.title.equals(this.title);
  }
  @Override
  public boolean same(Game o){
    return o.id.equals(this.id);
  }
}


// responsavel por gerar id, comparar rows pela PK
class Registry {


  private static final Map<Class<?>, Integer> last_ids = new ConcurrentHashMap<>();
  
  // FIXME: no momento método só gera o id, como fazer ele ficar responsavel por qualauqer valor automatico?
  public static <T extends Model<T>> T create(Class<T> type, Function<Integer, T> factory) {
    int id = last_ids.merge(type, 1, Integer::sum) + 1;
    return factory.apply(id);
  }
}
class GameRegistry {
  static GameId last_id = new GameId(0);

  public static Game create(String title, GameGenre[] genre, GamePlatform[] platform, Integer quantity, Float price) {
    GameRegistry.last_id = new GameId(GameRegistry.last_id.id() + 1);
    return new Game(new GameId(GameRegistry.last_id.id() - 1), title, genre, platform, quantity, price);
  }
}
enum RentState {pending, pending_late, returned, returned_late};
record Rent(RentId id, Date date, Date return_deadline, UserId user, GameId[] games, RentState state, Float price_paid) implements Model<Rent> {
  @Override
  public boolean equivalent(Rent o) {
    return this.user.equals(o.user) && this.return_deadline.equals(o.return_deadline) && this.games.equals(o.games) && this.date.equals(o.date);
  }
  public boolean same(Rent o) {
    return this.id.equals(o.id);

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

*/
class GameRentManage {
  public static boolean rent(GameId[] games, UserId user) {
    Game gs = DataPersistence.get_by_attribute(Game.class, o -> o.id().equals(game)).get();
    // FIXME: comparar com a quantidade que quer alugar
    if (g.quantity() < 0){
      return false;

    }
    DataPersistence.insert(
	Registry.create(Rent.class, 
	  id -> new Rent(new RentId(id), new Date(), new Date(), user, games, RentState.pending, 0f)
    ));
    return true;

  }
  public static boolean rent_return(RentId rent) {
    // TODO: verificar se ja foi devolvido
    Rent rent_whole = DataPersistence.get_by_attribute(Rent.class, o -> o.id().equals(rent)).get();
    if (!rent_whole.state().equals(RentState.pending) && !rent_whole.state().equals(RentState.pending_late)) {
      return false;
    }

    Float price_total = get_price(rent_whole);
    Rent new_rent = new Rent(rent_whole.id(), rent_whole.date(), rent_whole.return_deadline(), rent_whole.user(), rent_whole.games(), rent_whole.state(),  // rs
	price_total); 

    try {
      DataPersistence.update(Rent.class, new_rent);
    } catch (IOException e){
      System.err.println(e);

    }
    return true;
  }
  public static Float get_games_total_price(GameId[] games){
    Float result = 0f;
    for(GameId g : games) {
      Game game_whole = DataPersistence.get_by_attribute(Game.class, o -> o.id().equals(g)).get();
      result += game_whole.price();
    }
    return result;

  }
  public static Float get_price(Rent rent) {
    Float result = get_games_total_price(rent.games());

    Date today = new Date();
    if ( today.after(rent.return_deadline()) ){
      result *= 1.5f;
    }

    return result;

  }

}
// abstração do banco de dados ou filesystem
class DataPersistence {
  static public final class Row<T>{
    private int line;
    private T obj;

    private Row(int line, T obj) {
      this.line = line;
      this.obj = obj;

    }
    T get(){
      return this.obj;
    }

  }
  static String directory = "data/";

  // converte qualquer um dos modelos (game, rent, ...) em uma string no formato que sera armazenado
  static <T extends Model<T>> String model_object_to_string(T obj) {
    Gson gson = new Gson();
    String result = gson.toJson(obj);
    System.out.println(result);
    return result;


  }
  static <T extends Model<T>> List<T> read_all_rows(Path path_file, Class<T> type) throws IOException{
    Gson gson = new Gson();
    try (Stream<String> lines = Files.lines(path_file)){
      return lines.map(line -> gson.fromJson(line, type)).collect(Collectors.toList());
    }


  }
  static <T extends Model<T>> Row<T> get_by_attribute(Class<T> type, Function<T, Boolean> pred) {
    List<T> rows = get_row_all(type);
    for(int i = 0; i < rows.size(); i++) {
      T r = rows.get(i);
      if (r == null){
	continue;
      }
      if (pred.apply(r)){
	return new Row<T>(i, r);

      }
    }
    return null;


  }
  static <T extends Model<T>> Path dir_model(Class<T> type) {
    String dir = DataPersistence.directory + type.getSimpleName().toLowerCase() + "s/all.txt";
    System.out.println("dir: " + dir);
    Path path = Paths.get(dir);
    return path;
  }
  public static <T extends Model<T>> void update(Class<T> type, T newrow) throws IOException {
    Row<T> row = get_by_attribute(type, o -> o.same(newrow));
    System.out.println("update row find: " + model_object_to_string(row.get()));
    List<String> lines = Files.readAllLines(dir_model(type));
    String obj_str = model_object_to_string(newrow) + System.lineSeparator();
    lines.set(row.line, obj_str);
    System.out.println("update lines: " + lines);
    Files.write(dir_model(type), lines, StandardOpenOption.WRITE);
  }
  public static <T extends Model<T>> void insert(T obj) {
    // TODO: falhar se nome já está sendo usado

    Path path_file = dir_model(obj.getClass());
    try {
      System.out.println("parent: " + path_file.getParent());
      Files.createDirectories(path_file.getParent());
    } catch (IOException e){
      System.out.println(e);
    }
    try {



      Files.createFile(path_file);
    } catch (IOException e) {
      System.out.println("ERROR create file: " + e);

    }
      List<T> objs = get_row_all((Class<T>) obj.getClass());
      for (T o : objs) {
	if (o != null && obj.equivalent(o)) {
	  return;

	}
      	
      }


      String obj_str = model_object_to_string(obj) + System.lineSeparator();
      System.out.println(obj_str);

      try {

      Files.write(path_file, obj_str.getBytes(), StandardOpenOption.APPEND);
      } catch (IOException e) {
	System.out.println("ERROR write file: " + e);
      }


  }
  // public static GameRegistry get_game(GameId id) {
  //
  // }
  // public static GameRegistry get_game_by_name(String name) {
  //
  // }
  public static <T extends Model<T>> List<T> get_row_all(Class<T> type) {

    Path path = dir_model(type);
    try {

      List<T> objs = DataPersistence.read_all_rows(path, type);
    return objs;
    } catch (IOException e){
      System.err.println("ERROR read all rows: " + e);
      return new ArrayList<T>();
    }
    

  }


public static void delete_directory(Path diretorio) throws IOException {
    Files.walkFileTree(diretorio, new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (exc != null) throw exc;
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    });
}

}
class Cli {

}
// não deve se responsabilizar por detalhes de implementação do terminal ou da gui
class UI {

}
class App {

  public static void main(String[] args) {
    System.out.println("main");


    List<Game> games = DataPersistence.get_row_all(Game.class);
    for (Game g : games) {
      System.out.println("game: " + g.toString());
      if (g.id().id() > GameRegistry.last_id.id()){
	GameRegistry.last_id = g.id();
      }

    }

    Game g = Registry.create(Game.class, id -> 
	  new Game(new GameId(id), "fezes", new GameGenre[]{GameGenre.triple_a, GameGenre.three_d, GameGenre.fps}, new GamePlatform[]{GamePlatform.console}, 999, 9999.99f)
	  );
    DataPersistence.insert( g );



    User eu = Registry.create(User.class, id -> 
	  new User(new UserId(id), "gabriel", "123456789"));

    DataPersistence.insert( eu );

    User u = DataPersistence.get_by_attribute(User.class, o -> o.cpf().equals("123456789")).get();
    System.out.println("u by cpf" + u);


    GameRentManage.rent(g.id(), u.id());



    System.out.println("games:");
    for (Game game : games) {
      System.out.print(game);
    }
    // GameManage.register_game();
  }

}
