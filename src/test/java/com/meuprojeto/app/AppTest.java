package com.meuprojeto.app;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.meuprojeto.app.DataPersistence.Row;

class FilePersistenceTest {
  @AfterAll
  static void end(){
    // FIXME: repetindo diretorio em 2 lugares (aqui e na classe)
    DataPersistence.directory = "data/";
  }
  @AfterEach
  void clean(){
    try {
      DataPersistence.delete_directory(Path.of(DataPersistence.directory));
    } catch (NoSuchFileException e){
    } catch (IOException e) {
      System.out.println("ERROR clean temp: " + e);
      assertTrue(false);

    }
  }


  @Test
  public void shouldNotDuplicateInsert(){
    Game game = Registry.create(Game.class, id -> 
      new Game(new GameId(id), "teste", new GameGenre[]{GameGenre.indie}, new GamePlatform[]{GamePlatform.pc}, 3, 12.70f)
    );
    for (int i = 0; i < 5; i++){
      DataPersistence.insert(game); 
    }
    List<Game> objs = DataPersistence.get_row_all(Game.class);
    System.out.println("objs" + objs.size());
    assertTrue(objs.size() == 1);

  }

  @Test
  public void shouldUpdate(){
    Game g = Registry.create(Game.class, id -> new Game(new GameId(id), "teste", new GameGenre[] {GameGenre.indie}, new GamePlatform[] {GamePlatform.pc}, 1, 1f));
    DataPersistence.insert(g);

    Game g_new = new Game(g.id(), "teste update", g.genre(), g.platform(), g.quantity(), g.price());

    try {

      DataPersistence.update(Game.class, g_new);
    } catch (IOException e) {
      System.out.println("ERROR update row: " + e);
      assertTrue(false);
    }

    Row<Game> row = DataPersistence.get_by_attribute(Game.class, o -> o.id().equals(g.id()));
    System.out.println("row update: " + DataPersistence.model_object_to_string(row.get()));
    assertTrue(row.get().title().equals("teste update"));
  }
  @Test 
  public void incrementId() {
    DataPersistence.insert(Registry.create(Game.class,
	  id -> new Game(new GameId(id), "game1", new GameGenre[] {GameGenre.indie}, new GamePlatform[] {GamePlatform.pc}, 5, 100f)));

    DataPersistence.insert(Registry.create(Game.class,
	  id -> new Game(new GameId(id), "game2", new GameGenre[] {GameGenre.indie}, new GamePlatform[] {GamePlatform.pc}, 5, 100f)));

    DataPersistence.insert(Registry.create(Game.class,
	  id -> new Game(new GameId(id), "game3", new GameGenre[] {GameGenre.indie}, new GamePlatform[] {GamePlatform.pc}, 5, 100f)));
    
    Game g1 = DataPersistence.get_by_attribute(Game.class, o -> o.id().equals(new GameId(0))).get();
    Game g2 = DataPersistence.get_by_attribute(Game.class, o -> o.id().equals(new GameId(1))).get();
    Game g3 = DataPersistence.get_by_attribute(Game.class, o -> o.id().equals(new GameId(2))).get();


    assertTrue(g1.title().equals("game1"));
    assertTrue(g2.title().equals("game2"));
    assertTrue(g3.title().equals("game3"));
  }




}
class GameRentManageTest {

  @Test 
  public void shouldGetTotalPrice(){
    // GameRentManage.get_price();
    assertTrue(true);
  }
}
// requisitos do trabalho
public class AppTest {

    @Test
    public void shouldOnlyRentWhenAvailable() {
      DataPersistence.insert(Registry.create(User.class, 
	id -> new User(new UserId(id), "alguem", "123")));

      DataPersistence.insert(Registry.create(Game.class,
	id -> new Game(new GameId(id), "game1", new GameGenre[] {GameGenre.indie}, new GamePlatform[] {GamePlatform.pc}, 5, 100f)));

      DataPersistence.insert(Registry.create(Game.class,
	id -> new Game(new GameId(id), "game unavailable", new GameGenre[] {GameGenre.indie}, new GamePlatform[] {GamePlatform.pc}, 0, 100f)));


      boolean could1 = GameRentManage.rent(new GameId(0), new UserId(0));
      boolean couldunavailable = GameRentManage.rent(new GameId(1), new UserId(0));
      assertTrue(could1);
      assertFalse(couldunavailable);
    }


    @Test
    public void shouldDecreaseQuantityWhenRent() {
      DataPersistence.insert(Registry.create(User.class, 
	id -> new User(new UserId(id), "alguem", "123")));

      DataPersistence.insert(Registry.create(Game.class,
	id -> new Game(new GameId(id), "game1", new GameGenre[] {GameGenre.indie}, new GamePlatform[] {GamePlatform.pc}, 5, 100f)));


      Game gstart = DataPersistence.get_by_attribute(Game.class, o -> o.id().equals(0)).get();

      GameRentManage.rent(new GameId(0), new UserId(0));

      Game gafter = DataPersistence.get_by_attribute(Game.class, o -> o.id().equals(0)).get();


      assertTrue(gafter.quantity() < gstart.quantity());
    }

    @Test
    public void shouldIncreaseQuantityWhenRentReturn() {
      DataPersistence.insert(Registry.create(User.class, 
	id -> new User(new UserId(id), "alguem", "123")));

      DataPersistence.insert(Registry.create(Game.class,
	id -> new Game(new GameId(id), "game1", new GameGenre[] {GameGenre.indie}, new GamePlatform[] {GamePlatform.pc}, 5, 100f)));


      GameRentManage.rent(new GameId(0), new UserId(0));
      Game gstart = DataPersistence.get_by_attribute(Game.class, o -> o.id().equals(0)).get();


      GameRentManage.rent_return(new RentId(0));

      Game gafter = DataPersistence.get_by_attribute(Game.class, o -> o.id().equals(0)).get();


      assertTrue(gafter.quantity() > gstart.quantity());
    }

    @Test
    public void max3GamesRentPerUser() {
      // TODO: tem q considerar jogos em outros rents também
      DataPersistence.insert(Registry.create(User.class, 
	id -> new User(new UserId(id), "alguem", "123")));

      DataPersistence.insert(Registry.create(Game.class,
	id -> new Game(new GameId(id), "game1", new GameGenre[] {GameGenre.indie}, new GamePlatform[] {GamePlatform.pc}, 5, 100f)));

      DataPersistence.insert(Registry.create(Game.class,
	id -> new Game(new GameId(id), "game2", new GameGenre[] {GameGenre.indie}, new GamePlatform[] {GamePlatform.pc}, 5, 100f)));

      DataPersistence.insert(Registry.create(Game.class,
	id -> new Game(new GameId(id), "game3", new GameGenre[] {GameGenre.indie}, new GamePlatform[] {GamePlatform.pc}, 5, 100f)));

      DataPersistence.insert(Registry.create(Game.class,
	id -> new Game(new GameId(id), "game4", new GameGenre[] {GameGenre.indie}, new GamePlatform[] {GamePlatform.pc}, 5, 100f)));


      boolean couldrentless = GameRentManage.rent(new GameId[]{new GameId(0), new GameId(1), new GameId(2)}, new UserId(0));
      boolean couldrentmore = GameRentManage.rent(new GameId[]{new GameId(0), new GameId(1), new GameId(2), new GameId(3)}, new UserId(0));


      assertTrue(couldrentless);
      assertFalse(couldrentmore);

    }

    // TODO: testar requisito 6 (alguel deve possuir data prevista para devolução)
    

    @Test 
    void alreadyReturnedRentCantBeAgain() {
      
      DataPersistence.insert(Registry.create(User.class, 
	id -> new User(new UserId(id), "alguem", "123")));

      DataPersistence.insert(Registry.create(Game.class,
	id -> new Game(new GameId(id), "game1", new GameGenre[] {GameGenre.indie}, new GamePlatform[] {GamePlatform.pc}, 5, 100f)));


      GameRentManage.rent(new GameId[]{new GameId(0)}, new UserId(0));

      boolean couldreturnfirst = GameRentManage.rent_return(new RentId(0));

      boolean couldreturnsecond = GameRentManage.rent_return(new RentId(0));


      assertTrue(couldreturnfirst);
      assertFalse(couldreturnsecond);


    }

}
