package io.inprice.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Test {

  public static void main(String[] args) {
    Set<String>set = new HashSet<>(Arrays.asList("One", "Two", "Three", "Four", "Five", "Six"));
    System.out.println("Set = " + set);
    String str = "'"+ String.join("', '", set) + "'";
    System.out.println("Comma separated String: "+ str);

  }
  
}