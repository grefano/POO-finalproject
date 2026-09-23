package com.meuprojeto.app;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

// TODO: não escrever no mesmo arquivo usado na pratica
class FilePersistenceTest {
  @BeforeAll
  static void start(){
    DataPersistence.directory = "data/temp-tests/";
  }
  @AfterAll
  static void clean(){
    // try {
    // Files.delete(Path.of(DataPersistence.directory));
    // } catch (IOException e) {
    //   System.out.println(e);
    //
    // }
    DataPersistence.directory = "data/";
  }


  @Test
  public void shouldNotDuplicateInsert(){
    Game game = Registry.create(Game.class, id -> 
      new Game(new GameId(id), "celeste", new GameGenre[]{GameGenre.indie}, new GamePlatform[]{GamePlatform.pc}, 3, 12.70f)
    );
    for (int i = 0; i < 5; i++){
      DataPersistence.insert(game); 
    }
    List<Game> objs = DataPersistence.get_row_all(Game.class);
    System.out.println("objs" + objs.size());
    assertTrue(objs.size() == 1);

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
