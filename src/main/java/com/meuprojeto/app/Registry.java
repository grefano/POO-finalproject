package com.meuprojeto.app;
// responsavel por gerar id, comparar rows pela PK

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.meuprojeto.app.DataPersistence.Query;

public class Registry {


  private static final Map<Class<?>, Integer> last_ids = new ConcurrentHashMap<>();
  
  // FIXME: no momento método só gera o id, como fazer ele ficar responsavel por qualauqer valor automatico? 
  // public static <T extends Model<T>> T create(Class<T> type, Function<Integer, T> factory) {
  //   int id = last_ids.merge(type, 1, Integer::sum) - 1;
  //   return factory.apply(id);
  // }

  private static <T extends Model<T>> Integer get_id(Class<T> type){
    int id = last_ids.merge(type, 0, (last, ignored) -> last + 1);
    return id;
  }
  // if a object is created now, what would be its id?
  public static <T extends Model<T>> Integer get_inc_id(Class<T> type){
    Integer id = last_ids.get(type);
    return id == null ? 0 : id+1;
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
    for (T o : objs) {
      if (last_ids.get(type) == null ||  get_pk.apply(o) > last_ids.get(type)){
	Registry.last_ids.put(type, get_pk.apply(o));
      }
    }
    return last_ids.get(type) == null ? -1 : last_ids.get(type) ;
  }

  static void reset_last_ids(){
    // last_ids.replaceAll((k, v) -> null);
    last_ids.clear();
    System.out.println("last ids: " + last_ids.toString());
  }
}
