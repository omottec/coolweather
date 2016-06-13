package com.omottec.coolweather.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.omottec.coolweather.R;
import com.omottec.coolweather.db.CoolWeatherDB;
import com.omottec.coolweather.log.Logger;
import com.omottec.coolweather.model.City;
import com.omottec.coolweather.model.County;
import com.omottec.coolweather.model.Province;
import com.omottec.coolweather.net.HttpManager;
import com.omottec.coolweather.util.HttpCallbackListener;
import com.omottec.coolweather.util.HttpUtil;
import com.omottec.coolweather.util.Utility;

import org.apache.http.protocol.HTTP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChooseAreaActivity extends Activity {
	public static final String TAG = "ChooseAreaActivity";
	public static final int LEVEL_PROVINCE = 0;
	public static final int LEVEL_CITY = 1;
	public static final int LEVEL_COUNTY = 2;
	
	private ProgressDialog progressDialog;
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
	private CoolWeatherDB coolWeatherDB;
	private List<String> dataList = new ArrayList<String>();
	/**
	 * 省列表
	 */
	private List<Province> provinceList;
	/**
	 * 市列表
	 */
	private List<City> cityList;
	/**
	 * 县列表
	 */
	private List<County> countyList;
	/**
	 * 选中的省份
	 */
	private Province selectedProvince;
	/**
	 * 选中的城市
	 */
	private City selectedCity;
	/**
	 * 当前选中的级别
	 */
	private int currentLevel;
	/**
	 * 是否从WeatherActivity中跳转过来。
	 */
	private boolean isFromWeatherActivity;

	private Response.Listener<String> mListener;

	private Response.ErrorListener mErrorListener;

	private String mType;

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		Logger.logClassAndMethod(this);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Logger.logClassAndMethod(this);
		Logger.d(TAG, "sdkVersion=" + Build.VERSION.SDK_INT + ", local=" + Locale.getDefault());
		Log.d(TAG, "sdkVersion=" + Build.VERSION.SDK_INT + ", local=" + Locale.getDefault());
		isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity", false);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (prefs.getBoolean("city_selected", false) && !isFromWeatherActivity) {
			Intent intent = new Intent(this, WeatherActivity.class);
			startActivity(intent);
			finish();
			return;
		}
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.choose_area);
		listView = (ListView) findViewById(R.id.list_view);
		titleText = (TextView) findViewById(R.id.title_text);
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList);
		listView.setAdapter(adapter);
		mListener = new Response.Listener<String>() {
			@Override
			public void onResponse(String response) {
				boolean result = false;
				if ("province".equals(mType)) {
					result = Utility.handleProvincesResponse(coolWeatherDB,
							response);
				} else if ("city".equals(mType)) {
					result = Utility.handleCitiesResponse(coolWeatherDB,
							response, selectedProvince.getId());
				} else if ("county".equals(mType)) {
					result = Utility.handleCountiesResponse(coolWeatherDB,
							response, selectedCity.getId());
				}
				if (result) {
					closeProgressDialog();
					if ("province".equals(mType)) {
						queryProvinces();
					} else if ("city".equals(mType)) {
						queryCities();
					} else if ("county".equals(mType)) {
						queryCounties();
					}
				}
			}
		};
		mErrorListener = new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				closeProgressDialog();
				Toast.makeText(ChooseAreaActivity.this,
						"加载失败", Toast.LENGTH_SHORT).show();
			}
		};
		coolWeatherDB = CoolWeatherDB.getInstance(this);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int index,
					long arg3) {
				if (currentLevel == LEVEL_PROVINCE) {
					selectedProvince = provinceList.get(index);
					queryCities();
				} else if (currentLevel == LEVEL_CITY) {
					selectedCity = cityList.get(index);
					queryCounties();
				} else if (currentLevel == LEVEL_COUNTY) {
					String countyCode = countyList.get(index).getCountyCode();
					Intent intent = new Intent(ChooseAreaActivity.this, WeatherActivity.class);
					intent.putExtra("county_code", countyCode);
					startActivity(intent);
					finish();
				}
			}
		});
		queryProvinces();  // 加载省级数据
	}

	/**
	 * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询。
	 */
	private void queryProvinces() {
		provinceList = coolWeatherDB.loadProvinces();
		if (provinceList.size() > 0) {
			dataList.clear();
			for (Province province : provinceList) {
				dataList.add(province.getProvinceName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText("中国");
			currentLevel = LEVEL_PROVINCE;
		} else {
			queryFromServer(null, "province");
		}
	}

	/**
	 * 查询选中省内所有的市，优先从数据库查询，如果没有查询到再去服务器上查询。
	 */
	private void queryCities() {
		cityList = coolWeatherDB.loadCities(selectedProvince.getId());
		if (cityList.size() > 0) {
			dataList.clear();
			for (City city : cityList) {
				dataList.add(city.getCityName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedProvince.getProvinceName());
			currentLevel = LEVEL_CITY;
		} else {
			queryFromServer(selectedProvince.getProvinceCode(), "city");
		}
	}
	
	/**
	 * 查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询。
	 */
	private void queryCounties() {
		countyList = coolWeatherDB.loadCounties(selectedCity.getId());
		if (countyList.size() > 0) {
			dataList.clear();
			for (County county : countyList) {
				dataList.add(county.getCountyName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());
			currentLevel = LEVEL_COUNTY;
		} else {
			queryFromServer(selectedCity.getCityCode(), "county");
		}
	}
	
	/**
	 * 根据传入的代号和类型从服务器上查询省市县数据。
	 */
	private void queryFromServer(final String code, final String type) {
		mType = type;
		String address;
		if (!TextUtils.isEmpty(code)) {
			address = "http://www.weather.com.cn/data/list3/city" + code + ".xml";
		} else {
			address = "http://www.weather.com.cn/data/list3/city.xml";
		}
		showProgressDialog();
		StringRequest request = new StringRequest(Request.Method.GET, address, mListener, mErrorListener) {
			@Override
			protected Response<String> parseNetworkResponse(NetworkResponse response) {
				try {
					String contentType = response.headers.get(HTTP.CONTENT_TYPE);
					Logger.d(TAG, "contentType:" + contentType);
					if (TextUtils.isEmpty(contentType)) {
						response.headers.put(HTTP.CONTENT_TYPE, "charset=UTF-8");
					} else if (!contentType.contains(HTTP.UTF_8)) {
						StringBuilder sb = new StringBuilder(contentType)
								.append(HTTP.CHARSET_PARAM)
								.append(HTTP.UTF_8);
						response.headers.put(HTTP.CONTENT_TYPE, sb.toString());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return super.parseNetworkResponse(response);
			}
		};
		HttpManager.getRequestQueue().add(request);
	}
	
	/**
	 * 显示进度对话框
	 */
	private void showProgressDialog() {
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("正在加载...");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}
	
	/**
	 * 关闭进度对话框
	 */
	private void closeProgressDialog() {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
	}
	
	/**
	 * 捕获Back按键，根据当前的级别来判断，此时应该返回市列表、省列表、还是直接退出。
	 */
	@Override
	public void onBackPressed() {
		if (currentLevel == LEVEL_COUNTY) {
			queryCities();
		} else if (currentLevel == LEVEL_CITY) {
			queryProvinces();
		} else {
			if (isFromWeatherActivity) {
				Intent intent = new Intent(this, WeatherActivity.class);
				startActivity(intent);
			}
			finish();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		Logger.logClassAndMethod(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Logger.logClassAndMethod(this);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Logger.logClassAndMethod(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Logger.logClassAndMethod(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		Logger.logClassAndMethod(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Logger.logClassAndMethod(this);
	}

	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		Logger.logClassAndMethod(this);
	}
}