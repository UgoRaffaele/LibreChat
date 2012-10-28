package com.ugopiemontese.librechat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;

public class ChatActivity extends Activity {

	ScrollView Chat;	// ScrollView contenente il TableLayout
	TableLayout ChatList;	// TableLayout contenente i messaggi
	EditText editMessage;	// EditText per la scrittura dei messaggi
	Button Send;	// Bottone di invio del messaggio
	
	MulticastSocket s;	// Multicast socket per inviare e ricevere messaggi 
	int port = 8081;	// Numero della porta utilizzata dalla chat
	String group = "225.4.5.6";	// indirizzo ip del gruppo chat
	Thread t;	// Thread per multicast
	
	Handler p = new Handler();	// handler usato per comunicare tra activity e thread
	String targetIp, selfIp;	// targetIp : indirizzo ip a cui inviare il messaggio
	int targetPort;	// porta di destinazione
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        
        // Associazione delle variabili all'ID dei Widget //
        Chat = (ScrollView) findViewById(R.id.Chat);
        ChatList = (TableLayout) findViewById(R.id.ChatList);
        editMessage = (EditText)findViewById(R.id.modificaMessaggio);
        Send = (Button)findViewById(R.id.bottoneInvia);
        // Associazione terminata //
        
        ConnectivityManager conMan = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        State wifi = conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
        
        if (wifi != State.CONNECTED && wifi != State.CONNECTING) {
        	AlertDialog.Builder checkWiFi = new AlertDialog.Builder(this);
        	checkWiFi.setTitle("Attenzione!");
        	checkWiFi.setMessage("Devi essere connesso ad una rete WiFi per poter usare LibreChat.");
        	checkWiFi.setCancelable(false);
        	
        	checkWiFi.setPositiveButton("Gestore WiFi", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int id){
                	startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    }
            });
        	
        	checkWiFi.setNegativeButton("Chiudi", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int id){
                	dialog.dismiss();
                	finish();
                    }
            });
        	
        	checkWiFi.show();
        }
                
        t = new Thread(new MulticastRecieve());
        t.start();
        
        Send.setOnClickListener (new OnClickListener() {	// Azione da compiere al click del bottone Send
        	@Override
        	public void onClick(View arg0) {
        		new InviaMessaggio().execute();	// Il messaggio viene inviato a tutti gli utenti della Chat
        	} 	
        }); 
        
    }

    // GESTORE ACTIONBAR //
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_chat, menu);
        return true;
    }
    
    // GESTORE MENU IN ACTIONBAR //
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
        	case R.id.menu_preferenze:
        		Intent intent = new Intent(ChatActivity.this, PreferenzeActivity.class);
        		startActivity(intent);
        		return true;
    	}
    	return super.onOptionsItemSelected(item);
    }

 	// GESTORE DEI PACCHETTI MULTICAST
 	public class MulticastRecieve implements Runnable{

 		@Override
 		public void run() {
 			// TODO Auto-generated method stub
 			
 			// Acquisizione del lock per la ricezione dei messaggi
 			WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
 			MulticastLock mm = wifiManager.createMulticastLock("iTach discovery lock");
 			mm.setReferenceCounted(true);
 			mm.acquire();
 				
 			while (true) {
 				try {
 					s = new MulticastSocket(port);
 					s.joinGroup(InetAddress.getByName(group));
 					byte buf[] = new byte[1024];
 					final DatagramPacket pack = new DatagramPacket(buf, buf.length);
 					s.receive(pack);
 					String s = new String(buf);
 					s = s.trim();
 										
 					final String info = s;
 					
 					p.post(new Runnable(){      	
 							@Override
 							public void run() {
 								// TODO Auto-generated method stub
 								SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ChatActivity.this); // Preferenze
 								String message = info;
 								
 								// CHECK "CIFRATURA" //
 			 					Boolean Crypto = prefs.getBoolean("crypto", false);
 			 					if (Crypto == true) {
 									try {
 										message = decriptazione(info);
 									} catch (Exception e) {
 										// TODO Auto-generated catch block
 										e.printStackTrace();
 									}
 								}
 								
 								TextView messaggio = new TextView(getApplicationContext());
 								messaggio.setText(message);
 								ChatList.addView(messaggio);
 								Chat.fullScroll(ScrollView.FOCUS_DOWN);
 								
 								String Notifiche = prefs.getString("notifiche", "0");
 								int tipoNotifica = Integer.parseInt(Notifiche);
									
 								switch (tipoNotifica) {
 									case 1:
 										// produci vibrazione per 100 millisecondi
 										Vibrator vibrazione = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE); 
 			           					vibrazione.vibrate(100);
 										break;
 									case 2:
 										// riproduci avviso sonoro
 										Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
 										Ringtone alert = RingtoneManager.getRingtone(getApplicationContext(), notification);
 										alert.play();
 										break;
 									default:
 										// fai niente!
 										Log.d("Notifica", "Nulla di nulla!");
 										break;
 								}
 								 								
 							}
 						});
 		            
 				} catch (IOException e) {
 					// TODO Auto-generated catch block
 					e.printStackTrace();
 				}
 				 
 			} 
 			
 		}			
 		
 	}		
 	
 	public class InviaMessaggio extends AsyncTask<Object, Void, String> {
 		
 		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ChatActivity.this); // Preferenze
 		
		@Override
		protected String doInBackground(Object... arg0) {
			// TODO Auto-generated method stub
			String result = null;
			String Username = prefs.getString("username", "Utente001");
			
			String msg = editMessage.getText().toString();
			msg = Username.toString() + " : " + msg;
    		
    		// CHECK "CIFRATURA" //
    		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ChatActivity.this); // Preferenze
    		Boolean Crypto = prefs.getBoolean("crypto", false);
    		if (Crypto == true) {
				try {
					msg = criptazione(msg);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			byte buf[] = msg.getBytes();
    		DatagramPacket pack;

    		// Indirizzo a cui inviare
    		String group = "225.4.5.6";
    		int ttl = 1;
    		MulticastSocket ms;
    		try {
    			ms = new MulticastSocket();

    			// Crea un DatagramPacket
    			pack = new DatagramPacket(buf, buf.length, InetAddress.getByName(group), port);
    			ms.send(pack);
    			ms.setTimeToLive(ttl);

    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
			
			return result;
		}

		protected void onPostExecute(String result) {
			// Svuota il campo di inserimento del messaggio
	 		editMessage.setText("");
	 	}
		
 	}
 	
 	private String criptazione (String messaggio) throws Exception {
 		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ChatActivity.this); // Preferenze
 		String CryptoKey = prefs.getString("cryptokey", "prova");
 		
 		StringBuffer sb = new StringBuffer (messaggio);

 	    int lenStr = messaggio.length();
 	    int lenKey = CryptoKey.length();
 		
 		for ( int i = 0, j = 0; i < lenStr; i++, j++ ) {
 	         if ( j >= lenKey ) j = 0; 
 	         sb.setCharAt(i, (char)(messaggio.charAt(i) ^ CryptoKey.charAt(j))); 
 	    }
 		
 		messaggio = Base64.encodeToString(sb.toString().getBytes(), Base64.DEFAULT);
 		return messaggio;
 	}
 	
 	private String decriptazione (String messaggio) throws Exception {
		String decoded = new String(Base64.decode(messaggio, Base64.DEFAULT));
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ChatActivity.this); // Preferenze
 		String CryptoKey = prefs.getString("cryptokey", "prova");
 		
 		StringBuffer bs = new StringBuffer (decoded);

 	    int lenStr = decoded.length();
 	    int lenKey = CryptoKey.length();
 		
 		for ( int i = 0, j = 0; i < lenStr; i++, j++ ) {
 	         if ( j >= lenKey ) j = 0; 
 	         bs.setCharAt(i, (char)(decoded.charAt(i) ^ CryptoKey.charAt(j))); 
 	    }
 		
 		decoded = bs.toString();		
 		return decoded;
 	}
    
}
