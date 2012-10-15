/*
 * Copyright (C) 2012 SFR API - Hervé Hoareau

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License along
with this program; if not, write to the Free Software Foundation, Inc.,
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */


package demo.depot.message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.apache.http.client.ClientProtocolException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.Settings.Secure;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.text.ClipboardManager;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;


/**
 * Classe de la fenêtre principale contenant l'ensemble de l'interface
 * de l'application Plutar
 * 
 * @author Hervé Hoareau
 *
 */
public class PlutarActivity extends FragmentActivity {  
	
	//La constante MESSAGE_SERVER doit contenir l'adresse du serveur
	//par exemple, pour google app engine : http://nom_appli_GAE.appspot.com
	private static final String MESSAGE_SERVER = "http://plutarserver.appspot.com/api";
	
	
	//Utilisé pour gérer la liste des numéros non compatibles avec le dépot de messages
	SharedPreferences settings = null;
	
	Button cmdSend=null;
	ImageButton cmdRaz=null;
	Button cmdQuit=null;
	Button cmdDate=null;
	Button cmdTime=null;
	
	ImageButton cmdRecord=null;
	ImageButton cmdListen=null;
	ImageButton cmdClear=null;
	ImageButton cmdSelFile=null;
	ImageButton cmdWeb=null;
	ImageButton cmdTTS=null;
	ImageButton cmdFX=null;
	ImageButton cmdDest=null;
	
	String sTime="",sDate="";
	String filter=null;

	TextView txtDest=null;
	TextView txtHelp=null;
	TextView lblDelay=null;
	TextView lblMessage=null;
	
	Boolean bRecording=false;	
	ProgressBar waiting=null;
	
	//Utilisation du moteur de synthèse vocal pour générer des messages depuis un texte
	//voir http://mobile.tutsplus.com/tutorials/android/android-sdk-using-the-text-to-speech-engine/
	private TextToSpeech myTTS;
	
	String deviceID="";
	private Context thisContext;
	
	//MessageDirectory contiendra l'ensemble des messages sauvegardés
	//l'application créé le répertoire "repondeur" dans le répertoire "music"
	private String MessageDirectory=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath()+"/repondeur/";
	private String filePath=null;
	
	//Utilisé pour la lecture des messages
	final MediaRecorder recorder = new MediaRecorder();

	//Classe utilisée pour les appels Rest sans perturbation de la fenêtre principale
	//http://developer.android.com/reference/android/os/AsyncTask.html
    private class APICall extends AsyncTask<String, Integer, Long> {	
		
		private String reponse="";
		
		@Override
		protected Long doInBackground(String... params) {		
			String urlServeur=MESSAGE_SERVER;
        	
			//Si le serveur est utilisé en local, l'application peut s'y connecter depuis
			//l'émulateur via l'adresse ci-dessous
			//String urlServeur="http://10.0.2.2:8888";		
			
			if(params.length==2){
				new RestCall(urlServeur+params[0],params[1]){@Override public void onSuccess(String rep) {reponse=rep;};};
			}else{
				new RestCall(urlServeur+params[0],null){@Override public void onSuccess(String rep) {reponse=rep;};};
			}					
			return null;
		}
		
		@Override
		 protected void onPostExecute(Long result) {
			onSuccess(reponse);
		}
		
		public void onSuccess(String rep){};
	}

	private static final int CONTACT_PICKER_RESULT = 1001; 	 
	
	private void Hourglass(String mes){
		if(mes==null){
			waiting.setVisibility(waiting.INVISIBLE);
			lblMessage.setText("");
		}
		else{
			if(!mes.startsWith("#")) //Affichage d'un message sans le sablié
				waiting.setVisibility(waiting.VISIBLE);
			else {
				waiting.setVisibility(waiting.INVISIBLE);
				mes=mes.substring(1);
			}
				
			
			lblMessage.setText(mes);			
		}
	}
	
	public static byte[] getBytesFromFile(File file) throws IOException {
	    InputStream is = new FileInputStream(file);

	    // Get the size of the file
	    long length = file.length();

	    // You cannot create an array using a long type.
	    // It needs to be an int type.
	    // Before converting to an int type, check
	    // to ensure that file is not larger than Integer.MAX_VALUE.
	    if (length > Integer.MAX_VALUE) {
	        // File is too large
	    }

	    // Create the byte array to hold the data
	    byte[] bytes = new byte[(int)length];

	    // Read in the bytes
	    int offset = 0;
	    int numRead = 0,totalRead=0;
	    while (offset < bytes.length
	           && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
	        offset += numRead;
	        totalRead+=numRead;
	    }

	    // Ensure all the bytes have been read in
	    if (offset < bytes.length) {
	        throw new IOException("Could not completely read file "+file.getName());
	    }

	    // Close the input stream and return bytes
	    is.close();
	    return Arrays.copyOfRange(bytes, 0, totalRead-1);
	}
	
	
	
	
	private String encodeFile(String path){
		String contenu=null;			
		try {	
			byte[] bytes =getBytesFromFile(new File(path));
	        contenu=URLEncoder.encode(Base64.encodeToString(bytes,Base64.DEFAULT));
		} catch (FileNotFoundException e1) {e1.printStackTrace();
		} catch (ClientProtocolException e) {e.printStackTrace();
		} catch (IOException e) {e.printStackTrace();}
		
		return(contenu);
	}
	
	
	
	
	protected void playFile(String path){
		  MediaPlayer MPX = new MediaPlayer();
		  try {
			  if(!path.startsWith("http")){
				FileInputStream fileInputStream = new FileInputStream(path);
				MPX.setDataSource(fileInputStream.getFD());
			  } else {
				  MPX.setDataSource(thisContext,Uri.parse(path));
			  }
			
			MPX.prepareAsync();
			Hourglass("Chargement du message ...");
		} catch (IllegalArgumentException e) {e.printStackTrace();
		} catch (IllegalStateException e) {e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		  
		  MPX.setOnPreparedListener(new OnPreparedListener() {
		        public void onPrepared(MediaPlayer mp) {
		        	Hourglass("Lecture en cours");
		        	mp.start();
		        }
		    });
		
		
		MPX.setOnCompletionListener(new OnCompletionListener(){ 
            public void onCompletion(MediaPlayer arg0){
            	Hourglass(null);
            }
		});
	}
	
	
	protected String getDelay(){
		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		Date expiry =null;
		try {expiry = formatter.parse(cmdDate.getText()+" "+cmdTime.getText());} catch (Exception e) {e.printStackTrace();}				
    	Long minutes=(expiry.getTime()-new Date().getTime())/(60*1000);
    	if(minutes>60*24*2)return(minutes/60/24+" jours");
    	if(minutes>60)return(minutes/60+" heures");

    	return(minutes+" minutes");
	}
	
	
	
	private TextToSpeech.OnInitListener ttsInitListener = new TextToSpeech.OnInitListener() {
        public void onInit(int version) {
        }
      };
	
    
      
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        this.thisContext=this.getApplicationContext();
        settings = getSharedPreferences("Plutar", 0);
             
        waiting=(ProgressBar)this.findViewById(R.id.progressBar1);      
        lblMessage=(TextView)this.findViewById(R.id.textView3);
              
        deviceID=Secure.getString(this.getContentResolver(),Secure.ANDROID_ID);
        Hourglass(null);
        
        lblDelay=(TextView)this.findViewById(R.id.lblDelay);
        
        //Gestion du champs des destinataires
        txtDest=(TextView)this.findViewById(R.id.txtDestinataires);
        txtDest.setOnFocusChangeListener(new View.OnFocusChangeListener(){ 
           @Override
           public void onFocusChange(View v, boolean lostfocus){
        	  txtDest.setText(chkDestinataires(txtDest.getText().toString()));
           }
        });
        
        
        //Initialisation du répertoire des messages
        File dir=new File(MessageDirectory);
		if(!dir.isDirectory())dir.mkdir();
        
        cmdWeb=(ImageButton)this.findViewById(R.id.cmdWebSound);
        cmdWeb.setOnClickListener(new View.OnClickListener() {
			  public void onClick(View v) {
				  Hourglass("Récupération des messages prédéfinis");
				  new APICall(){
					  @Override public void onSuccess(String r){
						  Intent m=new Intent(getBaseContext(), listActivity.class);
						  m.putExtra("liste", r);
						  startActivityForResult(m,18);	  
					  }
				  }.execute("/getmp3",null);
			  }
        });
        
        
        cmdSelFile=(ImageButton)this.findViewById(R.id.cmdSel);
        cmdSelFile.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {			
				String listFile="";
				File dir=new File(MessageDirectory);
				for(File f:dir.listFiles())
					listFile+=f.getName()+","+f.getAbsolutePath()+",";
				
				Intent intent = new Intent(getBaseContext(), listActivity.class);
                intent.putExtra("liste", listFile.substring(0, listFile.length()-1));
				startActivityForResult(intent, 10);
			}
			  	
        });
        
        cmdClear=(ImageButton)this.findViewById(R.id.cmdClear);
        cmdClear.setOnClickListener(new View.OnClickListener() {
			  public void onClick(View v) {
				  txtDest.setText("");
			  }
        });
        
        cmdFX=(ImageButton)this.findViewById(R.id.cmdFX);
        cmdFX.setOnClickListener(new View.OnClickListener() {
			  public void onClick(View v) {
				  Intent m=new Intent(getBaseContext(), listActivity.class);
				  m.putExtra("liste", "Canard,Son d'un canard,Robot,Voix métallique");
				  startActivityForResult(m,19);	  
			  }
        });
        
        cmdListen=(ImageButton)this.findViewById(R.id.cmdEcouter);
        cmdListen.setOnClickListener(new View.OnClickListener() {
			  public void onClick(View v) {
				if(filePath!=null){
					  playFile(filePath);									
					  Hourglass("Lecture du message");
				} else
					Hourglass("#Aucun message à écouter");
			  }
        });
               
        myTTS=new TextToSpeech(getBaseContext(), ttsInitListener);
        cmdTTS=(ImageButton)this.findViewById(R.id.cmdTTS);
        cmdTTS.setOnClickListener(new View.OnClickListener() {
			  public void onClick(View v) {
				  myTTS.setLanguage(Locale.FRANCE);	
				  ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
				  
				  String s="J'arrive dans 10 minutes";
				  if(clipboard.getText()!=null)s=clipboard.getText().toString();
					  
				  InputBox ib=new InputBox(PlutarActivity.this,"Text2Speech","Texte à convertir",s)
				  {
					  @Override 
					  public void onOk(String value){
							myTTS.speak(value, 0, null);
							myTTS.synthesizeToFile(value, null, filePath);
							if(value.length()>40)value=value.substring(0, 40);
							filePath=MessageDirectory+value+".wav";
							Hourglass("#Message disponible pour envoi");
					  }

					@Override
					public void onCancel() {}
				  };
			  };
        });
        
       
        txtHelp=(TextView)this.findViewById(R.id.txtHelp);
        txtHelp.setOnClickListener(new View.OnClickListener() {
			  public void onClick(View v) {
				  startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(MESSAGE_SERVER+"/help.html")));
			  }
        });
        
        cmdRecord=(ImageButton)this.findViewById(R.id.cmdRecord);
        cmdRecord.setOnClickListener(new View.OnClickListener() {
			  public void onClick(View v) {
				  String nameFile="message.3gpp";
				  if(!bRecording){
					  filePath=MessageDirectory+nameFile;
					  
					  recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
					  recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
					  recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
					  recorder.setOutputFile(filePath);
					  try {
						recorder.prepare();
					  } catch (IllegalStateException e) {e.printStackTrace();
					  } catch (IOException e) {e.printStackTrace();
					  }
					  recorder.start();
					  bRecording=true;
					  Hourglass("Enregistrement en cours. Stopper avec le même bouton.");
					  
					  cmdRecord.setImageDrawable(getResources().getDrawable(R.drawable.stop));
				  } else {
					recorder.stop();
					bRecording=false;
					Hourglass(null);  
					cmdRecord.setImageDrawable(getResources().getDrawable(R.drawable.rec));
					playFile(filePath);
					
					//enregistrement du fichier
					String s="Message du "+new Date().getDay()+"-"+new Date().getMonth()+" à "+new Date().getHours()+"h"+new Date().getMinutes();
					new InputBox(PlutarActivity.this,"Conserver le message ?","Nom du fichier :",s){

						@Override
						public void onOk(String value) {
							File f=new File(filePath);
							filePath=MessageDirectory+value+".3gpp";
							f.renameTo(new File(filePath));
							Hourglass("#message sauvegardé");
						}

						@Override
						public void onCancel() {}

					};
				  }
			  }
        });
              
        
        
        
        cmdRaz = (ImageButton)this.findViewById(R.id.cmdRaz);  
        cmdRaz.setOnClickListener(new View.OnClickListener() {
			  public void onClick(View v) {
				  Hourglass("Effacement des messages en cours");
				  new APICall(){
					  @Override public void onSuccess(String r){
						Hourglass("#Messages effacés");  
					  }
				  }.execute("/raz?device="+deviceID);
			  }
        });
        
        
        
        Date now=new GregorianCalendar().getTime();
        cmdDate=(Button)this.findViewById(R.id.cmdDate);
        cmdDate.setText(now.getDate()+"/"+(now.getMonth()+1)+"/"+(1900+now.getYear()));
        cmdTime=(Button)this.findViewById(R.id.cmdTime);
        int minutes=now.getMinutes()+5;if(minutes>59)minutes=59;
        cmdTime.setText(now.getHours()+":"+minutes);
        lblDelay.setText("(dans "+getDelay()+")");
           
        
        cmdSend=(Button)this.findViewById(R.id.cmdSend);
        cmdSend.setEnabled(true);
        cmdSend.setOnClickListener(new View.OnClickListener() {
			  public void onClick(View v) {
				  String numbers=txtDest.getText().toString().replace(" 06", ";06").replace(" ","");
				  txtDest.setText(chkDestinataires(numbers));
				  
				  if(txtDest.length()<10){
					  Hourglass("#Aucun destinataire valide");
					  return;
				  }
				  
				  if(filePath==null){
					Hourglass("#Aucun message sélectionné");
					return;
				}
				
				SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
				Date expiry =null;
				try {expiry = formatter.parse(cmdDate.getText()+" "+cmdTime.getText());} catch (Exception e) {e.printStackTrace();}
            	Long dt=expiry.getTime();				
            	
            	String sdests=txtDest.getText().toString();
            	if(sdests.endsWith(";"))sdests=sdests.substring(0,sdests.length()-1);
            	
            	Hourglass("Envoi en cours");
            	
            	String fileformat=filePath.substring(filePath.indexOf(".")+1);
            	String param="&fileformat="+fileformat+"&messageFile="+encodeFile(filePath);
            	if(filePath.startsWith("http"))param="&urlFile="+filePath;
            	            		
            	
            	new APICall(){
            		@Override public void onSuccess(String r){
            			Hourglass("#Message envoyé");
            		}
            	}.execute("/postmessage","device="+deviceID+
            			"&userIdentifier="+sdests+param+"&date="+dt.toString());
                
			  }
        });
        
        cmdDest=(ImageButton)this.findViewById(R.id.cmdExpediteur);
        cmdDest.setOnClickListener(new View.OnClickListener() {
			  public void onClick(View v) {
				Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, 
										ContactsContract.CommonDataKinds.Phone.CONTENT_URI);  
			    startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT); 			
			  }
        });   
    }
    
/**
 * Vérifie que les numéros présents dans "numbers" disposent bien d'une messagerie compatible
 * avec le service de dépot de message
 * @param numbers liste de numéros séparés par un ";"
 * @return Liste des numéros effectivement compatible
 */
	protected String chkDestinataires(String numbers) {
	   final String incompatibles=settings.getString("numeros_incompatibles", "");
	   final String compatibles=settings.getString("numeros_compatibles", "");
	   String toAnalyse="";
	   
  	   for(String number:numbers.split(";")){
  		   if(number.length()>8){
  	  		   //Si un number est dans la liste des numéros exlus on le supprime
  	  		   if((incompatibles+";"+compatibles).indexOf(number)==-1)
  	  			   toAnalyse+=number+";";
  	  		   else
  	  			 if(incompatibles.contains(number)){
  	  				numbers=numbers.replace(number, "");
  	  				Hourglass("#Le "+number+" n'accepte pas le dépôt direct de message");
  	  			 }    			   
  		   }
  	   }
  	   
  	   numbers=numbers.replace(";;", ";");
  	   if(numbers.startsWith(";"))numbers=numbers.substring(1, numbers.length());
  	   if(numbers.endsWith(";"))numbers=numbers.substring(0, numbers.length()-1);
  	   
  	   if(toAnalyse.length()>8){
  	  	   Hourglass("Analyse des numéros compatibles avec le dépot de message");
  	  	   new APICall(){
  	  		   @Override public void onSuccess(String rep){
  	  			  //On récupere la liste des numéros incompatibles
  	  			  //on met donc à jour cette liste sur le mobiles
  	  			   String compatibles="",incompatibles="";
  	  			   if(rep.indexOf("=&")==-1)compatibles=rep.split("&")[0].split("=")[1]; else compatibles="";
  	  			   if(rep.endsWith("="))incompatibles=""; else incompatibles=rep.split("&")[1].split("=")[1];
	  			   
  	  			   Editor edt=settings.edit();
  	  			   edt.putString("numeros_incompatibles", settings.getString("numeros_incompatibles", "")+";"+incompatibles);
  	  			   edt.putString("numeros_compatibles", settings.getString("numeros_compatibles", "")+";"+compatibles);
  	  			   edt.commit();
  	  			   
  	  			   if(incompatibles.length()>8){
  	  				Hourglass("#"+incompatibles+" incompatible(s) avec le dépôt de message");
  	  				txtDest.setText(txtDest.getText().toString().replace(incompatibles,""));
  	  			   }
  	  			   else
  	  				   Hourglass(null);   
  	  		   }
  	  	   }.execute("/checksfr?numbers="+toAnalyse);  		   
  	   }
     
  	   return(numbers);
	}

	public void showTimeDialog(final View v) {
        FragmentManager fm = getSupportFragmentManager();
        TimePickerFragment editNameDialog = new TimePickerFragment() {
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            	cmdTime.setText(hourOfDay+":"+minute);
            	lblDelay.setText("(dans "+getDelay()+")");
            }
        };
        editNameDialog.show(fm, "time_picker");
    }
	

    public void showDateDialog(View v) {
    	FragmentManager fm = getSupportFragmentManager();
        DatePickerFragment editNameDialog = new DatePickerFragment() {
            public void onDateSet(DatePicker view,  int year, int monthOfYear, int dayOfMonth) {
            	cmdDate.setText(dayOfMonth+"/"+(monthOfYear+1)+"/"+year);
            	lblDelay.setText("(dans "+getDelay()+")");
            }
        };    
        editNameDialog.show(fm, "date_picker");
    }
    
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
    	if (resultCode == RESULT_OK) { 
    		
            switch (requestCode) {  
	            case CONTACT_PICKER_RESULT:
	            	String id = data.getData().getLastPathSegment();

	            	Cursor cursor= getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
	            			null,
	            			ContactsContract.CommonDataKinds.Phone._ID +" = ?", new String[] {id}, null);
	            	
	            	if (cursor.moveToFirst()) {
	            		String phone=normalise(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
            			txtDest.setText(chkDestinataires(txtDest.getText()+";"+phone.replace("(", "").replace(")","")+";"));
            			cmdSend.setEnabled(true);
	            	}
	            break;
	            
	            case 10:
	            	this.filePath =data.getStringExtra("result");
	            	if(new File(filePath).length()>100000000){
	            		Toast.makeText(getApplicationContext(), "Fichier trop long", Toast.LENGTH_LONG);
	            		this.filePath =null;
	            	} else 
	            		playFile(filePath);	
	            break;
	            
	            case 18:
	            	this.filePath = data.getStringExtra("result");
	            	playFile(filePath);
	            break;
	            
	            case 19:
	            	this.filter=data.getStringExtra("result");
	            break;
	        }  
        }
    }

	private String normalise(String number) {
		if(number!=null)
			number=number.replace(" ", "").replace(".", "").replace("-", "").replace("0033", "0").replace("+33", "0");
		return number;
	}

    
}