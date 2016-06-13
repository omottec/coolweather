package com.omottec.coolweather.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
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

import com.activeandroid.query.Select;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.omottec.coolweather.R;
import com.omottec.coolweather.log.Logger;
import com.omottec.coolweather.model.City;
import com.omottec.coolweather.model.County;
import com.omottec.coolweather.model.Province;
import com.omottec.coolweather.net.HttpManager;
import com.omottec.coolweather.util.Utility;

//import org.apache.http.protocol.HTTP;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChooseAreaActivity extends Activity {
	public static final String TAG = "ChooseAreaActivity";
	public static final int LEVEL_PROVINCE = 0;
	public static final int LEVEL_CITY = 1;
	public static final int LEVEL_COUNTY = 2;
	
	private ProgressDialog progressDialog;
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
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
				Logger.d(TAG, "onResponse in ui thread:"
						+ (Looper.myLooper() == Looper.getMainLooper()));
				boolean result = false;
				if ("province".equals(mType)) {
					result = Utility.handleProvincesResponse(response);
				} else if ("city".equals(mType)) {
					result = Utility.handleCitiesResponse(response, selectedProvince);
				} else if ("county".equals(mType)) {
					result = Utility.handleCountiesResponse(response, selectedCity);
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
//		coolWeatherDB = CoolWeatherDB.getInstance(this);
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
					String countyCode = countyList.get(index).getCode();
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
		provinceList = new Select().from(Province.class).execute();
//		provinceList = coolWeatherDB.loadProvinces();
		if (provinceList.size() > 0) {
			dataList.clear();
			for (Province province : provinceList) {
				dataList.add(province.getName());
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
		if (selectedProvince != null)
			cityList = selectedProvince.getCities();
		if (cityList.size() > 0) {
			dataList.clear();
			for (City city : cityList) {
				dataList.add(city.getName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedProvince.getName());
			currentLevel = LEVEL_CITY;
		} else {
			queryFromServer(selectedProvince.getCode(), "city");
		}
	}
	
	/**
	 * 查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询。
	 */
	private void queryCounties() {
		countyList = new Select().from(County.class).execute();
		if (countyList.size() > 0) {
			dataList.clear();
			for (County county : countyList) {
				dataList.add(county.getName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getName());
			currentLevel = LEVEL_COUNTY;
		} else {
			queryFromServer(selectedCity.getCode(), "county");
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
				Logger.d(TAG, "parseNetworkResponse in ui thread:"
						+ (Looper.myLooper() == Looper.getMainLooper()));
				try {
					String contentType = response.headers.get("Content-Type");
					Logger.d(TAG, "contentType:" + contentType);
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