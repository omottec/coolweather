package com.omottec.coolweather.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.omottec.coolweather.R;
import com.omottec.coolweather.net.HttpManager;

import org.json.JSONObject;


public class WeatherActivity extends FragmentActivity implements OnClickListener{
	public static final String WEATHER_INFO = "weather_info";
	public static final String COUNTY_CODE = "county_code";
	public static final String WEATHER_CODE = "weather_code";
	public static final String COUNTY_SELECTED = "county_selected";
    public static final String FROM_WEATHER_ACTIVITY = "from_weather_activity";
	private LinearLayout mWeatherInfoLl;
	private TextView mCityNameTv;
	private TextView mPublishTv;
	private TextView mWeatherDespTv;
	private TextView mTemp1Tv;
	private TextView mTemp2Tv;
	/**
	 * 用于显示当前日期
	 */
	private TextView currentDateText;
	private Button mSwitchCityBtn;
	private Button mRefreshWeatherBtn;
	private Response.Listener<String> mListener;
	private Response.ErrorListener mErrorListener;
	private String mCodeType;
    private String mCountyCode;
    private String mWeatherCode;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.a_weather);
		initViews();
		mCountyCode = getIntent().getStringExtra(COUNTY_CODE);
		mListener = new Response.Listener<String>() {
			@Override
			public void onResponse(String response) {
				if (TextUtils.isEmpty(response)) return;
				switch (mCodeType) {
					case COUNTY_CODE:
						// 从服务器返回的数据中解析出天气代号
						String[] array = response.split("\\|");
						if (array.length == 2) {
							mWeatherCode = array[1];
							queryWeatherInfo(mWeatherCode);
						}
						break;
					case WEATHER_CODE:
						showWeather(response);
						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this);

						break;
				}
			}
		};
		mErrorListener = new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				mPublishTv.setText(R.string.sync_failed);
			}
		};
		if (!TextUtils.isEmpty(mCountyCode)) {
			mPublishTv.setText(R.string.syncing);
			mWeatherInfoLl.setVisibility(View.INVISIBLE);
			mCityNameTv.setVisibility(View.INVISIBLE);
			queryWeatherCode(mCountyCode);
		} else {
			showWeather(null);
		}
		mSwitchCityBtn.setOnClickListener(this);
		mRefreshWeatherBtn.setOnClickListener(this);
	}

	private void initViews() {
		mWeatherInfoLl = (LinearLayout) findViewById(R.id.weather_info_ll);
		mCityNameTv = (TextView) findViewById(R.id.city_name_tv);
		mPublishTv = (TextView) findViewById(R.id.publish_tv);
		mWeatherDespTv = (TextView) findViewById(R.id.weather_desc_tv);
		mTemp1Tv = (TextView) findViewById(R.id.temp1_tv);
		mTemp2Tv = (TextView) findViewById(R.id.temp2_tv);
		currentDateText = (TextView) findViewById(R.id.current_date_tv);
		mSwitchCityBtn = (Button) findViewById(R.id.switch_city_btn);
		mRefreshWeatherBtn = (Button) findViewById(R.id.refresh_weather_tv);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.switch_city_btn:
			Intent intent = new Intent(this, ChooseAreaActivity.class);
			intent.putExtra(FROM_WEATHER_ACTIVITY, true);
			startActivity(intent);
			finish();
			break;
		case R.id.refresh_weather_tv:
			mPublishTv.setText(R.string.syncing);
            if (!TextUtils.isEmpty(mWeatherCode)) {
                queryWeatherInfo(mWeatherCode);
                return;
            }
            if (!TextUtils.isEmpty(mCountyCode)) {
                queryWeatherCode(mCountyCode);
                return;
            }
			break;
		}
	}
	
	/**
	 * 查询县级代号所对应的天气代号。
	 */
	private void queryWeatherCode(String countyCode) {
		String url = "http://www.weather.com.cn/data/list3/city" + countyCode + ".xml";
		queryFromServer(url, COUNTY_CODE);
	}

	/**
	 * 查询天气代号所对应的天气。
	 */
	private void queryWeatherInfo(String weatherCode) {
		String url = "http://www.weather.com.cn/data/cityinfo/" + weatherCode + ".html";
		queryFromServer(url, WEATHER_CODE);
	}
	
	/**
	 * 根据传入的地址和类型去向服务器查询天气代号或者天气信息。
	 */
	private void queryFromServer(String url, String codeType) {
		mCodeType = codeType;
		Request request = new StringRequest(
				Request.Method.GET,
				url,
				mListener,
				mErrorListener) {
			@Override
			protected Response<String> parseNetworkResponse(NetworkResponse response) {
				try {
					String contentType = response.headers.get("Content-Type");
					if (TextUtils.isEmpty(contentType)) {
						response.headers.put("Content-Type", "charset=UTF-8");
					} else if (!contentType.contains("UTF-8")) {
						StringBuilder sb = new StringBuilder(contentType)
								.append("; charset=")
								.append("UTF-8");
						response.headers.put("Content-Type", sb.toString());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return super.parseNetworkResponse(response);
			}
		};
        request.setShouldCache(false);
		HttpManager.getRequestQueue().add(request);
	}
	
	private void showWeather(String weatherInfoStr) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (TextUtils.isEmpty(weatherInfoStr)) {
            weatherInfoStr = prefs.getString(WEATHER_INFO, null);
            if (TextUtils.isEmpty(weatherInfoStr)) return;
        }
		try {
            JSONObject obj = new JSONObject(weatherInfoStr);
            obj = obj.getJSONObject("weatherinfo");
            if (obj == null) return;
            mCityNameTv.setText(obj.getString("city"));
            mTemp1Tv.setText(obj.getString("temp1"));
            mTemp2Tv.setText(obj.getString("temp2"));
            mWeatherDespTv.setText(obj.getString("weather"));
            mPublishTv.setText(getString(R.string.push_time_desc, obj.getString("ptime")));
			mWeatherInfoLl.setVisibility(View.VISIBLE);
			mCityNameTv.setVisibility(View.VISIBLE);
            prefs
                    .edit()
                    .putBoolean(COUNTY_SELECTED, true)
                    .putString(WEATHER_INFO, weatherInfoStr)
                    .commit();
//			Intent intent = new Intent(this, AutoUpdateService.class);
//			startService(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}