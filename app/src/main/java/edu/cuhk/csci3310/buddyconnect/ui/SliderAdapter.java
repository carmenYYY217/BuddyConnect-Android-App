package edu.cuhk.csci3310.buddyconnect.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.List;

import edu.cuhk.csci3310.buddyconnect.R;

public class SliderAdapter extends RecyclerView.Adapter<SliderAdapter.SliderViewHolder>{

    private List<Slider_item> sliderItemList;
    private ViewPager2 viewPager2;
    private OnItemClickListener onItemClickListener;

    public  interface OnItemClickListener{
        void onItemClick(int position);
    }

    SliderAdapter(List<Slider_item> sliderItemList, ViewPager2 viewPager2, OnItemClickListener onItemClickListener) {
        this.viewPager2 = viewPager2;
        this.sliderItemList = sliderItemList;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SliderViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.imageslider_item,
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
        holder.setImageView(sliderItemList.get(position));
        holder.itemView.setOnClickListener(view -> {
            if (onItemClickListener != null){
                onItemClickListener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return sliderItemList.size();
    }

    class SliderViewHolder extends RecyclerView.ViewHolder{
        private ImageView imageView;

        SliderViewHolder(@NonNull View itemView){
            super(itemView);
            imageView = itemView.findViewById(R.id.image_slider);
        }

        void setImageView(Slider_item sliderItem){
            //
            imageView.setImageResource(sliderItem.getImage());
        }


    }
}
