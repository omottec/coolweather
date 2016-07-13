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

import com.omottec.coolweather.R;
import com.omottec.coolweather.net.LogInterceptor;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;


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
	private String mCodeType;
    private String mCountyCode;
    private String mWeatherCode;
	private final OkHttpClient mClient = new OkHttpClient.Builder()
			.addInterceptor(new LogInterceptor())
			.build();
	private Callback mCallback;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.a_weather);
		initViews();
		mCountyCode = getIntent().getStringExtra(COUNTY_CODE);
		mCallback = new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mPublishTv.setText(R.string.sync_failed);
					}
				});
			}

			@Override
			public void onResponse(Call call, final okhttp3.Response response) throws IOException {
				if (!response.isSuccessful()) return;
				final String responseStr = response.body().string();
				response.close();
				switch (mCodeType) {
					case COUNTY_CODE:
						// 从服务器返回的数据中解析出天气代号
						String[] array = responseStr.split("\\|");
						if (array.length == 2) {
							mWeatherCode = array[1];
							queryWeatherInfo(mWeatherCode);
						}
						break;
					case WEATHER_CODE:
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								showWeather(responseStr);
							}
						});
						break;
				}
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
		Request request = new Request.Builder()
				.url(url)
				.cacheControl(new CacheControl.Builder().noStore().build())
				.build();
		mClient.newCall(request).enqueue(mCallback);
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