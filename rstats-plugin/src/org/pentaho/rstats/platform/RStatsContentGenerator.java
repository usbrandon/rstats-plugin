package org.pentaho.rstats.platform;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.ISolutionFile;
import org.pentaho.platform.api.repository.ISolutionRepository;
import org.pentaho.platform.engine.core.solution.ActionInfo;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.services.solution.SimpleContentGenerator;
import org.pentaho.platform.util.UUIDUtil;
import org.pentaho.platform.util.web.MimeHelper;

public class RStatsContentGenerator extends SimpleContentGenerator {

  private static final long serialVersionUID = 724434656509054486L;

  private RStatsAction rstatsAction;

  @Override
  public void createContent(OutputStream out) throws Exception {
    final String id = UUIDUtil.getUUIDAsString();
    setInstanceId(id);
    final IParameterProvider requestParams = parameterProviders.get(IParameterProvider.SCOPE_REQUEST);

    final String solution = URLDecoder.decode(requestParams.getStringParameter("solution", ""), "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    final String path = URLDecoder.decode(requestParams.getStringParameter("path", ""), "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    final String name = URLDecoder.decode(requestParams.getStringParameter("name", requestParams.getStringParameter("action", "")), "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    final String rExpressionPath = ActionInfo.buildSolutionPath(solution, path, name);

    try {
      final Map<String, Object> inputs = createInputs(requestParams);
      createReportContent(out, rExpressionPath, inputs);
    } catch (Exception ex) {
      final String exceptionMessage = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName();
      getLogger().error(exceptionMessage, ex);

      if (out != null) {
        out.write(exceptionMessage.getBytes("UTF-8")); //$NON-NLS-1$
        out.flush();
      } else {
        throw new IllegalArgumentException();
      }
    } finally {
      rstatsAction = null;
    }
  }

  private Map<String, Object> createInputs(final IParameterProvider requestParams) {
    final Map<String, Object> inputs = new HashMap<String, Object>();
    final Iterator paramIter = requestParams.getParameterNames();
    while (paramIter.hasNext()) {
      final String paramName = (String) paramIter.next();
      final Object paramValue = requestParams.getParameter(paramName);
      inputs.put(paramName, paramValue);
    }
    return inputs;
  }

  @SuppressWarnings("nls")
  private void createReportContent(final OutputStream outputStream, final String rExpressionPath,
      final Map<String, Object> inputs) throws Exception {
    final ByteArrayOutputStream reportOutput = new ByteArrayOutputStream();
    // produce rendered report
    if (rstatsAction == null) {
      rstatsAction = new RStatsAction();
    }
    rstatsAction.setResponseStream(reportOutput);

    final ISolutionRepository repository = PentahoSystem.get(ISolutionRepository.class, userSession);
    final ISolutionFile file = repository.getSolutionFile(rExpressionPath, ISolutionRepository.ACTION_EXECUTE);

    //rstatsAction.setrEvalExpression(new ByteArrayInputStream(file.getData()));
    rstatsAction.execute();
    final String mimeType = rstatsAction.getMimeType();

    final byte[] bytes = reportOutput.toByteArray();
    if (parameterProviders.get("path") != null && 
        parameterProviders.get("path").getParameter("httpresponse") != null){ //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      final HttpServletResponse response = (HttpServletResponse) parameterProviders
          .get("path").getParameter("httpresponse"); //$NON-NLS-1$ //$NON-NLS-2$
      
      final String extension = MimeHelper.getExtension(mimeType);
      String filename = file.getFileName();
      if (filename.lastIndexOf(".") != -1) { //$NON-NLS-1$
        filename = filename.substring(0, filename.lastIndexOf(".")); //$NON-NLS-1$
      }
      
      response.setHeader("Content-Disposition", "inline; filename=\"" + filename + extension + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      response.setHeader("Content-Description", file.getFileName()); //$NON-NLS-1$
      response.setHeader("Pragma", "no-cache");
      response.setHeader("Cache-Control", "no-cache");
      response.setHeader("Content-Size", String.valueOf(bytes.length));

      outputStream.write(bytes);
      outputStream.flush();
    }
  }

  @SuppressWarnings("nls")
  @Override
  public String getMimeType() {
    return "image/jpeg";
  }

  @Override
  public Log getLogger() {
    return LogFactory.getLog(RStatsContentGenerator.class);
  }

}
