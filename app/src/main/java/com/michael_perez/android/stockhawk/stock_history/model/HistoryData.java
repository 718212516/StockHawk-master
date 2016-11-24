package com.michael_perez.android.stockhawk.stock_history.model;

import java.util.List;

/**
 * Created by rnztx on 16/6/16.
 * This Class is used for Gson library
 */
public class HistoryData{
	// see Json Data of YQL stock historical data

	public HistoryData() {
	}

	Query query;
	public List<Quote> getQuotes(){
		return this.query.results.quote;
	}

}
