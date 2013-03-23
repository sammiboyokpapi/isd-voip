
package isdtechnology;

import net.rim.device.api.system.Bitmap;

import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.EditField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.RichTextField;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.component.TextField;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.container.VerticalFieldManager;
import net.rim.device.api.ui.decor.Background;
import net.rim.device.api.ui.decor.BackgroundFactory;
import net.rim.device.api.ui.decor.Border;
import net.rim.device.api.ui.decor.BorderFactory;

//import isdtechnology.MyScreen;

//import isdtechnology.ServiceRequestThread;

import java.io.IOException;
import java.lang.Object;


/**
 * A class extending the MainScreen class, which provides default standard
 * behavior for BlackBerry GUI applications.
 */
public final class MyScreen extends MainScreen implements FieldChangeListener 
{
    /**
     * Creates a new MyScreen object
     */
	
private VerticalFieldManager _manager;	
		
protected RichTextField _Output = new RichTextField();
	
	
	EditField autoCompleteField = new EditField(EditField.FILTER_EMAIL);
	PasswordEditField autoCompleteFields = new PasswordEditField();
	
	public MyScreen()
    {        
        // Set the displayed title of the screen       
        // Initialize and configure title bar
		
		
	//menu border	
	
		_manager = (VerticalFieldManager)getMainManager();
		
		Background bg = BackgroundFactory.createSolidBackground(Color.STEELBLUE);
		_manager.setBackground(bg);
		XYEdges edges =new XYEdges(10, 10, 10, 10);
		Border border = BorderFactory.createBevelBorder(edges);
		_manager.setBorder(border);
		
		
		setTitle("ISD VOIP APLICATION");
		

    	
    	LabelField usernamew = new LabelField("");
    	LabelField usernames = new LabelField("ISD VOIP user login");
    	add(new SeparatorField());
    	LabelField usernameww = new LabelField("");
    	LabelField username = new LabelField("Email:");
    	add(usernamew);
    	add(usernames);
     	add(new SeparatorField());
    	add(new SeparatorField());
    	add(usernameww);
    	add(username);
        
     
        _Output.setLabel("");
        
        autofield();
        
        convert();
        
        add(new SeparatorField());
    	
        _Output.setMaxSize(4000);
    	//Background backgrounds = BackgroundFactory.createSolidTransparentBackground(Color.WHITE, 90);
        //_Output.setBackground(backgrounds);
    	add(_Output);
    

    }


	
	
	


   public void autofield(){
	
	   autoCompleteField.setBorder(BorderFactory.createRoundedBorder(new XYEdges(2, 2, 2, 2), 0x35586c, Border.STYLE_SOLID));
	   Background backgrounds = BackgroundFactory.createSolidTransparentBackground(Color.WHITE, 90);
	   autoCompleteField.setBackground(backgrounds);
	   add(autoCompleteField);
	
	   LabelField password = new LabelField("PASSWORD:");
       add(password);
   
       
       autoCompleteFields.setBorder(BorderFactory.createRoundedBorder(new XYEdges(2, 2, 2, 2), 0x35586c, Border.STYLE_SOLID));
       autoCompleteFields.setBackground(backgrounds);
       add(autoCompleteFields);
	
	   add(new SeparatorField());
   
   }

   private void convert(){

		 ButtonField button2 = new ButtonField("LOGIN",
		 ButtonField.CONSUME_CLICK | Field.FIELD_HCENTER);
		 button2.setChangeListener(this);
		 add(button2);
		 // A separator field between each type of control
		 add(new SeparatorField());
		
	}	
		


public void fieldChanged(Field field, int context) {
	// TODO Auto-generated method stub
	
 //UiApplication.getUiApplication().pushScreen(new dialogsam());

	   String text = autoCompleteField.toString();
	   String texts = autoCompleteFields.toString();	
	   		
	   //link to webservice		
	   String URL ="http://127.0.0.1/isdvoip/webservices/userauthenticate.php?method=methodname&username="+text+"&password="+texts+";deviceside=true";
	   		ServiceRequestThread svc = new ServiceRequestThread(URL,(MyScreen)UiApplication.getUiApplication().getActiveScreen());
	   		svc.start();

}

public void updateDestination(final String text) {
	UiApplication.getUiApplication().invokeLater(new Runnable() {
		public void run()
		{
			int sami = text.indexOf("<?xml");
			String value = text.substring(0, sami);
			String samm = "true";
			_Output.setText(value);
			if(value.equals(samm)){
			UiApplication.getUiApplication().pushScreen(new dialogsam());	
			}
			else{
						
			}
			}
	});
}
/*
protected boolean onSavePrompt()
{
    // Suppress the save dialog
    return true;
}  
*/
public void close()
{
    // Display a farewell message before closing the application
    Dialog.alert("Goodbye!");     
    super.close();
}  
}
