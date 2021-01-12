package com.inuker.bluetooth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener;
import com.inuker.bluetooth.library.connect.response.BleMtuResponse;
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse;
import com.inuker.bluetooth.library.connect.response.BleReadResponse;
import com.inuker.bluetooth.library.connect.response.BleUnnotifyResponse;
import com.inuker.bluetooth.library.connect.response.BleWriteResponse;
import com.inuker.bluetooth.library.utils.BluetoothLog;
import com.inuker.bluetooth.library.utils.ByteUtils;

import static com.inuker.bluetooth.library.Constants.*;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Created by dingjikerbo on 2016/9/6.
 */
public class CharacterActivity extends Activity implements View.OnClickListener {

    private FileOutputStream outputStream;
    private String filename;
    private int cnt = 0;

    private String mMac;
    private UUID mService;
    private UUID mCharacter;

    private TextView mTvTitle;

    private Button mBtnRead;
    private TextView volt;


    private Button mBtnNotify;
    private Button mBtnUnnotify;



    /**
     * 上传文件到服务器
     * @param filePath       本地文件路径
     */
    public static void uploadFile(String filePath){
        String result = null;
        String TAG = "uploadFile";
        String  BOUNDARY =  UUID.randomUUID().toString();  //边界标识   随机生成
        String PREFIX = "--" , LINE_END = "\r\n";
        String CONTENT_TYPE = "multipart/form-data";   //内容类型
        int TIME_OUT = 10*1000;
        File file = new File(filePath);
        try {
            URL url = new URL("http://192.168.123.32:8000/upload/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(TIME_OUT);
            conn.setConnectTimeout(TIME_OUT);
            conn.setDoInput(true);  //允许输入流
            conn.setDoOutput(true); //允许输出流
            conn.setUseCaches(false);  //不允许使用缓存
            conn.setRequestMethod("POST");  //请求方式
            conn.setRequestProperty("Charset", "utf-8");  //设置编码
            conn.setRequestProperty("connection", "keep-alive");
            conn.setRequestProperty("Content-Type", CONTENT_TYPE + ";boundary=" + BOUNDARY);

            if(file!=null)
            {
                /**
                 * 当文件不为空，把文件包装并且上传
                 */
                DataOutputStream dos = new DataOutputStream( conn.getOutputStream());
                StringBuffer sb = new StringBuffer();
                sb.append(PREFIX);
                sb.append(BOUNDARY);
                sb.append(LINE_END);
                /**
                 * 这里重点注意：
                 * name里面的值为服务器端需要key   只有这个key 才可以得到对应的文件
                 * filename是文件的名字，包含后缀名的   比如:abc.png
                 */

                sb.append("Content-Disposition: form-data; name=\"file\"; filename=\""+file.getName()+"\""+LINE_END);
                sb.append("Content-Type: application/octet-stream; charset="+"utf-8"+LINE_END);
                sb.append(LINE_END);
                dos.write(sb.toString().getBytes());
                InputStream is = new FileInputStream(file);
                byte[] bytes = new byte[1024];
                int len = 0;
                while((len=is.read(bytes))!=-1)
                {
                    dos.write(bytes, 0, len);
                }
                is.close();
                dos.write(LINE_END.getBytes());
                byte[] end_data = (PREFIX+BOUNDARY+PREFIX+LINE_END).getBytes();
                dos.write(end_data);
                dos.flush();
                /**
                 * 获取响应码  200=成功
                 * 当响应成功，获取响应的流
                 */
                int res = conn.getResponseCode();
                Log.e(TAG, "response code:"+res);
//                if(res==200)
//                {
                Log.e(TAG, "request 成功");

                Looper.prepare();
                CommonUtils.toast("数据上传成功");
                Looper.loop();

                InputStream input =  conn.getInputStream();
                StringBuffer sb1= new StringBuffer();
                int ss ;
                while((ss=input.read())!=-1)
                {
                    sb1.append((char)ss);
                }
                result = sb1.toString();
                Log.e(TAG, "result : "+ result);
//                }
//                else{
//                    Log.e(TAG, "request 失败");
//                }
            }
        } catch (MalformedURLException e) {
            Looper.prepare();
            CommonUtils.toast("数据上传失败");
            Looper.loop();
            e.printStackTrace();
        } catch (IOException e) {
            Looper.prepare();
            CommonUtils.toast("数据上传失败");
            Looper.loop();
            e.printStackTrace();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.character_activity);

        Intent intent = getIntent();
        mMac = intent.getStringExtra("mac");
        mService = (UUID) intent.getSerializableExtra("service");
        mCharacter = (UUID) intent.getSerializableExtra("character");

        mTvTitle = (TextView) findViewById(R.id.title);
        mTvTitle.setText(String.format("%s", mMac));

        mBtnRead = (Button) findViewById(R.id.read);
        volt = (TextView) findViewById(R.id.volt);

        mBtnNotify = (Button) findViewById(R.id.notify);
        mBtnUnnotify = (Button) findViewById(R.id.unnotify);


        mBtnRead.setOnClickListener(this);

        mBtnNotify.setOnClickListener(this);
        mBtnNotify.setEnabled(true);

        mBtnUnnotify.setOnClickListener(this);
        mBtnUnnotify.setEnabled(false);

    }

    private final BleReadResponse mReadRsp = new BleReadResponse() {
        @Override
        public void onResponse(int code, byte[] data) {
            if (code == REQUEST_SUCCESS) {
                cnt++;
                mBtnRead.setText(ByteUtils.byteToString(data));
                float batteryvolt;
                Log.w("time", "" + System.currentTimeMillis() + ' ' + cnt);
                batteryvolt = (float) ((data[12] & 0x00ff) + (float) (data[13] & 0x00ff) / 256.0);
                DecimalFormat decimalFormat =new DecimalFormat("0.00");
                volt.setText("电池电压: "+decimalFormat.format(batteryvolt)+"V");
                CommonUtils.toast("成功");

//                ClientManager.getClient().read(mMac, mService, mCharacter, mReadRsp);
            } else {
                CommonUtils.toast("失败");
                mBtnRead.setText("读取");
            }
        }
    };

    private final BleWriteResponse mWriteRsp = new BleWriteResponse() {
        @Override
        public void onResponse(int code) {
            if (code == REQUEST_SUCCESS) {
                CommonUtils.toast("成功");
            } else {
                CommonUtils.toast("失败");
            }
        }
    };

    private final BleNotifyResponse mNotifyRsp = new BleNotifyResponse() {
        @Override
        public void onNotify(UUID service, UUID character, byte[] value) {
            if (service.equals(mService) && character.equals(mCharacter)) {
                mBtnNotify.setText(String.format("%s", ByteUtils.byteToString(value)));
                cnt++;
                short Accel_X = 0, Accel_Y = 0, Accel_Z = 0, Gyro_X = 0, Gyro_Y, Gyro_Z = 0;
                float batteryvolt;
                Accel_X = (short) (value[0] & 0x00ff | (value[1] << 8));
                Accel_Y = (short) (value[2] & 0x00ff | (value[3] << 8));
                Accel_Z = (short) (value[4] & 0x00ff | (value[5] << 8));
                Gyro_X = (short) (value[6] & 0x00ff | (value[7] << 8));
                Gyro_Y = (short) (value[8] & 0x00ff | (value[9] << 8));
                Gyro_Z = (short) (value[10] & 0x00ff | (value[11] << 8));
                batteryvolt = (float) ((value[12] & 0x00ff) + (float) (value[13] & 0x00ff) / 256.0);
                DecimalFormat decimalFormat =new DecimalFormat("0.00");
                volt.setText("电池电压: "+decimalFormat.format(batteryvolt)+"V");
                String final_str;
                final_str = Integer.toString(Accel_X) + ',' + Integer.toString(Accel_Y) + ',' + Integer.toString(Accel_Z) + ',' +
                        Integer.toString(Gyro_X) + ',' + Integer.toString(Gyro_Y) + ',' + Integer.toString(Gyro_Z) + '\n';
                Log.w("time", "" + System.currentTimeMillis() + ' ' + cnt + ' ' + final_str);
                try {
                    outputStream.write(final_str.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onResponse(int code) {
            if (code == REQUEST_SUCCESS) {
                mBtnNotify.setEnabled(false);
                mBtnUnnotify.setEnabled(true);
                CommonUtils.toast("成功");
            } else {
                CommonUtils.toast("失败");
            }
        }
    };

    private final BleUnnotifyResponse mUnnotifyRsp = new BleUnnotifyResponse() {
        @Override
        public void onResponse(int code) {
            if (code == REQUEST_SUCCESS) {
                CommonUtils.toast("成功");
                mBtnNotify.setEnabled(true);
                mBtnUnnotify.setEnabled(false);
            } else {
                CommonUtils.toast("失败");
            }
        }
    };

    private final BleMtuResponse mMtuResponse = new BleMtuResponse() {
        @Override
        public void onResponse(int code, Integer data) {
            if (code == REQUEST_SUCCESS) {
                CommonUtils.toast("request mtu 成功,mtu = " + data);
            } else {
                CommonUtils.toast("request mtu failed");
            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.read:
                ClientManager.getClient().read(mMac, mService, mCharacter, mReadRsp);
                break;
            case R.id.notify:
                Context context = getApplicationContext();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");// HH:mm:ss
//获取当前时间
                Date date = new Date(System.currentTimeMillis());
                filename = simpleDateFormat.format(date);
                File createFiles = new File(context.getExternalFilesDir("csv"), filename + ".csv");
                try {
                    createFiles.createNewFile();
                } catch (IOException e) {
                    Log.d("files err", "files err:" + e.getMessage());
                }
                try {
                    //mode参数注意下,这里使用的Context.MODE_PRIVATE
                    File file = new File(context.getExternalFilesDir("csv").toString() + File.separator + filename + ".csv");
//                    outputStream = context.openFileOutput(context.getExternalFilesDir("csv").toString()+ File.separator +filename+".txt", Context.MODE_PRIVATE );
                    outputStream = new FileOutputStream(file);

                    outputStream.write("Accel_X,Accel_Y,Accel_Z,Gyro_X,Gyro_Y,Gyro_Z\n".getBytes());
//                    outputStream.close();
                } catch (IOException e) {
                    Log.d("outputStream err", "outputStream err:" + e.getMessage());
                }
                ClientManager.getClient().notify(mMac, mService, mCharacter, mNotifyRsp);
                break;
            case R.id.unnotify:
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            outputStream.close();
                            new Thread(new Runnable(){
                                @Override
                                public void run() {
                                    Context context = getApplicationContext();
                                    uploadFile(context.getExternalFilesDir("csv")+"/"+filename + ".csv");
                                }
                            }).start();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, 50); //毫秒单位

                ClientManager.getClient().unnotify(mMac, mService, mCharacter, mUnnotifyRsp);
                break;
        }
    }

    private final BleConnectStatusListener mConnectStatusListener = new BleConnectStatusListener() {
        @Override
        public void onConnectStatusChanged(String mac, int status) {
            BluetoothLog.v(String.format("CharacterActivity.onConnectStatusChanged status = %d", status));

            if (status == STATUS_DISCONNECTED) {
                CommonUtils.toast("连接断开");
                mBtnRead.setEnabled(false);
                mBtnNotify.setEnabled(false);
                mBtnUnnotify.setEnabled(false);

                mTvTitle.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        finish();
                    }
                }, 300);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        ClientManager.getClient().registerConnectStatusListener(mMac, mConnectStatusListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ClientManager.getClient().unregisterConnectStatusListener(mMac, mConnectStatusListener);
    }


}
