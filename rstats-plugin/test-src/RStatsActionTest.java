import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IPlatformPlugin;
import org.pentaho.platform.api.engine.IPluginManager;
import org.pentaho.platform.api.engine.IPluginProvider;
import org.pentaho.platform.api.engine.IServiceManager;
import org.pentaho.platform.api.engine.ISolutionEngine;
import org.pentaho.platform.api.engine.PlatformPluginRegistrationException;
import org.pentaho.platform.api.engine.PluginBeanDefinition;
import org.pentaho.platform.api.engine.IPentahoDefinableObjectFactory.Scope;
import org.pentaho.platform.api.repository.ISolutionRepository;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.platform.engine.core.system.boot.PlatformInitializationException;
import org.pentaho.platform.engine.services.solution.SolutionEngine;
import org.pentaho.platform.engine.services.solution.SolutionHelper;
import org.pentaho.platform.plugin.services.connections.sql.SQLConnection;
import org.pentaho.platform.plugin.services.pluginmgr.DefaultPluginManager;
import org.pentaho.platform.plugin.services.pluginmgr.PlatformPlugin;
import org.pentaho.platform.plugin.services.pluginmgr.PluginAdapter;
import org.pentaho.platform.plugin.services.pluginmgr.servicemgr.DefaultServiceManager;
import org.pentaho.platform.repository.solution.filebased.FileBasedSolutionRepository;
import org.pentaho.test.platform.engine.core.MicroPlatform;

public class RStatsActionTest {
  
  private MicroPlatform booter;

  StandaloneSession session;

  @SuppressWarnings("nls")
  @Before
  public void init() {
    booter = new MicroPlatform("solutions");
    booter.define(ISolutionEngine.class, SolutionEngine.class, Scope.GLOBAL);
    booter.define(ISolutionRepository.class, FileBasedSolutionRepository.class, Scope.GLOBAL);
    booter.define(IServiceManager.class, DefaultServiceManager.class, Scope.GLOBAL);
    booter.define(IPluginManager.class, DefaultPluginManager.class, Scope.GLOBAL);
    booter.define("systemStartupSession", StandaloneSession.class, Scope.GLOBAL);
    booter.define( "connection-SQL", SQLConnection.class, Scope.LOCAL );
    
    session = new StandaloneSession();
  }
  
  @SuppressWarnings("nls")
  @Test
  public void testRStatsActionExecute() throws PlatformInitializationException, FileNotFoundException {
    
    try {
      booter.define(IPluginProvider.class, TestPluginProvider.class);
      booter.addLifecycleListener(new PluginAdapter());
      booter.start();
      
      OutputStream outputStream = new FileOutputStream(new File("testRStatsActionExecute.html"));
 //     SolutionHelper.execute("testing RStatsAction", "devuser", "bi-developers/RStatsPlugin/SimpleRStats.xaction", new HashMap(), outputStream);
      SolutionHelper.execute("testing RStatsAction", "devuser", "bi-developers/RStatsPlugin/RStatsData.xaction", new HashMap(), outputStream);
      
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public static class TestPluginProvider implements IPluginProvider {
    @SuppressWarnings("nls")
    public List<IPlatformPlugin> getPlugins(IPentahoSession session) throws PlatformPluginRegistrationException {
      PlatformPlugin p = new PlatformPlugin();
      p.setId("RStatsAction");
      p.addBean(new PluginBeanDefinition("RStatsAction", "org.pentaho.rstats.platform.RStatsAction"));
      return Arrays.asList((IPlatformPlugin) p);
    }
  }

}
