package com.icmonroe.adaptablecircleview;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Random;


public class MainActivity extends Activity {

    AdaptableCircleView circle;
    float[] tests = {.3f,.78f,.5f,.2f,.89f};
    int index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return 60;
            }

            @Override
            public Object getItem(int i) {
                return null;
            }

            @Override
            public long getItemId(int i) {
                return 0;
            }

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                if(view==null){
                    view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.large_list_item,viewGroup,false);
                    ViewHolder holder = new ViewHolder();
                    holder.circle = (AdaptableCircleView) view.findViewById(R.id.circle);
                    holder.circle.setBackgroundColor(Color.LTGRAY);
                    holder.circle.setForegroundColor(Color.DKGRAY);
                    holder.title = (TextView) view.findViewById(R.id.title);
                    holder.subtitle = (TextView) view.findViewById(R.id.subtitle);
                    view.setTag(holder);
                }
                ViewHolder holder = (ViewHolder) view.getTag();
                float percent = new Random().nextFloat() * (1.0f - 0.0f) + 0.0f;
                holder.circle.setPercentage(0);
                holder.circle.setPercentage(percent,500);
                holder.title.setText("Title "+i);
                holder.subtitle.setText("Subtitle for position "+i);
                return view;
            }
        });


        /*
        circle = (AdaptableCircleView) findViewById(R.id.circle);
        circle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                circle.setPercentage(tests[index],500);
                index = index<tests.length-1 ? index+1 : 0;
            }
        });
        circle.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher),true);
        */
    }

    class ViewHolder{
        AdaptableCircleView circle;
        TextView title;
        TextView subtitle;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
