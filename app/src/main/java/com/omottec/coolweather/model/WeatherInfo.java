package com.omottec.coolweather.model;

import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

/**
 * Created by qinbingbing on 6/14/16.
 */
public class WeatherInfo {

    @SerializedName("city")
    private String city;

    @SerializedName("cityid")
    private String cityId;

    @SerializedName("temp1")
    private String temp1;

    @SerializedName("temp2")
    private String temp2;

    @SerializedName("weather")
    private String weather;

    @SerializedName("ptime")
    private String ptime;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCityId() {
        return cityId;
    }

    public void setCityId(String cityId) {
        this.cityId = cityId;
    }

    public String getTemp1() {
        return temp1;
    }

    public void setTemp1(String temp1) {
        this.temp1 = temp1;
    }

    public String getTemp2() {
        return temp2;
    }

    public void setTemp2(String temp2) {
        this.temp2 = temp2;
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public String getPtime() {
        return ptime;
    }

    public void setPtime(String ptime) {
        this.ptime = ptime;
    }
}
