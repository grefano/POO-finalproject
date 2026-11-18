package com.meuprojeto.app;

import java.util.ArrayList;
import java.util.Date;

record GameId(Integer id) {};
record UserId(Integer id) {};
record RentId(Integer id) {};

enum GameGenre { two_d, three_d, platformer, fps, triple_a, indie, schmovement, bossfight }
enum GamePlatform { console, mobile, pc }

enum UserPermissions { adm } 

interface Model <T extends Model<T>> {
  // usado pra verificar se um objeto semelhante já foi criado (visto que não pode comparar ids nem cada atributo individualemente)
  boolean equivalent(T o);
  // usado pra verificar se é o objeto que estou procurando (geralmente pelo id)
  // comparar primary key
  boolean same(T o);

}

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


record UserProfile(ArrayList<Rent> rent_story)  implements Model<UserProfile> {
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
