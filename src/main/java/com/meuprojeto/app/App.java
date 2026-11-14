package com.meuprojeto.app;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.meuprojeto.app.DataPersistence.Query;
import com.meuprojeto.app.DataPersistence.Row;
import com.meuprojeto.app.UI.EVENTS_AUTH;
import com.meuprojeto.app.UI.Permission;

import java.nio.file.*;


// TODO: cli
// TODO: search game by X AND Y

record GameId(Integer id) {};
record UserId(Integer id) {};
record RentId(Integer id) {};

enum GameGenre { two_d, three_d, platformer, fps, triple_a, indie, schmovement, bossfight }
enum GamePlatform { console, mobile, pc }

enum UserPermissions { adm } 

record User(UserId id, String name, String cpf, UserPermissions[] perms) implements Model<User>{
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
  // public static <T extends Model<T>> T create(Class<T> type, Function<Integer, T> factory) {
  //   int id = last_ids.merge(type, 1, Integer::sum) - 1;
  //   return factory.apply(id);
  // }

  private static <T extends Model<T>> Integer get_id(Class<T> type){
    int id = last_ids.merge(type, 1, Integer::sum) - 1;
    System.err.println("get id for " + type + " = " + id);
    return id;
  }
  // if a object is created now, what would be its id?
  public static <T extends Model<T>> Integer get_inc_id(Class<T> type){
    Integer id = last_ids.get(type);
    return id == null ? 0 : id;
  }

  public static Game create_game(String title, GameGenre[] genre, GamePlatform[] platform, Integer quantity, Float price){
    return new Game(new GameId(get_id(Game.class)), title, genre, platform, quantity, price);
  }
  public static Rent create_rent(UserId user, Date deadline, GameId[] games){
    return create_rent(user, deadline, new Date(), games);
  }
  public static Rent create_rent(UserId user, Date date, Date deadline, GameId[] games){
    return new Rent(new RentId(get_id(Rent.class)), date, deadline, user, games, RentState.pending, 0f);
  }
  public static User create_user(String name, String cpf){
    return new User(new UserId(get_id(User.class)), name, cpf, new UserPermissions[]{});
  }
  public static User create_user(String name, String cpf, UserPermissions[] perms){
    return new User(new UserId(get_id(User.class)), name, cpf, perms);
  }

  // atualiza o last_id do model de acordo com as rows dos txt
  // pq precisa começar a gerar os ids a partir do maior id anterior
  public static <T extends Model<T>> Integer load_inc_id(Class<T> type, Function<T, Integer> get_pk){


    List<T> objs = Query.get_all(type);
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
  public static boolean rent(Map<GameId, Integer> games, UserId user){
    Date nextweek = Date.from(Instant.now().plus(7, ChronoUnit.DAYS));

    return rent(games, user, nextweek);
  }
  public static boolean rent(Map<GameId, Integer> games, UserId user, Date deadline) {
    List<Rent> user_rents = Query.get_by_attribute(Rent.class, o -> o.user().equals(user));
    AtomicReference<Integer> amount_games_rented = new AtomicReference<>(0);
    user_rents.forEach((o) -> amount_games_rented.set(amount_games_rented.get() + o.games().length));

    int amount_games = games.values().stream().mapToInt(Integer::intValue).sum();


    System.out.println("amount games rented: " + amount_games_rented.get() + " amount games: " + amount_games);
    if (amount_games_rented.get() + amount_games > 3) {
      return false;
    }
    HashMap<GameId, Game> whole_games = new HashMap<>();

    for(Map.Entry<GameId, Integer> entry : games.entrySet() ){
      GameId g = entry.getKey();
      Integer qtd = entry.getValue();

      System.out.println("search same as " + g.id());
      Game gg = Query.get_first_by_attribute(Game.class, o -> o.id().equals(g));
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
	Registry.create_rent(user, deadline, games.keySet().toArray(new GameId[0]))
    );


    return true;

  }
  public static boolean rent_return(RentId rent) {
    Rent rent_whole = Query.get_first_by_attribute(Rent.class, o -> o.id().equals(rent));
    if (!rent_whole.state().equals(RentState.pending) && !rent_whole.state().equals(RentState.pending_late)) {
      return false;
    }

    Float price_total = get_price(rent_whole);
    Rent new_rent = new Rent(rent_whole.id(), rent_whole.date(), rent_whole.return_deadline(), rent_whole.user(), rent_whole.games(), RentState.returned,  // rs
	price_total); 

    DataPersistence.update(Rent.class, new_rent);


    // update games quantity
    for(GameId gid : rent_whole.games() ){
      Game game = Query.get_first_by_attribute(Game.class, o -> o.id().equals(gid));

      Game newgame =new Game(game.id(), game.title(), game.genre(), game.platform(), game.quantity() + 1, game.price()); // FIXME: adicionar quantidade real q foi alugado

      DataPersistence.update(Game.class, newgame);
    }
    return true;
  }
  public static Float get_games_total_price(GameId[] games){
    Float result = 0f;
    for(GameId g : games) {
      Game game_whole = Query.get_first_by_attribute(Game.class, o -> o.id().equals(g));
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

  static <T extends Model<T>> List<T> string_to_model_object(List<String> str, Class<T> type) {
    List<T> result = new ArrayList<>(); 
    Gson gson = new Gson();
    for (String line : str) {
      T o = gson.fromJson(line, type);
      result.add(o);
    }
    return result;

  }
  // converte qualquer um dos modelos (game, rent, ...) em uma string no formato que sera armazenado
  static <T extends Model<T>> List<String> model_object_to_string(List<T> os) {
    Gson gson = new Gson();
    List<String> result = new ArrayList<>();
    for (T o : os) {
      String str = gson.toJson(o);
      result.add(str);
    }
    System.out.println(result);
    return result;


  }
  static <T extends Model<T>> T string_to_model_object(String str, Class<T> type) {
    List<String> ss = new ArrayList<>();
    ss.add(str);
    return string_to_model_object(ss, type).get(0);
  }
  static <T extends Model<T>> String model_object_to_string(T o){
    List<T> os = new ArrayList<T>();
    os.add(o);
    return model_object_to_string(os).get(0);

  }
  static <T extends Model<T>> Path dir_model(Class<T> type) {
    String dir = DataPersistence.directory + type.getSimpleName().toLowerCase() + "s/all.txt";
    Path path = Paths.get(dir);
    return path;
  }
  public static <T extends Model<T>> void update(Class<T> type, T newrow){
    Row<T> row = Query.search_first_by_attribute(type, o -> o.get().same(newrow));
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
      List<T> objs = Query.get_all((Class<T>) obj.getClass());
      for (T o : objs) {
	if (o != null && obj.equivalent(o)) {
	System.out.println("equivalent get bent" + model_object_to_string(obj) + " " + model_object_to_string(o));
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
  public static <T extends Model<T>> void remove(Class<T> type, Function<T, Boolean> pred) {

    Path path_file = dir_model(type);
    Row<T> row = Query.search_first_by_attribute(type, r -> pred.apply(r.get()));
    System.out.println("remove row " + row.get());
    try {
      List<String> lines = Files.readAllLines(dir_model(type));
      System.out.println(row.line + " remove lines " + lines);
      lines.set(row.line, "");
Files.write(path_file, lines, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException e) {
      System.out.println("ERROR remove " + e);
    }

  }

  public static <T extends Model<T>> List<Row<T>> get_row_all(Class<T> type) {

    Path path = dir_model(type);
    try {

      List<Row<T>> objs = new ArrayList<>();
      List<String> lines = Files.lines(path).collect(Collectors.toList());
      for (int i = 0; i < lines.size(); i++) {
	if (lines.get(i).isBlank()){
	  continue;
	}

	objs.add(new Row<T>(i, string_to_model_object(lines.get(i), type) ));
      }
      return objs;
    } catch (IOException e){
      System.err.println("ERROR read all rows: " + e);
      return new ArrayList<Row<T>>();
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


  public class Query {

    static <T extends Model<T>> void search_by_attribute(List<Row<T>> rows, Function<Row<T>, Boolean> pred, Function<Row<T>, Boolean> act) {
      // search(Game.class, o -> o.id < 1000, (o, i) -> )
      for(int i = 0; i < rows.size(); i++) {
	Row<T> r = rows.get(i);
	if (r == null || r.get() == null){
	  continue;
	}
	if (pred.apply(r)){
	  Boolean end = act.apply(r);
	  if (end){
	    return;
	  }

	}
      }
    }
    static <T extends Model<T>> void search_by_attribute(Class<T> type, Function<Row<T>, Boolean> pred, Function<Row<T>, Boolean> act) {

      List<Row<T>> rows = get_row_all(type);
      search_by_attribute(rows, pred, act);

    } 
    static <T extends Model<T>> Row<T> search_first_by_attribute(Class<T> type, Function<Row<T>, Boolean> pred) {

      AtomicReference<Row<T>> result = new AtomicReference<>();
      search_by_attribute(type, pred, o -> {
	result.set(o);
	return true;
      });
      return result.get();


    }
    static <T extends Model<T>> List<T> get_by_attribute(Class<T> type, Function<T, Boolean> pred) {
      List<T> result = new ArrayList<>();
      search_by_attribute(type, o -> {
	return pred.apply(o.get());
      }, r -> {
	  result.add(r.get());
	  return false; 
	});
      return result;

    } 
    static <T extends Model<T>> T get_first_by_attribute(Class<T> type, Function<T, Boolean> pred) {
      Row<T> result = search_first_by_attribute(type, o -> {
	return pred.apply(o.get());
      });

      return result == null ? null : result.get();

    }
    static <T extends Model<T>> List<T> get_all(Class<T> type) {
      List<T> result = new ArrayList<>();
      List<Row<T>> rows = get_row_all(type);
      rows.forEach(r -> result.add(r.get()));
      return result;
    }
  }
}
class PermissionException extends Exception {
  public PermissionException() {}
  public PermissionException(String msg) {
    super(msg);
  }
}
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
    UI.finish_rent();
  }
  static public void show_all_games() {
    List<Game> games = Query.get_all(Game.class);
    for (Game g : games) {
      System.out.println(g.title());
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
      System.out.println("- 1 rent\n- 2 show all games\n- 3 show rent history\n- 4 login\n- 0 quit");
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
      if (line.equals("0") || line.equals("quit") || line.equals("q")) {
	break;
      }

    }
    scan.close();

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

// não deve se responsabilizar por detalhes de implementação do terminal ou da gui
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
      User u = Query.get_first_by_attribute(User.class, o -> o.id().equals(token));
      if (u == null) {
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
    rent_games.put(game, amount);
  }
  static void finish_rent(){
    // TODO: handle case where couldnt rent
    GameRentManage.rent(rent_games, Permission.get_user_id());
  }

  static List<Rent> get_rent_history() {
    UserId id = Permission.get_user_id();
    List<Rent> rents = Query.get_by_attribute(Rent.class, o -> o.user().equals(id));

    Collections.sort(rents, (a, b) -> a.date().compareTo(b.date())); 

    return rents;


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
    System.out.println("login after register: " + newuser.toString());
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
class App {

  public static void main(String[] args) {


    // TODO: evitar repetição e rodar pra cada classe manualmente
    Registry.load_inc_id(Game.class, o -> o.id().id());
    Registry.load_inc_id(Rent.class, o -> o.id().id());
    Registry.load_inc_id(User.class, o -> o.id().id());

    try {
      UI.login("123456789", "1234");
    } catch (Exception e){
      System.err.println("ERROR login " + e);
    }

    // Game g = Registry.create(Game.class, id -> 
    //   new Game(new GameId(id), "fezes", new GameGenre[]{GameGenre.triple_a, GameGenre.three_d, GameGenre.fps}, new GamePlatform[]{GamePlatform.console}, 999, 9999.99f)
    // );
    Game g = Registry.create_game("fezes", new GameGenre[]{GameGenre.triple_a, GameGenre.three_d, GameGenre.fps}, new GamePlatform[]{GamePlatform.console}, 999, 9999.99f);
    DataPersistence.insert( g );



    User eu = Registry.create_user("gabriel", "123456789", new UserPermissions[]{} );

    DataPersistence.insert( eu );

    User u = Query.get_first_by_attribute(User.class, o -> o.cpf().equals("123456789"));
    System.out.println("u by cpf" + u);


    // HashMap<GameId, Integer> games_rent = new HashMap<GameId, Integer>();
    // games_rent.put(g.id(), 1);
    // GameRentManage.rent(games_rent, u.id());


    CLI.loop();

    // GameManage.register_game();
  }

}
