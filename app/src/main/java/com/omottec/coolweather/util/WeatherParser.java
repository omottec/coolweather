package com.omottec.coolweather.util;

import android.text.TextUtils;

import com.omottec.coolweather.model.City;
import com.omottec.coolweather.model.County;
import com.omottec.coolweather.model.Province;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by qinbingbing on 6/15/16.
 */
public final class WeatherParser {
    public List<Province> parseProvinces(String response) {
        List<Province> provinces = new ArrayList<>();
        if (!TextUtils.isEmpty(response)) {
            try {
                String[] allProvinces = response.split(",");
                Province province;
                String[] array;
                for (String p : allProvinces) {
                    array = p.split("\\|");
                    province = new Province();
                    province.setCode(array[0]);
                    province.setName(array[1]);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return provinces;
    }

    public List<City> parseCities(String response, Province province) {
        List<City> cities = new ArrayList<>();
        if (!TextUtils.isEmpty(response)) {
            try {
                String[] allCities = response.split(",");
                City city;
                String[] array;
                for (String c : allCities) {
                    array = c.split("\\|");
                    city = new City();
                    city.setCode(array[0]);
                    city.setName(array[1]);
                    city.setProvince(province);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return cities;
    }

    public List<County> parseCounties(String response, City city) {
        List<County> counties = new ArrayList<>();
        if (!TextUtils.isEmpty(response)) {
            try {
                String[] allCounties = response.split(",");
                String[] array;
                County county;
                for (String c : allCounties) {
                    array = c.split("\\|");
                    county = new County();
                    county.setCode(array[0]);
                    county.setName(array[1]);
                    county.setCity(city);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return counties;
    }
}
