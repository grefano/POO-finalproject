package com.meuprojeto.app;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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





}
class GameRentManageTest {

  @Test 
  public void shouldGetTotalPrice(){
    // GameRentManage.get_price();
    assertTrue(true);
  }
}
/**
 * Unit test for simple App.
 */
public class AppTest {

    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue() {
        assertTrue(true);
    }
}
