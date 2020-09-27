package io.inprice.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Threair<K, V1, V2> {

  private K key;
  private V1 value1;
  private V2 value2;

  public Threair(K key, V1 value1, V2 value2) {
    this.key = key;
    this.value1 = value1;
    this.value2 = value2;
  }
  
  
}
