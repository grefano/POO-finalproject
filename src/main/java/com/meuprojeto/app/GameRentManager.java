package com.meuprojeto.app;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.meuprojeto.app.DataPersistence.Query;

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
