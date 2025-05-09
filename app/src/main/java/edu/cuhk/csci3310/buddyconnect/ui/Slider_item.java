package edu.cuhk.csci3310.buddyconnect.ui;

public class Slider_item {
    private int image;
    private String title;

    Slider_item(int image, String title){
        this.image = image;
        this.title = title;
    }
    public int getImage(){
        return image;
    }

    public String getTitle(){
        return title;
    }
}
