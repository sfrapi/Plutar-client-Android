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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

/**
 * Cette classe d'afficher une zone de saisie sous forme de boite de dialogue
 * pour la fenêtre principale de l'application
 * 
 * L'implémentation nécessite la définition des méthode onOk et onCancel pour traitement
 * du résultat par l'appellant.
 * 
 * @author Hervé Hoareau (SFR)
 */
public abstract class InputBox {

	String title;
	String texte;
	String defaut;
	Context ctx;
	
	 EditText input=null;
	 Spinner lstInput=null;
	
	public InputBox(Context ctx,String title,String texte,String defaut){
		this.ctx=ctx;
		this.title=title;
   	 	this.texte=texte;
   	 	this.defaut=defaut;
   	 	Create();
    }
	
	
	private void Create(){
    	
    	AlertDialog.Builder alert = new AlertDialog.Builder(ctx);

		  alert.setTitle(title);
		  alert.setMessage(texte);

		  if(!defaut.contains(";")){
			  input = new EditText(ctx);
			  input.setText(defaut);
			  alert.setView(input);
		  }else{
			  lstInput = new Spinner(ctx);
			  ArrayAdapter<String> adapter=new ArrayAdapter<String>(ctx, lstInput.getId(), defaut.split(";"));
			  adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			  lstInput.setAdapter(adapter);
			  alert.setView(lstInput);
		  }
			  
		  
		  alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			  public void onClick(DialogInterface dialog, int whichButton){
				  if(input==null)
					  onOk(lstInput.getSelectedItem().toString());
				  else
					  onOk(input.getText().toString());
			  }});
		  
		  alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			  public void onClick(DialogInterface dialog, int whichButton){
				  onCancel();
			  }});
		  
		  alert.show();
		  
    }
    
    abstract public void onOk(String value);  
    abstract public void onCancel();   
}
