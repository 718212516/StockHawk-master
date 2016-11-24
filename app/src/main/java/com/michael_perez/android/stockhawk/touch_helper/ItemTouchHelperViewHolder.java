package com.michael_perez.android.stockhawk.touch_helper;

/**
 * Created by michael_perez on 10/6/16.
 * credit to Paul Burke (ipaulpro)
 * Interface for enabling swiping to delete
 */
public interface ItemTouchHelperViewHolder {
  void onItemSelected();

  void onItemClear();
}
