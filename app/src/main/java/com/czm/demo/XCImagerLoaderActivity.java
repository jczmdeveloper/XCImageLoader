package com.czm.demo;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.czm.xcimageloader.ImageSources;
import com.czm.xcimageloader.R;
import com.czm.xcimageloader.XCImageLoader;

public class XCImagerLoaderActivity extends AppCompatActivity {

    private GridView mGridView;
    private String[] mUrlStrs = ImageSources.imageUrls;
    private XCImageLoader mImageLoader;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xcimager_loader);
        init();
        mImageLoader = XCImageLoader.getInstance(3, XCImageLoader.Type.LIFO);
    }

    private void init() {
        mGridView = (GridView) findViewById(R.id.gridview);
        GridViewAdpter adapter = new GridViewAdpter(this,0,mUrlStrs);
        mGridView.setAdapter(adapter);
    }
    private class GridViewAdpter extends ArrayAdapter<String>
    {
        private Context mContext;
        public GridViewAdpter(Context context, int resource, String[] datas)
        {
            super(context, 0, datas);
            mContext = context;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            if (convertView == null)
            {
                convertView = LayoutInflater.from(mContext).inflate(
                        R.layout.layout_gridview_item, parent, false);
            }
            ImageView imageview = (ImageView) convertView
                    .findViewById(R.id.image_view);
            imageview.setImageResource(R.mipmap.img_default);
            TextView textview = (TextView)convertView.findViewById(R.id.text_pos);
            textview.setText(""+(position + 1));
            mImageLoader.displayImage(getItem(position), imageview, true);
            return convertView;
        }
    }
}
