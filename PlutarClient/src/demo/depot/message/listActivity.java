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

import java.util.ArrayList;
import java.util.HashMap;
import android.os.Bundle;
import android.app.ListActivity;
import android.content.Intent;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

/**
 * Form utilisée pour afficher une liste de choix par l'activité principale.
 * La liste est fournis sous la forme d'une String chaque item étant sous la forme
 * de couple (titre,description)
 * 
 * elle est appelée via StartActivityForResult. elle retourne le champs description sélectionné
 * @see http://developer.android.com/training/basics/intents/result.html
 * 
 * @author Hervé Hoareau
 * 
 */
public class listActivity extends ListActivity  {
  
	ArrayList<HashMap<String, String>> mylistData =new ArrayList<HashMap<String, String>>();

  @Override 
  public void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  setContentView(R.layout.list_main);
	  
	  String[] columnTags = new String[] {"titre", "url"};
	  int[] columnIds = new int[] {R.id.column1, R.id.column2};
	  
	  String[] listeStrings = getIntent().getStringExtra("liste").replace("\"", "").replace("[","").replace("]", "").split(",");
	   
	  for(int i=0;i<listeStrings.length-1;i+=2){
		  HashMap<String, String> maplist = new HashMap<String, String>();
		  maplist.put("titre",listeStrings[i]);
		  maplist.put("url",listeStrings[i+1]);
		  mylistData.add(maplist);
	  }
	  
	  SimpleAdapter arrayAdapter =new SimpleAdapter(getBaseContext(),mylistData,R.layout.list_row,columnTags,columnIds);
	  
	  setListAdapter(arrayAdapter);
	 }
  

  @Override 
  public void onListItemClick(ListView l, View v, int position, long id) {
	  Intent i=new Intent(this.getApplicationContext(),listActivity.class);
	  i.putExtra("result", mylistData.get(position).get("url"));
      setResult(RESULT_OK, i);
      finish();
  }

  
}
