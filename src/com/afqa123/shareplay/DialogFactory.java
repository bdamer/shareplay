package com.afqa123.shareplay;

import java.net.InetAddress;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afqa123.log.Logger;
import com.afqa123.log.LoggerFactory;
import com.afqa123.shareplay.common.Constants;
import com.afqa123.shareplay.impl.Client;
import com.afqa123.shareplay.impl.Server;
import com.afqa123.shareplay.interfaces.IServerProvider;

public class DialogFactory {

	private static final Logger logger = LoggerFactory.getLogger(DialogFactory.class);
	
	public static final int DIALOG_ADD_SERVER = 1;
	public static final int DIALOG_EDIT_SERVER = 2;
	public static final int DIALOG_CONFIRM_CLEAR = 3;
	public static final int DIALOG_EDIT_PASSWORD = 4;
	public static final int DIALOG_INFO = 5;
	public static final int DIALOG_ERROR = 6;
	public static final int DIALOG_PASSWORD = 7;
	public static final int DIALOG_ITEM_ACTIONS = 8;
	public static final int DIALOG_ERROR_FEEDBACK = 9;
	public static final int DIALOG_ERROR_ITUNES = 10;
	public static final int DIALOG_ERROR_CONNECTION = 11;
	
	public static Dialog createAddServerDialog(final Activity anActivity, final IServerProvider aProvider) {
		final Dialog dialog = new Dialog(anActivity);
		dialog.setContentView(R.layout.dialog_add_server);
		dialog.setTitle(R.string.dialog_add_server);			
		dialog.setCancelable(true);

		final TextView textName = (TextView)dialog.findViewById(R.id.edit_name);
		final TextView textAddress = (TextView)dialog.findViewById(R.id.edit_address);
		final TextView textPort = (TextView)dialog.findViewById(R.id.edit_port);
		
		dialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				textName.setText("");
				textAddress.setText("");
				textPort.setText("");
			}
		});
		
		dialog.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				textName.setText("");
				textAddress.setText("");
				textPort.setText("");
			}
		});
		
		Button buttonOk = (Button)dialog.findViewById(R.id.button_ok);
		buttonOk.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View button) {
				try {
					String name = textName.getText().toString().trim();
					if (name.length() == 0) {
						throw new Exception("Invalid name.");
					}

					String address = textAddress.getText().toString().trim();
					if (address.length() == 0) {
						throw new Exception("Invalid address.");
					}

					int port = Client.DEFAULT_PORT;
					String portVal = textPort.getText().toString().trim();
					if (portVal.length() > 0) {
						port = Integer.parseInt(portVal);
					}
					
					// This method does not support ipv6!
					InetAddress ia = InetAddress.getByName(address);
				
					Server server = new Server();
					server.setName(name);
					server.setHost(ia.getHostName());
					server.setAddress(ia.getHostAddress());
					server.setPort(port);
					server.setLastDiscovered(new Date());
					aProvider.addServer(server);
					dialog.dismiss();

				} catch (Exception ex) {
					logger.error("Error adding host.", ex);
					Toast msg = Toast.makeText(anActivity, anActivity.getString(R.string.message_error_server), Toast.LENGTH_SHORT);
					msg.setGravity(Gravity.CENTER, msg.getXOffset() / 2, msg.getYOffset() / 2);
					msg.show();
				}
			}
		});			
		return dialog;
	}

	public static Dialog createEditServerDialog(final Activity anActivity, final IServerProvider aProvider, final Server selectedServer) {
		final AlertDialog.Builder ab = new AlertDialog.Builder(anActivity);
		final String options[] = { anActivity.getString(R.string.label_edit_password), 
				anActivity.getString(R.string.label_remove )};
		ab.setTitle(R.string.dialog_edit_server);
		ab.setItems(options, new DialogInterface.OnClickListener() {
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
			case 0:
				dialog.dismiss();
				anActivity.showDialog(DIALOG_EDIT_PASSWORD);
				break;
			case 1:
				aProvider.deleteServer(selectedServer);
				dialog.dismiss();
				break;				
			default:
				break;
			}
		}
		});
		return ab.create();
	}
	
	public static Dialog createdEditPasswordDialog(final Activity anActivity, final IServerProvider aProvider, final Server selectedServer) {
		final AlertDialog.Builder ab = new AlertDialog.Builder(anActivity);

		final EditText input = new EditText(anActivity);
		input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		
		return 
			ab.setTitle(R.string.dialog_edit_password)
			.setMessage(R.string.label_enter_password)
			.setView(input)
			.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					input.setText("");
				}
			})
			.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					selectedServer.setPassword(input.getText().toString());
					if (selectedServer.getId() == null) {
						aProvider.addServer(selectedServer);
					} else {
						aProvider.updateServer(selectedServer);
					}
				}
			})
			.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					input.setText("");
				}
			})
			.create();
	}
	
	public static Dialog createInfoDialog(final Activity anActivity) {
		String version;
		try {
			version = anActivity.getPackageManager().getPackageInfo(anActivity.getPackageName(), 0).versionName;
		} catch (PackageManager.NameNotFoundException ex) {
			version = "";
		}
		
		final Dialog dialog = new Dialog(anActivity);
		dialog.setContentView(R.layout.dialog_info);
		dialog.setTitle(R.string.dialog_info);			
		dialog.setCancelable(true);
		
		TextView textMessage = (TextView)dialog.findViewById(R.id.text_message);
		textMessage.setText(String.format(anActivity.getString(R.string.message_info), version));
		
		final SharedPreferences prefs = anActivity.getSharedPreferences(Constants.PREFERENCES, Activity.MODE_PRIVATE);
		final CheckBox checkFeedback = (CheckBox)dialog.findViewById(R.id.check_feedback);
		checkFeedback.setChecked(prefs.getBoolean(Constants.PREFERENCE_FEEDBACK, true));
		final CheckBox checkProxy = (CheckBox)dialog.findViewById(R.id.check_proxy);
		checkFeedback.setChecked(prefs.getBoolean(Constants.PREFERENCE_FEEDBACK, false));
		
		Button btnOk = (Button)dialog.findViewById(R.id.button_ok);
		btnOk.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SharedPreferences.Editor editor = prefs.edit();
				editor.putBoolean(Constants.PREFERENCE_FEEDBACK, checkFeedback.isChecked());
				editor.putBoolean(Constants.PREFERENCE_USE_PROXY, checkProxy.isChecked());
				editor.commit();
				dialog.dismiss();
			}
	    });
		
		return dialog;
	}
	
}
