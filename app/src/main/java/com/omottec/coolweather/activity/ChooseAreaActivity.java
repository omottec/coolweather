package com.omottec.coolweather.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.activeandroid.Model;
import com.activeandroid.query.Select;
import com.omottec.coolweather.R;
import com.omottec.coolweather.log.Logger;
import com.omottec.coolweather.model.City;
import com.omottec.coolweather.model.County;
import com.omottec.coolweather.model.Province;
import com.omottec.coolweather.net.LogInterceptor;
import com.omottec.coolweather.util.WeatherParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;

public class ChooseAreaActivity extends FragmentActivity {
	public static final String TAG = "ChooseAreaActivity";
	public static final int LEVEL_PROVINCE = 0;
	public static final int LEVEL_CITY = 1;
	public static final int LEVEL_COUNTY = 2;
	
	private ProgressDialog mDialog;
	private TextView mTitleTv;
	private ListView mListView;

    private boolean isFromWeatherActivity;
	private ArrayAdapter<String> mAdapter;
	private List<String> mDataList = new ArrayList<String>();
	private List<Province> mProvinceList;
	private List<City> mCityList;
	private List<County> mCountyList;
	private Province mSelectedProvince;
	private City mSelectedCity;
	private int mUiLevel = LEVEL_PROVINCE;
    private int mQueryLevel;
	private WeatherParser mParser = new WeatherParser();
	private ExecutorService mExecutor = Executors.newCachedThreadPool();
    private final OkHttpClient mClient = new OkHttpClient.Builder()
            .addInterceptor(new LogInterceptor())
            .build();
    private Callback mCallback = new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            e.printStackTrace();
            closeProgressDialog();
            Toast.makeText(ChooseAreaActivity.this,
                    "加载失败", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onResponse(Call call, okhttp3.Response response) throws IOException {
            if (!response.isSuccessful() || response.body() == null) return;
            switch (mQueryLevel) {
                case LEVEL_COUNTY:
                    final List<County> counties = mParser.parseCounties(response.body().string(), mSelectedCity);
                    response.body().close();
                    if (counties != null && !counties.isEmpty()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateCountyUi(counties);
                            }
                        });
                        insert2Db(counties);
                    }
                    break;
                case LEVEL_CITY:
                    final List<City> cities = mParser.parseCities(response.body().string(), mSelectedProvince);
                    response.body().close();
                    if (cities != null && !cities.isEmpty()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateCityUi(cities);
                            }
                        });
                        insert2Db(cities);
                    }
                    break;
                case LEVEL_PROVINCE:
                    final List<Province> provinces = mParser.parseProvinces(response.body().string());
                    response.body().close();
                    if (provinces != null && !provinces.isEmpty()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateProvinceUi(provinces);
                            }
                        });
                        insert2Db(provinces);
                    }
                    break;
            }
        }
    };

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		Logger.logClassAndMethod(this);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Logger.logClassAndMethod(this);
		isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity", false);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (prefs.getBoolean(WeatherActivity.COUNTY_SELECTED, false) && !isFromWeatherActivity) {
			Intent intent = new Intent(this, WeatherActivity.class);
			startActivity(intent);
			finish();
			return;
		}
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.a_choose_area);
		mListView = (ListView) findViewById(R.id.list_view);
		mTitleTv = (TextView) findViewById(R.id.title_tv);
		mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mDataList);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int index,
					long arg3) {
                switch (mUiLevel) {
                    case LEVEL_COUNTY:
                        String countyCode = mCountyList.get(index).getCode();
                        Intent intent = new Intent(ChooseAreaActivity.this, WeatherActivity.class);
                        intent.putExtra(WeatherActivity.COUNTY_CODE, countyCode);
                        startActivity(intent);
                        finish();
                        break;
                    case LEVEL_CITY:
                        mSelectedCity = mCityList.get(index);
                        queryCounties();
                        break;
                    case LEVEL_PROVINCE:
                        mSelectedProvince = mProvinceList.get(index);
                        queryCities();
                        break;
                }
			}
		});
		queryProvinces();  // 加载省级数据
	}

	private void queryProvinces() {
        showProgressDialog();
		new AsyncTask<Void, Void, List<Province>>() {

			@Override
			protected List<Province> doInBackground(Void... params) {
				return new Select().from(Province.class).execute();
			}

			@Override
			protected void onPostExecute(List<Province> provinces) {
				if (provinces != null && !provinces.isEmpty()) {
                    updateProvinceUi(provinces);
				} else {
					queryFromServer(null, LEVEL_PROVINCE);
				}
			}
		}.execute();
	}

	private void updateProvinceUi(List<Province> provinces) {
		mProvinceList = provinces;
		mDataList.clear();
		for (Province province : mProvinceList)
			mDataList.add(province.getName());
		mAdapter.notifyDataSetChanged();
		mListView.setSelection(0);
		mTitleTv.setText("中国");
		mUiLevel = LEVEL_PROVINCE;
		closeProgressDialog();
	}

	private void insert2Db(final List<? extends Model> models) {
		if (models == null || models.isEmpty()) return;
		mExecutor.submit(new Runnable() {
			@Override
			public void run() {
				for (Model m : models)
					m.save();
			}
		});
	}

	private void queryCities() {
        showProgressDialog();
		new AsyncTask<Void, Void, List<City>>() {

			@Override
			protected List<City> doInBackground(Void... params) {
				return mSelectedProvince.getCities();
			}

			@Override
			protected void onPostExecute(List<City> cities) {
				if (cities != null && !cities.isEmpty()) {
                    updateCityUi(cities);
				} else {
					queryFromServer(mSelectedProvince.getCode(), LEVEL_CITY);
				}
			}
		}.execute();
	}

	private void updateCityUi(List<City> cities) {
		mCityList = cities;
		mDataList.clear();
		for (City city : mCityList) {
			mDataList.add(city.getName());
		}
		mAdapter.notifyDataSetChanged();
		mListView.setSelection(0);
		mTitleTv.setText(mSelectedProvince.getName());
		mUiLevel = LEVEL_CITY;
		closeProgressDialog();
	}
	
	private void queryCounties() {
        showProgressDialog();
		new AsyncTask<Void, Void, List<County>>() {

			@Override
			protected List<County> doInBackground(Void... params) {
				return mSelectedCity.getCounties();
			}

			@Override
			protected void onPostExecute(List<County> counties) {
				if (counties != null && !counties.isEmpty()) {
                    updateCountyUi(counties);
				} else {
					queryFromServer(mSelectedCity.getCode(), LEVEL_COUNTY);
				}
			}
		}.execute();
	}

	private void updateCountyUi(List<County> counties) {
		mCountyList = counties;
		mDataList.clear();
		for (County county : mCountyList)
			mDataList.add(county.getName());
		mAdapter.notifyDataSetChanged();
		mListView.setSelection(0);
		mTitleTv.setText(mSelectedCity.getName());
		mUiLevel = LEVEL_COUNTY;
		closeProgressDialog();
	}

	private void queryFromServer(String code, int queryLevel) {
        mQueryLevel = queryLevel;
		String url;
		if (!TextUtils.isEmpty(code))
			url = "http://www.weather.com.cn/data/list3/city" + code + ".xml";
		else
			url = "http://www.weather.com.cn/data/list3/city.xml";
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .cacheControl(new CacheControl.Builder().noStore().build())
                .build();
        mClient.newCall(request).enqueue(mCallback);
	}
	
	/**
	 * 显示进度对话框
	 */
	private void showProgressDialog() {
		if (mDialog == null) {
			mDialog = new ProgressDialog(this);
			mDialog.setMessage("正在加载...");
			mDialog.setCanceledOnTouchOutside(false);
		}
		mDialog.show();
	}
	
	/**
	 * 关闭进度对话框
	 */
	private void closeProgressDialog() {
		if (mDialog != null) {
			mDialog.dismiss();
		}
	}
	
	@Override
	public void onBackPressed() {
		if (mUiLevel == LEVEL_COUNTY) {
			queryCities();
		} else if (mUiLevel == LEVEL_CITY) {
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