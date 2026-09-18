import java.util.Date;

// TODO: terminal

interface UserId {
  Integer id;
}
interface GameId {
  Integer id;
}
interface RentId {
  Integer id;
}

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
// ? dispositivo (console / pc), versão (ps2 / ps5), marca (nintendo / playstation), sistema operacional (windows / linux)
public enum GamePlatform {
  console, mobile, pc
}
public class GameRegistry {
  private GameId id;
  private String genre; 
  private String title;
  private GamePlatform platform; // TODO: >1 plataforma
  private String quantity;
}
public class GameManage {
  private GameRegistry[] games;
  public static void register_game() {

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
// abstração do banco de dados ou filesystem
public class DataPersistence {
  public static void insert_game() {

  }
  public static GameRegistry get_game(GameId id) {

  }
  public static GameRegistry[] get_games_all() {

  }
  public static void insert_rent() {

  }
  public static RentRegistry get_rent(RentId id) {

  }
  public static void insert_user() {

  }
  public static UserRegistry get_user(UserId id) {

  }



}
class Cli {

}
// não deve se responsabilizar por detalhes de implementação do terminal ou da gui
class UI {

}
class Main {

  public static void main(String[] args) {
    GameRegistry[] registry_games = DataPersistence.get_games_all();


  }
}
