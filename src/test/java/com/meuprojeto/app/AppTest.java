package com.meuprojeto.app;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

// TODO: não escrever no mesmo arquivo usado na pratica
class FilePersistenceTest {
  @Test
  public void shouldNotDuplicateInsert(){
    Game game = Registry.create(Game.class, id -> new Game(new GameId(id), "celeste", new GameGenre[]{GameGenre.indie, GameGenre.platformer}, new GamePlatform[]{GamePlatform.pc}, 3, 12.70f));
    for (int i = 0; i < 5; i++){
      DataPersistence.insert(game); 
    }
    List<Game> objs = DataPersistence.get_row_all(Game.class);
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
