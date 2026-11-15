package com.meuprojeto.app;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gson.Gson;

// abstração do banco de dados ou filesystem
class DataPersistence {
  static public final class Row<T>{
    private int line;
    private T obj;

    private Row(int line, T obj) {
      this.line = line;
      this.obj = obj;

    }
    T get(){
      return this.obj;
    }

  }
  static String directory = "data/";

  static <T extends Model<T>> List<T> string_to_model_object(List<String> str, Class<T> type) {
    List<T> result = new ArrayList<>(); 
    Gson gson = new Gson();
    for (String line : str) {
      T o = gson.fromJson(line, type);
      result.add(o);
    }
    return result;

  }
  // converte qualquer um dos modelos (game, rent, ...) em uma string no formato que sera armazenado
  static <T extends Model<T>> List<String> model_object_to_string(List<T> os) {
    Gson gson = new Gson();
    List<String> result = new ArrayList<>();
    for (T o : os) {
      String str = gson.toJson(o);
      result.add(str);
    }
    System.out.println(result);
    return result;


  }
  static <T extends Model<T>> T string_to_model_object(String str, Class<T> type) {
    List<String> ss = new ArrayList<>();
    ss.add(str);
    return string_to_model_object(ss, type).get(0);
  }
  static <T extends Model<T>> String model_object_to_string(T o){
    List<T> os = new ArrayList<T>();
    os.add(o);
    return model_object_to_string(os).get(0);

  }
  static <T extends Model<T>> Path dir_model(Class<T> type) {
    String dir = DataPersistence.directory + type.getSimpleName().toLowerCase() + "s/all.txt";
    Path path = Paths.get(dir);
    return path;
  }
  public static <T extends Model<T>> void update(Class<T> type, T newrow){
    Row<T> row = Query.search_first_by_attribute(type, o -> o.get().same(newrow));
    System.out.println("update row find: " + model_object_to_string(row.get()));
    try {
      List<String> lines = Files.readAllLines(dir_model(type));

      String obj_str = model_object_to_string(newrow) + System.lineSeparator();
      lines.set(row.line, obj_str);
      System.out.println("update lines: " + lines);
      Files.write(dir_model(type), lines, StandardOpenOption.WRITE);
    } catch (IOException e) {
      System.err.println("ERROR " + e);

    }
  }
  public static <T extends Model<T>> void insert(T obj) {
    Path path_file = dir_model(obj.getClass());
    try {
      System.out.println("parent: " + path_file.getParent());
      Files.createDirectories(path_file.getParent());
    } catch (FileAlreadyExistsException e){
    } catch (IOException e){
      System.out.println("ERROR create dir " + e);
    }
    try {



      Files.createFile(path_file);
    } catch (FileAlreadyExistsException e){
    } catch (IOException e) {
      System.out.println("ERROR create file: " + e);

    }
      List<T> objs = Query.get_all((Class<T>) obj.getClass());
      for (T o : objs) {
	if (o != null && obj.equivalent(o)) {
	System.out.println("equivalent get bent" + model_object_to_string(obj) + " " + model_object_to_string(o));
	  return;

	}
      	
      }



      String obj_str = model_object_to_string(obj) + System.lineSeparator();
      System.out.println(obj_str);

      try {

      Files.write(path_file, obj_str.getBytes(), StandardOpenOption.APPEND);
      } catch (IOException e) {
	System.out.println("ERROR write file: " + e);
      }


  }
  public static <T extends Model<T>> void remove(Class<T> type, Function<T, Boolean> pred) {

    Path path_file = dir_model(type);
    Row<T> row = Query.search_first_by_attribute(type, r -> pred.apply(r.get()));
    System.out.println("remove row " + row.get());
    try {
      List<String> lines = Files.readAllLines(dir_model(type));
      System.out.println(row.line + " remove lines " + lines);
      lines.set(row.line, "");
Files.write(path_file, lines, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException e) {
      System.out.println("ERROR remove " + e);
    }

  }

  public static <T extends Model<T>> List<Row<T>> get_row_all(Class<T> type) {

    Path path = dir_model(type);
    try {

      List<Row<T>> objs = new ArrayList<>();
      List<String> lines = Files.lines(path).collect(Collectors.toList());
      for (int i = 0; i < lines.size(); i++) {
	if (lines.get(i).isBlank()){
	  continue;
	}

	objs.add(new Row<T>(i, string_to_model_object(lines.get(i), type) ));
      }
      return objs;
    } catch (IOException e){
      System.err.println("ERROR read all rows: " + e);
      return new ArrayList<Row<T>>();
    }
    

  }


  public static void delete_directory(Path diretorio) throws IOException {
    Files.walkFileTree(diretorio, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
	Files.delete(file);
	return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
	if (exc != null) throw exc;
	Files.delete(dir);
	return FileVisitResult.CONTINUE;
      }
    });
  }


  public class Query {

    static <T extends Model<T>> void search_by_attribute(List<Row<T>> rows, Function<Row<T>, Boolean> pred, Function<Row<T>, Boolean> act) {
      // search(Game.class, o -> o.id < 1000, (o, i) -> )
      for(int i = 0; i < rows.size(); i++) {
	Row<T> r = rows.get(i);
	if (r == null || r.get() == null){
	  continue;
	}
	if (pred.apply(r)){
	  Boolean end = act.apply(r);
	  if (end){
	    return;
	  }

	}
      }
    }
    static <T extends Model<T>> void search_by_attribute(Class<T> type, Function<Row<T>, Boolean> pred, Function<Row<T>, Boolean> act) {

      List<Row<T>> rows = get_row_all(type);
      search_by_attribute(rows, pred, act);

    } 
    static <T extends Model<T>> Row<T> search_first_by_attribute(Class<T> type, Function<Row<T>, Boolean> pred) {

      AtomicReference<Row<T>> result = new AtomicReference<>();
      search_by_attribute(type, pred, o -> {
	result.set(o);
	return true;
      });
      return result.get();


    }
    static <T extends Model<T>> List<T> get_by_attribute(Class<T> type, Function<T, Boolean> pred) {
      List<T> result = new ArrayList<>();
      search_by_attribute(type, o -> {
	return pred.apply(o.get());
      }, r -> {
	  result.add(r.get());
	  return false; 
	});
      return result;

    } 
    static <T extends Model<T>> T get_first_by_attribute(Class<T> type, Function<T, Boolean> pred) {
      Row<T> result = search_first_by_attribute(type, o -> {
	return pred.apply(o.get());
      });

      return result == null ? null : result.get();

    }
    static <T extends Model<T>> List<T> get_all(Class<T> type) {
      List<T> result = new ArrayList<>();
      List<Row<T>> rows = get_row_all(type);
      rows.forEach(r -> result.add(r.get()));
      return result;
    }
  }
}
