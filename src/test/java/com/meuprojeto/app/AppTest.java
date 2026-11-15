package com.meuprojeto.app;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.crypto.Data;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.meuprojeto.app.DataPersistence.Query;
import com.meuprojeto.app.DataPersistence.Row;
import com.meuprojeto.app.UI.Permission;

class Util {
  public static List<Game> create_games(int amount, int amount_in_stock) {
    List<Game> result = new ArrayList<>();

    int last_id = Registry.get_inc_id(Game.class);
    for (int i = 0; i < amount; i++){
      final int index = last_id + i;

      Game o = Registry.create_game("game"+index, new GameGenre[]{GameGenre.indie}, new GamePlatform[]{GamePlatform.pc}, amount_in_stock, 100f);
      result.add(o);
    }

    return result;
  }
  public static List<Game> insert_games(int amount, int amount_in_stock) {
    List<Game> result = create_games(amount, amount_in_stock);
    System.out.println("created games: " + result);
    for (int i = 0; i < amount; i++){
      DataPersistence.insert(result.get(i));
    }
    return result;
  }

  public static List<User> insert_users(int amount) {

    List<User> result = new ArrayList<>();
    for (int i = 0; i < amount; i++){
      final int index = i;
      User o = Registry.create_user("user"+index, "12345689");
      DataPersistence.insert(o);
      result.add(o);
    }
    return result;
  }
  public static void login_as_client(){
    DataPersistence.insert(
      Registry.create_user("admin", "123456789")
    );
    try {
      UI.login("123456789", "1234");
    } catch (Exception e){
      System.err.println("TEST ERROR: " + e);
      assertTrue(false);
    }

  }
  public static void login_as_admin(){
    DataPersistence.insert(
      Registry.create_user("admin", "123456789", new UserPermissions[]{UserPermissions.adm})
    );
    try {
      UI.login("123456789", "1234");
    } catch (Exception e){
      System.err.println("TEST ERROR: " + e);
      assertTrue(false);
    }
  }
}
abstract class FilePersistenceBaseTest {

  @AfterAll
  static void end(){
    // FIXME: repetindo diretorio em 2 lugares (aqui e na classe)
    DataPersistence.directory = "data/";
    System.out.println("set dir: data");
  }
  @BeforeAll 
  static void start(){
    System.out.println("set dir: temp");
    DataPersistence.directory = "data/temp-tests/";
  }
  @BeforeEach
  void clean(){
    Registry.reset_last_ids();
    try {
      DataPersistence.delete_directory(Path.of(DataPersistence.directory));
    } catch (NoSuchFileException e){
    } catch (IOException e) {
      System.out.println("ERROR clean temp: " + e);
      assertTrue(false);

    }
  }
}
class FilePersistenceTest extends FilePersistenceBaseTest {


  @Test
  public void shouldNotDuplicateInsert(){
    Game game = Registry.create_game("teste", new GameGenre[]{GameGenre.indie}, new GamePlatform[]{GamePlatform.pc}, 3, 12.70f);

    for (int i = 0; i < 5; i++){
      DataPersistence.insert(game); 
    }
    List<Game> objs = Query.get_all(Game.class);
    System.out.println("objs" + objs.size());
    assertTrue(objs.size() == 1);

  }

  @Test
  public void shouldUpdate(){
    Game g = Registry.create_game("teste", new GameGenre[] {GameGenre.indie}, new GamePlatform[] {GamePlatform.pc}, 1, 1f);
    DataPersistence.insert(g);

    Game g_new = new Game(g.id(), "teste update", g.genre(), g.platform(), g.quantity(), g.price());


    DataPersistence.update(Game.class, g_new);

    Game row = Query.get_first_by_attribute(Game.class, o -> o.id().equals(g.id()));
    System.out.println("row update: " + DataPersistence.model_object_to_string(row));
    assertTrue(row.title().equals("teste update"));
  }
  @Test
  public void shouldRemove() {
    System.out.println("TEST should remove");
    Game g = Util.insert_games(1, 5).get(0);

    DataPersistence.remove(Game.class, o -> o.id().equals(g.id()));
    

    Game gafter = Query.get_first_by_attribute(Game.class, o -> {
      System.out.println("get removed o " + o);

      return o.id().equals(g.id());
    });
    System.out.println("g after " + gafter);
    assertTrue(gafter == null);
  }
  @Test 

  public void incrementId() {
    System.out.println("TEST incrementId");
    Util.insert_games(3, 5);
    
    Game g1 = Query.get_first_by_attribute(Game.class, o -> o.id().equals(new GameId(0)));
    Game g2 = Query.get_first_by_attribute(Game.class, o -> o.id().equals(new GameId(1)));
    Game g3 = Query.get_first_by_attribute(Game.class, o -> o.id().equals(new GameId(2)));
    System.out.println(DataPersistence.model_object_to_string(g1) + " " + DataPersistence.model_object_to_string(g2) + " " + DataPersistence.model_object_to_string(g3));


    assertTrue(g1.title().equals("game0"));
    assertTrue(g2.title().equals("game1"));
    assertTrue(g3.title().equals("game2"));
  }




}


class UITest extends FilePersistenceBaseTest {
  @Test
  void getRentHistory() {
    System.out.println("TEST get rent history");
    // Util.login_as_client();
    // Util.insert_games(3, 5);
    //
    // UI.start_rent();
    // UI.add_game_to_rent(new GameId(2), 1);
    // UI.add_game_to_rent(new GameId(1), 1);
    // UI.finish_rent();
    //
    // UI.start_rent();
    // UI.add_game_to_rent(new GameId(0), 1);
    // UI.finish_rent();


    DataPersistence.insert(
      Registry.create_rent(new UserId(0), new Date(), new GameId[]{new GameId(0)})
    );
    Date ontem = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
    DataPersistence.insert(
      Registry.create_rent(new UserId(0), ontem, new GameId[]{new GameId(2), new GameId(1)})
    );

    List<Rent> rents = UI.get_rent_history();
    System.out.println("rents: ");
    rents.forEach(o -> System.out.println(DataPersistence.model_object_to_string(o)));

    assertTrue(rents.get(0).id().equals(new RentId(1)));
    assertTrue(rents.get(1).id().equals(new RentId(0)));
  }
}

class RegistryTest extends FilePersistenceBaseTest {
  @Test
  void shouldUpdateIncId(){
    Util.insert_games(2, 10);


    System.out.println("TEST shouldUpdateIncId: " + Query.get_all(Game.class));


    Integer new_last_id = Registry.load_inc_id(Game.class, o -> o.id().id());

    System.out.println("TEST new last id:" + new_last_id);
    assertTrue(new_last_id == 2);
  }
  @Test
  void shouldLoadIncId(){ // inserir jogos, load, inserir jogos
    System.out.println("TEST shouldLoadIncId");
    assertTrue(Registry.get_inc_id(Game.class) == 0);

    Util.insert_games(3, 1);
    assertTrue(Registry.get_inc_id(Game.class) == 3);

    Registry.reset_last_ids();
    Integer id = Registry.get_inc_id(Game.class);
    System.out.println( "get inc id: " + id );
    assertTrue(id == 0);

    Registry.load_inc_id(Game.class, o -> o.id().id());
    System.out.println( "get inc id after load: " + 
      Registry.get_inc_id(Game.class)

    );
    assertTrue(Registry.get_inc_id(Game.class) == 2);

    Util.insert_games(3, 1);
    System.out.println( "get inc id after insert: " + 
      Registry.get_inc_id(Game.class)

    );
    assertTrue(Registry.get_inc_id(Game.class) == 5);
  }
}
class QueryTest extends FilePersistenceBaseTest {
  @Test
  public void getFirstByAttribute() {
    Util.insert_games(1, 5);
    Game g = Query.get_first_by_attribute(Game.class, o -> o.id().equals(new GameId(0)));
    assertTrue(g.id().equals(new GameId(0)));

  }
}
class GameRentManageTest extends FilePersistenceBaseTest {

  @Test 
  public void shouldGetTotalPrice(){
    // GameRentManage.get_price();
    assertTrue(true);
  }
}
class AuthTest extends FilePersistenceBaseTest {
  @Test
  public void guestCantInsert() {

    Game g = Util.create_games(1, 5).get(0);
    assertThrows(PermissionException.class, () -> UI.insert_game(g));
  }

  @Test
  public void clientCantInsert() {
    try {
      UI.register("user1", "123456789", "1234");
      System.out.println("username: " + Permission.get_user_name() + " " + Permission.get_user_id());
    } catch (Exception e){
      assertTrue(false);
    }
    Game g = Util.create_games(1, 5).get(0);
    assertThrows(PermissionException.class, () -> UI.insert_game(g));

  }

  @Test
  public void adminCanInsert() {
    Util.login_as_admin();
    Game g = Util.create_games(1, 5).get(0);
    try {
      UI.insert_game(g);
    } catch (PermissionException e){
      assertTrue(false);
    }

    
  }
}
// requisitos do trabalho
public class AppTest extends FilePersistenceBaseTest {

    @Test
    public void shouldOnlyRentWhenAvailable() {
    System.err.println("TEST should only rent when available");
      Util.insert_users(1);

      Util.insert_games(1, 5);
      Util.insert_games(1, 0);
      

      boolean could1 = GameRentManage.rent(Map.of(new GameId(0), 1), new UserId(0));
      boolean couldunavailable = GameRentManage.rent(Map.of(new GameId(1), 1), new UserId(0));
      assertTrue(could1);
      assertFalse(couldunavailable);
    }


    @Test
    public void shouldDecreaseQuantityWhenRent() {
      Util.insert_users(1);

      DataPersistence.insert(
	Registry.create_game("game1", new GameGenre[] {GameGenre.indie}, new GamePlatform[] {GamePlatform.pc}, 5, 100f)
      );


      Game gstart = Query.get_first_by_attribute(Game.class, o -> {
      System.out.println("search game " + DataPersistence.model_object_to_string(o));
      return o.id().id().equals(0);
    });
    System.out.println("g start " + gstart.id());

      GameRentManage.rent(Map.of(new GameId(0), 1), new UserId(0));

      Game gafter = Query.get_first_by_attribute(Game.class, o -> o.id().id().equals(0));

    System.out.println("g end " + gafter.id());

      assertTrue(gafter.quantity() < gstart.quantity());
    }

    @Test
    public void shouldIncreaseQuantityWhenRentReturn() {
      System.out.println("TEST should increase quantity when rent return");
      Util.insert_users(1);

      Util.insert_games(1, 5);


      GameRentManage.rent(Map.of(new GameId(0), 1), new UserId(0));
      Game gstart = Query.get_first_by_attribute(Game.class, o -> o.id().id().equals(0));


      GameRentManage.rent_return(new RentId(0));

      Game gafter = Query.get_first_by_attribute(Game.class, o -> o.id().id().equals(0));


      System.out.println("END TEST should increase quantity when rent return");
      System.out.println("start " + gstart.quantity() + " end " + gafter.quantity());
      assertTrue(gafter.quantity() > gstart.quantity());
    }

    @Test
    public void max3GamesRentPerUser() {
      System.out.println("TEST max 3 games rent per user");

      Util.insert_users(1);

      Util.insert_games(4, 5);

      
      boolean couldrentmore = GameRentManage.rent(Map.of(new GameId(0), 1, new GameId(1), 1, new GameId(2), 1, new GameId(3), 1), new UserId(0));
      boolean couldrentless = GameRentManage.rent(Map.of(new GameId(0), 1, new GameId(1), 1, new GameId(2), 1), new UserId(0));
      boolean couldrentmoreadd = GameRentManage.rent(Map.of(new GameId(0), 1), new UserId(0)); // 3 + 1

      System.out.println("END TEST max 3 games rent per user");
      assertFalse(couldrentmore);
      assertTrue(couldrentless);
      assertFalse(couldrentmoreadd);

    }

    // TODO: testar requisito 6 (alguel deve possuir data prevista para devolução)
    

    @Test 
    void alreadyReturnedRentCantBeAgain() {
      System.out.println("TEST already returned rent cant be again");
      
      Util.insert_users(1);
      Util.insert_games(1, 5);


      System.out.println("TEST already rent: ");
      boolean couldrent = GameRentManage.rent(Map.of(new GameId(0), 1), new UserId(0));
      System.out.println("could rent " + couldrent);
      
      System.out.println(" ADJLAJ: " + Query.get_all(Rent.class));

      boolean couldreturnfirst = GameRentManage.rent_return(new RentId(0));
      System.out.println("first return: " + 
	  DataPersistence.model_object_to_string(Query.get_first_by_attribute(Rent.class, o -> o.id().id() == 0))
	  );

      boolean couldreturnsecond = GameRentManage.rent_return(new RentId(0));
      System.out.println("second return " + 
	  DataPersistence.model_object_to_string(Query.get_first_by_attribute(Rent.class, o -> o.id().id() == 0))
	  );


      System.out.println("TEST  alreadyReturnedRentCantBeAgain " + couldreturnfirst + " "+ couldreturnsecond);
      assertTrue(couldreturnfirst);
      assertFalse(couldreturnsecond);


    }





  @Test
  void shouldApplyLateReturnPenalty(){
    Game g = Util.insert_games(1, 5).get(0);
    User u = Util.insert_users(1).get(0);

    
    Date ontem = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
    GameRentManage.rent(Map.of(g.id(), 1), u.id(), ontem);

    Rent r = Query.get_first_by_attribute(Rent.class, o -> o.id().id() == 0);

    GameRentManage.rent_return(r.id());

    Rent rr = Query.get_first_by_attribute(Rent.class, o -> o.id().id() == 0);


    Float games_total_price = GameRentManage.get_games_total_price(r.games());
    assertTrue(r != null);
    assertTrue(r.return_deadline().before(new Date()));
    assertTrue(rr.price_paid() > games_total_price);


  }

}
