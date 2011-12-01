package org.pentaho.rstats;

import java.util.Iterator;

import org.pentaho.commons.connection.IPentahoMetaData;
import org.pentaho.commons.connection.IPentahoResultSet;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPList;
import org.rosuda.REngine.REXPNull;
import org.rosuda.REngine.REXPReference;
import org.rosuda.REngine.REXPWrapper;
import org.rosuda.REngine.RList;

/*
 * This class helps print REXP data in a way that simulates the R command line interface
 */
@SuppressWarnings("nls")

public class REXPHelper {
  private static int ValuesPerColumn = 25;
  private static int MAX_COLUMNS = 80;

  public static Object asObject(REXP rx) {

    if (rx == null) {
      return (null);
    }

    try {
      if (rx.isVector()) {
        if (rx.isFactor()) {
          return (rx.asFactor());
        } // A Factor is also Numeric & Integer
        if (rx.isNumeric()) {
          return (rx.isInteger() ? rx.asIntegers() : rx.asDoubles());
        }
        if (rx.isString()) {
          return (rx.asString());
        }
        if (rx.isLogical()) {
          return (rx.asIntegers());
        }
        if (rx.isRaw()) {
          return (rx.asBytes());
        }
        if (rx.isList()) {
          return (rx.asList());
        }
      } else {
        if (rx.isNull()) {
          return (null);
        }
        if (rx.isReference()) {
          return (asObject(((REXPReference) rx).resolve()));
        } // Recursive call
        if (rx.isSymbol()) {
          return (rx.asString());
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Don't know about these, just return passed in value
    return (rx);
  }

  public static String asCmdLineOutput(REXP rx) {
    return( attributesAsCmdLineOutput(rx) + "\n" + dataAsCmdLineOutput(rx) );
  }


  public static String attributesAsCmdLineOutput(REXP rx) {
    if (rx == null) { return ("null"); }

    final REXPList a = rx._attr();
    if (a==null || !a.isList()) return( "" );

    String[] keys = a.asList().keys();
    StringBuffer sb = new StringBuffer();
    for ( int i = 0; i < keys.length; ++i ) {

      sb.append( keys[i] ).append(": ").append( asCmdLineOutput( rx.getAttribute(keys[i]) ) ).append('\n' );
    }
    return (sb.toString()); 
  }

  public static String dataAsCmdLineOutput(REXP rx) {

    if (rx == null) { return ("null"); }

    try {
      if (rx.isVector()) {

        // A Factor is also Numeric & Integer
        if (rx.isFactor()) { return (rx.asFactor().toString()); } 

        if (rx.isRaw()) { return ("Raw Data: " + rx.length() + " bytes"); }

        // REXPList, REXPLanguage, REXPExpressionVector, REXPGenericVector        
        if (rx.isList()) {
          StringBuffer sb = new StringBuffer();
          for (Iterator it = rx.asList().iterator(); it.hasNext();) {
            sb.append( asCmdLineOutput( (REXP) it.next() ) ).append('\n' );
          }
          return (sb.toString()); 
        }

        // REXPString
        if (rx.isString()) { return ( asCmdLineOutput( rx.asStrings(), "\"" ) ); }

        // REXPDouble, REXPInteger, REXPLogical
        return( asCmdLineOutput( rx.asStrings() ) );

      } else {

        if (rx.isNull()) { return ("NULL"); }

        // Recursive call
        if (rx.isReference()) { return (asCmdLineOutput(((REXPReference) rx).resolve())); }

        if (rx.isSymbol()) { return (rx.asString()); }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Don't know about these, just return passed in value
    return (rx.toDebugString());
  }

  public static String asCmdLineOutput( String[] o ) {
    return( asCmdLineOutput( o, "" ) );
  }

  public static String asCmdLineOutput( String[] o, String delim ) {
    StringBuilder sb = new StringBuilder();
    int i = 0;
    while ( i < o.length ) {
      sb.append('[').append(i+1).append(']');
      for (int j = 0; j < MAX_COLUMNS && i < o.length; ++i) {
        j += String.valueOf(o[i]).length();
        sb.append(' ').append( delim ).append( o[i] ).append( delim );
      }
      sb.append('\n');
    }
    return( sb.toString() );
  }
  
  public static REXP createDataFrame( Object data ) {
    
    if ( data == null ) {
      return( null );
    }

    if ( data instanceof IPentahoResultSet ) {
      return( dataFrameFromResultSet((IPentahoResultSet)data) );
    }

    throw new IllegalArgumentException();
  }


  public static REXP dataFrameFromResultSet( IPentahoResultSet resultSet ) {
    if ( (resultSet == null) || (resultSet.getColumnCount() == 0) ) {
      return( new REXPNull() );
    }

    RList rL = new RList();
    REXP rCol;

    IPentahoMetaData md = resultSet.getMetaData();
    Object[][] colHeaders = md.getColumnHeaders();

    for (int i = 0; i < resultSet.getColumnCount(); i++) {
      rCol = REXPWrapper.wrap( objectToActualTypeArray( resultSet.getDataColumn(i) ) );
      rL.put(colHeaders[0][i].toString(), (rCol == null) ? new REXPNull() : rCol );
    }

    try {
      return( REXP.createDataFrame(rL) );
    }
    catch ( Exception e ){
      e.printStackTrace();
    }

    return( new REXPNull() );
  }
  
  public static Object objectToActualTypeArray( Object[] objArray ) {
    if ( objArray == null || objArray.length == 0) { return( objArray ); }
    
    Object o = objArray[0];    
    Object cvtObj;
    
    if      ( o instanceof String )  { cvtObj = new String[objArray.length]; }
    else if ( o instanceof Long )    { cvtObj = new Long[objArray.length]; }
    else if ( o instanceof Short )   { cvtObj = new Short[objArray.length]; }
    else if ( o instanceof Integer ) { cvtObj = new Integer[objArray.length]; }
    else if ( o instanceof Float )   { cvtObj = new Float[objArray.length]; }
    else if ( o instanceof Double )  { cvtObj = new Double[objArray.length]; }
    else if ( o instanceof Boolean ) { cvtObj = new Boolean[objArray.length]; }
    else if ( o instanceof Byte )    { cvtObj = new Byte[objArray.length]; }
    else { return( objArray ); }
    
    System.arraycopy(objArray, 0, cvtObj, 0, objArray.length);    
    return( cvtObj );
  }
}