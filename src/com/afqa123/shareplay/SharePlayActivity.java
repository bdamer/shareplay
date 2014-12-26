package com.afqa123.shareplay;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.afqa123.log.Logger;
import com.afqa123.log.LoggerFactory;
import com.afqa123.shareplay.common.DBHelper;
import com.afqa123.shareplay.impl.Server;
import com.afqa123.shareplay.impl.ServerProvider;
import com.afqa123.shareplay.interfaces.IServerProvider;

public class SharePlayActivity extends Activity {
	
	private static final Logger logger = LoggerFactory.getLogger(SharePlayActivity.class);
	
	private IServerProvider provider;
	private ListView listServers;
	private ListAdapter listAdapter;
	private int selectedRow;
	private DBHelper db;
	
	private class ListAdapter extends BaseAdapter {
		
		private LayoutInflater inflater;
		private List<Server> servers;
		
		public ListAdapter(Context context) {
			updateData();
			inflater = LayoutInflater.from(context);
		}
		
		public void updateData() {
			servers = provider.getServers();
		}
		
		@Override
		public int getCount() {	
			return servers.size();
		}

		@Override
		public Object getItem(int position) {
			return servers.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			convertView = inflater.inflate(R.layout.row_server, null);
			Server server = servers.get(position);
			
			final TextView textName = (TextView)convertView.findViewById(R.id.text_name);
			textName.setText(server.getName());
			if (server.isOnline()) {
				textName.setTypeface(Typeface.DEFAULT_BOLD);
			}
			
			final TextView textInfo = (TextView)convertView.findViewById(R.id.text_info);
			textInfo.setText(server.getLastDiscovered().toLocaleString());
	        return convertView;
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		logger.info("Creating activity.");
		setContentView(R.layout.activity_servers);
		
		db = new DBHelper(this);
        provider = new ServerProvider(this, db, new Handler() {
    		@Override
    		public void handleMessage(Message msg) {
    			super.handleMessage(msg);
    			listAdapter.updateData();
    			listAdapter.notifyDataSetChanged();
    		}
    	});
        
        listAdapter = new ListAdapter(this);
        
        listServers = (ListView)findViewById(R.id.list_servers);
        listServers.setAdapter(listAdapter);
        listServers.setTextFilterEnabled(true);
		listServers.setOnItemClickListener(new OnItemClickListener() {
			/**
			 * Starts selection activity for a given server.
			 */
			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int pos, long id) {
				Server server = (Server)listAdapter.getItem(pos);
				if (server.getId() == null) {
					provider.addServer(server);
				}
				
				final Intent i = new Intent(SharePlayActivity.this, SelectionActivity.class);
				i.putExtra(SelectionActivity.PARAM_SERVER_ID, server.getId());
				startActivity(i);
			}
		});
		
		listServers.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> adapter, View view, int pos, long id) {
				selectedRow = pos;
				showDialog(DialogFactory.DIALOG_EDIT_SERVER);
				return true;
			}
		});

		ImageButton imgAddServer = (ImageButton)this.findViewById(R.id.image_add_server);
		imgAddServer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(DialogFactory.DIALOG_ADD_SERVER);
			}
		});
		ImageButton imgClearServers = (ImageButton)this.findViewById(R.id.image_clear_servers);
		imgClearServers.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(DialogFactory.DIALOG_CONFIRM_CLEAR);
			}
		});
		ImageButton imgInfo = (ImageButton)this.findViewById(R.id.image_info);
		imgInfo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(DialogFactory.DIALOG_INFO);
			}
		});
		provider.beginScan();
	}
		
	@Override
	protected void onDestroy() {
		logger.info("Destroying activity.");
		provider.endScan();
		db.close();
		super.onDestroy();
	}
		
	@Override
	protected Dialog onCreateDialog(int id) {
		final AlertDialog.Builder ab = new AlertDialog.Builder(SharePlayActivity.this);
		final Dialog dialog;
		
		switch (id) {
		case DialogFactory.DIALOG_ADD_SERVER:
			dialog = DialogFactory.createAddServerDialog(SharePlayActivity.this, provider);			
			break;
			
		case DialogFactory.DIALOG_EDIT_SERVER:
			dialog = DialogFactory.createEditServerDialog(SharePlayActivity.this, provider, (Server)listAdapter.getItem(selectedRow));
			break;
			
		case DialogFactory.DIALOG_EDIT_PASSWORD:
			dialog = DialogFactory.createdEditPasswordDialog(SharePlayActivity.this, provider, (Server)listAdapter.getItem(selectedRow));
			
			break;
			
		case DialogFactory.DIALOG_CONFIRM_CLEAR:
			ab.setMessage(R.string.message_confirm_clear)
				.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						provider.deleteAll();
						listAdapter.updateData();
					}
				})
				.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// do nothing
					}
				});
			dialog = ab.create();
			break;
			
		case DialogFactory.DIALOG_INFO:
			dialog = DialogFactory.createInfoDialog(SharePlayActivity.this);
			break;
			
		default:
			dialog = super.onCreateDialog(id);
			break;
		}
		
		return dialog;
	}	
}