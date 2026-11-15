package com.meuprojeto.app;


// TODO: search game by X AND Y

class PermissionException extends Exception {
  public PermissionException() {}
  public PermissionException(String msg) {
    super(msg);
  }
}



class App {

  public static void main(String[] args) {


    // TODO: evitar repetição e rodar pra cada classe manualmente
    Registry.load_inc_id(Game.class, o -> o.id().id());
    Registry.load_inc_id(Rent.class, o -> o.id().id());
    Registry.load_inc_id(User.class, o -> o.id().id());

    System.out.println("inc id: " + Registry.get_inc_id(Game.class));

    try {
      UI.login("123456789", "1234");
    } catch (Exception e){
      System.err.println("ERROR login " + e);
    }



    CLI.loop();

  }

}
