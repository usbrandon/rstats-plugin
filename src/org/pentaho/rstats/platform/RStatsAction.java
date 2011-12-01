package org.pentaho.rstats.platform;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.pentaho.commons.connection.IPentahoResultSet;
import org.pentaho.platform.api.engine.IActionSequenceResource;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.rstats.REXPHelper;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPWrapper;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;

import com.sun.org.apache.bcel.internal.generic.NEW;

/**
 *  
 * @author DMoran
 *
 */
@SuppressWarnings("nls")
public class RStatsAction  {

  Map<String, Object> varInputs;
  HashMap<String,Object> theOutputs = new HashMap<String,Object>();
  OutputStream responseStream = new ByteArrayOutputStream();
  
  String responseMimeType = "text/plain";

  RConnection c = null;
  REXP xp = null;
  String errorMsg = null;
  int cmd = 0;

  /******** Special POJO component methods ************/
  
  public void configure(Map<String, String>config) {
    System.out.println( "configure" );  	
  }
  
  public Set<String> getConfigSettingsPaths() {
    System.out.println( "configure" );  	
  	return( new HashSet<String>() );
  }
  
  /**
   * This method will determine if the component instance 'is valid.' The validate() is called after all of the bean 'setters' have been called, so we may
   * validate on the actual values, not just the presence of inputs as we were historically accustomed to.
   * <p/>
   * Since we should have a list of all action-sequence inputs, we can determine if we have sufficient inputs to meet the parameter requirements of the
   * report-definition. This would include validation of values and ranges of values.
   *
   * @return true if valid
   * @throws Exception 
   */
  public boolean validate() throws Exception {
    System.out.println( "validate" );
    doPreExecution();
  	return( true );
  }

  /**
   * Perform the primary function of this component, this is, to execute. This method will be invoked immediately following a successful validate().
   *
   * @return true if successful execution
   * @throws Exception 
   */
  public boolean execute() throws Exception {
    System.out.println( "execute" );
    doExecute();
  	return( true );
  }
  
  public void done() {
    System.out.println( "done" );
  }
  
  public Map<String,Object> getOutputs() {
    System.out.println( "getOutputs" );
    return( theOutputs );
  }
  
  public void setResources(Map<String, IActionSequenceResource>resources) {
    System.out.println( "setResources" );  	
  }

  public void setInputs(Map<String,Object>inputs) {
    System.out.println( "setInputs" );  	  	
    varInputs = inputs;
  }
  
  public void setLogger(Log logger) {
    System.out.println( "setLogger" );  	  	  
  }
  
  public void setSession(IPentahoSession session ) {
    System.out.println( "setSession" );  	  	  
  }
  
  public void setOutputStream(OutputStream outputStream) {
    System.out.println( "setOutputStream" );  	  	  
  	this.responseStream = outputStream;
  }
  
  public OutputStream getOutputStream() {
    System.out.println( "getOutputStream" );  	  	  
  	return( this.responseStream );
  }
  
  public String getMimeType() {
    System.out.println( "getMimeType" );
    return responseMimeType;
  }
    
  /******** Special IAction methods ************/

  public void setVarArgs(Map<String, Object> args) {
    System.out.println( "setVarArgs" );
    varInputs = args;
  }

  public void setResponseStream(OutputStream responseStream) {
    this.responseStream = responseStream;
  }

  /******** Plugin property getters and setters ************/
  
  public void setMimeType(String responseMimeType) {
    System.out.println( "setMimeType: "+responseMimeType );
    this.responseMimeType = responseMimeType;
  }
  
  /******** Implementation methods ************/
  
  public void doPreExecution() throws Exception /*throws ActionPreProcessingException*/ {
    System.out.println( "PreExecute" );
    try {
      c = new RConnection();
      System.out.println(">>Connectng to R " + c.eval("R.version$version.string").asString() + "<<");
    } 
    catch (Exception e) {
      /*throw new ActionPreProcessingException(e)*/;
      throw e;
    }
  }
  
  public String getErrorMessage() {
    return( errorMsg );
  }
  
  public void doExecute() throws Exception {
    
    if ( varInputs != null ) {
      Object value;
      for (String name : varInputs.keySet()) {
        if ( "mimeType".equalsIgnoreCase(name)) {
          continue;
        }
      
        value = varInputs.get(name);
        System.out.println( "IN >> " + name + " - " + value );

        if ( "rEvalExpression".equalsIgnoreCase( name ) ) {
          doEvalExpressionStr(value.toString());
        }
        else if ( value instanceof IPentahoResultSet ) {
          c.assign(name, REXPHelper.createDataFrame( value ));
        }
        else {
          c.assign( name, REXPWrapper.wrap(value) );
        }      
      }
    }
  }
    
  void doEvalExpressionStr( String rEvalExpressionStr ) throws Exception {

    if (rEvalExpressionStr == null) {
      responseMimeType = "text/plain";
      errorMsg = ">> " + c.eval("R.version$version.string").asString() + " <<\n"
          + "No Expression to evaluate was specified.";
      System.out.println(errorMsg);
      return;
    }

    try {
        if( rEvalExpressionStr.startsWith("#PTHO DEBUG") ) {
          cmd = 1;
          xp = c.parseAndEval(rEvalExpressionStr.toString());
        }
        else if (rEvalExpressionStr.startsWith("#PTHO CONSOLE") ) {
          cmd = 2;
          xp = c.parseAndEval( "paste(capture.output(print(" + 
                               rEvalExpressionStr.toString() +
                               ")),collapse=\"\\n\")" );
        }
        else if (rEvalExpressionStr.startsWith("#PTHO MYCON") ) {
          cmd = 3;
          xp = c.parseAndEval(rEvalExpressionStr.toString());
        }
        else {
          xp = c.parseAndEval(rEvalExpressionStr.toString());
        }
    } catch (REngineException e) {
      responseMimeType = "text/plain";
      errorMsg = "R Engine Exception." + e.getLocalizedMessage();
      System.out.println(errorMsg);
      return;
    }

    if (xp == null) {
      responseMimeType = "text/plain";
      errorMsg = "Parse and Evaluate returned null.";
      System.out.println(errorMsg);
      return;
    }

    if ( responseStream == null) {
      return;
    }

    if (xp.inherits("try-error")) { // if the result is of the class try-error then there was a problem
      responseMimeType = "text/plain";
      errorMsg = "R Exception:\n" + xp.asString();
      System.err.println("R Exception:\n" + xp.asString());
      return;
    }

    if (xp.isRaw()&&(cmd==0)) {
      responseMimeType = "image/jpeg";
    } else {
      responseMimeType = "text/plain";
    }

    System.out.println( "Execute" );

    if (errorMsg != null) {
      responseStream.write(errorMsg.getBytes());
      return;
    }
    
    if ( xp == null ) {
      return;
    }
    if (cmd == 1) { // DEBUG
      responseStream.write(xp.toDebugString().getBytes());
      System.out.println("______\n" + xp.toDebugString() + "\n______");
    }
    else if (cmd == 2) { // CONSOLE
      responseStream.write(xp.asString().getBytes());
      System.out.println("______\n" + xp.asString() + "\n______");
    }
    else if (cmd == 3) { // MYCON
      responseStream.write(REXPHelper.asCmdLineOutput(xp).getBytes());
      System.out.println("______\n" + REXPHelper.asCmdLineOutput(xp) + "\n______");
    }
    else {
      if (xp.isRaw()) {
        responseStream.write(xp.asBytes());
      } else {
        responseStream.write(REXPHelper.asCmdLineOutput(xp).getBytes());
      }
      System.out.println("______\n" + REXPHelper.asCmdLineOutput(xp) + "\n______");
    }
  }

  
  public static String inputStreamToString( InputStream inStream ) {
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inStream));
    StringBuffer sb = new StringBuffer();
    char[] buffer = new char[4 * 1024];
    int charsRead;
    try { 
      while ((charsRead = bufferedReader.read(buffer)) != -1) {
        sb.append(buffer, 0, charsRead);
      }
    }
    catch ( Exception e ) {
      e.printStackTrace();
    }
    return( sb.toString() );   
  }
  
  public Object getVarFromRserv( String name ) {
    Object res = REXPHelper.asObject( getREXPFromRserv( name ) );
    System.out.println(">>FROM RSERV:" + String.valueOf(res) );
    return( res );
  }
  
  public REXP getREXPFromRserv( String name ) {
    REXP res = null;
    
    if ( c != null ) {
      try {
        res = c.get(name, null, true);
      } catch (REngineException e) {
        errorMsg = name + ": " + e.getMessage();
      }
    }
    
    return( res );
  }

}