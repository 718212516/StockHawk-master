package com.michael_perez.android.stockhawk.rest;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.michael_perez.android.stockhawk.R;
import com.michael_perez.android.stockhawk.data.QuoteColumns;
import com.michael_perez.android.stockhawk.data.QuoteProvider;
import com.michael_perez.android.stockhawk.stock_history.realm.RealmController;
import com.michael_perez.android.stockhawk.touch_helper.ItemTouchHelperAdapter;
import com.michael_perez.android.stockhawk.touch_helper.ItemTouchHelperViewHolder;

/**
 * Created by michael_perez on 10/6/16.
 *    Credit to skyfishjy gist:
 *        https://gist.github.com/skyfishjy/443b7448f59be978bc59
 * for the code structure
 */
public class QuoteCursorAdapter extends CursorRecyclerViewAdapter<QuoteCursorAdapter.ViewHolder>
        implements ItemTouchHelperAdapter{

    private static Context mContext;
    private static Typeface robotoLight;
    private RealmController mRealmController;
    private boolean isPercent;
    public QuoteCursorAdapter(Context context, Cursor cursor, Application application){
        super(context, cursor);
        mContext = context;
        mRealmController = RealmController.with(application);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        robotoLight = Typeface.createFromAsset(mContext.getAssets(), "fonts/Roboto-Light.ttf");
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_quote, parent, false);
        ViewHolder vh = new ViewHolder(itemView);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final Cursor cursor){
        viewHolder.symbol.setText(cursor.getString(cursor.getColumnIndex(QuoteColumns.SYMBOL)));
        viewHolder.bidPrice.setText(cursor.getString(cursor.getColumnIndex(QuoteColumns.BIDPRICE)));
        int sdk = Build.VERSION.SDK_INT;
        if (cursor.getInt(cursor.getColumnIndex(QuoteColumns.ISUP)) == 1){
            if (sdk < Build.VERSION_CODES.JELLY_BEAN){
                viewHolder.change.setBackgroundDrawable(
                        mContext.getResources().getDrawable(R.drawable.percent_change_pill_green));
            }else {
                viewHolder.change.setBackground(
                        mContext.getResources().getDrawable(R.drawable.percent_change_pill_green));
            }
        } else{
            if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
                viewHolder.change.setBackgroundDrawable(
                        mContext.getResources().getDrawable(R.drawable.percent_change_pill_red));
            } else{
                viewHolder.change.setBackground(
                        mContext.getResources().getDrawable(R.drawable.percent_change_pill_red));
            }
        }
        if (Utils.showPercent){
            viewHolder.change.setText(cursor.getString(cursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE)));
        } else{
            viewHolder.change.setText(cursor.getString(cursor.getColumnIndex(QuoteColumns.CHANGE)));
        }
    }

    @Override public void onItemDismiss(int position) {
        Cursor c = getCursor();
        c.moveToPosition(position);
        String symbol = c.getString(c.getColumnIndex(QuoteColumns.SYMBOL));
        mContext.getContentResolver().delete(QuoteProvider.Quotes.withSymbol(symbol), null, null);
        notifyItemRemoved(position);
        // send broadcast so Widget can Update data
        Intent broadcastIntent = new Intent(Constants.ACTION_STOCK_UPDATE)
                        .setPackage(mContext.getPackageName());
        mContext.sendBroadcast(broadcastIntent);
        // delete stock graph data
        mRealmController.deleteStockGraphData(symbol);
    }

    @Override public int getItemCount() {
        return super.getItemCount();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder{
        public final TextView symbol;
        public final TextView bidPrice;
        public final TextView change;
        public ViewHolder(View itemView){
            super(itemView);
            symbol = (TextView) itemView.findViewById(R.id.stock_symbol);
            symbol.setTypeface(robotoLight);
            bidPrice = (TextView) itemView.findViewById(R.id.bid_price);
            change = (TextView) itemView.findViewById(R.id.change);
        }

	    /**
         * To ensure CardView functionality on MainActivity,
         * removing background color operations
         */
        @Override
        public void onItemSelected(){
//            itemView.setBackgroundColor(Color.LTGRAY);
        }

        @Override
        public void onItemClear(){
//            itemView.setBackgroundColor(0);
        }

    }
}
