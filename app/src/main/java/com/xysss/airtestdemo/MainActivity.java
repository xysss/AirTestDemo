package com.xysss.airtestdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    //192.168.0.7
    private Button openSpe, setDevice, cancle, commit, history, commitWarnBt, checkWarnNumBT;
    private DatagramPacket dp, packet;
    private DatagramSocket ds; //指明发送端的端口号
    private boolean isopen = true;
    private List<Byte> recList = new ArrayList();
    private byte[] dealAfterRec = null;
    private Handler mHandler;
    private static final int chartMessage = 1;
    private static final int TIMER_OUT = 5;
    private static final int uiToast = 6;
    private static final int getWarnNum = 2;
    private static final int setWarnNumResult = 3;
    private int timerFlag = 0;
    private static BlockingDeque<ComBean> blockingDeque = new LinkedBlockingDeque(10000);  //队列
    private String sreiaData = "";//接收数据
    private LinearLayout setDetail, warnLl;
    private ConstraintLayout settingLinL;
    private final String ipSP = "IP";
    private final String portSP = "PORT";
    private final String warnSP = "WARN_NUMBER_SP";
    private String ipStr = "", portStr = "";
    private EditText ipET, portET, setWarnNumEt;
    private final String testCode1 = "55000A06010001000023";  //开始原始数据上传
    private final String testCodeOpen1 = "55000B0604000201010023";
    private final String testCode2 = "55000B0604000201010023";  //打开1号传感器
    private final String testCode4 = "55000E0602000501";  //设置阈值
    private final String testCode5 = "55000A06030001010023";  //读取阈值
    private final String[] testCodeClose=new String[]{"55000B0604000200000023","55000B0604000201000023","55000B0604000202000023",
            "55000B0604000203000023","55000B0604000204000023","55000B0604000205000023"};  ////关闭某一个传感器
    private BarChart air_chart;
    private final int airMinV = 5;
    private final int airMaxV = 20;
    private TextView resultNumber, resultName;
    private MyDataBaseHelper mDBHelper;
    private Boolean[] isOpenLEDNumber=new Boolean[6];


    /*读取设备信息 	 55 00 0A 06 00 00 01 00 00 23
    读取设备当前状态 55 00 0A 06 01 00 01 00 00 23
    设置报警阈值	 55 00 0E 06 02 00 05 00 xx xx xx xx 00 23
            55 00 0E 06 02 00 05 01 xx xx xx xx 00 23
            55 00 0E 06 02 00 05 02 xx xx xx xx 00 23
            55 00 0E 06 02 00 05 03 xx xx xx xx 00 23
            55 00 0E 06 02 00 05 04 xx xx xx xx 00 23
            55 00 0E 06 02 00 05 05 xx xx xx xx 00 23
    读取报警阈值	 55 00 0A 06 03 00 01 00 00 23
            55 00 0A 06 03 00 01 01 00 23
            55 00 0A 06 03 00 01 02 00 23
            55 00 0A 06 03 00 01 03 00 23
            55 00 0A 06 03 00 01 04 00 23
            55 00 0A 06 03 00 01 05 00 23
    检测启动/停止	 55 00 0B 06 04 00 02 00 00/01 00 23
            55 00 0B 06 04 00 02 01 00/01 00 23
            55 00 0B 06 04 00 02 02 00/01 00 23
            55 00 0B 06 04 00 02 03 00/01 00 23
            55 00 0B 06 04 00 02 04 00/01 00 23
            55 00 0B 06 04 00 02 05 00/01 00 23*/

    @SuppressLint("HandlerLeak")
    private static class MyHandler extends Handler {
        private WeakReference<MainActivity> mWeakReferenceActivity;

        public MyHandler(MainActivity activity) {
            mWeakReferenceActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (null != mWeakReferenceActivity) {
                MainActivity activity = mWeakReferenceActivity.get();
                switch (msg.what) {
                    case chartMessage:
                        String[] data = (String[]) msg.obj;
                        activity.setAirNucData(data);
                        break;
                    case TIMER_OUT:
                        activity.ShowMessage("检测超时");
                        try {
                            activity.openSpe.setText("开始检测");
                            activity.mHandler.removeCallbacks(activity.delayTest);
                            activity.mHandler.removeCallbacks(activity.timerTest);
                            activity.mHandler.removeCallbacksAndMessages(null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case uiToast:
                        String strSor1 = (String) msg.obj;
                        activity.ShowMessage(strSor1);
                        break;
                    case getWarnNum:  //读取阈值
                        int dataWarn = (int) msg.obj;
                        MyFunc.setIntSharedPreference(activity, activity.warnSP, dataWarn);
                        if (dataWarn != 0) {
                            activity.setWarnNumEt.setText(dataWarn + "");
                        }
                        break;
                    case setWarnNumResult:  //设置阈值响应
                        int setDataWarn = (int) msg.obj;
                        if (setDataWarn == 0) {
                            activity.ShowMessage("设置成功！");
                        } else {
                            activity.ShowMessage("设置失败！");
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //全屏去掉标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        initView();
        initData();
    }

    private void initView() {
        openSpe = findViewById(R.id.openSpe);
        setDevice = findViewById(R.id.setDevice);
        setDetail = findViewById(R.id.setDetail);
        cancle = findViewById(R.id.cancle);
        commit = findViewById(R.id.commit);
        ipET = findViewById(R.id.ipET);
        portET = findViewById(R.id.portET);
        settingLinL = findViewById(R.id.settingLinL);
        history = findViewById(R.id.history);
        air_chart = findViewById(R.id.air_chart);
        resultNumber = findViewById(R.id.resultNumber);
        resultName = findViewById(R.id.resultName);
        warnLl = findViewById(R.id.warnLl);
        setWarnNumEt = findViewById(R.id.setWarnNumEt);
        commitWarnBt = findViewById(R.id.commitWarnBt);
        checkWarnNumBT = findViewById(R.id.checkWarnNumBT);

        settingLinL.setVisibility(View.VISIBLE);
        setDetail.setVisibility(View.GONE);
        warnLl.setVisibility(View.GONE);
        openSpe.setOnClickListener(this);
        setDevice.setOnClickListener(this);
        cancle.setOnClickListener(this);
        commit.setOnClickListener(this);
        history.setOnClickListener(this);
        commitWarnBt.setOnClickListener(this);
        checkWarnNumBT.setOnClickListener(this);
        initBarChart(air_chart);
    }

    private void initData() {
        if (ds == null) {
            try {
                ds = new DatagramSocket(6677); //指明发送端的端口号
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        //申请权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
        }
        new ReceiveThread().start();
        mHandler = new MyHandler(MainActivity.this);

        mDBHelper = MyDataBaseHelper.getInstance(MainActivity.this);

    }

    private void sendUIData(int what, Object obj) throws Exception {
        mHandler.obtainMessage(what, obj).sendToTarget();
    }


    private void updateBarChart(float airVal) {
        //float airPpmVF = ((float) airVal / airPara) - 1;
        //double airPpmVD = new BigDecimal(airVal).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        BarData barData = air_chart.getBarData();
        barData.setBarWidth(0.85f); // set bar width
        BarDataSet dataSet = (BarDataSet) barData.getDataSetByIndex(0);
        if (dataSet == null) {
            dataSet = createSet();
            barData.addDataSet(dataSet);
        }
        int entryCount = dataSet.getEntryCount();
        BarEntry barEntry = new BarEntry(entryCount, (float) airVal);
        barData.addEntry(barEntry, 0);
        //
        air_chart.notifyDataSetChanged();
        air_chart.setVisibleXRangeMaximum(10);
        air_chart.moveViewToX(dataSet.getEntryCount() - 10);

    }

    private BarColorDataSet createSet() {
//        BarDataSet barDataSet = new BarDataSet(new ArrayList<BarEntry>(), "气体数据");
        BarColorDataSet barDataSet = new BarColorDataSet(new ArrayList<BarEntry>(), "气体数据");
        barDataSet.setColor(Color.BLUE);
        barDataSet.setFormLineWidth(1f);
        barDataSet.setFormSize(15.f);
        barDataSet.setDrawValues(false); // 不显示顶部值；
        return barDataSet;
    }

    private void setAirNucData(String[] air_nuc_data) {
        String Name = air_nuc_data[6];
        String data = air_nuc_data[1]; //0--uSv  1--mSv  2--Sv
        int number = 0;
        if (data != null) {
            number = Integer.parseInt(data);
        }
        if (!Name.equals("")) {
            resultName.setTextColor(Color.parseColor("#FF0000"));
            resultName.setText("气体报警");

            //存储历史数据
            CustomLibs cLib = new CustomLibs();
            cLib.setName("气体报警");
            //String format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis());
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String time = formatter.format(new Date());
            cLib.setSaveUrl(time);
            DBUtils.setCustomToDB(mDBHelper, cLib);

        } else {
            resultName.setTextColor(Color.parseColor("#FF669900"));
            resultName.setText("正常");
        }
        double n = number * 0.01;
        if (n <= 0) {
            n = 0.01;
        }
        double airPpmVD = new BigDecimal(n).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        resultNumber.setText(airPpmVD + "");
        updateBarChart((float) airPpmVD);

    }

    private void initBarChart(BarChart barChart) {
        // 图标设置；
//        barChart.setVisibleXRange(0,8);
        barChart.setBackgroundColor(Color.TRANSPARENT); // 背景为透明；
        barChart.setDrawGridBackground(false); // 不显示grid网格；
        barChart.setDrawBarShadow(false); // 背景阴影；
        barChart.setHighlightFullBarEnabled(false);
        barChart.setDrawBorders(false); // 不显示边框；
        barChart.animateX(1000, Easing.Linear); //动画效果；
        barChart.animateY(1000, Easing.Linear);
        // XY轴的设置；
        XAxis xAxis = barChart.getXAxis();
        xAxis.setEnabled(false);
//        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
////        xAxis.setAxisMinimum(0f);  //设置从0开始后显示的图会有些不完整；
//        xAxis.setTextColor(Color.WHITE);
//        xAxis.setGranularity(1f);
        xAxis.setDrawAxisLine(false); //不显示x轴线条；
        xAxis.setDrawGridLines(false); // 不显示X轴网格线；
        //
//        xAxis.setValueFormatter(new ValueFormatter() {
//            @Override
//            public String getFormattedValue(float value) {
//                return nucData.getExecptionTime();
//            }
//        });
        // 不设置得话会上移一些；
        YAxis axisLeft = barChart.getAxisLeft();
        axisLeft.setAxisMinimum(0f); // 从0开始 折线图相关； 打开注释柱状图会显示不全。
        axisLeft.setDrawAxisLine(false);
        axisLeft.setTextColor(Color.WHITE);
        axisLeft.enableGridDashedLine(10f, 10f, 0f); // Y轴网格线改为虚线；
        YAxis axisRight = barChart.getAxisRight();
//        axisRight.setAxisMinimum(0f);
        axisRight.setDrawAxisLine(false);
        axisRight.setEnabled(false); //不显示右侧Y
        // 修改Y轴的数据；
//        axisLeft.setValueFormatter(new ValueFormatter() {
//            @Override
//            public String getFormattedValue(float value) {
//                return (int)value+"uSv/h";
//            }
//        });

        // 标签设置；
        Legend legend = barChart.getLegend();
        legend.setEnabled(false);
        legend.setForm(Legend.LegendForm.LINE);
        legend.setTextSize(11f);
        legend.setTextColor(Color.BLUE);
        // 显示位置；
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        // 是否绘制在图表里面
        legend.setDrawInside(false);
        // 去掉右下角得描述内容；
        Description description = new Description();
        description.setEnabled(false);
        barChart.setDescription(description);
        //
        barChart.setDoubleTapToZoomEnabled(false);
        //禁止拖拽
//        barChart.setDragEnabled(false);
        //X轴或Y轴禁止缩放
//        barChart.setScaleXEnabled(false);
//        barChart.setScaleYEnabled(false);
//        barChart.setScaleEnabled(false);
        //禁止所有事件
        barChart.setTouchEnabled(false);

        //
        BarData barData = new BarData();
//        BarDataSet dataSet = (BarDataSet) barData.getDataSetByIndex(0);
        BarColorDataSet dataSet = (BarColorDataSet) barData.getDataSetByIndex(0);
        if (dataSet == null) {
            dataSet = createSet();
        }
        dataSet.setColors(new int[]{Color.GREEN, Color.YELLOW, Color.RED});
        for (int i = 0; i < 10; i++) {
            BarEntry barEntry = new BarEntry(i, 0);
            dataSet.addEntry(barEntry);
        }
        barData.addDataSet(dataSet);

        barData.setValueTextColor(Color.WHITE);
        barChart.setData(barData);
    }

    class BarColorDataSet extends BarDataSet {

        public BarColorDataSet(List<BarEntry> yVals, String label) {
            super(yVals, label);
        }


        @Override
        public int getColor(int index) {
//            return super.getColor(index);
            float y = getEntryForIndex(index).getY();
            if (y > 0 && y <= airMinV) {
                return mColors.get(0);
            } else if (y > airMinV && y < airMaxV) {
                return mColors.get(1);
            } else {
                return mColors.get(2);
            }
        }
    }


    private void sendUdp(final String testCode) {
        new Thread() {
            public void run() {
                try {
                    //byte[] buf = testCode.getBytes();
                    byte[] buf = MyFunc.HexToByteArr(testCode);
                    //UDP是无连接的，所以要在发送的数据包裹中指定要发送到的ip：port
                    String userIpStr = MyFunc.getStringSharedPreference(MainActivity.this, ipSP);
                    String userPortStr = MyFunc.getStringSharedPreference(MainActivity.this, portSP);
                    if (!userIpStr.equals("") && !userPortStr.equals("")) {
                        dp = new DatagramPacket(buf, buf.length, new InetSocketAddress(userIpStr, Integer.parseInt(userPortStr)));
                        ds.send(dp);
                        //ds.close();
                    } else {
                        sendUIData(uiToast, "设备参数配置错误!");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ds != null) {
            ds.close();
        }
        mHandler.removeCallbacks(delayTest);
        mHandler.removeCallbacks(timerTest);
        isopen = false;
        mHandler.removeCallbacksAndMessages(null);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.openSpe:
                if (openSpe.getText().equals("开始检测")) {
                    sendUdp(testCode2);
                    openSpe.setText("停止检测");  //停止检测
                    timerFlag = 0;
                    sendUdp(testCodeOpen1);
                    mHandler.postDelayed(delayTest, 0);
                    mHandler.postDelayed(timerTest, 30000);//延时30秒
                } else if (openSpe.getText().equals("停止检测")) {
                    openSpe.setText("开始检测");
                    for (int i=0;i<isOpenLEDNumber.length;i++){
                        if(isOpenLEDNumber[i]) {
                            sendUdp(testCodeClose[i]);  //停止检测
                        }
                    }
                    mHandler.removeCallbacks(delayTest);
                    mHandler.removeCallbacks(timerTest);
                    mHandler.removeCallbacksAndMessages(null);
                }

                break;
            case R.id.setDevice:
                String userIpStr = MyFunc.getStringSharedPreference(MainActivity.this, ipSP);
                String userPortStr = MyFunc.getStringSharedPreference(MainActivity.this, portSP);
                if (userIpStr != null && userPortStr != null) {
                    ipET.setText(userIpStr);
                    portET.setText(userPortStr);
                }
                setDetail.setVisibility(View.VISIBLE);
                warnLl.setVisibility(View.GONE);
                break;
            case R.id.cancle:
                setDetail.setVisibility(View.GONE);
                break;
            case R.id.commit:
                setDetail.setVisibility(View.GONE);
                ipStr = ipET.getText().toString();
                portStr = portET.getText().toString();
                if (!ipStr.equals("") && !portStr.equals("")) {
                    MyFunc.setStringSharedPreference(MainActivity.this, ipSP, ipStr);
                    MyFunc.setStringSharedPreference(MainActivity.this, portSP, portStr);
                    ShowMessage("设置成功!");
                } else {
                    ShowMessage("请正确输入!");
                }
                break;
            case R.id.history:
                startActivity(new Intent(MainActivity.this, historyActivity.class));
                break;
            case R.id.commitWarnBt:
                String warnNumStr = setWarnNumEt.getText().toString();
                if (!warnNumStr.equals("")) {
                    int warnNumInt = Integer.parseInt(warnNumStr);
                    if (warnNumInt != 0) {
                        byte[] warnNumByte = MyFunc.intToBytesBig(warnNumInt);
                        String textResultdd = MyFunc.bytesToHexString(warnNumByte);
                        String sendWarnNumStr = testCode4 + textResultdd + "" + "0023";
                        sendUdp(sendWarnNumStr);  //设置阈值
                    }
                    warnLl.setVisibility(View.GONE);
                } else {
                    ShowMessage("请正确输入！");
                }
                break;
            case R.id.checkWarnNumBT:
                setDetail.setVisibility(View.GONE);
                warnLl.setVisibility(View.VISIBLE);
                sendUdp(testCode5);  //读取阈值
                /*int warnNumber= MyFunc.getIntSharedPreference(MainActivity.this,warnSP);
                if(warnNumber!=0) {
                    setWarnNumEt.setText(warnNumber+"");
                }
                else {
                    setWarnNumEt.setText("");
                }*/
                break;
            default:
                break;
        }
    }

    Runnable timerTest = new Runnable() {
        @Override
        public void run() {
            if (timerFlag == 0) {
                try {
                    sendUIData(TIMER_OUT, "");
                } catch (Exception e) {
                    ShowMessage(e.getMessage());
                }
            }
        }
    };

    Runnable delayTest = new Runnable() {
        @Override
        public void run() {
            sendUdp(testCode1);
            mHandler.postDelayed(this, 1000);
        }
    };

    private void ShowMessage(String sMsg) {
        Toast.makeText(MainActivity.this, sMsg, Toast.LENGTH_SHORT).show();
    }

    public class ReceiveThread extends Thread {

        @Override
        public void run() {
            while (isopen) {//循环接收，isAlive() 判断防止无法预知的错误
                try {
                    //Thread.sleep(100);//显示性能高的话，可以把此数值调小。
                    recList.clear();
                    byte buffer[] = new byte[1024];
                    packet = new DatagramPacket(buffer, buffer.length);
                    ds.receive(packet); //阻塞式，接收发送方的 packet
                    //String dd=MyFunc.bytesToHexString(buffer);
                    //String text=new String(packet.getData(),0,packet.getLength(),"GBK");
                    if (packet.getLength() > 0) {
                        ComBean ComRecData = new ComBean(packet.getData(), packet.getLength());
                        blockingDeque.add(ComRecData);
                    }
                    timerFlag = 1;
                    mHandler.removeCallbacksAndMessages(timerTest);
                    final ComBean comData;
                    while ((comData = blockingDeque.poll()) != null) {
                        DispRecData(comData);
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("1111Catch",e.getMessage());
                    break; //当 catch 到错误时，跳出循环
                }
            }
        }
    }

    private void DispRecData(ComBean ComRecData) {
        try {

            /*[2021-03-31 14:40:09.731]# SEND HEX TO 192.168.0.7 :7788>
                    55 00 0A 06 01 00 01 00 00 23
                    [2021-03-31 14:40:09.742]# RECV HEX FROM 192.168.0.7 :7788>
                    55 00 25 06 81 00 1C 3F 00 00 01 00 00 00 05 00 00 00 00 00 00 00 05 00 00 00 00 00 00 00 00 00 00 00 00 73 23*/

            byte[] data2Byte = new byte[4];
            String[] warnDataResult = new String[7];  //第一位报警地址   后六位数据
            int tt[] = new int[6];
            StringBuilder sMsg = new StringBuilder();
            sMsg.append(MyFunc.ByteArrToHex(ComRecData.bRec));
            sreiaData += sMsg;
            byte[] getData = MyFunc.hexStringToBytes(sreiaData);

            for (int w = 0; w < getData.length; w++) {
                if (w == getData.length - 1) {
                    recList.add(getData[w]);
                    break;
                }
                if ((getData[w] & 0xff) != 0xFF) {
                    recList.add(getData[w]);
                } else {
                    if ((getData[w + 1] & 0xff) == 0xFF) {
                        recList.add(MyFunc.HexToByte("FF"));
                        w++;
                    } else if ((getData[w + 1] & 0xff) == 0x00) {
                        recList.add(MyFunc.HexToByte("55"));
                        w++;
                    } else {
                        recList.add(getData[w]);
                    }
                }
            }
            //解码后byte[]
            dealAfterRec = new byte[recList.size()];
            for (int e = 0; e < recList.size(); e++) {
                dealAfterRec[e] = recList.get(e);
            }

            StringBuilder warnID = new StringBuilder();
            String textResultdd = MyFunc.bytesToHexString(dealAfterRec);
            sreiaData = "";
            if (dealAfterRec.length == 37) {
                if ((dealAfterRec[9] & 0x01) != 0x00) {
                    warnID.append("0");
                } if ((dealAfterRec[9] & 0x02) != 0x00) {
                    warnID.append("1");
                }  if ((dealAfterRec[9] & 0x04) != 0x00) {
                    warnID.append("2");
                } if ((dealAfterRec[9] & 0x08) != 0x00) {
                    warnID.append("3");
                }  if ((dealAfterRec[9] & 0x10) != 0x00) {
                    warnID.append("4");
                } if ((dealAfterRec[9] & 0x20) != 0x00) {
                    warnID.append("5");
                }
                warnDataResult[6] = "";
                warnDataResult[6] += warnID;
                data2Byte[0] = dealAfterRec[14];
                data2Byte[1] = dealAfterRec[13];
                data2Byte[2] = dealAfterRec[12];
                data2Byte[3] = dealAfterRec[11];
                tt[0] = MyFunc.bytesToIntLittle(data2Byte, 0);
                //传感器1数据
                data2Byte[0] = dealAfterRec[18];
                data2Byte[1] = dealAfterRec[17];
                data2Byte[2] = dealAfterRec[16];
                data2Byte[3] = dealAfterRec[15];
                tt[1] = MyFunc.bytesToIntLittle(data2Byte, 0);
                data2Byte[0] = dealAfterRec[22];
                data2Byte[1] = dealAfterRec[21];
                data2Byte[2] = dealAfterRec[20];
                data2Byte[3] = dealAfterRec[19];
                tt[2] = MyFunc.bytesToIntLittle(data2Byte, 0);
                data2Byte[0] = dealAfterRec[26];
                data2Byte[1] = dealAfterRec[25];
                data2Byte[2] = dealAfterRec[24];
                data2Byte[3] = dealAfterRec[23];
                tt[3] = MyFunc.bytesToIntLittle(data2Byte, 0);
                data2Byte[0] = dealAfterRec[30];
                data2Byte[1] = dealAfterRec[29];
                data2Byte[2] = dealAfterRec[28];
                data2Byte[3] = dealAfterRec[27];
                tt[4] = MyFunc.bytesToIntLittle(data2Byte, 0);
                data2Byte[0] = dealAfterRec[34];
                data2Byte[1] = dealAfterRec[33];
                data2Byte[2] = dealAfterRec[32];
                data2Byte[3] = dealAfterRec[31];
                tt[5] = MyFunc.bytesToIntLittle(data2Byte, 0);
                for (int i = 0; i < 6; i++) {
                    if(tt[i]!=0) {
                        isOpenLEDNumber[i] = true;
                    }else {
                        isOpenLEDNumber[i] = false;
                    }
                    warnDataResult[i] = String.valueOf(tt[i]);
                }
                sendUIData(chartMessage, warnDataResult);
                Log.e("1111", "传感器报警: " + warnDataResult[6]+"1号数据"+warnDataResult[1]+"  接收数据 "+textResultdd);
            } else if (dealAfterRec.length == 14) {
                if ((dealAfterRec[6] & 0xff) == 0x05) {
                    //读取传感器2阈值
                    data2Byte[0] = dealAfterRec[11];
                    data2Byte[1] = dealAfterRec[10];
                    data2Byte[2] = dealAfterRec[9];
                    data2Byte[3] = dealAfterRec[8];
                    int recyclewarnNum = MyFunc.bytesToIntLittle(data2Byte, 0);
                    sendUIData(getWarnNum, recyclewarnNum);
                }
            } else if (dealAfterRec.length == 10) {
                if ((dealAfterRec[6] & 0xff) == 0x01) {
                    data2Byte[0] = dealAfterRec[7];
                    data2Byte[1] = 0;
                    data2Byte[2] = 0;
                    data2Byte[3] = 0;
                    int recyclewarnNum = MyFunc.bytesToIntLittle(data2Byte, 0);
                    sendUIData(setWarnNumResult, recyclewarnNum);
                }
            } else {
                Log.e("1111", "数据包长度错误:  " + textResultdd);
            }
        } catch (
                Exception e) {
            e.printStackTrace();
        }
    }

}