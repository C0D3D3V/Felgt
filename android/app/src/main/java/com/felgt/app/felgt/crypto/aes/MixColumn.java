package com.felgt.app.felgt.crypto.aes;

class MixColumn {
  private static final int[][] galois = {{0x02, 0x03, 0x01, 0x01},
      {0x01, 0x02, 0x03, 0x01},
      {0x01, 0x01, 0x02, 0x03},
      {0x03, 0x01, 0x01, 0x02}};
  private static final int[][] invgalois = {{0x0e, 0x0b, 0x0d, 0x09},
      {0x09, 0x0e, 0x0b, 0x0d},
      {0x0d, 0x09, 0x0e, 0x0b},
      {0x0b, 0x0d, 0x09, 0x0e}};


  private MixColumn() {
  }

  static void mixColumns(int[][] arr) //method for mixColumns
  {
    int[][] tarr = new int[4][4];
    for (int i = 0; i < 4; i++) {
      System.arraycopy(arr[i], 0, tarr[i], 0, 4);
    }
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        arr[i][j] = mcHelper(tarr, galois, i, j);
      }
    }
  }


  static private int mcHelper(int[][] arr, int[][] g, int i, int j) {
    int mcsum = 0;
    for (int k = 0; k < 4; k++) {
      int a = g[i][k];
      int b = arr[k][j];
      mcsum ^= mcCalc(a, b);
    }
    return mcsum;
  }

  static private int mcCalc(int a, int b) //Helper method for mcHelper
  {
    if (a == 1) {
      return b;
    } else if (a == 2) {
      return MCTables.mc2[b / 16][b % 16];
    } else if (a == 3) {
      return MCTables.mc3[b / 16][b % 16];
    }
    return 0;
  }

  static void invMixColumns(int[][] arr) {
    int[][] tarr = new int[4][4];
    for (int i = 0; i < 4; i++) {
      System.arraycopy(arr[i], 0, tarr[i], 0, 4);
    }
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        arr[i][j] = invMcHelper(tarr, invgalois, i, j);
      }
    }
  }

  static private int invMcHelper(int[][] arr, int[][] igalois, int i, int j) //Helper method for invMixColumns
  {
    int mcsum = 0;
    for (int k = 0; k < 4; k++) {
      int a = igalois[i][k];
      int b = arr[k][j];
      mcsum ^= invMcCalc(a, b);
    }
    return mcsum;
  }


  static private int invMcCalc(int a, int b) //Helper method for invMcHelper
  {
    if (a == 9) {
      return MCTables.mc9[b / 16][b % 16];
    } else if (a == 0xb) {
      return MCTables.mc11[b / 16][b % 16];
    } else if (a == 0xd) {
      return MCTables.mc13[b / 16][b % 16];
    } else if (a == 0xe) {
      return MCTables.mc14[b / 16][b % 16];
    }
    return 0;
  }
}
