package com.hipits.fishingalarm.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import com.hipits.fishingalarm.BluetoothChatService;
import com.hipits.fishingalarm.R;

public class MainActivity extends Activity {

	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	private static final int REQUEST_ENABLE_BT = 2;

	private String mConnectedDeviceName = null;

	private BluetoothAdapter bluetoothAdapter;
	private BluetoothChatService service;

	private EditText messageEditText;
	private List<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();

	private String[] bluetoothNames;

	@Override
	protected void onStart() {
		super.onStart();

		if (!bluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		} else {
			if (service == null) {
				setup();
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (service != null) {
			if (service.getState() == BluetoothChatService.STATE_NONE) {
				service.start();
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initScan();

	}

	public void onClick(View view) {
		if (view.getId() == R.id.scanButton) {

			if (bluetoothAdapter.isDiscovering()) {
				bluetoothAdapter.cancelDiscovery();
			}
			bluetoothAdapter.startDiscovery();
		}
	}

	public void initScan() {

		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		isSupport();

		this.registerReceiver(mReceiver, new IntentFilter(
				BluetoothDevice.ACTION_FOUND));
		this.registerReceiver(mReceiver, new IntentFilter(
				BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

	}

	public void showBlueDialog() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("BlueTooth List");

		builder.setItems(bluetoothNames, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int position) {
				BluetoothDevice device = bluetoothAdapter
						.getRemoteDevice(devices.get(position).getAddress());
				service.connect(device);
			}
		});

		builder.setCancelable(true);
		builder.create().show();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (service != null) {
			service.stop();
		}
	}

	public void isSupport() {

		if (bluetoothAdapter == null) {
			Toast.makeText(this, "블루투스를 이용할수 없습니다.", Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	public void setup() {

		messageEditText = (EditText) findViewById(R.id.messageEditText);

		findViewById(R.id.sendButton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				String message = messageEditText.getText().toString();
				sendMessage(message);
			}
		});

		service = new BluetoothChatService(getApplicationContext(), handler);
	}

	public void sendMessage(String message) {
		if (service.getState() != BluetoothChatService.STATE_CONNECTED) {
			Toast.makeText(this, "접속이되지 않았습니다", Toast.LENGTH_SHORT).show();
			return;
		}

		if (message.length() > 0) {
			byte[] send = message.getBytes();
			service.write(send);
			messageEditText.setText("");
		}
	}

	public Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			
			switch (msg.what) {
			
			case MESSAGE_STATE_CHANGE:
				Log.e("MESSAGE_CHANGE", "" + msg.arg1);
				break;
			case MESSAGE_WRITE:
				String message = new String((byte[]) msg.obj);
				Log.e("WRITE", message);

				break;
			case MESSAGE_READ:
				byte[] readBytes = (byte[]) msg.obj;
				String readMessage = new String(readBytes, 0, msg.arg1);

				Toast.makeText(MainActivity.this, readMessage,
						Toast.LENGTH_SHORT).show();
				break;
				
			case MESSAGE_DEVICE_NAME:
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(MainActivity.this,
						mConnectedDeviceName + "연결 성공", Toast.LENGTH_SHORT)
						.show();
				break;
				
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent

				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {

					Toast.makeText(getApplicationContext(),
							device.getName() + device.getAddress(),
							Toast.LENGTH_SHORT).show();
					devices.add(device);
				}

				// When discovery is finished, change the Activity title
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
					.equals(action)) {
				bluetoothNames = new String[devices.size()];

				for (int i = 0; i < devices.size(); i++) {
					bluetoothNames[i] = devices.get(i).getName();
				}

				if (devices.size() == 0) {
					Toast.makeText(MainActivity.this, "No devices found",
							Toast.LENGTH_SHORT).show();
				} else {
					showBlueDialog();
				}
			}
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
}
