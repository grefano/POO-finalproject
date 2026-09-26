package com.meuprojeto.app;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.meuprojeto.app.DataPersistence.Row;

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;


// TODO: terminal
// TODO: search game by X AND Y

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
    int id = last_ids.merge(type, 1, Integer::sum) - 1;
    return factory.apply(id);
  }
  

  // atualiza o last_id do model de acordo com as rows dos txt
  // pq precisa começar a gerar os ids a partir do maior id anterior
  public static <T extends Model<T>> Integer load_inc_id(Class<T> type, Function<T, Integer> get_pk){


    List<T> objs = DataPersistence.get_row_all(type);
    System.out.println("objs: " + objs);
    for (T o : objs) {
      System.out.println(type + " 119: " + last_ids.get(type));

      if (last_ids.get(type) != null &&  get_pk.apply(o) > last_ids.get(type)){
	Registry.last_ids.put(type, get_pk.apply(o));
      }
    }
    return last_ids.get(type) == null ? 0 : last_ids.get(type) ;
  }

  static void reset_last_ids(){
    last_ids.replaceAll((k, v) -> 0);
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
class GameRentManage {
  public static boolean rent(Map<GameId, Integer> games, UserId user) {
    List<Row<Rent>> user_rents = DataPersistence.get_by_attribute(Rent.class, o -> o.user().equals(user));
    AtomicReference<Integer> amount_games_rented = new AtomicReference<>(0);
    user_rents.forEach((o) -> amount_games_rented.set(amount_games_rented.get() + o.get().games().length));

    int amount_games = games.values().stream().mapToInt(Integer::intValue).sum();


    System.out.println("rent amount games: " + (amount_games_rented.get() + amount_games));
    if (amount_games_rented.get() + amount_games > 3) {
      return false;
    }
    HashMap<GameId, Game> whole_games = new HashMap<>();

    for(Map.Entry<GameId, Integer> entry : games.entrySet() ){
      GameId g = entry.getKey();
      Integer qtd = entry.getValue();

      Game gg = DataPersistence.get_first_by_attribute(Game.class, o -> o.id().equals(g)).get();
      whole_games.put(g, gg);

      if (gg == null){
	throw new Error("gameId doesnt correspond to an existing game");
      }
      if (gg.quantity() - qtd < 0){
	return false;
      }

    }
    // update games quantity
    for(Map.Entry<GameId, Integer> entry : games.entrySet() ){
      Game game = whole_games.get(entry.getKey());
      Game newgame =new Game(game.id(), game.title(), game.genre(), game.platform(), game.quantity() - entry.getValue(), game.price());

      DataPersistence.update(Game.class, newgame);
    }
    DataPersistence.insert(
	Registry.create(Rent.class, 
	  id -> new Rent(new RentId(id), new Date(), new Date(), user, games.keySet().toArray(new GameId[0]), RentState.pending, 0f)
    ));


    return true;

  }
  public static boolean rent_return(RentId rent) {
    Rent rent_whole = DataPersistence.get_first_by_attribute(Rent.class, o -> o.id().equals(rent)).get();
    System.out.println(rent_whole);
    if (!rent_whole.state().equals(RentState.pending) && !rent_whole.state().equals(RentState.pending_late)) {
      return false;
    }

    Float price_total = get_price(rent_whole);
    Rent new_rent = new Rent(rent_whole.id(), rent_whole.date(), rent_whole.return_deadline(), rent_whole.user(), rent_whole.games(), RentState.returned,  // rs
	price_total); 

    DataPersistence.update(Rent.class, new_rent);


    // update games quantity
    for(GameId gid : rent_whole.games() ){
      Game game = DataPersistence.get_first_by_attribute(Game.class, o -> o.id().equals(gid)).get();

      Game newgame =new Game(game.id(), game.title(), game.genre(), game.platform(), game.quantity() + 1, game.price()); // FIXME: adicionar quantidade real q foi alugado

      DataPersistence.update(Game.class, newgame);
    }
    return true;
  }
  public static Float get_games_total_price(GameId[] games){
    Float result = 0f;
    for(GameId g : games) {
      Game game_whole = DataPersistence.get_first_by_attribute(Game.class, o -> o.id().equals(g)).get();
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
    try (Stream<String> lines = Files.lines(path_file).filter(s -> s != "")){
      return lines.map(line -> gson.fromJson(line, type)).collect(Collectors.toList());
    }


  }
  static <T extends Model<T>> void search_by_attribute(Class<T> type, Function<T, Boolean> pred, BiFunction<T, Integer, Void> act) {

    List<T> rows = get_row_all(type);

    for(int i = 0; i < rows.size(); i++) {
      T r = rows.get(i);
      if (r == null){
	continue;
      }
      if (pred.apply(r)){
	act.apply(r, i);

      }
    }
    System.out.println("get by attr NULL: " + type.getSimpleName());
  } 
  static <T extends Model<T>> List<Row<T>> get_by_attribute(Class<T> type, Function<T, Boolean> pred) {
    List<Row<T>> result = new ArrayList<>();
    search_by_attribute(type, pred, (r, i) -> {
      result.add(new Row<T>(i.intValue(), r));
      return null; // ????? compilador burro
    });
    return result;

  } 
  static <T extends Model<T>> Row<T> get_first_by_attribute(Class<T> type, Function<T, Boolean> pred) {
    AtomicReference<Row<T>> result = new AtomicReference<>();
    search_by_attribute(type, pred, (r, i) -> {
      result.set(new Row<T>(i.intValue(), r));
      return null;
    });

    return result.get();

  }
  static <T extends Model<T>> Path dir_model(Class<T> type) {
    String dir = DataPersistence.directory + type.getSimpleName().toLowerCase() + "s/all.txt";
    System.out.println("dir: " + dir);
    Path path = Paths.get(dir);
    return path;
  }
  public static <T extends Model<T>> void update(Class<T> type, T newrow){
    Row<T> row = get_first_by_attribute(type, o -> o.same(newrow));
    System.out.println("update row find: " + model_object_to_string(row.get()));
    try {
      List<String> lines = Files.readAllLines(dir_model(type));

      String obj_str = model_object_to_string(newrow) + System.lineSeparator();
      lines.set(row.line, obj_str);
      System.out.println("update lines: " + lines);
      Files.write(dir_model(type), lines, StandardOpenOption.WRITE);
    } catch (IOException e) {
      System.err.println("ERROR " + e);

    }
  }
  public static <T extends Model<T>> void insert(T obj) {
    Path path_file = dir_model(obj.getClass());
    try {
      System.out.println("parent: " + path_file.getParent());
      Files.createDirectories(path_file.getParent());
    } catch (FileAlreadyExistsException e){
    } catch (IOException e){
      System.out.println("ERROR create dir " + e);
    }
    try {



      Files.createFile(path_file);
    } catch (FileAlreadyExistsException e){
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

    // TODO: evitar repetição e rodar pra cada classe manualmente
    // TODO: descomentar (comentei pq ainda n funciona)
    Registry.load_inc_id(Game.class, o -> o.id().id());
    Registry.load_inc_id(Rent.class, o -> o.id().id());
    Registry.load_inc_id(User.class, o -> o.id().id());

    Game g = Registry.create(Game.class, id -> 
	  new Game(new GameId(id), "fezes", new GameGenre[]{GameGenre.triple_a, GameGenre.three_d, GameGenre.fps}, new GamePlatform[]{GamePlatform.console}, 999, 9999.99f)
	  );
    DataPersistence.insert( g );



    User eu = Registry.create(User.class, id -> 
	  new User(new UserId(id), "gabriel", "123456789"));

    DataPersistence.insert( eu );

    User u = DataPersistence.get_first_by_attribute(User.class, o -> o.cpf().equals("123456789")).get();
    System.out.println("u by cpf" + u);


    HashMap<GameId, Integer> games_rent = new HashMap<GameId, Integer>();
    games_rent.put(g.id(), 1);
    GameRentManage.rent(games_rent, u.id());



    // GameManage.register_game();
  }

}
