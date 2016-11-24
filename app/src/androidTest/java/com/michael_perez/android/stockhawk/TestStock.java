package com.michael_perez.android.stockhawk;

import android.test.AndroidTestCase;

import com.michael_perez.android.stockhawk.rest.Utils;


public class TestStock extends AndroidTestCase{
	public void testDate(){
		String startDate = Utils.getStartDate();
		String endDate = Utils.getEndDate();
		String today = "2016-24-11"; // this should be today's date in yyyy-MM-dd
		String lastMonth = "2016-05-17";
		assertEquals(today,endDate);
		assertEquals(lastMonth,startDate);
	}
}
