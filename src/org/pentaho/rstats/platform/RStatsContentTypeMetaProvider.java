package org.pentaho.rstats.platform;

import java.io.InputStream;

import org.dom4j.Document;
import org.pentaho.platform.api.engine.IActionSequence;
import org.pentaho.platform.api.engine.IFileInfo;
import org.pentaho.platform.api.engine.ISolutionFile;
import org.pentaho.platform.api.engine.SolutionFileMetaAdapter;
import org.pentaho.platform.engine.core.solution.FileInfo;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.services.actionsequence.SequenceDefinition;
import org.pentaho.platform.util.logging.Logger;
import org.pentaho.platform.util.xml.dom4j.XmlDom4JHelper;

public class RStatsContentTypeMetaProvider extends SolutionFileMetaAdapter {

  @SuppressWarnings("nls")
  public IFileInfo getFileInfo(ISolutionFile solutionFile, InputStream in) {
    System.out.println( solutionFile.getFileName() );
    try {
      Document actionSequenceDocument = XmlDom4JHelper.getDocFromStream(in);
//      if (actionSequenceDocument == null) {
//        return null;
//     }

      String filename = solutionFile.getFileName();
      String path = solutionFile.getSolutionPath();
      String solution = solutionFile.getSolution();

      //FIXME: we are getting a NPE here.  The fallback file info is being returned by the plugin manager
      IActionSequence actionSequence = SequenceDefinition.ActionSequenceFactory(actionSequenceDocument, filename, path,
          solution, logger, PentahoSystem.getApplicationContext(), Logger.getLogLevel());
      if (actionSequence == null) {
        Logger.error(getClass().toString(), "failed to get meta information on action sequence "+solutionFile.getFullPath());
        return null;
      }

      IFileInfo info = new FileInfo();
      info.setAuthor(actionSequence.getAuthor());
      info.setDescription(actionSequence.getDescription());
      info.setDisplayType(actionSequence.getResultType());
      info.setIcon(actionSequence.getIcon());
      info.setTitle(actionSequence.getTitle());
      return info;
    } catch (Exception e) {
      if (logger != null) {
        logger.error(getClass().toString(), e);
      }
      return null;
    }
  }

}
