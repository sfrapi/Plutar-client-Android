package demo.depot.message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import android.content.Context;
import android.content.Intent;

/**
 * Classe utilisée pour les appelles aux web services du serveur en charge de la gestion des
 * messages programmés
 * 
 * @author Hervé Hoareau
 *
 */
public class RestCall extends DefaultHttpClient {
	
	Context context;
	Intent intentResp;
	
	/**
	 * Requête POST
	 * @param urlServer Adresse du serveur
	 * @param parametres Paramétres de la requête
	 * @return Le retour est traité dans les méthodes onSuccess et onFailure
	 * 			surchargée par l'appellant
	 * 
	 * @author Hervé Hoareau
	 */
	private String ExecuteRest(String urlServer,String parametres){
				
		HttpResponse response=null;
		HttpPost post=new HttpPost(urlServer);			
		
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		
		for(String param:parametres.split("&")){
			String contenu=param.split("=")[1];	
			nameValuePairs.add(new BasicNameValuePair(param.split("=")[0], contenu));
		}

        try {
        	post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			response = execute(post);
			if(response!=null){
				int reponseCode=response.getStatusLine().getStatusCode();
				if(reponseCode==200){			
					InputStream instream = response.getEntity().getContent();
					return(stream2String(instream));				
				} else 
					onFailure(reponseCode);
			} 
        } catch (ClientProtocolException e1) {e1.printStackTrace();
        } catch (IOException e1) {e1.printStackTrace();}

		
		return(null);
	}
	
	/**
	 * Requête GET
	 * @param urlServer Adresse du serveur
	 * @return Le retour est traité dans les méthodes onSuccess et onFailure
	 * 			surchargée par l'appellant
	 * 
	 * @author Hervé Hoareau
	 */
	private String ExecuteRest(String url){
		HttpGet request=new HttpGet(url);
		HttpResponse response=null; 
		try {
			response = execute(request);
			} 
		catch (ClientProtocolException e) {e.printStackTrace();} 
		catch (IOException e) {e.printStackTrace();}
		
		if(response!=null){
			int reponseCode=response.getStatusLine().getStatusCode();
			if(reponseCode==200){			
				try {
					InputStream instream = response.getEntity().getContent();
					return(stream2String(instream));
				} catch (UnsupportedEncodingException e) {e.printStackTrace();
				} catch (IllegalStateException e) {e.printStackTrace();
				} catch (IOException e) {e.printStackTrace();}
			} else onFailure(reponseCode);
		}	
		return(null);
	}
	
	public RestCall(String url,String parametres) {
		String rep=null;
		
		if(parametres==null)
			rep=this.ExecuteRest(url);
		else
			rep=this.ExecuteRest(url,parametres);	
		
		if(rep!=null)onSuccess(rep);
	}
	
	/**
	 * Conversion d'un inputStream en String
	 * @param stream
	 * @return 
	 * @throws UnsupportedEncodingException
	 */
	private String stream2String(InputStream stream) throws UnsupportedEncodingException {
		String charSet = "UTF-8";
		InputStreamReader reader = new InputStreamReader(stream, charSet);
 
		BufferedReader buffer = new BufferedReader(reader);
		StringBuilder sb = new StringBuilder();
		try {
			String cur;
			while ((cur = buffer.readLine()) != null) {
				sb.append(cur);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}
	
	/**
	 * Méthode à redéfinir par l'appellant pour récupérer en asynchrone
	 * le résultat de la requête http
	 * @param rep
	 */
	public void onSuccess(String rep) {};
	public void onFailure(int reponseCode) {};
}
