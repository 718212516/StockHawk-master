package com.michael_perez.android.stockhawk.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.util.Log;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.michael_perez.android.stockhawk.data.QuoteColumns;
import com.michael_perez.android.stockhawk.data.QuoteProvider;
import com.michael_perez.android.stockhawk.rest.Constants;
import com.michael_perez.android.stockhawk.rest.Utils;
import com.michael_perez.android.stockhawk.stock_history.StockHistoryDataHandler;
import com.michael_perez.android.stockhawk.stock_history.model.Quote;
import com.michael_perez.android.stockhawk.stock_history.realm.RealmController;
import com.michael_perez.android.stockhawk.stock_history.realm.StockData;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import io.realm.Realm;

/**
 * Created by michael_perez on 9/30/16.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService{
    private String LOG_TAG = StockTaskService.class.getSimpleName();

    private OkHttpClient client = new OkHttpClient();
    private Context mContext;
    private StringBuilder mStoredSymbols = new StringBuilder();
    private boolean isUpdate;

    public StockTaskService(){}

    public StockTaskService(Context context){
        mContext = context;
    }
    String fetchData(String url) throws IOException{
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    @Override
    public int onRunTask(TaskParams params){
        // default value
        String stock_symbol = Constants.STOCK_SYMBOL_YHOO;
        if(params!=null && params.getExtras()!=null)
            if(params.getExtras().containsKey(Constants.KEY_STOCK_SYMBOL))
            stock_symbol = params.getExtras().getString(Constants.KEY_STOCK_SYMBOL);
        Cursor initQueryCursor;
        if (mContext == null){
            mContext = this;
        }
        StringBuilder urlStringBuilder = new StringBuilder();
        try{
            // Base URL for the Yahoo query
            urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
            urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
                + "in (", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (params.getTag().equals(Constants.VAL_TAG_INIT) || params.getTag().equals(Constants.VAL_TAG_PEREODIC)){
            isUpdate = true;
            initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[] { "Distinct " + QuoteColumns.SYMBOL }, null,
                    null, null);
            if (initQueryCursor.getCount() == 0 || initQueryCursor == null){
                // Init task. Populates DB with quotes for the symbols seen below
                try {
                    urlStringBuilder.append(
                            URLEncoder.encode("\""+stock_symbol+"\")", "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else if (initQueryCursor != null){
                DatabaseUtils.dumpCursor(initQueryCursor);
                initQueryCursor.moveToFirst();
                for (int i = 0; i < initQueryCursor.getCount(); i++){
                    mStoredSymbols.append("\""+
                            initQueryCursor.getString(initQueryCursor.getColumnIndex(Constants.KEY_STOCK_SYMBOL))+"\",");
                    initQueryCursor.moveToNext();
                }
                mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
                try {
                    urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        } else if (params.getTag().equals(Constants.VAL_TAG_ADD)){
            isUpdate = false;
            // get symbol from params.getExtra and build query

            try {
                urlStringBuilder.append(URLEncoder.encode("\""+stock_symbol+"\")", "UTF-8"));
            } catch (UnsupportedEncodingException e){
                e.printStackTrace();
            }
        }
        // finalize the URL for the API query.
        urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                + "org%2Falltableswithkeys&callback=");

        String urlString;
        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;

        if (urlStringBuilder != null){
            urlString = urlStringBuilder.toString();
            try{
                // get current stock info
                getResponse = fetchData(urlString);
                // store stock history
                storeStockHistoryData(stock_symbol);

                result = GcmNetworkManager.RESULT_SUCCESS;
                try {
                    ContentValues contentValues = new ContentValues();
                    // update ISCURRENT to 0 (false) so new data is current
                    if (isUpdate){
                        contentValues.put(QuoteColumns.ISCURRENT, 0);
                        mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                                null, null);
                    }
                    mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                            Utils.quoteJsonToContentVals(getResponse));
                    // send broadcast so Widget can Update data
                    Intent broadcastIntent = new Intent(Constants.ACTION_STOCK_UPDATE)
                            .setPackage(mContext.getPackageName());
                    mContext.sendBroadcast(broadcastIntent);

                }catch (RemoteException | OperationApplicationException e){
                    Log.e(LOG_TAG, "Error applying batch insert", e);
                }
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        return result;
    }

    public void storeStockHistoryData(String stockSymbol){
        try {
            String url = Utils.buildStockHistoryDataUrl(stockSymbol);
            List<Quote> rawQuotes = new StockHistoryDataHandler().getStockQuotes(url);
            StockData stockData = new StockData(stockSymbol, rawQuotes);
            Realm realm = new RealmController(getApplication()).getRealm();
            realm.beginTransaction();
            realm.copyToRealmOrUpdate(stockData);
            realm.commitTransaction();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
