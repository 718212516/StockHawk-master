package com.michael_perez.android.stockhawk.data;

import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.Table;

/**
 * Created by michael_perez on 10/10/16.
 */
@Database(version = QuoteDatabase.VERSION)
public class QuoteDatabase {
  private QuoteDatabase(){}

  public static final int VERSION = 8;

  @Table(QuoteColumns.class) public static final String QUOTES = "quotes";
}
