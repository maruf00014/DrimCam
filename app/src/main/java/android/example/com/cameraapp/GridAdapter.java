package android.example.com.cameraapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

/**
 * Created by Maruf on 07-Nov-17.
 */

public class GridAdapter  extends BaseAdapter {

    private Context mContext;
    private  int[] Imageid;

    public GridAdapter(Context c,int[] imageId)
    {
        mContext = c;
        this.Imageid = imageId;


    }

    @Override
    public int getCount() {
        return  Imageid.length;
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
    public View getView(int position, View convertView, ViewGroup parent) {
        View grid;
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {

            //grid = new View(mContext);
            grid = inflater.inflate(R.layout.grid_item_layout, null);
            ImageView imageView = grid.findViewById(R.id.grid_image);
            imageView.setImageResource(Imageid[position]);
        } else {
            grid = convertView;
        }

        return grid;
    }
}
