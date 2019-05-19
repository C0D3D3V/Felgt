package com.felgt.app.felgt.crypto.aes;

class ShiftRows {
  private ShiftRows() {
  }

  static void shiftRows(int[][] arr) {
    for (int i = 1; i < arr.length; i++) {
      arr[i] = leftrotate(arr[i], i);
    }
  }


  static public int[] leftrotate(int[] arr, int times) {
    assert (arr.length == 4);
    if (times % 4 == 0) {
      return arr;
    }
    while (times > 0) {
      int temp = arr[0];
      for (int i = 0; i < arr.length - 1; i++) {
        arr[i] = arr[i + 1];
      }
      arr[arr.length - 1] = temp;
      --times;
    }
    return arr;
  }


  static void invShiftRows(int[][] arr) {
    for (int i = 1; i < arr.length; i++) {
      arr[i] = rightrotate(arr[i], i);
    }
  }


  static private int[] rightrotate(int[] arr, int times) {
    if (arr.length == 0 || arr.length == 1 || times % 4 == 0) {
      return arr;
    }
    while (times > 0) {
      int temp = arr[arr.length - 1];
      for (int i = arr.length - 1; i > 0; i--) {
        arr[i] = arr[i - 1];
      }
      arr[0] = temp;
      --times;
    }
    return arr;
  }
}
