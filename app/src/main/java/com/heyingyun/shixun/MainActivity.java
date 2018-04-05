package com.heyingyun.shixun;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.qrcode.Constant;
import com.example.qrcode.ScannerActivity;


import java.io.IOException;

import cn.jpush.android.api.JPushInterface;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity {
    private WebView viewById;
    private String device;
    private String url;
    public final String HOST = "http://shixun.heyingyun.com/index.php/Home/Login/login.html";
    private final int REQUEST_PERMISION_CODE_CAMARE = 0;
    private final int RESULT_REQUEST_CODE = 1;
    public ValueCallback<Uri[]> mUploadMessageForAndroid5;
    public final static int FILECHOOSER_RESULTCODE_FOR_ANDROID_5 = 2;

    @SuppressLint({"JavascriptInterface", "SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        IntentFilter intentFilter = new IntentFilter("android.intent.action.MAIN");
        registerReceiver(mReceiver, intentFilter);
        //推送注册
        JPushInterface.setDebugMode(false);
        JPushInterface.init(this);
        //启用支持javascript
        viewById = (WebView) findViewById(R.id.webview);
        WebSettings settings = viewById.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setAllowFileAccess(true);// 设置允许访问文件数据
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setUseWideViewPort(true);//将图片调整到适合webview的大小
        settings.setLoadWithOverviewMode(true);// 缩放至屏幕的大小
        //verifyStoragePermissions(this);
        viewById.requestFocus();
        //viewById.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        viewById.loadUrl("http://shixun.heyingyun.com");
        // 通过addJavascriptInterface()将Java对象映射到JS对象
        //参数1：Javascript对象名
        //参数2：Java对象名
        viewById.addJavascriptInterface(new JavaScriptObject(), "androidJs");
        //选取相册图片
        viewById.setWebChromeClient(
                new WebChromeClient() {
                    public void onProgressChanged(WebView view, int progress) {// 载入进度改变而触发
                        if (progress == 100) {
                            //handler.sendEmptyMessage(1);// 如果全部载入,隐藏进度对话框
                        }
                        super.onProgressChanged(view, progress);
                    }

                    //扩展支持alert事件
                    @Override
                    public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                        builder.setTitle("提示").setMessage(message).setPositiveButton("确定", null);
                        builder.setCancelable(false);
                        //builder.setIcon(R.drawable.ic_launcher);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        result.confirm();
                        return true;
                    }

                    // For Android > 5.0
                    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> uploadMsg, WebChromeClient.FileChooserParams fileChooserParams) {
                        openFileChooserImplForAndroid5(uploadMsg);
                        return true;
                    }
                }
        );
        //覆盖WebView默认使用第三方或系统默认浏览器打开网页的行为，使网页用WebView打开
        viewById.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // TODO Auto-generated method stub
                //返回值是true的时候控制去WebView打开，为false调用系统浏览器或第三方浏览器
                view.loadUrl(url);
                return true;
            }
        });
    }

    //选取相册图片
    private void openFileChooserImplForAndroid5(ValueCallback<Uri[]> uploadMsg) {
        mUploadMessageForAndroid5 = uploadMsg;
        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("image/*");
        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
        startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE_FOR_ANDROID_5);
    }

    //改写物理按键——返回的逻辑
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
        url = viewById.getUrl();
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (TextUtils.equals(HOST, url)) {
               /* if ((System.currentTimeMillis() - exitTime) > 2000) {
                    Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();
                    exitTime = System.currentTimeMillis();
                } else {*/
                finish();//如果是重定向网址就finish
                //}
                return true;
            } else {
                viewById.goBack();//返回上一个页面
                return true;
            }
        }
        return false;
    }

    //打开扫描
    private void Scanning() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            goScanner();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISION_CODE_CAMARE);
        }
    }


    private void goScanner() {
        Intent intent = new Intent(this, ScannerActivity.class);
//        这里可以用intent传递一些参数，比如扫码聚焦框尺寸大小，支持的扫码类型。
//        //设置扫码框的宽
//        intent.putExtra(Constant.EXTRA_SCANNER_FRAME_WIDTH, 400);
//        //设置扫码框的高
//        intent.putExtra(Constant.EXTRA_SCANNER_FRAME_HEIGHT, 400);
//        //设置扫码框距顶部的位置
//        intent.putExtra(Constant.EXTRA_SCANNER_FRAME_TOP_PADDING, 100);
//        //设置是否启用从相册获取二维码。
//        intent.putExtra(Constant.EXTRA_IS_ENABLE_SCAN_FROM_PIC,true);
//        Bundle bundle = new Bundle();
//        //设置支持的扫码类型
//        bundle.putSerializable(Constant.EXTRA_SCAN_CODE_TYPE, mHashMap);
//        intent.putExtras(bundle);
        startActivityForResult(intent, RESULT_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISION_CODE_CAMARE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    goScanner();
                }
                return;
            }

        }
    }

    //扫描返回结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case RESULT_REQUEST_CODE:
                    if (data == null) return;
                    //返回结果
                    String type = data.getStringExtra(Constant.EXTRA_RESULT_CODE_TYPE);
                    String content = data.getStringExtra(Constant.EXTRA_RESULT_CONTENT);
                    //跳转页面
                    viewById.loadUrl(content);
                    break;
                default:
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    //js交互的方法
    public class JavaScriptObject {
        //扫描
        @JavascriptInterface
        public void fun1FromAndroid() {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Scanning();
                }
            });
        }

        //极光推送
        @JavascriptInterface
        public void pushNotification(final String userId, final String loginType) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    viewById.loadUrl("javascript:alertMessage(\" " + userId + "看看" + loginType + "\")");
                    Log.v("大厦", userId + "======" + loginType);
                    try {
                        LoginByPost(userId, loginType);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            device = intent.getStringExtra("Registration");
            Log.e("接收到的数据是", device);
            @SuppressLint("CommitPrefEdits")
            SharedPreferences.Editor deviceEt = getSharedPreferences("device", MODE_PRIVATE).edit();
            deviceEt.putString("name", device);
            deviceEt.apply();
        }
    };


    @SuppressLint("WrongConstant")
    private void LoginByPost(String userId, String loginType) throws IOException {
        SharedPreferences pref = getSharedPreferences("device", MODE_PRIVATE);
        String devicedd = pref.getString("name", "");
        Log.v("阿萨飒飒啊吐热", devicedd);
        //新建客户端
        OkHttpClient client = new OkHttpClient();
        //新建请求
        Request request = new Request.Builder()
                .get() //get请求
                .url("http://shixun.gaoliuxu.com/index.php/Home/Bind/binding/user_id/" + userId + "/loginType/" + loginType + "/device/" + devicedd)            //URL
                .build();
        //返回对象
        Response response = client.newCall(request).execute();
        //阻塞线程。
        if (response.isSuccessful()) {
            Log.e("code", ":" + response.code());
            Log.e("body", response.body().string());
        } else {
            Log.e("---", "不成功");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

